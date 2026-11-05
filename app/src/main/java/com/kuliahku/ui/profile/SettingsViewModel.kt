package com.kuliahku.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuliahku.data.datastore.AppPreferences
import com.kuliahku.data.model.AuthResult
import com.kuliahku.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val deletePassword: String = "",
    val isChanging: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val message: String? = null,
    
    // UI Prefs
    val themeMode: String = "system",
    val themeColor: String = "#0080FF",
    val fontSize: String = "medium",
    val uiStyle: String = "soft",
    val appLanguage: String = "id"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                appPreferences.themeMode,
                appPreferences.themeColor,
                appPreferences.fontSize,
                appPreferences.uiStyle,
                appPreferences.appLanguage
            ) { mode, color, size, style, lang ->
                _uiState.update { it.copy(
                    themeMode = mode,
                    themeColor = color,
                    fontSize = size,
                    uiStyle = style,
                    appLanguage = lang
                ) }
            }.collect()
        }
    }

    fun onThemeModeChange(v: String) { viewModelScope.launch { appPreferences.setThemeMode(v) } }
    fun onThemeColorChange(v: String) { viewModelScope.launch { appPreferences.setThemeColor(v) } }
    fun onFontSizeChange(v: String) { viewModelScope.launch { appPreferences.setFontSize(v) } }
    fun onUiStyleChange(v: String) { viewModelScope.launch { appPreferences.setUiStyle(v) } }
    fun onLanguageChange(v: String) { viewModelScope.launch { appPreferences.setLanguage(v) } }

    fun onCurrentPasswordChange(v: String) { _uiState.update { it.copy(currentPassword = v) } }
    fun onNewPasswordChange(v: String) { _uiState.update { it.copy(newPassword = v) } }
    fun onDeletePasswordChange(v: String) { _uiState.update { it.copy(deletePassword = v) } }
    fun showDeleteDialog(show: Boolean) { _uiState.update { it.copy(showDeleteDialog = show) } }

    fun changePassword() {
        val s = _uiState.value
        if (s.newPassword.length < 8) { _uiState.update { it.copy(message = "Password baru minimal 8 karakter.") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isChanging = true, message = null) }
            when (val r = authRepo.changePassword(s.currentPassword, s.newPassword)) {
                is AuthResult.Success -> _uiState.update { it.copy(isChanging = false, message = "Password berhasil diubah.", currentPassword = "", newPassword = "") }
                is AuthResult.Error -> _uiState.update { it.copy(isChanging = false, message = r.message) }
            }
        }
    }

    fun deleteAccount() {
        val pwd = _uiState.value.deletePassword
        if (pwd.isBlank()) { _uiState.update { it.copy(message = "Password wajib diisi.") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, message = null) }
            when (val r = authRepo.deleteAccount(pwd)) {
                is AuthResult.Success -> {
                    appPreferences.setLoggedIn(false)
                    _uiState.update { it.copy(isDeleting = false, showDeleteDialog = false, message = "Akun dihapus.") }
                }
                is AuthResult.Error -> _uiState.update { it.copy(isDeleting = false, message = r.message) }
            }
        }
    }
}