package com.kuliahku.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuliahku.data.model.Task
import com.kuliahku.data.model.TaskPriority
import com.kuliahku.data.model.TaskStatus
import com.kuliahku.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TaskFilter(val label: String) {
    ALL("All"), TODAY("Hari ini"), UPCOMING("Akan datang"), DONE("Selesai")
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
    val pendingDelete: Task? = null
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
    private val repo: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private val _events = Channel<TasksEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repo.observeAllTasks()
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { list ->
                    _uiState.update { it.copy(tasks = list, isLoading = false, error = null) }
                    applyFilters()
                }
        }
    }

    fun onFilterChange(f: TaskFilter) {
        _uiState.update { it.copy(filter = f) }
        applyFilters()
    }

    fun onSearchChange(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
        applyFilters()
    }

    fun showFilterSheet(show: Boolean) { _uiState.update { it.copy(showFilterSheet = show) } }

    fun togglePriorityFilter(p: TaskPriority) {
        val set = _uiState.value.priorityFilter.toMutableSet()
        if (set.contains(p)) set.remove(p) else set.add(p)
        _uiState.update { it.copy(priorityFilter = set) }
        applyFilters()
    }

    fun onSortChange(s: TaskSort) {
        _uiState.update { it.copy(sortBy = s) }
        applyFilters()
    }

    fun toggleStatus(task: Task) {
        viewModelScope.launch {
            val done = task.statusEnum == TaskStatus.TODO
            repo.setTaskStatus(task.id, done)
        }
    }

    fun requestDelete(task: Task) { _uiState.update { it.copy(pendingDelete = task) } }

    fun confirmDelete() {
        val task = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            repo.deleteTask(task.id)
                .onSuccess { _events.send(TasksEvent.ShowMessage("Tugas dihapus. Ketuk Undo untuk memulihkan.")) }
                .onFailure { _events.send(TasksEvent.ShowMessage(it.message ?: "Gagal menghapus.")) }
            _uiState.update { it.copy(pendingDelete = null) }
        }
    }

    fun undoDelete(task: Task) {
        viewModelScope.launch {
            repo.restoreTask(task)
                .onFailure { _events.send(TasksEvent.ShowMessage(it.message ?: "Gagal memulihkan.")) }
        }
    }

    private fun applyFilters() {
        val s = _uiState.value
        val now = System.currentTimeMillis()
        var list = s.tasks

        // Chip filter
        list = when (s.filter) {
            TaskFilter.ALL -> list
            TaskFilter.TODAY -> {
                val (start, end) = todayRange()
                list.filter { it.deadline?.toDate()?.time in start..end }
            }
            TaskFilter.UPCOMING -> {
                val (_, end) = todayRange()
                list.filter { it.statusEnum == TaskStatus.TODO && (it.deadline?.toDate()?.time ?: 0L) > end }
            }
            TaskFilter.DONE -> list.filter { it.statusEnum == TaskStatus.DONE }
        }

        // Priority filter
        if (s.priorityFilter.isNotEmpty()) {
            list = list.filter { s.priorityFilter.contains(it.priorityEnum) }
        }

        // Search
        if (s.searchQuery.isNotBlank()) {
            val q = s.searchQuery.lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
            }
        }

        // Sort
        list = when (s.sortBy) {
            TaskSort.DEADLINE_ASC -> list.sortedBy { it.deadline?.toDate()?.time ?: Long.MAX_VALUE }
            TaskSort.PRIORITY_DESC -> list.sortedByDescending {
                when (it.priorityEnum) { TaskPriority.HIGH -> 3; TaskPriority.MEDIUM -> 2; TaskPriority.LOW -> 1 }
            }
            TaskSort.CREATED_DESC -> list.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }

        _uiState.update { it.copy(filtered = list) }
    }

    private fun todayRange(): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0); cal.set(java.util.Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        return start to (cal.timeInMillis - 1)
    }
}