package com.kuliahku.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.regex.Pattern

object YoutubeExtractor {

    /**
     * Mengekstrak transcript dari video YouTube berdasarkan URL.
     * Menggunakan metode scraping untuk mencari captionTracks dalam metadata halaman.
     */
    suspend fun extractTranscript(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val videoId = extractVideoId(url) ?: throw Exception("URL YouTube tidak valid")
            
            // 1. Ambil halaman video dengan User Agent browser desktop
            val response = Jsoup.connect("https://www.youtube.com/watch?v=$videoId")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .header("Accept-Language", "id,en-US;q=0.9,en;q=0.8")
                .timeout(15_000)
                .get()
                .html()

            // 2. Cari captionTracks di dalam JSON player response
            // Pola: "captionTracks":[{"baseUrl":"..."
            val regex = Pattern.compile("\"captionTracks\":\\[(.*?)\\]")
            val matcher = regex.matcher(response)
            
            if (!matcher.find()) {
                throw Exception("Transcript tidak tersedia untuk video ini. Pastikan video memiliki subtitle.")
            }
            
            val tracksJson = matcher.group(1) ?: throw Exception("Gagal mengekstrak data transcript")
            
            // Cari baseUrl untuk transcript. Kita ambil yang pertama (biasanya default language)
            val urlRegex = Pattern.compile("\"baseUrl\":\"(.*?)\"")
            val urlMatcher = urlRegex.matcher(tracksJson)
            
            if (!urlMatcher.find()) {
                throw Exception("Gagal menemukan URL transcript dalam metadata")
            }
            
            var transcriptUrl = urlMatcher.group(1)!!
            // Decode unicode escape sequences (\u0026 -> &)
            transcriptUrl = transcriptUrl.replace("\\u0026", "&")
            
            // 3. Ambil isi transcript (format XML dari YouTube)
            val transcriptDoc = Jsoup.connect(transcriptUrl)
                .userAgent("Mozilla/5.0")
                .timeout(10_000)
                .get()
            
            // 4. Parse XML <text> tags
            val textNodes = transcriptDoc.select("text")
            if (textNodes.isEmpty()) {
                throw Exception("Transcript kosong atau gagal dimuat.")
            }
            
            val fullText = textNodes.joinToString(" ") { it.text() }
            
            // Unescape HTML entities (seperti &#39; -> ')
            Jsoup.parse(fullText).text()
        }
    }

    fun extractVideoId(url: String): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|%2Fv%2F)[^#&?\\n]*"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)
        return if (matcher.find()) matcher.group() else null
    }
}
