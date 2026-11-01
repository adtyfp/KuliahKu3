package com.kuliahku.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class AppNotification(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",       // task | event | finance
    val deepLink: String? = null,
    val read: Boolean = false,
    val createdAt: Timestamp? = null
)