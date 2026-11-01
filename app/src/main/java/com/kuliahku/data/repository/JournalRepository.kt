package com.kuliahku.data.repository

import android.util.Log
import com.kuliahku.data.model.JournalReference
import com.kuliahku.data.remote.openalex.OpenAlexApi
import com.kuliahku.data.remote.openalex.OpenAlexMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepository @Inject constructor(
    private val api: OpenAlexApi
) {

    suspend fun search(
        query: String?,
        field: String?,
        yearFrom: Int?,
        yearTo: Int?,
        onlyOpenAccess: Boolean,
        sort: String?,
        page: Int,
        perPage: Int = 20
    ): Result<JournalSearchResult> = runCatching {
        // 1. Coba dengan filter lengkap
        val filters = buildList {
            field?.let { add("primary_topic.field.id:$it") }
            yearFrom?.let { y ->
                yearTo?.let { y2 -> add("publication_year:$y-$y2") } ?: add("publication_year:>$y")
            }
            if (onlyOpenAccess) add("is_oa:true")
            // Hapus filter type:journal-article sementara untuk melihat apakah ada data masuk
        }.joinToString(",").ifBlank { null }

        Log.d("JournalRepo", "Searching with filters: query=$query, filter=$filters, sort=$sort")

        var response = api.searchWorks(
            query = query?.takeIf { it.isNotBlank() },
            filter = filters,
            sort = sort ?: "publication_date:desc", // Default sort jika null
            page = page,
            perPage = perPage
        )

        // 2. Fallback: Jika tidak ada hasil, coba pencarian paling dasar
        if (response.results.isEmpty()) {
            Log.d("JournalRepo", "No results, trying absolute fallback...")
            response = api.searchWorks(
                query = query?.takeIf { it.isNotBlank() },
                filter = null,
                sort = "publication_date:desc",
                page = page,
                perPage = perPage
            )
        }

        Log.d("JournalRepo", "Final Found: ${response.results.size} items")

        JournalSearchResult(
            references = response.results.map { OpenAlexMapper.toReference(it) },
            totalCount = response.meta.count,
            page = response.meta.page,
            hasMore = response.meta.page * response.meta.perPage < response.meta.count
        )
    }
}

data class JournalSearchResult(
    val references: List<JournalReference> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val hasMore: Boolean = false
)

object OpenAlexFields {
    const val COMPUTER_SCIENCE = "17"     // Computer Science
    const val ENGINEERING = "22"          // Engineering
    const val BUSINESS = "14"             // Business, Management & Accounting
    const val MEDICINE = "27"             // Medicine
    const val SOCIAL_SCIENCES = "33"      // Social Sciences
    const val MATHEMATICS = "26"          // Mathematics
}
