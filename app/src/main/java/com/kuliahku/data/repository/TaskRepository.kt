package com.kuliahku.data.repository

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.kuliahku.data.model.Task
import com.kuliahku.data.model.TaskStatus
import com.kuliahku.notification.ReminderScheduler
import com.kuliahku.util.Constants
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val s3Client: AmazonS3,
    private val scheduler: ReminderScheduler,
    @ApplicationContext private val context: Context
) {
    private fun tasksCollection(uid: String) =
        firestore.collection(Constants.COLLECTION_USERS).document(uid)
            .collection(Constants.SUBCOLLECTION_TASKS)

    /** Semua tugas user tanpa filter, real-time. */
    fun observeTasksRaw(): Flow<List<Task>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }

        // Cleanup otomatis: Hapus tugas yang sudah lewat deadline (misal > 7 hari yang lalu)
        // atau hapus tugas yang sudah SELESAI jika sudah lewat harinya agar database bersih.
        cleanupOldTasks(uid)

        val listener = tasksCollection(uid)
            .orderBy("deadline", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { 
                    android.util.Log.e("TaskRepository", "Error observing tasks: ${error.message}")
                    trySend(emptyList()) 
                    return@addSnapshotListener 
                }
                val items = snapshot?.documents?.mapNotNull { it.toObject(Task::class.java) } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    private fun cleanupOldTasks(uid: String) {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000)

        tasksCollection(uid).get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                val task = doc.toObject(Task::class.java) ?: continue
                val deadline = task.deadline?.toDate()?.time ?: 0L
                
                // Aturan Hapus Otomatis:
                // 1. Jika tugas sudah DONE dan deadline sudah lewat 1 hari
                // 2. Jika tugas LATE (belum dikerjakan) dan deadline sudah lewat 7 hari (opsional, agar tidak langsung hilang)
                if (task.statusEnum == TaskStatus.DONE && deadline < oneDayAgo) {
                    doc.reference.delete()
                }
            }
        }
    }

    /** Semua tugas user untuk ditampilkan di UI (dengan filter). */
    fun observeAllTasks(): Flow<List<Task>> = observeTasksRaw().map { items ->
        val (todayStart, _) = todayRange()
        items.filter { task ->
            val deadlineTime = task.deadline?.toDate()?.time ?: 0L
            val isDone = task.statusEnum == TaskStatus.DONE
            val submittedTime = task.submittedAt?.toDate()?.time ?: 0L

            // Tampilkan jika:
            // 1. Belum selesai (TODO atau LATE)
            // 2. Selesai tapi deadline-nya hari ini atau nanti
            // 3. Selesai hari ini (agar progres card & daftar tugas tidak langsung hilang)
            !isDone || deadlineTime >= todayStart || submittedTime >= todayStart
        }
    }

    /** Tugas dengan deadline hari ini. */
    fun observeTodayTasks(): Flow<List<Task>> = observeAllTasks().map { list ->
        val (start, end) = todayRange()
        list.filter { task ->
            val d = task.deadline?.toDate() ?: return@filter false
            d.time in start..end
        }
    }

    /** Tugas akan datang (belum selesai, deadline > hari ini). */
    fun observeUpcomingTasks(): Flow<List<Task>> =
        observeAllTasks().map { list ->
            val (_, end) = todayRange()
            list.filter { it.statusEnum == TaskStatus.TODO && (it.deadline?.toDate()?.time ?: 0L) > end }
        }

    /** Tugas terlambat (belum selesai, deadline < sekarang). */
    fun observeOverdueTasks(): Flow<List<Task>> =
        observeAllTasks().map { list ->
            val now = System.currentTimeMillis()
            list.filter { it.statusEnum == TaskStatus.TODO && (it.deadline?.toDate()?.time ?: Long.MAX_VALUE) < now }
        }

    fun observeTask(taskId: String): Flow<Task?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) { trySend(null); close(); return@callbackFlow }
        val listener = tasksCollection(uid).document(taskId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    android.util.Log.e("TaskRepository", "Error observing task $taskId: ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snap?.toObject(Task::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun addTask(task: Task): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        val ref = tasksCollection(uid).document()
        val data = hashMapOf<String, Any?>(
            "title" to task.title,
            "description" to task.description,
            "deadline" to task.deadline,
            "priority" to task.priority,
            "status" to TaskStatus.TODO.value,
            "progress" to 0,
            "color" to task.color,
            "reminderEnabled" to task.reminderEnabled,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val saved = task.copy(id = ref.id)
        scheduler.scheduleTaskReminders(saved)
        ref.set(data).await()
        ref.id
    }

    suspend fun updateTask(taskId: String, updates: Map<String, Any?>): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        val map = updates.toMutableMap()
        map["updatedAt"] = FieldValue.serverTimestamp()
        tasksCollection(uid).document(taskId).set(map, SetOptions.merge()).await()
        val snap = tasksCollection(uid).document(taskId).get().await()
        snap.toObject(Task::class.java)?.let { scheduler.scheduleTaskReminders(it) }
        Unit

    }

    suspend fun setTaskStatus(taskId: String, done: Boolean): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        tasksCollection(uid).document(taskId).set(
            mapOf(
                "status" to if (done) TaskStatus.DONE.value else TaskStatus.TODO.value,
                "progress" to if (done) 100 else 0,
                "submittedAt" to if (done) FieldValue.serverTimestamp() else null,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
        if (done) scheduler.cancelTaskReminders(taskId)
        else {
            // re-schedule berdasarkan data terbaru
            val snap = tasksCollection(uid).document(taskId).get().await()
            snap.toObject(Task::class.java)?.let { scheduler.scheduleTaskReminders(it) }
        }
        Unit
    }

    suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        tasksCollection(uid).document(taskId).delete().await()
        scheduler.cancelTaskReminders(taskId)
        Unit
    }

    suspend fun restoreTask(task: Task): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        tasksCollection(uid).document(task.id).set(
            mapOf(
                "title" to task.title,
                "description" to task.description,
                "deadline" to task.deadline,
                "priority" to task.priority,
                "status" to task.status,
                "progress" to task.progress,
                "color" to task.color,
                "submissionPath" to task.submissionPath,
                "submissionType" to task.submissionType,
                "submittedAt" to task.submittedAt,
                "reminderEnabled" to task.reminderEnabled,
                "createdAt" to (task.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }

    suspend fun markSubmitted(
        taskId: String,
        storagePath: String,
        fileType: String
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        tasksCollection(uid).document(taskId).set(
            mapOf(
                "status" to TaskStatus.DONE.value,
                "progress" to 100,
                "submissionPath" to storagePath,
                "submissionType" to fileType,
                "submittedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
        Unit
    }

    suspend fun uploadSubmission(
        uri: android.net.Uri,
        mime: String,
        fileName: String,
        onProgress: (Float) -> Unit
    ): Result<Pair<String, String>> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
            val ext = fileName.substringAfterLast('.', "bin")
            val type = when {
                mime.startsWith("image/") -> "image"
                mime.startsWith("video/") -> "video"
                mime.contains("pdf") -> "pdf"
                mime.contains("word") -> "docx"
                mime.contains("presentation") -> "pptx"
                else -> "file"
            }
            val path = "users/$uid/submissions/${System.currentTimeMillis()}.$ext"
            
            val inputStream = context.contentResolver.openInputStream(uri) ?: error("Gagal membuka file.")
            val bytes = inputStream.readBytes()
            val metadata = ObjectMetadata().apply {
                contentType = mime
                contentLength = bytes.size.toLong()
            }

            s3Client.putObject("kuliahku-storage", path, bytes.inputStream(), metadata)
            
            // Mengembalikan path. Untuk URL publik, biasanya: 
            // "https://pub-<id>.r2.dev/$path" atau custom domain Anda.
            path to type
        }
    }

    private fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val end = cal.timeInMillis - 1
        return start to end
    }

    /**
     * Ring progres: (selesai, total) untuk tugas-tugas yang sedang aktif/ditampilkan.
     * Ini memastikan angka pada card progres sinkron dengan "Daftar Tugas" di Beranda.
     */
    fun observeTotalProgress(): Flow<Pair<Int, Int>> =
        observeAllTasks().map { list ->
            list.count { it.statusEnum == TaskStatus.DONE } to list.size
        }

    /**
     * Statistik Beranda: total, akan datang, selesai, terlambat.
     */
    fun observeStats(): Flow<TaskStats> =
        observeTasksRaw().map { list ->
            val now = System.currentTimeMillis()
            TaskStats(
                total = list.size,
                upcoming = list.count { it.statusEnum != TaskStatus.DONE && (it.deadline?.toDate()?.time ?: 0L) >= now },
                done = list.count { it.statusEnum == TaskStatus.DONE },
                overdue = list.count { it.statusEnum != TaskStatus.DONE && (it.deadline?.toDate()?.time ?: Long.MAX_VALUE) < now }
            )
        }
}

data class TaskStats(
    val total: Int = 0,
    val upcoming: Int = 0,
    val done: Int = 0,
    val overdue: Int = 0
)