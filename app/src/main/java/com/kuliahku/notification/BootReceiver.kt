package com.kuliahku.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kuliahku.data.repository.EventRepository
import com.kuliahku.data.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepo: TaskRepository
    @Inject lateinit var eventRepo: EventRepository
    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        NotificationChannels.ensureChannels(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = taskRepo.observeAllTasks().first()
                tasks.forEach { scheduler.scheduleTaskReminders(it) }
                val events = eventRepo.observeAll().first()
                events.forEach { scheduler.scheduleEventReminder(it) }
            } finally {
                pending.finish()
            }
        }
    }
}