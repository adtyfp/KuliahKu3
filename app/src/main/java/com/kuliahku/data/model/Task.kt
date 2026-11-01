package com.kuliahku.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

enum class TaskPriority(val value: String) {
    HIGH("high"), MEDIUM("medium"), LOW("low");
    companion object {
        fun from(v: String?): TaskPriority =
            entries.firstOrNull { it.value.equals(v, ignoreCase = true) } ?: MEDIUM
    }
}

enum class TaskStatus(val value: String) {
    TODO("todo"), DONE("done"), LATE("late");
    companion object {
        fun from(v: String?): TaskStatus =
            entries.firstOrNull { it.value.equals(v, ignoreCase = true) } ?: TODO
    }
}

data class Task(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val deadline: Timestamp? = null,
    val priority: String = TaskPriority.MEDIUM.value,
    val status: String = TaskStatus.TODO.value,
    val progress: Int = 0,
    val color: Int = 0,
    val submissionPath: String? = null,
    val submissionType: String? = null,
    val submittedAt: Timestamp? = null,
    val reminderEnabled: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val priorityEnum: TaskPriority get() = TaskPriority.from(priority)
    val statusEnum: TaskStatus get() = TaskStatus.from(status)
}