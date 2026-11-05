package com.kuliahku.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

enum class MaterialSourceType(val value: String) {
    FILE("file"), TEXT("text"), YOUTUBE("youtube"),
    WEB("web"), AUDIO("audio"), PHOTO("photo");
    companion object { fun from(v: String?) = entries.firstOrNull { it.value == v } ?: FILE }
}

data class Material(
    @DocumentId val id: String = "",
    val title: String = "",
    val sourceType: String = MaterialSourceType.FILE.value,
    val sourceRef: String? = null,       // storagePath / url / youtubeId
    val extractedText: String? = null,
    val summary: String? = null,
    val chatHistory: List<ChatMessage> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val sourceTypeEnum: MaterialSourceType get() = MaterialSourceType.from(sourceType)
}

data class ChatMessage(
    val role: String = "",
    val content: String = "",
    val timestamp: Timestamp? = null
)

data class QuizQuestion(
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val explanation: String = "",
    val topic: String = ""
)

data class QuizAttempt(
    @DocumentId val id: String = "",
    val materialId: String = "",
    val questions: List<QuizQuestion> = emptyList(),
    val answers: List<Int> = emptyList(),
    val score: Int = 0,
    val total: Int = 0,
    val createdAt: Timestamp? = null
)

data class Flashcard(
    val front: String = "",
    val back: String = ""
)