package com.kuliahku.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuliahku.data.datastore.AppPreferences
import com.kuliahku.data.model.AuthResult
import com.kuliahku.data.repository.AuthRepository
import com.kuliahku.data.repository.UserRepository
import com.kuliahku.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val identifierError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val showResetDialog: Boolean = false,
    val resetEmail: String = "",
    val resetLoading: Boolean = false,
    val resetMessage: String? = null
)

sealed interface LoginEvent {
    data object NavigateToHome : LoginEvent
    data class ShowMessage(val message: String) : LoginEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onIdentifierChange(v: String) {
        _uiState.update { it.copy(identifier = v, identifierError = null, generalError = null) }
        validateIdentifier(v)
    }

    fun onPasswordChange(v: String) {
        _uiState.update { it.copy(password = v, passwordError = null, generalError = null) }
        validatePassword(v)
    }

    private fun validateIdentifier(v: String) {
        val err = when {
            v.isBlank() -> null
            !v.contains("@") && v.length < 5 -> "NIM minimal 5 karakter."
            v.contains("@") && !android.util.Patterns.EMAIL_ADDRESS.matcher(v).matches() -> "Format email tidak valid."
            else -> null
        }
        _uiState.update { it.copy(identifierError = err) }
    }

    private fun validatePassword(v: String) {
        val err = when {
            v.isEmpty() -> null
            v.length < Constants.PASSWORD_MIN_LENGTH -> "Password minimal ${Constants.PASSWORD_MIN_LENGTH} karakter."
            else -> null
        }
        _uiState.update { it.copy(passwordError = err) }
    }

    fun login() {
        val s = _uiState.value
        if (s.identifier.isBlank() || s.password.isBlank()) {
            _uiState.update { it.copy(generalError = "Email/NIM dan password wajib diisi.") }
            return
        }
        if (s.identifierError != null || s.passwordError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            when (val res = authRepo.login(s.identifier, s.password)) {
                is AuthResult.Success -> {
                    appPreferences.setLoggedIn(true)
                    _events.send(LoginEvent.NavigateToHome)
                }
                is AuthResult.Error -> _uiState.update { it.copy(isLoading = false, generalError = res.message) }
            }
        }
    }

    fun showResetDialog(show: Boolean) {
        _uiState.update { it.copy(showResetDialog = show, resetEmail = if (show) it.identifier else "", resetMessage = null) }
    }

    fun onResetEmailChange(v: String) {
        _uiState.update { it.copy(resetEmail = v) }
    }

    fun sendResetEmail() {
        val email = _uiState.value.resetEmail.trim()
        if (email.isBlank() || !email.contains("@")) {
            _uiState.update { it.copy(resetMessage = "Masukkan email yang valid.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(resetLoading = true, resetMessage = null) }
            when (val res = authRepo.resetPassword(email)) {
                is AuthResult.Success -> _uiState.update { it.copy(resetLoading = false, resetMessage = "Email reset dikirim ke $email.") }
                is AuthResult.Error -> _uiState.update { it.copy(resetLoading = false, resetMessage = res.message) }
            }
        }
    }
}