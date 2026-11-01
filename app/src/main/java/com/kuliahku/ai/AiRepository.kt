package com.kuliahku.ai

import com.google.firebase.vertexai.type.content
import com.kuliahku.data.model.Flashcard
import com.kuliahku.data.model.QuizQuestion
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

sealed class AiResult<out T> {
    data class Success<T>(val data: T) : AiResult<T>()
    data class Error(val message: String) : AiResult<Nothing>()
    data object Timeout : AiResult<Nothing>()
}

@Singleton
class AiRepository @Inject constructor(
    private val client: AiClient
) {

    suspend fun summarize(text: String): AiResult<String> = call {
        val response = client.get().generateContent(Prompts.summary(text))
        response.text ?: ""
    }

    suspend fun summarizeAudio(audioBytes: ByteArray, mimeType: String): AiResult<String> = call {
        // Menggunakan DSL content dari Google AI SDK
        val userContent = content {
            inlineData(audioBytes, mimeType)
            text("Tolong dengarkan audio ini dan berikan ringkasan materi kuliah yang lengkap.")
        }
        val response = client.get().generateContent(userContent)
        response.text ?: ""
    }

    suspend fun generateQuiz(text: String, count: Int = 10): AiResult<List<QuizQuestion>> = call {
        val response = client.jsonModel().generateContent(Prompts.quiz(text, count))
        val raw = response.text ?: error("Empty response")
        parseQuiz(raw)
    }

    suspend fun generateFlashcards(text: String, count: Int = 12): AiResult<List<Flashcard>> = call {
        val response = client.jsonModel().generateContent(Prompts.flashcards(text, count))
        val raw = response.text ?: error("Empty response")
        parseFlashcards(raw)
    }

    suspend fun ask(text: String, question: String): AiResult<String> = call {
        val response = client.get().generateContent(Prompts.chat(text, question))
        response.text ?: ""
    }

    suspend fun suggestReferences(text: String): AiResult<List<String>> = call {
        val response = client.jsonModel().generateContent(Prompts.references(text))
        val raw = response.text ?: error("Empty response")
        parseKeywords(raw)
    }

    suspend fun getTaskPrioritizationAdvice(tasks: List<com.kuliahku.data.model.Task>): AiResult<String> = call {
        val tasksJson = tasks.joinToString(",", "[", "]") {
            """{"title":"${it.title}","deadline":"${it.deadline?.toDate()}","priority":"${it.priority}"}"""
        }
        val response = client.get().generateContent(Prompts.taskPrioritization(tasksJson))
        response.text ?: ""
    }

    suspend fun getFinanceInsight(summary: com.kuliahku.util.MonthlySummary): AiResult<String> = call {
        val json = """{"totalIncome":${summary.totalIncome},"totalExpense":${summary.totalExpense}}"""
        val response = client.get().generateContent(Prompts.financeInsight(json))
        response.text ?: ""
    }

    private suspend fun <T> call(block: suspend () -> T): AiResult<T> {
        return try {
            val data = withTimeout(60_000) { block() }
            AiResult.Success(data)
        } catch (e: Throwable) {
            Log.e("AiRepository", "AI Error: ${e.message}", e)
            AiResult.Error(mapError(e))
        }
    }

    private fun parseQuiz(raw: String): List<QuizQuestion> {
        val jsonText = raw.trim().removePrefix("```json").removeSuffix("```").trim()
        val json = JSONObject(jsonText)
        val arr = json.getJSONArray("questions")
        val list = mutableListOf<QuizQuestion>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val opts = o.getJSONArray("options").let { a ->
                (0 until a.length()).map { a.getString(it) }
            }
            list.add(
                QuizQuestion(
                    question = o.getString("question"),
                    options = opts,
                    correctIndex = o.getInt("correctIndex"),
                    explanation = o.optString("explanation")
                )
            )
        }
        return list
    }

    private fun parseFlashcards(raw: String): List<Flashcard> {
        val jsonText = raw.trim().removePrefix("```json").removeSuffix("```").trim()
        val json = JSONObject(jsonText)
        val arr = json.getJSONArray("cards")
        val list = mutableListOf<Flashcard>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(Flashcard(o.getString("front"), o.getString("back")))
        }
        return list
    }

    private fun parseKeywords(raw: String): List<String> {
        val jsonText = raw.trim().removePrefix("```json").removeSuffix("```").trim()
        val json = JSONObject(jsonText)
        val arr = json.getJSONArray("keywords")
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        return list
    }

    private fun mapError(e: Throwable): String = when {
        e is kotlinx.coroutines.TimeoutCancellationException -> "Koneksi timeout. Coba lagi."
        e.message?.contains("App Check", true) == true -> "Firebase App Check gagal. Pastikan Debug Token sudah terdaftar di Firebase Console."
        e.message?.contains("quota", true) == true -> "Kuota AI tercapai."
        e.message?.contains("API_KEY_INVALID", true) == true -> "API Key tidak valid."
        e.message?.contains("not found", true) == true -> "Model tidak ditemukan. Coba update API Key."
        e is NoClassDefFoundError -> "Kesalahan library (Ktor). Hubungi pengembang."
        else -> "Gagal merespon: ${e.localizedMessage?.take(40) ?: "Terjadi kesalahan"}"
    }
}
