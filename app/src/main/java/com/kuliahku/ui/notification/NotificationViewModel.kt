package com.kuliahku.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuliahku.data.model.AppNotification
import com.kuliahku.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    val notifications: StateFlow<List<AppNotification>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markAsRead(id: String) {
        viewModelScope.launch {
            repository.markRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllRead()
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }
}
