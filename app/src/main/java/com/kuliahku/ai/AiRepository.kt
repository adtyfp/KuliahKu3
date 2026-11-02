
package com.kuliahku.ai

import android.util.Log
import com.kuliahku.data.model.Flashcard
import com.kuliahku.data.model.QuizQuestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

sealed class AiResult<out T> {
    data class Success<T>(val data: T) : AiResult<T>()
    data class Error(val message: String) : AiResult<Nothing>()
    data object Timeout : AiResult<Nothing>()
}

@Singleton
class AiRepository @Inject constructor(
    private val client: AiClient
) {

    companion object {
        private const val SUMMARY_TIMEOUT = 300_000L
        private const val NORMAL_TIMEOUT = 180_000L

        private const val CHUNK_SIZE = 10_000
        private const val MAX_QUIZ_TEXT = 16_000
        private const val MAX_FLASHCARD_TEXT = 16_000
        private const val MAX_CHAT_TEXT = 12_000
        private const val MAX_REFERENCE_TEXT = 12_000
    }

    // =====================================================
    // SUMMARY WITH CHUNKING
    // =====================================================

    suspend fun summarize(
        text: String
    ): AiResult<String> = call(SUMMARY_TIMEOUT) {

        require(text.isNotBlank()) {
            "Materi tidak memiliki teks."
        }

        val chunks = splitMaterial(text)

        if (chunks.isEmpty()) {
            throw Exception("Materi tidak dapat diproses.")
        }

        Log.d(
            "AiRepository",
            "Memproses ${chunks.size} bagian materi."
        )

        // Jika materi pendek, langsung ringkas.
        if (chunks.size == 1) {
            return@call client.generateContent(
                Prompts.summary(chunks.first()),
                maxOutputTokens = 2048
            )
        }

        // Ringkas setiap bagian secara berurutan.
        val partialSummaries = mutableListOf<String>()

        chunks.forEachIndexed { index, chunk ->

            Log.d(
                "AiRepository",
                "Merangkum bagian ${index + 1}/${chunks.size}"
            )

            val summary = client.generateContent(
                Prompts.summary(chunk),
                maxOutputTokens = 1200
            )

            partialSummaries.add(
                "BAGIAN ${index + 1}:\n$summary"
            )
        }

        // Gabungkan ringkasan setiap bagian.
        val combinedSummary = partialSummaries.joinToString(
            separator = "\n\n"
        )

        Log.d(
            "AiRepository",
            "Membuat ringkasan akhir."
        )

        // Buat ringkasan akhir dari hasil setiap bagian.
        client.generateContent(
            Prompts.summary(
                """
                Berikut adalah ringkasan dari beberapa bagian
                sebuah materi kuliah.

                Gabungkan seluruh bagian menjadi satu ringkasan
                akhir yang sistematis, lengkap, dan mudah dipahami.

                Ketentuan:
                1. Pertahankan konsep dan informasi penting.
                2. Hilangkan pengulangan.
                3. Susun menggunakan judul dan subjudul.
                4. Gunakan bahasa Indonesia yang jelas.
                5. Jangan menambahkan informasi yang tidak ada
                   dalam materi.

                $combinedSummary
                """.trimIndent()
            ),
            maxOutputTokens = 3000
        )
    }

    // =====================================================
    // SPLIT MATERIAL
    // =====================================================

    private fun splitMaterial(
        text: String
    ): List<String> {

        val normalized = text
            .replace("\r\n", "\n")
            .trim()

        if (normalized.length <= CHUNK_SIZE) {
            return listOf(normalized)
        }

        val chunks = mutableListOf<String>()
        val paragraphs = normalized.split(
            Regex("\\n\\s*\\n")
        )

        val current = StringBuilder()

        for (paragraph in paragraphs) {

            if (paragraph.isBlank()) continue

            // Pecah paragraf yang terlalu panjang.
            if (paragraph.length > CHUNK_SIZE) {

                if (current.isNotEmpty()) {
                    chunks.add(current.toString().trim())
                    current.clear()
                }

                var remaining = paragraph

                while (remaining.length > CHUNK_SIZE) {

                    var splitIndex = remaining
                        .lastIndexOf(' ', CHUNK_SIZE)

                    if (splitIndex < CHUNK_SIZE / 2) {
                        splitIndex = CHUNK_SIZE
                    }

                    chunks.add(
                        remaining.substring(0, splitIndex).trim()
                    )

                    remaining = remaining
                        .substring(splitIndex)
                        .trim()
                }

                if (remaining.isNotBlank()) {
                    current.append(remaining)
                }

                continue
            }

            if (
                current.isNotEmpty() &&
                current.length + paragraph.length + 2 > CHUNK_SIZE
            ) {
                chunks.add(current.toString().trim())
                current.clear()
            }

            if (current.isNotEmpty()) {
                current.append("\n\n")
            }

            current.append(paragraph)
        }

        if (current.isNotEmpty()) {
            chunks.add(current.toString().trim())
        }

        return chunks.filter { it.isNotBlank() }
    }

    // =====================================================
    // AUDIO SUMMARY
    // =====================================================

    suspend fun summarizeAudio(
        audioBytes: ByteArray,
        mimeType: String
    ): AiResult<String> = call(SUMMARY_TIMEOUT) {

        client.generateAudio(
            audioBytes,
            mimeType
        )
    }

    // =====================================================
    // GENERATE QUIZ
    // =====================================================

    suspend fun generateQuiz(
        text: String,
        count: Int = 10
    ): AiResult<List<QuizQuestion>> = call(NORMAL_TIMEOUT) {

        require(text.isNotBlank()) {
            "Materi tidak memiliki teks."
        }

        val safeText = text.take(MAX_QUIZ_TEXT)
        val safeCount = count.coerceIn(1, 10)

        val raw = client.generateContent(
            Prompts.quiz(safeText, safeCount),
            jsonMode = true,
            maxOutputTokens = 3000
        )

        parseQuiz(raw)
    }

    // =====================================================
    // GENERATE FLASHCARDS
    // =====================================================

    suspend fun generateFlashcards(
        text: String,
        count: Int = 12
    ): AiResult<List<Flashcard>> = call(NORMAL_TIMEOUT) {

        require(text.isNotBlank()) {
            "Materi tidak memiliki teks."
        }

        val safeText = text.take(MAX_FLASHCARD_TEXT)
        val safeCount = count.coerceIn(1, 12)

        val raw = client.generateContent(
            Prompts.flashcards(safeText, safeCount),
            jsonMode = true,
            maxOutputTokens = 3000
        )

        parseFlashcards(raw)
    }

    // =====================================================
    // CHAT AI
    // =====================================================

    suspend fun ask(
        text: String,
        question: String
    ): AiResult<String> = call(NORMAL_TIMEOUT) {

        require(text.isNotBlank()) {
            "Materi tidak memiliki teks."
        }

        require(question.isNotBlank()) {
            "Pertanyaan tidak boleh kosong."
        }

        val safeText = text.take(MAX_CHAT_TEXT)

        client.generateContent(
            Prompts.chat(safeText, question),
            maxOutputTokens = 1500
        )
    }

    // =====================================================
    // JOURNAL REFERENCES
    // =====================================================

    suspend fun suggestReferences(
        text: String
    ): AiResult<List<String>> = call(NORMAL_TIMEOUT) {

        require(text.isNotBlank()) {
            "Materi tidak memiliki teks."
        }

        val safeText = text.take(MAX_REFERENCE_TEXT)

        val raw = client.generateContent(
            Prompts.references(safeText),
            jsonMode = true,
            maxOutputTokens = 1000
        )

        parseKeywords(raw)
    }

    // =====================================================
    // TASK PRIORITIZATION
    // =====================================================

    suspend fun getTaskPrioritizationAdvice(
        tasks: List<com.kuliahku.data.model.Task>
    ): AiResult<String> = call(NORMAL_TIMEOUT) {

        require(tasks.isNotEmpty()) {
            "Belum ada tugas untuk dianalisis."
        }

        val tasksArray = JSONArray()

        tasks.take(30).forEach { task ->

            val item = JSONObject()
                .put("title", task.title)
                .put(
                    "deadline",
                    task.deadline?.toDate()?.toString()
                        ?: JSONObject.NULL
                )
                .put("priority", task.priority)

            tasksArray.put(item)
        }

        client.generateContent(
            Prompts.taskPrioritization(
                tasksArray.toString()
            ),
            maxOutputTokens = 1500
        )
    }

    // =====================================================
    // FINANCE INSIGHT
    // =====================================================

    suspend fun getFinanceInsight(
        summary: com.kuliahku.util.MonthlySummary
    ): AiResult<String> = call(NORMAL_TIMEOUT) {

        val json = JSONObject()
            .put("totalIncome", summary.totalIncome)
            .put("totalExpense", summary.totalExpense)
            .toString()

        client.generateContent(
            Prompts.financeInsight(json),
            maxOutputTokens = 1500
        )
    }

    // =====================================================
    // CALL HANDLER
    // =====================================================

    private suspend fun <T> call(
        timeoutMillis: Long = NORMAL_TIMEOUT,
        block: suspend () -> T
    ): AiResult<T> {

        return try {

            val result = withTimeout(timeoutMillis) {
                block()
            }

            AiResult.Success(result)

        } catch (e: TimeoutCancellationException) {

            Log.e(
                "AiRepository",
                "AI request timeout setelah $timeoutMillis ms",
                e
            )

            AiResult.Timeout

        } catch (e: CancellationException) {

            throw e

        } catch (e: Exception) {

            Log.e(
                "AiRepository",
                "AI Error: ${e.message}",
                e
            )

            AiResult.Error(mapError(e))
        }
    }

    // =====================================================
    // PARSE QUIZ
    // =====================================================

    private fun parseQuiz(
        raw: String
    ): List<QuizQuestion> {

        val json = JSONObject(cleanJson(raw))
        val array = json.getJSONArray("questions")

        val result = mutableListOf<QuizQuestion>()

        for (i in 0 until array.length()) {

            val item = array.getJSONObject(i)
            val optionsArray = item.getJSONArray("options")

            val options = (0 until optionsArray.length())
                .map { index ->
                    optionsArray.getString(index)
                }

            val correctIndex = item.getInt("correctIndex")

            if (
                options.isEmpty() ||
                correctIndex !in options.indices
            ) {
                continue
            }

            result.add(
                QuizQuestion(
                    question = item.getString("question"),
                    options = options,
                    correctIndex = correctIndex,
                    explanation = item.optString("explanation")
                )
            )
        }

        return result
    }

    // =====================================================
    // PARSE FLASHCARDS
    // =====================================================

    private fun parseFlashcards(
        raw: String
    ): List<Flashcard> {

        val json = JSONObject(cleanJson(raw))
        val array = json.getJSONArray("cards")

        val result = mutableListOf<Flashcard>()

        for (i in 0 until array.length()) {

            val item = array.getJSONObject(i)

            val front = item.optString("front")
            val back = item.optString("back")

            if (front.isBlank() || back.isBlank()) {
                continue
            }

            result.add(
                Flashcard(
                    front,
                    back
                )
            )
        }

        return result
    }

    // =====================================================
    // PARSE KEYWORDS
    // =====================================================

    private fun parseKeywords(
        raw: String
    ): List<String> {

        val json = JSONObject(cleanJson(raw))
        val array = json.getJSONArray("keywords")

        val result = mutableListOf<String>()

        for (i in 0 until array.length()) {

            val keyword = array.optString(i).trim()

            if (keyword.isNotBlank()) {
                result.add(keyword)
            }
        }

        return result.distinct()
    }

    // =====================================================
    // CLEAN JSON
    // =====================================================

    private fun cleanJson(raw: String): String {

        return raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    // =====================================================
    // ERROR HANDLER
    // =====================================================

    private fun mapError(
        e: Exception
    ): String {

        val message = e.message.orEmpty()

        return when {

            e is UnknownHostException ||
                    message.contains("Unable to resolve host", true) ->
                "Tidak dapat terhubung ke internet. Periksa koneksi kamu."

            e is SocketTimeoutException ||
                    message.contains("timeout", true) ||
                    message.contains("timed out", true) ->
                "Server Gemini terlalu lama merespons. Silakan coba lagi."

            e is IOException ->
                "Koneksi ke server Gemini terputus. Periksa internet dan coba lagi."

            message.contains("API_KEY_INVALID", true) ->
                "API Key Gemini tidak valid."

            message.contains("PERMISSION_DENIED", true) ->
                "Akses Gemini API ditolak. Periksa API Key dan pengaturan API."

            message.contains("RESOURCE_EXHAUSTED", true) ||
                    message.contains("quota", true) ->
                "Kuota Gemini API telah tercapai."

            message.contains("NOT_FOUND", true) ->
                "Model Gemini tidak ditemukan. Periksa nama model."

            message.contains("UNAUTHENTICATED", true) ->
                "Autentikasi Gemini gagal. Periksa API Key."

            else ->
                "Gagal merespons: ${message.take(250)}"
        }
    }
}
