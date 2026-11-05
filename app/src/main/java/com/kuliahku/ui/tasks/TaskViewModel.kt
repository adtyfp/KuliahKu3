package com.kuliahku.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuliahku.ai.AiRepository
import com.kuliahku.ai.AiResult
import com.kuliahku.data.model.Task
import com.kuliahku.data.model.TaskPriority
import com.kuliahku.data.model.TaskStatus
import com.kuliahku.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class TaskFilter(val label: String) {
    ALL("All"),
    TODAY("Hari ini"),
    UPCOMING("Akan datang"),
    DONE("Selesai")
}

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val filtered: List<Task> = emptyList(),
    val filter: TaskFilter = TaskFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val showFilterSheet: Boolean = false,
    val priorityFilter: Set<TaskPriority> = emptySet(),
    val sortBy: TaskSort = TaskSort.DEADLINE_ASC,
    val pendingDelete: Task? = null,
    val selectedTasks: Set<String> = emptySet(),
    val aiAdvice: String? = null,
    val isAiLoading: Boolean = false
)

enum class TaskSort(val label: String) {
    DEADLINE_ASC("Deadline terdekat"),
    PRIORITY_DESC("Prioritas tertinggi"),
    CREATED_DESC("Terbaru")
}

sealed interface TasksEvent {
    data class ShowMessage(val message: String) : TasksEvent
}

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repo: TaskRepository,
    private val aiRepo: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private val _events = Channel<TasksEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repo.observeAllTasks()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Gagal memuat tugas."
                        )
                    }
                }
                .collect { list ->
                    _uiState.update {
                        it.copy(
                            tasks = list,
                            isLoading = false,
                            error = null
                        )
                    }

                    applyFilters()

                    if (list.isNotEmpty()) {
                        fetchAiAdvice(list)
                    } else {
                        _uiState.update {
                            it.copy(
                                aiAdvice = null,
                                isAiLoading = false
                            )
                        }
                    }
                }
        }
    }

    private fun fetchAiAdvice(tasks: List<Task>) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isAiLoading = true)
            }

            try {
                val result = aiRepo.getTaskPrioritizationAdvice(tasks)

                _uiState.update {
                    it.copy(
                        isAiLoading = false,
                        aiAdvice = if (result is AiResult.Success) {
                            result.data
                        } else {
                            null
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiLoading = false,
                        aiAdvice = null
                    )
                }
            }
        }
    }

    fun onFilterChange(filter: TaskFilter) {
        _uiState.update {
            it.copy(filter = filter)
        }
        applyFilters()
    }

    fun onSearchChange(query: String) {
        _uiState.update {
            it.copy(searchQuery = query)
        }
        applyFilters()
    }

    fun showFilterSheet(show: Boolean) {
        _uiState.update {
            it.copy(showFilterSheet = show)
        }
    }

    fun selectTask(taskId: String) {
        _uiState.update { state ->
            val newSelected = if (state.selectedTasks.contains(taskId)) {
                state.selectedTasks - taskId
            } else {
                state.selectedTasks + taskId
            }
            state.copy(selectedTasks = newSelected)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedTasks = emptySet()) }
    }

    fun togglePriorityFilter(priority: TaskPriority) {
        val priorities = _uiState.value.priorityFilter.toMutableSet()

        if (priorities.contains(priority)) {
            priorities.remove(priority)
        } else {
            priorities.add(priority)
        }

        _uiState.update {
            it.copy(priorityFilter = priorities)
        }

        applyFilters()
    }

    fun onSortChange(sort: TaskSort) {
        _uiState.update {
            it.copy(sortBy = sort)
        }
        applyFilters()
    }

    fun toggleStatus(task: Task) {
        viewModelScope.launch {
            val done = task.statusEnum == TaskStatus.TODO

            repo.setTaskStatus(task.id, done)
                .onFailure { error ->
                    _events.send(
                        TasksEvent.ShowMessage(
                            error.message ?: "Gagal mengubah status tugas."
                        )
                    )
                }
        }
    }

    // Menampilkan dialog konfirmasi hapus
    fun requestDelete(task: Task) {
        _uiState.update {
            it.copy(pendingDelete = task)
        }
    }

    // Membatalkan dialog hapus
    fun requestDeleteCancel() {
        _uiState.update {
            it.copy(pendingDelete = null)
        }
    }

    // Menghapus tugas setelah dikonfirmasi
    fun confirmDelete() {
        val tasksToDelete = if (_uiState.value.selectedTasks.isNotEmpty()) {
            _uiState.value.tasks.filter { _uiState.value.selectedTasks.contains(it.id) }
        } else {
            listOfNotNull(_uiState.value.pendingDelete)
        }

        if (tasksToDelete.isEmpty()) return

        viewModelScope.launch {
            var allSuccess = true
            tasksToDelete.forEach { task ->
                repo.deleteTask(task.id)
                    .onFailure { allSuccess = false }
            }
            
            if (allSuccess) {
                _events.send(TasksEvent.ShowMessage("${tasksToDelete.size} tugas berhasil dihapus."))
            } else {
                _events.send(TasksEvent.ShowMessage("Beberapa tugas gagal dihapus."))
            }

            _uiState.update {
                it.copy(
                    pendingDelete = null,
                    selectedTasks = emptySet()
                )
            }
        }
    }

    // Memulihkan tugas jika fungsi Undo nantinya digunakan
    fun undoDelete(task: Task) {
        viewModelScope.launch {
            repo.restoreTask(task)
                .onSuccess {
                    _events.send(
                        TasksEvent.ShowMessage("Tugas berhasil dipulihkan.")
                    )
                }
                .onFailure { error ->
                    _events.send(
                        TasksEvent.ShowMessage(
                            error.message ?: "Gagal memulihkan tugas."
                        )
                    )
                }
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        var list = state.tasks

        // Filter berdasarkan kategori
        list = when (state.filter) {
            TaskFilter.ALL -> list

            TaskFilter.TODAY -> {
                val (start, end) = todayRange()
                list.filter {
                    val deadline = it.deadline?.toDate()?.time
                    deadline != null && deadline in start..end
                }
            }

            TaskFilter.UPCOMING -> {
                val (_, end) = todayRange()
                list.filter {
                    it.statusEnum == TaskStatus.TODO &&
                            (it.deadline?.toDate()?.time ?: 0L) > end
                }
            }

            TaskFilter.DONE -> {
                list.filter {
                    it.statusEnum == TaskStatus.DONE
                }
            }
        }

        // Filter berdasarkan prioritas
        if (state.priorityFilter.isNotEmpty()) {
            list = list.filter {
                state.priorityFilter.contains(it.priorityEnum)
            }
        }

        // Pencarian tugas
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()

            list = list.filter {
                it.title.lowercase().contains(query) ||
                        it.description.lowercase().contains(query)
            }
        }

        // Pengurutan tugas
        list = when (state.sortBy) {
            TaskSort.DEADLINE_ASC -> {
                list.sortedBy {
                    it.deadline?.toDate()?.time ?: Long.MAX_VALUE
                }
            }

            TaskSort.PRIORITY_DESC -> {
                list.sortedByDescending {
                    when (it.priorityEnum) {
                        TaskPriority.HIGH -> 3
                        TaskPriority.MEDIUM -> 2
                        TaskPriority.LOW -> 1
                    }
                }
            }

            TaskSort.CREATED_DESC -> {
                list.sortedByDescending {
                    it.createdAt?.toDate()?.time ?: 0L
                }
            }
        }

        _uiState.update {
            it.copy(filtered = list)
        }
    }

    private fun todayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val start = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)

        val end = calendar.timeInMillis - 1

        return start to end
    }
}