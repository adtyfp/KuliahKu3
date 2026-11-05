
package com.kuliahku.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kuliahku.MainActivity
import com.kuliahku.R
import com.kuliahku.data.repository.EventRepository
import com.kuliahku.data.repository.NotificationRepository
import com.kuliahku.util.DateFormatters
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationRepo: NotificationRepository

    @Inject
    lateinit var eventRepo: EventRepository

    @Inject
    lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(ReminderScheduler.EXTRA_TYPE) ?: ReminderScheduler.TYPE_EVENT
        
        if (type == ReminderScheduler.TYPE_TASK) {
            handleTaskReminder(context, intent)
        } else {
            handleEventReminder(context, intent)
        }
    }

    private fun handleTaskReminder(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID) ?: return
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Tugas"
        val deadline = intent.getLongExtra(ReminderScheduler.EXTRA_DEADLINE, 0L)
        val tag = intent.getStringExtra(ReminderScheduler.EXTRA_TAG) ?: ""

        val deadlineDate = Date(deadline)
        val body = when (tag) {
            "H-1" -> "Deadline mendekat! Tugas \"$title\" harus dikumpulkan besok pukul ${DateFormatters.formatTime(deadlineDate)}."
            "H-2jam" -> "Deadline 2 jam lagi! Segera selesaikan tugas \"$title\" sebelum pukul ${DateFormatters.formatTime(deadlineDate)}."
            "OVERDUE" -> "Tugas \"$title\" telah melewati batas pengumpulan pada ${DateFormatters.formatDateTime(deadlineDate)}."
            else -> "Pengingat tugas \"$title\"."
        }

        val notificationTitle = when (tag) {
            "H-1" -> "Deadline Besok"
            "H-2jam" -> "Deadline 2 Jam Lagi"
            "OVERDUE" -> "Tugas Terlambat"
            else -> "Deadline Tugas"
        }

        NotificationChannels.ensureChannels(context)
        val deepLink = "kuliahku://task/$taskId"
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            data = android.net.Uri.parse(deepLink)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            (taskId + tag).hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NotificationChannels.DEADLINE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify((taskId + tag).hashCode(), builder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "Izin notifikasi belum diberikan.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menampilkan notifikasi tugas", e)
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                notificationRepo.push(notificationTitle, body, "task", deepLink)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleEventReminder(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(ReminderScheduler.EXTRA_EVENT_ID)
            ?: return

        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE)
            ?: "Pengingat Acara"

        val start = intent.getLongExtra(
            ReminderScheduler.EXTRA_START,
            0L
        )

        val style = intent.getStringExtra(ReminderScheduler.EXTRA_STYLE)
            ?: "banner"

        val triggerAt = intent.getLongExtra(
            ReminderScheduler.EXTRA_TRIGGER_AT,
            0L
        )

        if (eventId.isBlank() || start <= 0L) {
            Log.e(TAG, "Data pengingat tidak valid.")
            return
        }

        val receivedAt = System.currentTimeMillis()

        if (triggerAt > 0L) {
            val delay = receivedAt - triggerAt

            Log.d(
                TAG,
                "Alarm diterima. Selisih dari jadwal: ${delay}ms"
            )
        }

        val body =
            "Acara \"$title\" pukul ${DateFormatters.formatTime(Date(start))} WIB."

        val deepLink = "kuliahku://event/$eventId"

        // Pastikan channel tersedia sebelum menampilkan notifikasi.
        NotificationChannels.ensureChannels(context)

        val mainIntent = Intent(
            context,
            MainActivity::class.java
        ).apply {
            data = android.net.Uri.parse(deepLink)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val channel = when (style) {
            "alarm" -> "${NotificationChannels.AGENDA}.alarm"
            else -> NotificationChannels.AGENDA
        }

        val builder = NotificationCompat.Builder(context, channel)
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

        // Tampilkan notifikasi SEBELUM menjalankan proses jaringan.
        try {
            NotificationManagerCompat.from(context)
                .notify(eventId.hashCode(), builder.build())

            Log.d(TAG, "Notifikasi acara $eventId berhasil ditampilkan.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Izin notifikasi belum diberikan.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menampilkan notifikasi acara.", e)
        }

        // Proses tambahan dilakukan setelah notifikasi ditampilkan.
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Simpan notifikasi ke Inbox aplikasi.
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

                // Ambil acara dan jadwalkan pengulangan berikutnya.
                try {
                    val event = eventRepo.observeAll()
                        .first()
                        .firstOrNull { it.id == eventId }

                    if (event != null) {
                        scheduler.scheduleEventReminder(
                            event = event,
                            afterMillis = start,
                            cancelExisting = true
                        )
                    } else {
                        Log.w(TAG, "Acara $eventId tidak ditemukan.")
                    }
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Gagal menjadwalkan pengulangan acara.",
                        e
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}
