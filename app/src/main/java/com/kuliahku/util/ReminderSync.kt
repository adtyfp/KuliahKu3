package com.kuliahku.util

import com.kuliahku.data.repository.EventRepository
import com.kuliahku.data.repository.TaskRepository
import com.kuliahku.notification.ReminderScheduler
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Membantu sinkronisasi semua reminder: dipanggil setelah login,
 * setelah BootReceiver, atau saat pengguna mengubah setting.
 */
@Singleton
class ReminderSync @Inject constructor(
    private val taskRepo: TaskRepository,
    private val eventRepo: EventRepository,
    private val scheduler: ReminderScheduler
) {
    suspend fun rescheduleAll() {
        taskRepo.observeAllTasks().first().forEach { scheduler.scheduleTaskReminders(it) }
        eventRepo.observeAll().first().forEach { scheduler.scheduleEventReminder(it) }
    }
}