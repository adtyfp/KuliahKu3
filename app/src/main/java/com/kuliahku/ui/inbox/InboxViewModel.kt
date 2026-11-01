package com.kuliahku.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuliahku.data.model.AppNotification
import com.kuliahku.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxUiState(
    val items: List<AppNotification> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repo: NotificationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeAll()
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { list -> _uiState.update { it.copy(items = list, isLoading = false) } }
        }
    }

    private fun it2(@Suppress("UNUSED_PARAMETER") list: List<AppNotification>): List<AppNotification> = list

    fun markRead(id: String) = viewModelScope.launch { repo.markRead(id) }
    fun markAllRead() = viewModelScope.launch { repo.markAllRead() }
    fun delete(id: String) = viewModelScope.launch { repo.delete(id) }
}