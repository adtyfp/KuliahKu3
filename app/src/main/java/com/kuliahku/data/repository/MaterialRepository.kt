package com.kuliahku.data.repository

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.kuliahku.data.model.Material
import com.kuliahku.util.Constants
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaterialRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val s3Client: AmazonS3,
    @ApplicationContext private val context: Context
) {
    private fun col(uid: String) =
        firestore.collection(Constants.COLLECTION_USERS).document(uid).collection("materials")

    fun observeAll(): Flow<List<Material>> = callbackFlow {
        var snapshotListener: com.google.firebase.firestore.ListenerRegistration? = null
        
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            
            // Bersihkan listener lama jika ada (saat ganti akun/logout)
            snapshotListener?.remove()
            
            if (uid == null) {
                trySend(emptyList())
                return@AuthStateListener
            }

            val query = col(uid).orderBy("createdAt", Query.Direction.DESCENDING)
            snapshotListener = query.addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val materials = snap?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Material::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) { null }
                } ?: emptyList()
                
                trySend(materials)
            }
        }

        auth.addAuthStateListener(authListener)
        
        awaitClose {
            auth.removeAuthStateListener(authListener)
            snapshotListener?.remove()
        }
    }

    fun observe(id: String): Flow<Material?> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(null); close(); return@callbackFlow }
        val listener = col(uid).document(id)
            .addSnapshotListener { snap, err ->
                if (err != null) { 
                    android.util.Log.e("MaterialRepository", "Error observing material $id: ${err.message}")
                    trySend(null)
                    return@addSnapshotListener 
                }
                trySend(snap?.toObject(Material::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun create(material: Material): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        android.util.Log.d("MaterialRepository", "Mencoba menyimpan materi ke Firestore untuk UID: $uid")
        
        val id = material.id.ifBlank { col(uid).document().id }
        val data = mutableMapOf<String, Any?>(
            "title" to material.title,
            "sourceType" to material.sourceType,
            "sourceRef" to material.sourceRef,
            "extractedText" to material.extractedText,
            "summary" to material.summary,
            "quiz" to material.quiz,
            "flashcards" to material.flashcards,
            "createdAt" to (material.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (material.chatHistory.isNotEmpty()) {
            data["chatHistory"] = material.chatHistory
        }
        
        col(uid).document(id).set(data, SetOptions.merge()).await()
        android.util.Log.d("MaterialRepository", "Berhasil menyimpan materi dengan ID: $id ke database")
        id
    }.onFailure { e ->
        android.util.Log.e("MaterialRepository", "GAGAL menyimpan materi ke database: ${e.message}", e)
    }

    suspend fun updateSummary(id: String, summary: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        col(uid).document(id).update(
            "summary", summary,
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
        Unit
    }

    suspend fun updateQuiz(id: String, quiz: List<com.kuliahku.data.model.QuizQuestion>): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        col(uid).document(id).update(
            "quiz", quiz,
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
        Unit
    }

    suspend fun updateFlashcards(id: String, flashcards: List<com.kuliahku.data.model.Flashcard>): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        col(uid).document(id).update(
            "flashcards", flashcards,
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
        Unit
    }

    suspend fun updateReferences(id: String, references: List<com.kuliahku.data.model.JournalReference>): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        col(uid).document(id).update(
            "suggestedReferences", references,
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
        Unit
    }

    suspend fun saveChatMessage(materialId: String, message: com.kuliahku.data.model.ChatMessage): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        col(uid).document(materialId).update(
            "chatHistory", FieldValue.arrayUnion(message),
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
        Unit
    }

    suspend fun uploadFile(uri: android.net.Uri, mime: String, name: String): Result<String> = 
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
                val ext = name.substringAfterLast('.', "bin")
                val path = "users/$uid/materials/${System.currentTimeMillis()}.$ext"
                
                val inputStream = context.contentResolver.openInputStream(uri) ?: error("Gagal membuka file.")
                val bytes = inputStream.readBytes()
                val metadata = ObjectMetadata().apply {
                    contentType = mime
                    contentLength = bytes.size.toLong()
                }

                val bucket = com.kuliahku.BuildConfig.R2_BUCKET_NAME
                s3Client.putObject(bucket, path, bytes.inputStream(), metadata)
                
                // Return URL publik untuk Material
                val publicUrl = com.kuliahku.BuildConfig.R2_PUBLIC_URL.removeSuffix("/")
                "$publicUrl/$path"
            }
        }

    suspend fun saveQuizAttempt(attemptId: String, data: Map<String, Any?>): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Sesi tidak ditemukan.")
        firestore.collection(Constants.COLLECTION_USERS).document(uid)
            .collection("quizAttempts").document(attemptId).set(data).await()
        Unit
    }
}