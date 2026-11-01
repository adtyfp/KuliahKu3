package com.kuliahku.data.remote.openalex

import com.kuliahku.data.model.JournalReference
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object OpenAlexMapper {

    fun toReference(work: OpenAlexWork): JournalReference {
        val authors = work.authorships?.mapNotNull { it.author?.displayName }?.filter { it.isNotBlank() } ?: emptyList()
        
        val titleResult = when {
            !work.title.isNullOrBlank() -> work.title
            !work.displayName.isNullOrBlank() -> work.displayName
            else -> "Tanpa judul"
        }

        // Cari gambar dari primary_location, jika tidak ada cari di list locations lainnya
        val imageUrlResult = work.primaryLocation?.source?.imageUrl 
            ?: work.locations?.firstOrNull { it.source?.imageUrl != null }?.source?.imageUrl

        return JournalReference(
            id = work.id,
            doi = work.doi?.removePrefix("https://doi.org/"),
            title = titleResult,
            authors = authors,
            authorCount = authors.size,
            year = work.publicationYear,
            citedByCount = work.citedByCount ?: 0,
            isOpenAccess = work.openAccess?.isOpenAccess ?: false,
            field = work.primaryTopic?.field?.displayName ?: work.primaryTopic?.displayName,
            type = work.type,
            imageUrl = imageUrlResult,
            publishedAtMillis = parseDate(work.publicationDate)
        )
    }

    private fun parseDate(s: String?): Long? {
        if (s.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(s)?.time
        } catch (_: Exception) { null }
    }
}
