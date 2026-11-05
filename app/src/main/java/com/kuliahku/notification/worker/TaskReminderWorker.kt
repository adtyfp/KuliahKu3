
package com.kuliahku.notification.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kuliahku.MainActivity
import com.kuliahku.R
import com.kuliahku.data.repository.NotificationRepository
import com.kuliahku.notification.NotificationChannels
import com.kuliahku.util.DateFormatters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date

@HiltWorker
class TaskReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationRepo: NotificationRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        // =====================================================
        // 1. AMBIL DATA TUGAS
        // =====================================================

        val taskId = inputData.getString(KEY_TASK_ID)
            ?: return Result.failure()

        val title = inputData.getString(KEY_TITLE)
            ?: return Result.failure()

        val deadline = inputData.getLong(KEY_DEADLINE, 0L)

        val tag = inputData.getString(KEY_TAG)
            ?: return Result.failure()

        if (taskId.isBlank() || deadline <= 0L) {
            Log.e(TAG, "Data pengingat tugas tidak valid.")
            return Result.failure()
        }

        // =====================================================
        // 2. SIAPKAN PESAN NOTIFIKASI
        // =====================================================

        val deadlineDate = Date(deadline)

        val body = when (tag) {

            "H-1" ->
                "Deadline mendekat! Tugas \"$title\" harus dikumpulkan besok pukul ${DateFormatters.formatTime(deadlineDate)}."

            "H-2jam" ->
                "Deadline 2 jam lagi! Segera selesaikan tugas \"$title\" sebelum pukul ${DateFormatters.formatTime(deadlineDate)}."

            "OVERDUE" ->
                "Tugas \"$title\" telah melewati batas pengumpulan pada ${DateFormatters.formatDateTime(deadlineDate)}."

            else ->
                "Pengingat tugas \"$title\"."
        }

        val notificationTitle = when (tag) {
            "H-1" -> "Deadline Besok"
            "H-2jam" -> "Deadline 2 Jam Lagi"
            "OVERDUE" -> "Tugas Terlambat"
            else -> "Deadline Tugas"
        }

        val deepLink = "kuliahku://task/$taskId"

        // =====================================================
        // 3. SIAPKAN CHANNEL NOTIFIKASI
        // =====================================================

        NotificationChannels.ensureChannels(applicationContext)

        // =====================================================
        // 4. SIAPKAN DEEP LINK
        // =====================================================

        val intent = Intent(
            applicationContext,
            MainActivity::class.java
        ).apply {
            data = android.net.Uri.parse(deepLink)

            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        // =====================================================
        // 5. BUAT NOTIFIKASI
        // =====================================================

        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationChannels.DEADLINE
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationTitle)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .build()

        // Gunakan ID berbeda untuk setiap jenis pengingat.
        // Dengan begitu H-1, H-2, dan deadline tidak saling menimpa.
        val notificationId = 31 * taskId.hashCode() + tag.hashCode()

        // =====================================================
        // 6. TAMPILKAN NOTIFIKASI
        // =====================================================

        try {
            NotificationManagerCompat
                .from(applicationContext)
                .notify(notificationId, notification)

            Log.d(
                TAG,
                "Notifikasi tugas berhasil ditampilkan: $taskId ($tag)"
            )

        } catch (e: SecurityException) {
            Log.e(
                TAG,
                "Izin notifikasi belum diberikan.",
                e
            )
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Gagal menampilkan notifikasi tugas.",
                e
            )
        }

        // =====================================================
        // 7. SIMPAN KE INBOX
        // =====================================================

        try {
            notificationRepo.push(
                title = notificationTitle,
                body = body,
                type = "task",
                deepLink = deepLink
            )

            Log.d(
                TAG,
                "Notifikasi tugas berhasil disimpan ke Inbox."
            )

        } catch (e: Exception) {
            // Jangan retry seluruh Worker karena notifikasi
            // mungkin sudah berhasil ditampilkan.
            Log.e(
                TAG,
                "Gagal menyimpan notifikasi tugas ke Inbox.",
                e
            )
        }

        // =====================================================
        // 8. SELESAI
        // =====================================================

        return Result.success()
    }

    companion object {
        private const val TAG = "TaskReminderWorker"

        const val KEY_TASK_ID = "task_id"
        const val KEY_TITLE = "title"
        const val KEY_DEADLINE = "deadline"
        const val KEY_TAG = "tag"
    }
}
