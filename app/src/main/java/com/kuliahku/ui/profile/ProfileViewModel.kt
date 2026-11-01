package com.kuliahku.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kuliahku.data.datastore.AppPreferences
import com.kuliahku.data.model.User
import com.kuliahku.data.repository.AuthRepository
import com.kuliahku.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showLogoutDialog: Boolean = false
)

sealed interface ProfileEvent {
    data object NavigateToLogin : ProfileEvent
    data class ShowMessage(val message: String) : ProfileEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val userRepo: UserRepository,
    private val auth: FirebaseAuth,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeUser()
    }

    private fun observeUser() {
        auth.currentUser?.uid?.let { uid ->
            viewModelScope.launch {
                userRepo.observeUser(uid)
                    .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                    .collect { user ->
                        _uiState.update { it.copy(user = user, isLoading = false, error = null) }
                    }
            }
        } ?: run {
            _uiState.update { it.copy(isLoading = false, error = "Sesi tidak ditemukan.") }
        }
    }

    fun showLogoutDialog(show: Boolean) { _uiState.update { it.copy(showLogoutDialog = show) } }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
            appPreferences.setLoggedIn(false)
            _events.send(ProfileEvent.NavigateToLogin)
        }
    }
}