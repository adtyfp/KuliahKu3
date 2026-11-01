package com.kuliahku.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

enum class EventType(val value: String) {
    ACARA("acara"), PENGINGAT("pengingat"), TARGET("target");
    companion object { fun from(v: String?) = entries.firstOrNull { it.value == v } ?: ACARA }
}

enum class EventRepeat(val value: String) {
    NONE("none"), DAILY("daily"), WEEKLY("weekly"), MONTHLY("monthly"), YEARLY("yearly");
    companion object { fun from(v: String?) = entries.firstOrNull { it.value == v } ?: NONE }
}

enum class ReminderStyle(val value: String) {
    BANNER("banner"), NOTIFICATION("notification"), ALARM("alarm");
    companion object { fun from(v: String?) = entries.firstOrNull { it.value == v } ?: BANNER }
}

data class Event(
    @DocumentId val id: String = "",
    val type: String = EventType.ACARA.value,
    val title: String = "",
    val allDay: Boolean = false,
    val start: Timestamp? = null,
    val end: Timestamp? = null,
    val repeat: String = EventRepeat.NONE.value,
    val reminderStyle: String = ReminderStyle.BANNER.value,
    val reminderOffsetMinutes: Int? = 10,
    val exceptions: List<String> = emptyList(), // format yyyy-MM-dd
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val typeEnum: EventType get() = EventType.from(type)
    val repeatEnum: EventRepeat get() = EventRepeat.from(repeat)
    val reminderStyleEnum: ReminderStyle get() = ReminderStyle.from(reminderStyle)
}