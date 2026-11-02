package com.kuliahku.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kuliahku.data.model.User
import com.kuliahku.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val user: User? = null,
    val name: String = "",
    val nim: String = "",
    val prodi: String = "",
    val email: String = "",
    val semester: String = "",
    val photoUrl: String? = null,
    val avatarPreview: Uri? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val nameError: String? = null,
    val nimError: String? = null,
    val prodiError: String? = null,
    val semesterError: String? = null,
    val generalError: String? = null
)

sealed interface EditProfileEvent {
    data object Success : EditProfileEvent
    data class ShowMessage(val message: String) : EditProfileEvent
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<EditProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val user = userRepo.getUser(uid)
            if (user != null) {
                _uiState.update { it.copy(
                    user = user,
                    name = user.name,
                    nim = user.nim,
                    prodi = user.prodi,
                    email = user.email,
                    semester = user.semester.toString(),
                    photoUrl = user.photoUrl,
                    isLoading = false
                ) }
            } else {
                _uiState.update { it.copy(isLoading = false, generalError = "User tidak ditemukan") }
            }
        }
    }

    fun onNameChange(value: String) { _uiState.update { it.copy(name = value, nameError = null) } }
    fun onNimChange(value: String) { _uiState.update { it.copy(nim = value, nimError = null) } }
    fun onProdiChange(value: String) { _uiState.update { it.copy(prodi = value, prodiError = null) } }
    fun onEmailChange(value: String) { _uiState.update { it.copy(email = value) } }
    fun onSemesterChange(value: String) { _uiState.update { it.copy(semester = value, semesterError = null) } }

    fun onAvatarPicked(context: Context, uri: Uri) {
        _uiState.update { it.copy(avatarPreview = uri) }
    }

    fun save() {
        val uid = auth.currentUser?.uid ?: return
        val state = _uiState.value
        
        var hasError = false
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Nama tidak boleh kosong") }
            hasError = true
        }
        if (state.nim.isBlank()) {
            _uiState.update { it.copy(nimError = "NIM tidak boleh kosong") }
            hasError = true
        }
        
        if (hasError) return

        _uiState.update { it.copy(isSaving = true, generalError = null) }
        viewModelScope.launch {
            try {
                android.util.Log.d("EditProfileVM", "Starting save process...")
                // Upload avatar if changed
                state.avatarPreview?.let { uri ->
                    android.util.Log.d("EditProfileVM", "New avatar detected, uploading: $uri")
                    
                    val bytes = try {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: throw Exception("Gagal membaca file gambar")
                    } catch (e: Exception) {
                        android.util.Log.e("EditProfileVM", "Error reading URI", e)
                        throw Exception("File tidak dapat diakses")
                    }
                    
                    if (bytes.size > 2 * 1024 * 1024) { // 2MB Limit
                         throw Exception("Ukuran foto terlalu besar (maks 2MB)")
                    }
                    
                    val uploadResult = userRepo.uploadAvatar(uid, bytes)
                    if (uploadResult.isFailure) {
                        val error = uploadResult.exceptionOrNull()
                        android.util.Log.e("EditProfileVM", "Upload failed", error)
                        throw Exception("Gagal mengunggah ke Cloudflare: ${error?.message}")
                    }
                    android.util.Log.d("EditProfileVM", "Avatar upload success")
                }

                android.util.Log.d("EditProfileVM", "Updating profile info in Firestore...")
                val result = userRepo.updateProfile(
                    uid = uid,
                    name = state.name,
                    nim = state.nim,
                    prodi = state.prodi,
                    email = state.email,
                    semester = state.semester.toIntOrNull() ?: state.user?.semester ?: 1
                )
                
                if (result.isSuccess) {
                    android.util.Log.d("EditProfileVM", "Profile save success")
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(EditProfileEvent.Success)
                    _events.send(EditProfileEvent.ShowMessage("Profil berhasil diperbarui"))
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Gagal menyimpan profil"
                    android.util.Log.e("EditProfileVM", "Update profile failed: $error")
                    throw Exception(error)
                }
            } catch (e: Exception) {
                android.util.Log.e("EditProfileVM", "Error in save(): ${e.message}", e)
                _uiState.update { it.copy(isSaving = false, generalError = e.message) }
                _events.send(EditProfileEvent.ShowMessage(e.message ?: "Terjadi kesalahan"))
            }
        }
    }
}
