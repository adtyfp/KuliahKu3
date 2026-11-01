package com.kuliahku.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object WebExtractor {

    suspend fun fetch(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val doc: Document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Android) KuliahKu/1.0")
                .timeout(20_000)
                .get()
            clean(doc)
        }
    }

    private fun clean(doc: Document): String {
        // Buang elemen non-konten
        doc.select("script, style, nav, header, footer, aside, noscript, iframe, form, .ads, #ads").remove()
        val title = doc.title()
        val body = doc.body()?.text()?.trim() ?: ""
        return buildString {
            if (title.isNotBlank()) appendLine("Judul: $title").appendLine()
            append(body)
        }
    }
}