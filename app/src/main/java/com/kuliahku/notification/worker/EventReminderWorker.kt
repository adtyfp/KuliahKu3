
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
import com.kuliahku.data.repository.EventRepository
import com.kuliahku.data.repository.NotificationRepository
import com.kuliahku.notification.NotificationChannels
import com.kuliahku.notification.ReminderScheduler
import com.kuliahku.util.DateFormatters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Date

@HiltWorker
class EventReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationRepo: NotificationRepository,
    private val eventRepo: EventRepository,
    private val scheduler: ReminderScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID)
            ?: return Result.failure()

        val title = inputData.getString(KEY_TITLE)
            ?: "Pengingat Acara"

        val start = inputData.getLong(KEY_START, 0L)

        val style = inputData.getString(KEY_STYLE)
            ?: "banner"

        if (eventId.isBlank() || start <= 0L) {
            Log.e(TAG, "Data pengingat Worker tidak valid.")
            return Result.failure()
        }

        val body =
            "Acara \"$title\" pukul ${DateFormatters.formatTime(Date(start))} WIB."

        val deepLink = "kuliahku://event/$eventId"

        NotificationChannels.ensureChannels(applicationContext)

        val intent = Intent(
            applicationContext,
            MainActivity::class.java
        ).apply {
            data = android.net.Uri.parse(deepLink)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val channel = when (style) {
            "alarm" -> "${NotificationChannels.AGENDA}.alarm"
            else -> NotificationChannels.AGENDA
        }

        val builder = NotificationCompat.Builder(
            applicationContext,
            channel
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Pengingat Acara")
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(body)
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)

        when (style) {
            "notification" -> {
                builder
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
            }

            "alarm" -> {
                builder
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setFullScreenIntent(contentIntent, true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            }

            else -> {
                builder
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
            }
        }

        // Tampilkan notifikasi terlebih dahulu.
        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(eventId.hashCode(), builder.build())

            Log.d(TAG, "Notifikasi Worker ditampilkan: $eventId")
        } catch (e: SecurityException) {
            Log.e(TAG, "Izin notifikasi belum diberikan.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menampilkan notifikasi Worker.", e)
        }

        // Simpan notifikasi ke Inbox.
        try {
            notificationRepo.push(
                title = "Pengingat Acara",
                body = body,
                type = "event",
                deepLink = deepLink
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menyimpan notifikasi Inbox.", e)
        }

        // Jadwalkan pengulangan berikutnya.
        try {
            val event = eventRepo.observeAll()
                .first()
                .firstOrNull { it.id == eventId }

            if (event != null) {
                scheduler.scheduleEventReminder(
                    event = event,
                    afterMillis = start,
                    cancelExisting = false
                )
            } else {
                Log.w(TAG, "Acara $eventId tidak ditemukan.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menjadwalkan pengulangan acara.", e)
            // Jangan retry notifikasi yang sudah ditampilkan,
            // karena bisa menyebabkan notifikasi ganda.
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "EventReminderWorker"

        const val KEY_EVENT_ID = "event_id"
        const val KEY_TITLE = "title"
        const val KEY_START = "start"
        const val KEY_END = "end"
        const val KEY_STYLE = "style"
    }
}
