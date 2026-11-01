package com.kuliahku.ui.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuliahku.data.model.Note
import com.kuliahku.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteEditorUiState(
    val noteId: String = "new",
    val title: String = "",
    val content: String = "",
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val lastSavedAt: Long? = null,
    val error: String? = null
)

sealed interface NoteEditorEvent {
    data object Saved : NoteEditorEvent
    data class ShowMessage(val message: String) : NoteEditorEvent
}

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val repo: NoteRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<NoteEditorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadedAt: Long = 0L

    init {
        val id: String = savedState.get<String>("noteId") ?: "new"
        _uiState.update { it.copy(noteId = id, isNew = id == "new") }
        if (id != "new") load(id)

        // Autosave: 1.5 detik setelah perubahan terakhir
        viewModelScope.launch {
            _uiState
                .map { Triple(it.title, it.content, it.isDirty) }
                .distinctUntilChanged()
                .debounce(1500)
                .collect { (title, content, isDirty) ->
                    if (!isDirty) return@collect
                    if (title.isBlank() && content.isBlank()) return@collect
                    save(silent = true)
                }
        }
    }

    private fun load(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repo.observe(id).first()?.let { note ->
                loadedAt = System.currentTimeMillis()
                _uiState.update {
                    it.copy(
                        title = note.title,
                        content = note.content,
                        isNew = false,
                        isLoading = false,
                        isDirty = false
                    )
                }
            } ?: _uiState.update { it.copy(isLoading = false, error = "Catatan tidak ditemukan.") }
        }
    }

    fun onTitleChange(v: String) { _uiState.update { it.copy(title = v, isDirty = true) } }
    fun onContentChange(v: String) { _uiState.update { it.copy(content = v, isDirty = true) } }

    fun save(silent: Boolean = false, onDone: (() -> Unit)? = null) {
        val s = _uiState.value
        if (s.title.isBlank() && s.content.isBlank()) {
            if (!silent) _events.trySend(NoteEditorEvent.ShowMessage("Catatan kosong, tidak disimpan."))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val note = Note(
                id = if (s.isNew) "" else s.noteId,
                title = s.title.trim(),
                content = s.content
            )
            repo.upsert(note)
                .onSuccess { newId ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isNew = false,
                            noteId = newId,
                            isDirty = false,
                            lastSavedAt = System.currentTimeMillis()
                        )
                    }
                    if (!silent) _events.trySend(NoteEditorEvent.Saved)
                    onDone?.invoke()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                    if (!silent) _events.trySend(NoteEditorEvent.ShowMessage(e.message ?: "Gagal menyimpan."))
                }
        }
    }
}