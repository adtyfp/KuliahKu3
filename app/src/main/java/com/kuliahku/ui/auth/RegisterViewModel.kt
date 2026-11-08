package com.kuliahku.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuliahku.data.datastore.AppPreferences
import com.kuliahku.data.model.AuthResult
import com.kuliahku.data.repository.AuthRepository
import com.kuliahku.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val nim: String = "",
    val semester: String = "",
    val prodi: String = "",
    val isLoading: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val nimError: String? = null,
    val semesterError: String? = null,
    val prodiError: String? = null,
    val generalError: String? = null,
    val isProdiExpanded: Boolean = false,
    val prodiOptions: List<String> = listOf(
        "Teknik Informatika", "Teknik Industri", "Manajemen",
        "Akuntansi", "Sistem Informasi", "Teknik Elektro", "Lainnya"
    )
)

sealed interface RegisterEvent {
    data object NavigateToHome : RegisterEvent
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _events = Channel<RegisterEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onNameChange(v: String) { _uiState.update { it.copy(name = v, nameError = null, generalError = null) } }
    fun onEmailChange(v: String) {
        _uiState.update { it.copy(email = v, emailError = null, generalError = null) }
        if (v.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(v).matches())
            _uiState.update { it.copy(emailError = "Format email tidak valid.") }
    }
    fun onPasswordChange(v: String) {
        _uiState.update { it.copy(password = v, passwordError = null, generalError = null) }
        if (v.isNotEmpty() && v.length < Constants.PASSWORD_MIN_LENGTH)
            _uiState.update { it.copy(passwordError = "Password minimal ${Constants.PASSWORD_MIN_LENGTH} karakter.") }
    }
    fun onNimChange(v: String) {
        val filtered = v.filter { it.isDigit() }
        _uiState.update { it.copy(nim = filtered, nimError = null, generalError = null) }
        if (filtered.isNotEmpty() && filtered.length < 5)
            _uiState.update { it.copy(nimError = "NIM minimal 5 digit.") }
    }
    fun onSemesterChange(v: String) {
        val filtered = v.filter { it.isDigit() }
        _uiState.update { it.copy(semester = filtered, semesterError = null, generalError = null) }
        val num = filtered.toIntOrNull()
        if (filtered.isNotEmpty() && (num == null || num < Constants.SEMESTER_MIN || num > Constants.SEMESTER_MAX))
            _uiState.update { it.copy(semesterError = "Semester ${Constants.SEMESTER_MIN}-${Constants.SEMESTER_MAX}.") }
    }
    fun onProdiChange(v: String) { _uiState.update { it.copy(prodi = v, prodiError = null, generalError = null) } }
    fun toggleProdiDropdown() { _uiState.update { it.copy(isProdiExpanded = !it.isProdiExpanded) } }

    fun register() {
        val s = _uiState.value
        val errors = mutableMapOf<String, String?>()
        if (s.name.isBlank()) errors["name"] = "Nama wajib diisi."
        if (s.email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(s.email).matches()) errors["email"] = "Email valid wajib diisi."
        if (s.password.length < Constants.PASSWORD_MIN_LENGTH) errors["password"] = "Password minimal ${Constants.PASSWORD_MIN_LENGTH} karakter."
        if (s.nim.isBlank()) errors["nim"] = "NIM wajib diisi."
        val sem = s.semester.toIntOrNull()
        if (sem == null || sem < Constants.SEMESTER_MIN || sem > Constants.SEMESTER_MAX) errors["semester"] = "Semester ${Constants.SEMESTER_MIN}-${Constants.SEMESTER_MAX}."
        if (s.prodi.isBlank()) errors["prodi"] = "Program studi wajib diisi."

        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    nameError = errors["name"], emailError = errors["email"],
                    passwordError = errors["password"], nimError = errors["nim"],
                    semesterError = errors["semester"], prodiError = errors["prodi"]
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            when (val res = authRepo.register(
                name = s.name.trim(), email = s.email.trim(), password = s.password,
                nim = s.nim.trim(), semester = sem!!, prodi = s.prodi
            )) {
                is AuthResult.Success -> {
                    appPreferences.setLoggedIn(true)
                    _events.send(RegisterEvent.NavigateToHome)
                }
                is AuthResult.Error -> _uiState.update { it.copy(isLoading = false, generalError = res.message) }
            }
        }
    }
}