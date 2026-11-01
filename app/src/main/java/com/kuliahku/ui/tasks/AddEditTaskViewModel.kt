package com.kuliahku.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.kuliahku.data.model.Task
import com.kuliahku.data.model.TaskPriority
import com.kuliahku.data.repository.TaskRepository
import com.kuliahku.util.DateFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class AddEditTaskUiState(
    val taskId: String? = null,
    val title: String = "",
    val description: String = "",
    val deadlineMillis: Long? = null,
    val deadlineLabel: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val titleError: String? = null,
    val deadlineError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false
)

sealed interface AddEditTaskEvent {
    data object Saved : AddEditTaskEvent
    data class ShowMessage(val message: String) : AddEditTaskEvent
}

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val repo: TaskRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTaskUiState())
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddEditTaskEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        // Kita pakai route arg lewat SubmitTaskScreen; untuk edit, nanti di FASE lanjutan.
        // Di FASE 3 ini layar Tambah Tugas saja.
    }

    fun onTitleChange(v: String) { _uiState.update { it.copy(title = v, titleError = null) } }
    fun onDescriptionChange(v: String) { _uiState.update { it.copy(description = v) } }
    fun onPriorityChange(p: TaskPriority) { _uiState.update { it.copy(priority = p) } }

    fun onDeadlineChange(millis: Long) {
        _uiState.update {
            it.copy(
                deadlineMillis = millis,
                deadlineLabel = DateFormatters.formatMediumDate(java.util.Date(millis)) +
                        " " + DateFormatters.formatTime(java.util.Date(millis)),
                deadlineError = null
            )
        }
    }

    fun save() {
        val s = _uiState.value
        val errors = mutableMapOf<String, String?>()
        if (s.title.isBlank()) errors["title"] = "Nama tugas wajib diisi."
        if (s.deadlineMillis == null) errors["deadline"] = "Deadline wajib diisi."
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(titleError = errors["title"], deadlineError = errors["deadline"]) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val task = Task(
                title = s.title.trim(),
                description = s.description.trim(),
                deadline = Timestamp(java.util.Date(s.deadlineMillis!!)),
                priority = s.priority.value,
                color = 0
            )
            repo.addTask(task)
                .onSuccess { _events.send(AddEditTaskEvent.Saved) }
                .onFailure { _events.send(AddEditTaskEvent.ShowMessage(it.message ?: "Gagal menyimpan.")) }
            _uiState.update { it.copy(isSaving = false) }
        }
    }
}