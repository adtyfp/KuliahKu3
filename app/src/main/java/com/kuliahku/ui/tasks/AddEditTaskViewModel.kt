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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
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
        val taskId = savedState.get<String>("taskId")

        if (!taskId.isNullOrBlank()) {
            loadTask(taskId)
        }
    }

    private fun loadTask(taskId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    taskId = taskId,
                    isEditMode = true,
                    isLoading = true
                )
            }

            try {
                val task = repo.observeTask(taskId).first()

                if (task == null) {
                    _events.send(
                        AddEditTaskEvent.ShowMessage("Tugas tidak ditemukan.")
                    )
                } else {
                    val deadlineMillis = task.deadline?.toDate()?.time

                    _uiState.update {
                        it.copy(
                            taskId = task.id,
                            title = task.title,
                            description = task.description,
                            deadlineMillis = deadlineMillis,
                            deadlineLabel = deadlineMillis?.let { millis ->
                                val date = Date(millis)
                                DateFormatters.formatMediumDate(date) +
                                        " " +
                                        DateFormatters.formatTime(date)
                            } ?: "",
                            priority = TaskPriority.from(task.priority)
                        )
                    }
                }
            } catch (e: Exception) {
                _events.send(
                    AddEditTaskEvent.ShowMessage(
                        e.message ?: "Gagal memuat tugas."
                    )
                )
            } finally {
                _uiState.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update {
            it.copy(
                title = value,
                titleError = null
            )
        }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update {
            it.copy(description = value)
        }
    }

    fun onPriorityChange(priority: TaskPriority) {
        _uiState.update {
            it.copy(priority = priority)
        }
    }

    fun onDeadlineChange(millis: Long) {
        val date = Date(millis)

        _uiState.update {
            it.copy(
                deadlineMillis = millis,
                deadlineLabel = DateFormatters.formatMediumDate(date) +
                        " " +
                        DateFormatters.formatTime(date),
                deadlineError = null
            )
        }
    }

    fun save() {
        val current = _uiState.value

        if (current.isSaving) return

        val titleError = if (current.title.isBlank()) {
            "Nama tugas wajib diisi."
        } else {
            null
        }

        val deadlineError = if (current.deadlineMillis == null) {
            "Deadline wajib diisi."
        } else {
            null
        }

        if (titleError != null || deadlineError != null) {
            _uiState.update {
                it.copy(
                    titleError = titleError,
                    deadlineError = deadlineError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true)
            }

            try {
                val deadline = Timestamp(
                    Date(current.deadlineMillis!!)
                )

                if (current.isEditMode) {
                    // PROSES EDIT TUGAS
                    val taskId = current.taskId

                    if (taskId.isNullOrBlank()) {
                        _events.send(
                            AddEditTaskEvent.ShowMessage(
                                "ID tugas tidak ditemukan."
                            )
                        )
                        return@launch
                    }

                    val result = repo.updateTask(
                        taskId = taskId,
                        updates = mapOf(
                            "title" to current.title.trim(),
                            "description" to current.description.trim(),
                            "deadline" to deadline,
                            "priority" to current.priority.value
                        )
                    )

                    result
                        .onSuccess {
                            _events.send(AddEditTaskEvent.Saved)
                        }
                        .onFailure { error ->
                            _events.send(
                                AddEditTaskEvent.ShowMessage(
                                    error.message ?: "Gagal memperbarui tugas."
                                )
                            )
                        }

                } else {
                    // PROSES TAMBAH TUGAS
                    val result = repo.addTask(
                        Task(
                            title = current.title.trim(),
                            description = current.description.trim(),
                            deadline = deadline,
                            priority = current.priority.value,
                            color = 0
                        )
                    )

                    result
                        .onSuccess {
                            _events.send(AddEditTaskEvent.Saved)
                        }
                        .onFailure { error ->
                            _events.send(
                                AddEditTaskEvent.ShowMessage(
                                    error.message ?: "Gagal menambahkan tugas."
                                )
                            )
                        }
                }
            } catch (e: Exception) {
                _events.send(
                    AddEditTaskEvent.ShowMessage(
                        e.message ?: "Terjadi kesalahan saat menyimpan tugas."
                    )
                )
            } finally {
                _uiState.update {
                    it.copy(isSaving = false)
                }
            }
        }
    }
}