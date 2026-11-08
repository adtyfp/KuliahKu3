package com.kuliahku.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.regex.Pattern

object YoutubeExtractor {

    data class YoutubeData(
        val title: String,
        val description: String,
        val transcript: String?
    )

    /**
     * Mengekstrak data video YouTube (Judul, Deskripsi, Transcript).
     */
    suspend fun extractData(url: String): Result<YoutubeData> = withContext(Dispatchers.IO) {
        runCatching {
            val videoId = extractVideoId(url) ?: throw Exception("URL YouTube tidak valid")
            
            val response = Jsoup.connect("https://www.youtube.com/watch?v=$videoId")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .header("Accept-Language", "id,en-US;q=0.9,en;q=0.8")
                .timeout(15_000)
                .get()

            val title = response.select("meta[name=title]").attr("content").ifBlank { 
                response.title().replace(" - YouTube", "") 
            }
            val description = response.select("meta[name=description]").attr("content")
            
            val html = response.html()
            val transcript = tryExtractTranscript(html)

            YoutubeData(title, description, transcript)
        }
    }

    private fun tryExtractTranscript(html: String): String? {
        return try {
            val regex = Pattern.compile("\"captionTracks\":\\[(.*?)\\]")
            val matcher = regex.matcher(html)
            
            if (!matcher.find()) return null
            
            val tracksJson = matcher.group(1) ?: return null
            val urlRegex = Pattern.compile("\"baseUrl\":\"(.*?)\"")
            val urlMatcher = urlRegex.matcher(tracksJson)
            
            if (!urlMatcher.find()) return null
            
            var transcriptUrl = urlMatcher.group(1)!!
            transcriptUrl = transcriptUrl.replace("\\u0026", "&")
            
            val transcriptDoc = Jsoup.connect(transcriptUrl)
                .userAgent("Mozilla/5.0")
                .timeout(10_000)
                .get()
            
            val textNodes = transcriptDoc.select("text")
            if (textNodes.isEmpty()) return null
            
            val fullText = textNodes.joinToString(" ") { it.text() }
            Jsoup.parse(fullText).text()
        } catch (e: Exception) {
            null
        }
    }

    fun extractVideoId(url: String): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|%2Fv%2F)[^#&?\\n]*"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)
        return if (matcher.find()) matcher.group() else null
    }
}
