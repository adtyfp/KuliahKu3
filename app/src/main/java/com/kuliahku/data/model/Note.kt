package com.kuliahku.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

enum class NoteSource(val value: String) {
    MANUAL("manual"), SCAN("scan"), AI("ai");
    companion object {
        fun from(v: String?) = entries.firstOrNull { it.value.equals(v, true) } ?: MANUAL
    }
}

data class Note(
    @DocumentId val id: String = "",
    val title: String = "",
    val content: String = "",
    val source: String = NoteSource.MANUAL.value,
    val materialId: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val sourceEnum: NoteSource get() = NoteSource.from(source)
}