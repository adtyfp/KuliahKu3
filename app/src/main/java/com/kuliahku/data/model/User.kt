package com.kuliahku.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val nim: String = "",
    val prodi: String = "",
    val semester: Int = 1,
    val photoUrl: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val settings: UserSettings = UserSettings()
)

data class UserSettings(
    val themeMode: String = "system", // light | dark | system
    val reminderEnabled: Boolean = true,
    val financeListenerEnabled: Boolean = false
)

data class NimIndex(
    val email: String = ""
)