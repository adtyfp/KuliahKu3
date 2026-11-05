package com.kuliahku.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kuliahku.data.model.Task
import com.kuliahku.data.model.TaskStatus
import com.kuliahku.data.model.User
import com.kuliahku.data.repository.TaskRepository
import com.kuliahku.data.repository.UserRepository
import com.kuliahku.util.DateFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val user: User? = null,
    val greeting: String = "Pagi",
    val taskProgress: Float = 0f,
    val tasksDone: Int = 0,
    val tasksTotal: Int = 0,
    val motivation: String = "",
    val stats: HomeStats = HomeStats(),
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val unreadNotifications: Int = 0
)

data class HomeStats(
    val total: Int = 0,
    val upcoming: Int = 0,
    val done: Int = 0,
    val overdue: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepo: TaskRepository,
    private val userRepo: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false, error = "Sesi tidak ditemukan.") }
        } else {
            observeUser(uid)
            observeProgress()
            observeStats()
            observeAllTasks()
        }
    }

    private fun observeUser(uid: String) {
        viewModelScope.launch {
            userRepo.observeUser(uid)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { u ->
                    _uiState.update {
                        it.copy(
                            user = u,
                            greeting = DateFormatters.greeting(),
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun observeProgress() {
        viewModelScope.launch {
            taskRepo.observeTotalProgress().collect { (done, total) ->
                val ratio = if (total == 0) 0f else done.toFloat() / total
                _uiState.update {
                    it.copy(
                        tasksDone = done,
                        tasksTotal = total,
                        taskProgress = ratio,
                        motivation = motivationFor(ratio, total)
                    )
                }
            }
        }
    }

    private fun observeStats() {
        viewModelScope.launch {
            taskRepo.observeStats().collect { s ->
                _uiState.update {
                    it.copy(stats = HomeStats(s.total, s.upcoming, s.done, s.overdue))
                }
            }
        }
    }

    private fun observeAllTasks() {
        viewModelScope.launch {
            taskRepo.observeAllTasks().collect { list ->
                val sortedTasks = list.sortedWith(
                    compareBy<Task> { it.statusEnum == TaskStatus.DONE }
                        .thenBy { it.priorityEnum }
                        .thenBy { it.deadline?.seconds ?: Long.MAX_VALUE }
                )
                _uiState.update { it.copy(tasks = sortedTasks) }
            }
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val done = task.statusEnum == TaskStatus.TODO
            taskRepo.setTaskStatus(task.id, done)
        }
    }

    private fun motivationFor(ratio: Float, total: Int): String = when {
        total == 0 -> "Tidak ada tugas hari ini. Nikmati harimu!"
        ratio >= 1f -> "Keren! Semua tugas hari ini selesai 🎉"
        ratio >= 0.5f -> "Sedikit lagi, kamu pasti bisa!"
        ratio > 0f -> "Tetap semangat! Kamu pasti bisa."
        else -> "Ayo mulai kerjakan tugasmu!"
    }
}