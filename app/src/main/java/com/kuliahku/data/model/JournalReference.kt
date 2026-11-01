package com.kuliahku.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class JournalReference(
    val id: String = "",
    val doi: String? = null,
    val title: String = "",
    val authors: List<String> = emptyList(),
    val authorCount: Int = 0,
    val year: Int? = null,
    val citedByCount: Int = 0,
    val isOpenAccess: Boolean = false,
    val field: String? = null,
    val type: String? = null,
    val imageUrl: String? = null,
    val publishedAtMillis: Long? = null,
    val savedAt: Timestamp? = null
) {
    val firstAuthorInitial: String
        get() = authors.firstOrNull()?.trim()?.firstOrNull()?.uppercase() ?: "?"

    val firstAuthor: String
        get() = authors.firstOrNull() ?: "Anonim"
}

data class Bookmark(
    @DocumentId val id: String = "",
    val title: String = "",
    val doi: String? = null,
    val authors: List<String> = emptyList(),
    val year: Int? = null,
    val citedByCount: Int = 0,
    val isOpenAccess: Boolean = false,
    val url: String? = null,
    val source: String = "openalex",
    val createdAt: Timestamp? = null
)