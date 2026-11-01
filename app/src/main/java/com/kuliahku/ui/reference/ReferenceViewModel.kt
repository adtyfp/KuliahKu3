package com.kuliahku.ui.reference

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuliahku.data.model.JournalReference
import com.kuliahku.data.repository.BookmarkRepository
import com.kuliahku.data.repository.JournalRepository
import com.kuliahku.data.repository.OpenAlexFields
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReferenceFilter(val label: String, val fieldId: String?, val keyword: String? = null) {
    ALL("All", null),
    INFORMATIKA("Teknik Informatika", OpenAlexFields.COMPUTER_SCIENCE),
    INDUSTRI("Teknik Industri", OpenAlexFields.ENGINEERING),
    BISNIS_DIGITAL("Bisnis Digital", OpenAlexFields.BUSINESS, "Digital Business"),
    MANAJEMEN_RITEL("Manajemen Ritel", OpenAlexFields.BUSINESS, "Retail Management")
}

enum class SortOrder(val label: String, val apiValue: String?) {
    RELEVANCE("Relevan", null),
    NEWEST("Terbaru", "publication_date:desc"),
    MOST_CITED("Terbanyak dikutip", "cited_by_count:desc")
}

data class ReferenceUiState(
    val references: List<JournalReference> = emptyList(),
    val bookmarkedIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedFilter: ReferenceFilter = ReferenceFilter.ALL,
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val onlyOpenAccess: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NEWEST, // Ubah ke NEWEST agar jurnal terbaru muncul duluan
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val page: Int = 1,
    val hasMore: Boolean = true,
    val showFilterSheet: Boolean = false,
    val totalCount: Int = 0
)

sealed interface ReferenceEvent {
    data class ShowMessage(val message: String) : ReferenceEvent
    data class OpenUrl(val url: String) : ReferenceEvent
}

@OptIn(FlowPreview::class)
@HiltViewModel
class ReferenceViewModel @Inject constructor(
    private val repo: JournalRepository,
    private val bookmarkRepo: BookmarkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReferenceUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<ReferenceEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val searchTrigger = MutableSharedFlow<Long>(replay = 0, extraBufferCapacity = 1) // Gunakan Timestamp agar selalu dianggap berubah

    init {
        viewModelScope.launch {
            bookmarkRepo.observeAll().collect { list ->
                _uiState.update { it.copy(bookmarkedIds = list.map { b -> b.id }.toSet()) }
            }
        }
        viewModelScope.launch {
            searchTrigger
                .debounce(400)
                .collect { performSearch(reset = true) } // Hapus distinctUntilChanged
        }
        // Muat awal
        triggerSearch()
    }

    fun onSearchChange(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
        triggerSearch()
    }

    private fun triggerSearch() {
        searchTrigger.tryEmit(System.currentTimeMillis())
    }

    fun setFilter(f: ReferenceFilter) {
        _uiState.update { it.copy(selectedFilter = f) }
        triggerSearch()
    }

    fun setSort(s: SortOrder) {
        _uiState.update { it.copy(sortOrder = s) }
        triggerSearch()
    }

    fun setYearRange(from: Int?, to: Int?) {
        _uiState.update { it.copy(yearFrom = from, yearTo = to) }
        triggerSearch()
    }

    fun setOnlyOpenAccess(only: Boolean) {
        _uiState.update { it.copy(onlyOpenAccess = only) }
        triggerSearch()
    }

    fun showFilterSheet(b: Boolean) { _uiState.update { it.copy(showFilterSheet = b) } }

    fun loadMore() {
        val s = _uiState.value
        if (s.isLoadingMore || !s.hasMore || s.isLoading) return
        viewModelScope.launch { performSearch(reset = false) }
    }

    private suspend fun performSearch(reset: Boolean) {
        val s = _uiState.value
        val nextPage = if (reset) 1 else s.page + 1
        _uiState.update {
            it.copy(
                isLoading = reset,
                isLoadingMore = !reset,
                error = null
            )
        }

        // Gunakan query dari kotak pencarian, jika kosong gunakan keyword default dari filter
        val queryToUse = s.searchQuery.ifBlank { s.selectedFilter.keyword }

        val result = repo.search(
            query = queryToUse,
            field = s.selectedFilter.fieldId,
            yearFrom = s.yearFrom,
            yearTo = s.yearTo,
            onlyOpenAccess = s.onlyOpenAccess,
            sort = s.sortOrder.apiValue,
            page = nextPage,
            perPage = 20
        )
        result.onSuccess { res ->
            _uiState.update {
                it.copy(
                    references = if (reset) res.references else it.references + res.references,
                    page = res.page,
                    hasMore = res.hasMore,
                    totalCount = res.totalCount,
                    isLoading = false,
                    isLoadingMore = false
                )
            }
        }.onFailure { e ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = mapError(e)
                )
            }
            _events.send(ReferenceEvent.ShowMessage(mapError(e)))
        }
    }

    fun toggleBookmark(ref: JournalReference) {
        viewModelScope.launch {
            bookmarkRepo.toggle(ref)
                .onSuccess { added ->
                    _events.send(ReferenceEvent.ShowMessage(
                        if (added) "Disimpan ke bookmark." else "Dihapus dari bookmark."
                    ))
                }
                .onFailure { _events.send(ReferenceEvent.ShowMessage(it.message ?: "Gagal.")) }
        }
    }

    fun openReference(ref: JournalReference) {
        val url = ref.doi?.let { "https://doi.org/$it" }
        if (url != null) {
            viewModelScope.launch { _events.send(ReferenceEvent.OpenUrl(url)) }
        } else {
            viewModelScope.launch { _events.send(ReferenceEvent.ShowMessage("DOI tidak tersedia.")) }
        }
    }

    private fun mapError(e: Throwable): String {
        Log.e("ReferenceVM", "Search Error", e)
        return when {
            e.message?.contains("429", true) == true -> "Kuota harian API tercapai. Coba lagi besok."
            e.message?.contains("timeout", true) == true -> "Koneksi lambat. Coba lagi."
            else -> "Gagal memuat jurnal: ${e.localizedMessage?.take(50) ?: "koneksi bermasalah"}"
        }
    }
}