package com.kuliahku.notification.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
class EventReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationRepo: NotificationRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val start = inputData.getLong(KEY_START, 0L)
        val style = inputData.getString(KEY_STYLE) ?: "banner"
        if (start == 0L) return Result.failure()

        val body = "Acara \"$title\" pukul ${DateFormatters.formatTime(Date(start))} WIB."

        val deepLink = "kuliahku://event/$eventId"
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            data = android.net.Uri.parse(deepLink)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            applicationContext,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channel = when (style) {
            "alarm" -> "${NotificationChannels.AGENDA}.alarm"
            else -> NotificationChannels.AGENDA
        }

        val builder = NotificationCompat.Builder(applicationContext, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Pengingat Acara")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)

        when (style) {
            "notification" -> builder.setPriority(NotificationCompat.PRIORITY_HIGH)
            "alarm" -> {
                builder.setPriority(NotificationCompat.PRIORITY_MAX)
                builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                // Full-screen intent bila izin tersedia
                builder.setFullScreenIntent(pi, true)
            }
            else -> builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        }

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(eventId.hashCode(), builder.build())
        } catch (_: SecurityException) { return Result.retry() }

        notificationRepo.push(
            title = "Pengingat Acara",
            body = body,
            type = "event",
            deepLink = deepLink
        )
        return Result.success()
    }

    companion object {
        const val KEY_EVENT_ID = "event_id"
        const val KEY_TITLE = "title"
        const val KEY_START = "start"
        const val KEY_END = "end"
        const val KEY_STYLE = "style"
    }
}