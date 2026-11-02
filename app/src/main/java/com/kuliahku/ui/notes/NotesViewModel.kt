package com.kuliahku.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuliahku.data.model.Note
import com.kuliahku.data.model.NoteSource
import com.kuliahku.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NoteLayout { GRID, LIST }
enum class NoteFilter(val label: String) {
    ALL("Semua"), LATEST("Terbaru"), MANUAL("Manual"), SCAN("Scan"), AI("AI")
}

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val filtered: List<Note> = emptyList(),
    val layout: NoteLayout = NoteLayout.GRID,
    val filter: NoteFilter = NoteFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val showFilterMenu: Boolean = false,
    val pendingDelete: Note? = null,
    val selectedNoteIds: Set<String> = emptySet()
)

sealed interface NotesEvent {
    data class ShowMessage(val message: String) : NotesEvent
}

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repo: NoteRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState = _uiState.asStateFlow()
    private val _events = Channel<NotesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repo.observeAll()
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { list ->
                    _uiState.update { it.copy(notes = list, isLoading = false, error = null) }
                    applyFilters()
                }
        }
    }

    fun toggleLayout() {
        _uiState.update {
            it.copy(layout = if (it.layout == NoteLayout.GRID) NoteLayout.LIST else NoteLayout.GRID)
        }
    }

    fun setFilter(f: NoteFilter) { _uiState.update { it.copy(filter = f) }; applyFilters() }
    fun onSearch(q: String) { _uiState.update { it.copy(searchQuery = q) }; applyFilters() }
    fun showFilterMenu(b: Boolean) { _uiState.update { it.copy(showFilterMenu = b) } }
    fun requestDelete(note: Note) { _uiState.update { it.copy(pendingDelete = note) } }

    fun toggleSelection(noteId: String) {
        _uiState.update {
            val current = it.selectedNoteIds
            val next = if (current.contains(noteId)) current - noteId else current + noteId
            it.copy(selectedNoteIds = next)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedNoteIds = emptySet()) }
    }

    private var lastDeletedNotes: List<Note> = emptyList()

    fun deleteSelected() {
        val selectedIds = _uiState.value.selectedNoteIds
        if (selectedIds.isEmpty()) return
        
        val notesToDelete = _uiState.value.notes.filter { it.id in selectedIds }
        lastDeletedNotes = notesToDelete
        
        viewModelScope.launch {
            var success = true
            selectedIds.forEach { id ->
                repo.delete(id).onFailure { success = false }
            }
            
            if (success) {
                val count = selectedIds.size
                _events.send(NotesEvent.ShowMessage("$count Catatan dihapus."))
            } else {
                _events.send(NotesEvent.ShowMessage("Beberapa catatan gagal dihapus."))
            }
            clearSelection()
        }
    }

    fun undoDelete() {
        if (lastDeletedNotes.isEmpty()) return
        viewModelScope.launch {
            lastDeletedNotes.forEach { note ->
                repo.upsert(note)
            }
            lastDeletedNotes = emptyList()
            _events.send(NotesEvent.ShowMessage("Catatan dipulihkan."))
        }
    }

    private fun applyFilters() {
        val s = _uiState.value
        var list = s.notes
        list = when (s.filter) {
            NoteFilter.ALL -> list
            NoteFilter.LATEST -> list.sortedByDescending { it.updatedAt?.toDate()?.time ?: 0L }
            NoteFilter.MANUAL -> list.filter { it.sourceEnum == NoteSource.MANUAL }
            NoteFilter.SCAN -> list.filter { it.sourceEnum == NoteSource.SCAN }
            NoteFilter.AI -> list.filter { it.sourceEnum == NoteSource.AI }
        }
        if (s.searchQuery.isNotBlank()) {
            val q = s.searchQuery.lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
            }
        }
        _uiState.update { it.copy(filtered = list) }
    }
}