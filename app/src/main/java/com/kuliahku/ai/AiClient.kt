
package com.kuliahku.ai

import android.util.Base64
import com.kuliahku.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiClient @Inject constructor() {

    companion object {
        const val MODEL_NAME = "gemini-3.1-flash-lite"
        const val LABEL_AI = "Dibuat oleh Gemini AI"

        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/"

        // Maksimal 2 request: 1 utama + 1 retry
        private const val MAX_ATTEMPTS = 2

        private const val CONNECT_TIMEOUT = 20_000
        private const val READ_TIMEOUT = 120_000

        private const val DEFAULT_MAX_OUTPUT_TOKENS = 2048
        private const val JSON_MAX_OUTPUT_TOKENS = 4096
    }

    // =====================================================
    // GENERATE TEXT
    // =====================================================

    suspend fun generateContent(
        prompt: String,
        jsonMode: Boolean = false,
        maxOutputTokens: Int = if (jsonMode) {
            JSON_MAX_OUTPUT_TOKENS
        } else {
            DEFAULT_MAX_OUTPUT_TOKENS
        }
    ): String = withContext(Dispatchers.IO) {

        require(prompt.isNotBlank()) {
            "Prompt Gemini tidak boleh kosong."
        }

        require(maxOutputTokens > 0) {
            "Jumlah token output harus lebih dari 0."
        }

        val generationConfig = JSONObject()
            .put("temperature", if (jsonMode) 0.5 else 0.4)
            .put("maxOutputTokens", maxOutputTokens)

        if (jsonMode) {
            generationConfig.put(
                "responseMimeType",
                "application/json"
            )
        }

        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put("text", prompt)
                            )
                        )
                )
            )
            .put("generationConfig", generationConfig)

        sendRequest(body)
    }

    // =====================================================
    // GENERATE AUDIO
    // =====================================================

    suspend fun generateAudio(
        audioBytes: ByteArray,
        mimeType: String
    ): String = withContext(Dispatchers.IO) {

        require(audioBytes.isNotEmpty()) {
            "File audio kosong."
        }

        require(mimeType.isNotBlank()) {
            "Format audio tidak diketahui."
        }

        val audioBase64 = Base64.encodeToString(
            audioBytes,
            Base64.NO_WRAP
        )

        val parts = JSONArray()
            .put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", mimeType)
                        .put("data", audioBase64)
                )
            )
            .put(
                JSONObject().put(
                    "text",
                    "Dengarkan audio ini dan buat ringkasan materi kuliah secara lengkap dalam bahasa Indonesia."
                )
            )

        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put("parts", parts)
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.4)
                    .put("maxOutputTokens", 2048)
            )

        sendRequest(body)
    }

    // =====================================================
    // SEND REQUEST WITH LIMITED RETRY
    // =====================================================

    private suspend fun sendRequest(
        body: JSONObject
    ): String = withContext(Dispatchers.IO) {

        val apiKey = BuildConfig.GEMINI_API_KEY

        require(apiKey.isNotBlank()) {
            "Gemini API Key belum dikonfigurasi."
        }

        for (attempt in 1..MAX_ATTEMPTS) {

            try {
                return@withContext executeRequest(
                    body = body,
                    apiKey = apiKey
                )

            } catch (e: CancellationException) {
                throw e

            } catch (e: RetryableGeminiException) {

                if (attempt == MAX_ATTEMPTS) {
                    throw Exception(
                        "Gemini sedang sibuk atau batas penggunaan API tercapai. " +
                                "Silakan coba beberapa saat lagi. " +
                                "Detail: ${e.message}",
                        e
                    )
                }

                // Hanya satu kali retry setelah 3 detik.
                delay(3000L)
            }
        }

        throw Exception("Gemini gagal memberikan respons.")
    }

    // =====================================================
    // EXECUTE HTTP REQUEST
    // =====================================================

    private fun executeRequest(
        body: JSONObject,
        apiKey: String
    ): String {

        val url = URL(
            "$BASE_URL$MODEL_NAME:generateContent"
        )

        val connection =
            url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.doOutput = true
            connection.useCaches = false

            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
            )

            connection.setRequestProperty(
                "x-goog-api-key",
                apiKey
            )

            connection.outputStream.use { output ->
                output.write(
                    body.toString().toByteArray(Charsets.UTF_8)
                )
            }

            val statusCode = connection.responseCode

            val responseText = (
                    if (statusCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }
                    )?.bufferedReader()?.use {
                    it.readText()
                }.orEmpty()

            // HTTP ERROR HANDLING

            if (statusCode !in 200..299) {

                val errorMessage = try {
                    JSONObject(responseText)
                        .getJSONObject("error")
                        .optString("message", responseText)
                } catch (_: Exception) {
                    responseText.ifBlank {
                        "Tidak ada detail error dari server."
                    }
                }

                val message =
                    "Gemini API ($statusCode): $errorMessage"

                when (statusCode) {
                    429, 500, 502, 503, 504 -> {
                        throw RetryableGeminiException(message)
                    }

                    else -> {
                        throw Exception(message)
                    }
                }
            }

            // PARSE GEMINI RESPONSE

            val response = JSONObject(responseText)

            val candidates = response.optJSONArray("candidates")
                ?: throw Exception(
                    "Gemini tidak memberikan kandidat jawaban."
                )

            if (candidates.length() == 0) {
                throw Exception(
                    "Jawaban Gemini kosong atau diblokir."
                )
            }

            val candidate = candidates.optJSONObject(0)
                ?: throw Exception(
                    "Format kandidat jawaban tidak valid."
                )

            val content = candidate.optJSONObject("content")
                ?: throw Exception(
                    "Konten jawaban tidak ditemukan. Finish reason: ${
                        candidate.optString("finishReason", "UNKNOWN")
                    }"
                )

            val responseParts = content.optJSONArray("parts")
                ?: throw Exception(
                    "Bagian jawaban tidak ditemukan."
                )

            val result = StringBuilder()

            for (i in 0 until responseParts.length()) {

                val part = responseParts.optJSONObject(i)
                    ?: continue

                val text = part.optString("text", "")

                if (text.isNotBlank()) {
                    result.append(text)
                }
            }

            val finalResult = result.toString().trim()

            if (finalResult.isBlank()) {
                throw Exception(
                    "Gemini mengembalikan jawaban kosong."
                )
            }

            return finalResult

        } finally {
            connection.disconnect()
        }
    }

    // =====================================================
    // RETRYABLE ERROR
    // =====================================================

    private class RetryableGeminiException(
        message: String
    ) : Exception(message)
}
