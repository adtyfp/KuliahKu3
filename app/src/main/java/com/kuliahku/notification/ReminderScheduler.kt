
package com.kuliahku.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kuliahku.MainActivity
import com.kuliahku.data.model.Event
import com.kuliahku.data.model.EventRepeat
import com.kuliahku.data.model.Task
import com.kuliahku.notification.worker.EventReminderWorker
import com.kuliahku.notification.worker.TaskReminderWorker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    private val context: Context,
    private val workManager: WorkManager
) {

    private val alarmManager: AlarmManager? =
        context.getSystemService<AlarmManager>()

    // =========================================================
    // TASK REMINDERS
    // =========================================================

    fun scheduleTaskReminders(task: Task) {
        if (task.id.isBlank()) return

        cancelTaskReminders(task.id)

        val deadline = task.deadline?.toDate()?.time ?: return

        if (task.status == "done") return
        if (!task.reminderEnabled) return

        scheduleTaskAt(
            task = task,
            triggerAt = deadline - TimeUnit.DAYS.toMillis(1),
            tagSuffix = "H-1"
        )

        scheduleTaskAt(
            task = task,
            triggerAt = deadline - TimeUnit.HOURS.toMillis(2),
            tagSuffix = "H-2jam"
        )

        scheduleTaskAt(
            task = task,
            triggerAt = deadline,
            tagSuffix = "OVERDUE"
        )
    }

    private fun scheduleTaskAt(
        task: Task,
        triggerAt: Long,
        tagSuffix: String
    ) {
        val now = System.currentTimeMillis()
        if (triggerAt <= now) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TYPE, TYPE_TASK)
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TITLE, task.title)
            putExtra(EXTRA_DEADLINE, task.deadline?.toDate()?.time ?: 0L)
            putExtra(EXTRA_TAG, tagSuffix)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id + tagSuffix).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val manager = alarmManager ?: throw IllegalStateException("AlarmManager not available")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
                scheduleTaskWorkManager(task, triggerAt, tagSuffix)
                return
            }

            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
            Log.d(TAG, "Task alarm scheduled: ${task.id} ($tagSuffix) at $triggerAt")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule task alarm, falling back to WorkManager", e)
            scheduleTaskWorkManager(task, triggerAt, tagSuffix)
        }
    }

    private fun scheduleTaskWorkManager(task: Task, triggerAt: Long, tagSuffix: String) {
        val now = System.currentTimeMillis()
        val data = Data.Builder()
            .putString(TaskReminderWorker.KEY_TASK_ID, task.id)
            .putString(TaskReminderWorker.KEY_TITLE, task.title)
            .putLong(TaskReminderWorker.KEY_DEADLINE, task.deadline?.toDate()?.time ?: 0L)
            .putString(TaskReminderWorker.KEY_TAG, tagSuffix)
            .build()

        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(maxOf(0, triggerAt - now), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.NONE)
            .setInputData(data)
            .addTag(taskTag(task.id))
            .build()

        workManager.enqueueUniqueWork(
            taskWorkName(task.id, tagSuffix),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelTaskReminders(taskId: String) {
        if (taskId.isBlank()) return
        
        // Cancel Alarms
        val intent = Intent(context, AlarmReceiver::class.java)
        listOf("H-1", "H-2jam", "OVERDUE").forEach { suffix ->
            val pi = PendingIntent.getBroadcast(
                context,
                (taskId + suffix).hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                alarmManager?.cancel(pi)
                pi.cancel()
            }
        }

        workManager.cancelAllWorkByTag(taskTag(taskId))
    }

    // =========================================================
    // EVENT REMINDERS
    // =========================================================

    fun scheduleEventReminder(
        event: Event,
        afterMillis: Long = System.currentTimeMillis(),
        cancelExisting: Boolean = true
    ) {
        if (event.id.isBlank()) {
            Log.e(TAG, "ID acara kosong. Pengingat tidak dijadwalkan.")
            return
        }

        if (cancelExisting) {
            cancelEventReminders(event.id)
        }

        val startMillis = event.start?.toDate()?.time

        if (startMillis == null) {
            Log.e(TAG, "Waktu mulai acara ${event.id} tidak tersedia.")
            return
        }

        val nextOccurrence = nextOccurrenceMillis(
            event = event,
            startMillis = startMillis,
            afterMillis = afterMillis
        ) ?: run {
            Log.d(TAG, "Tidak ada kemunculan berikutnya: ${event.id}")
            return
        }

        val offsetMillis =
            (event.reminderOffsetMinutes ?: 0).toLong() * 60_000L

        var triggerAt = nextOccurrence - offsetMillis
        val now = System.currentTimeMillis()

        // Jika waktu pengingat sudah lewat, tetapi acara belum dimulai,
        // tampilkan pengingat pada waktu mulai acara.
        if (triggerAt <= now && nextOccurrence > now) {
            triggerAt = nextOccurrence
        }

        if (triggerAt <= now) {
            Log.d(TAG, "Waktu pengingat acara ${event.id} sudah lewat.")
            return
        }

        val delay = triggerAt - now

        val intent = Intent(
            context,
            AlarmReceiver::class.java
        ).apply {
            putExtra(EXTRA_TYPE, TYPE_EVENT)
            putExtra(EXTRA_EVENT_ID, event.id)
            putExtra(EXTRA_TITLE, event.title)
            putExtra(EXTRA_START, nextOccurrence)
            putExtra(EXTRA_STYLE, event.reminderStyle)
            putExtra(EXTRA_TRIGGER_AT, triggerAt)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        Log.d(
            TAG,
            "Menjadwalkan acara=${event.id}, " +
                    "style=${event.reminderStyle}, " +
                    "triggerAt=$triggerAt, delay=$delay ms"
        )

        try {
            val manager = alarmManager
                ?: throw IllegalStateException("AlarmManager tidak tersedia.")

            // Android 12 ke atas membutuhkan izin exact alarm.
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !manager.canScheduleExactAlarms()
            ) {
                Log.e(
                    TAG,
                    "Izin exact alarm belum diberikan. " +
                            "Menggunakan WorkManager sebagai cadangan."
                )

                pendingIntent.cancel()

                scheduleEventReminderWorkManager(
                    event = event,
                    next = nextOccurrence,
                    delay = delay
                )
                return
            }

            if (event.reminderStyle == "alarm") {
                // Alarm pengguna diprioritaskan oleh Android.
                val showIntent = PendingIntent.getActivity(
                    context,
                    event.id.hashCode() + 1,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )

                val alarmInfo = AlarmManager.AlarmClockInfo(
                    triggerAt,
                    showIntent
                )

                manager.setAlarmClock(
                    alarmInfo,
                    pendingIntent
                )
            } else {
                // Spanduk dan Notifikasi + Suara.
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }

            Log.d(
                TAG,
                "Alarm berhasil dijadwalkan: ${event.id} pada $triggerAt"
            )

        } catch (e: SecurityException) {
            Log.e(TAG, "Izin exact alarm ditolak.", e)

            scheduleEventReminderWorkManager(
                event = event,
                next = nextOccurrence,
                delay = delay
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menjadwalkan exact alarm.", e)

            scheduleEventReminderWorkManager(
                event = event,
                next = nextOccurrence,
                delay = delay
            )
        }
    }

    private fun scheduleEventReminderWorkManager(
        event: Event,
        next: Long,
        delay: Long
    ) {
        if (event.id.isBlank()) return

        val data = Data.Builder()
            .putString(EventReminderWorker.KEY_EVENT_ID, event.id)
            .putString(EventReminderWorker.KEY_TITLE, event.title)
            .putLong(EventReminderWorker.KEY_START, next)
            .putString(EventReminderWorker.KEY_STYLE, event.reminderStyle)
            .build()

        val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInitialDelay(
                maxOf(0L, delay),
                TimeUnit.MILLISECONDS
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .setConstraints(Constraints.NONE)
            .setInputData(data)
            .addTag(eventTag(event.id))
            .build()

        // Nama unik berdasarkan waktu kemunculan.
        // Worker yang sedang berjalan tidak akan tergantikan.
        workManager.enqueueUniqueWork(
            eventWorkName(event.id, next),
            ExistingWorkPolicy.REPLACE,
            request
        )

        Log.d(
            TAG,
            "WorkManager cadangan dijadwalkan untuk ${event.id}."
        )
    }

    fun cancelEventReminders(eventId: String) {
        if (eventId.isBlank()) return

        val intent = Intent(
            context,
            AlarmReceiver::class.java
        )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or
                    PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        // Batalkan seluruh pekerjaan WorkManager milik acara.
        workManager.cancelAllWorkByTag(eventTag(eventId))

        // Batalkan pekerjaan dari versi kode sebelumnya.
        workManager.cancelUniqueWork(eventWorkNameLegacy(eventId))

        Log.d(TAG, "Pengingat acara $eventId dibatalkan.")
    }

    // =========================================================
    // NEXT EVENT OCCURRENCE
    // =========================================================

    private fun nextOccurrenceMillis(
        event: Event,
        startMillis: Long,
        afterMillis: Long
    ): Long? {

        if (event.repeatEnum == EventRepeat.NONE) {
            return if (startMillis > afterMillis) {
                startMillis
            } else {
                null
            }
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = startMillis
        }

        var safety = 0

        // Lewati kemunculan yang sudah diproses.
        while (calendar.timeInMillis <= afterMillis && safety < 10_000) {
            advanceOccurrence(calendar, event.repeatEnum)
            safety++
        }

        if (safety >= 10_000) {
            Log.e(TAG, "Gagal menghitung pengulangan acara ${event.id}.")
            return null
        }

        // Lewati tanggal yang dikecualikan.
        var attempts = 0

        while (attempts < 120) {
            val occurrenceKey = dateKey(calendar)

            if (!event.exceptions.contains(occurrenceKey)) {
                return calendar.timeInMillis
            }

            advanceOccurrence(calendar, event.repeatEnum)
            attempts++
        }

        Log.w(TAG, "Tidak menemukan kemunculan valid: ${event.id}")
        return null
    }

    private fun advanceOccurrence(
        calendar: Calendar,
        repeat: EventRepeat
    ) {
        when (repeat) {
            EventRepeat.DAILY ->
                calendar.add(Calendar.DAY_OF_MONTH, 1)

            EventRepeat.WEEKLY ->
                calendar.add(Calendar.WEEK_OF_YEAR, 1)

            EventRepeat.MONTHLY ->
                calendar.add(Calendar.MONTH, 1)

            EventRepeat.YEARLY ->
                calendar.add(Calendar.YEAR, 1)

            EventRepeat.NONE -> Unit
        }
    }

    private fun dateKey(calendar: Calendar): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(calendar.time)
    }

    // =========================================================
    // TAGS AND WORK NAMES
    // =========================================================

    private fun taskTag(id: String) =
        "task_reminder_$id"

    private fun eventTag(id: String) =
        "event_reminder_$id"

    private fun taskWorkName(id: String, suffix: String) =
        "task_work_${id}_$suffix"

    private fun eventWorkName(id: String, occurrence: Long) =
        "event_work_${id}_$occurrence"

    private fun eventWorkNameLegacy(id: String) =
        "event_work_$id"

    companion object {
        private const val TAG = "ReminderScheduler"

        const val EXTRA_TYPE = "type"
        const val TYPE_TASK = "task"
        const val TYPE_EVENT = "event"

        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_DEADLINE = "deadline"
        const val EXTRA_TAG = "tag"

        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_START = "start"
        const val EXTRA_STYLE = "style"
        const val EXTRA_TRIGGER_AT = "trigger_at"
    }
}
