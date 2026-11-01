package com.kuliahku.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kuliahku.data.model.Event
import com.kuliahku.data.model.EventRepeat
import com.kuliahku.data.model.ReminderStyle
import com.kuliahku.data.model.Task
import com.kuliahku.notification.worker.EventReminderWorker
import com.kuliahku.notification.worker.TaskReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    private val context: Context,
    private val workManager: WorkManager
) {

    /** Jadwalkan H-1 dan H-2jam untuk sebuah task. */
    fun scheduleTaskReminders(task: Task) {
        cancelTaskReminders(task.id)
        val deadline = task.deadline?.toDate()?.time ?: return
        if (task.status == "done") return
        if (!task.reminderEnabled) return

        val now = System.currentTimeMillis()
        scheduleTaskAt(task, deadline - TimeUnit.DAYS.toMillis(1), "H-1")
        scheduleTaskAt(task, deadline - TimeUnit.HOURS.toMillis(2), "H-2jam")
        scheduleTaskAt(task, deadline, "OVERDUE")
    }

    private fun scheduleTaskAt(task: Task, triggerAt: Long, tagSuffix: String) {
        val delay = triggerAt - System.currentTimeMillis()
        if (delay <= 0) return
        val data = Data.Builder()
            .putString(TaskReminderWorker.KEY_TASK_ID, task.id)
            .putString(TaskReminderWorker.KEY_TITLE, task.title)
            .putLong(TaskReminderWorker.KEY_DEADLINE, task.deadline?.toDate()?.time ?: 0L)
            .putString(TaskReminderWorker.KEY_TAG, tagSuffix)
            .build()
        val req = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(taskTag(task.id))
            .build()
        workManager.enqueueUniqueWork(
            taskWorkName(task.id, tagSuffix),
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    fun cancelTaskReminders(taskId: String) {
        workManager.cancelAllWorkByTag(taskTag(taskId))
    }

    /** Jadwalkan reminder acara; hitung kemunculan berikutnya (termasuk repeat). */
    fun scheduleEventReminder(event: Event) {
        cancelEventReminders(event.id)
        val start = event.start?.toDate()?.time ?: return

        val next = nextOccurrenceMillis(event, start) ?: return
        val offset = (event.reminderOffsetMinutes ?: 0).toLong() * 60_000L
        val triggerAt = next - offset
        val delay = triggerAt - System.currentTimeMillis()
        if (delay <= 0) return

        val data = Data.Builder()
            .putString(EventReminderWorker.KEY_EVENT_ID, event.id)
            .putString(EventReminderWorker.KEY_TITLE, event.title)
            .putLong(EventReminderWorker.KEY_START, next)
            .putLong(EventReminderWorker.KEY_END, event.end?.toDate()?.time ?: next)
            .putString(EventReminderWorker.KEY_STYLE, event.reminderStyle)
            .build()
        val req = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(eventTag(event.id))
            .build()
        workManager.enqueueUniqueWork(
            eventWorkName(event.id),
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    fun cancelEventReminders(eventId: String) {
        workManager.cancelAllWorkByTag(eventTag(eventId))
    }

    /** Hitung kemunculan berikutnya dari event (memperhitungkan repeat + exceptions). */
    private fun nextOccurrenceMillis(event: Event, startMillis: Long): Long? {
        val now = System.currentTimeMillis()
        if (event.repeatEnum == EventRepeat.NONE) {
            return if (startMillis > now) startMillis else null
        }
        var cal = Calendar.getInstance().apply { timeInMillis = startMillis }
        var safety = 0
        while (cal.timeInMillis <= now && safety < 1000) {
            when (event.repeatEnum) {
                EventRepeat.DAILY -> cal.add(Calendar.DAY_OF_MONTH, 1)
                EventRepeat.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                EventRepeat.MONTHLY -> cal.add(Calendar.MONTH, 1)
                EventRepeat.YEARLY -> cal.add(Calendar.YEAR, 1)
                else -> return null
            }
            safety++
        }
        // Skip exceptions
        var attempts = 0
        while (attempts < 60) {
            val key = EventRepository_key(cal)
            if (!event.exceptions.contains(key)) return cal.timeInMillis
            when (event.repeatEnum) {
                EventRepeat.DAILY -> cal.add(Calendar.DAY_OF_MONTH, 1)
                EventRepeat.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                EventRepeat.MONTHLY -> cal.add(Calendar.MONTH, 1)
                EventRepeat.YEARLY -> cal.add(Calendar.YEAR, 1)
                else -> return null
            }
            attempts++
        }
        return null
    }

    private fun EventRepository_key(cal: Calendar): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return fmt.format(cal.time)
    }

    private fun taskTag(id: String) = "task_reminder_$id"
    private fun eventTag(id: String) = "event_reminder_$id"
    private fun taskWorkName(id: String, suffix: String) = "task_work_${id}_$suffix"
    private fun eventWorkName(id: String) = "event_work_$id"
}