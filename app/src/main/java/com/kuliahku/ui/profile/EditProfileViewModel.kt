
package com.kuliahku.ui.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kuliahku.data.model.User
import com.kuliahku.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// =====================================================
// EDIT PROFILE UI STATE
// =====================================================

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

// =====================================================
// EDIT PROFILE EVENT
// =====================================================

sealed interface EditProfileEvent {
    data object Success : EditProfileEvent
    data class ShowMessage(val message: String) : EditProfileEvent
}

// =====================================================
// EDIT PROFILE VIEW MODEL
// =====================================================

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> =
        _uiState.asStateFlow()

    private val _events = Channel<EditProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    companion object {
        private const val TAG = "EditProfileVM"
        private const val MAX_PHOTO_SIZE = 2 * 1024 * 1024
    }

    init {
        loadUser()
    }

    // =====================================================
    // LOAD USER
    // =====================================================

    private fun loadUser() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    generalError = "Pengguna belum login"
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                val user = userRepo.getUser(uid)

                if (user != null) {
                    _uiState.update {
                        it.copy(
                            user = user,
                            name = user.name,
                            nim = user.nim,
                            prodi = user.prodi,
                            email = user.email,
                            semester = user.semester.toString(),
                            photoUrl = user.photoUrl,
                            isLoading = false,
                            generalError = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = "User tidak ditemukan"
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Gagal memuat profil", e)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        generalError = e.message
                            ?: "Gagal memuat data profil"
                    )
                }
            }
        }
    }

    // =====================================================
    // INPUT HANDLERS
    // =====================================================

    fun onNameChange(value: String) {
        _uiState.update {
            it.copy(
                name = value,
                nameError = null,
                generalError = null
            )
        }
    }

    fun onNimChange(value: String) {
        _uiState.update {
            it.copy(
                nim = value,
                nimError = null,
                generalError = null
            )
        }
    }

    fun onProdiChange(value: String) {
        _uiState.update {
            it.copy(
                prodi = value,
                prodiError = null
            )
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(email = value)
        }
    }

    fun onSemesterChange(value: String) {
        _uiState.update {
            it.copy(
                semester = value,
                semesterError = null
            )
        }
    }

    // =====================================================
    // AVATAR HANDLER
    // =====================================================

    fun onAvatarPicked(context: Context, uri: Uri) {
        _uiState.update {
            it.copy(
                avatarPreview = uri,
                generalError = null
            )
        }

        Log.d(TAG, "Foto profil baru dipilih: $uri")
    }

    // =====================================================
    // SAVE PROFILE
    // =====================================================

    fun save() {
        val state = _uiState.value

        // Cegah penyimpanan berulang
        if (state.isSaving) return

        // =============================================
        // VALIDASI INPUT
        // =============================================

        var hasError = false

        if (state.name.isBlank()) {
            _uiState.update {
                it.copy(nameError = "Nama tidak boleh kosong")
            }
            hasError = true
        }

        if (state.nim.isBlank()) {
            _uiState.update {
                it.copy(nimError = "NIM tidak boleh kosong")
            }
            hasError = true
        }

        if (state.semester.isNotBlank() &&
            state.semester.toIntOrNull() == null
        ) {
            _uiState.update {
                it.copy(
                    semesterError = "Semester harus berupa angka"
                )
            }
            hasError = true
        }

        if (hasError) return

        // =============================================
        // MULAI PROSES PENYIMPANAN
        // =============================================

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isSaving = true,
                    generalError = null
                )
            }

            try {
                val uid = auth.currentUser?.uid
                    ?: throw Exception("Pengguna belum login")

                Log.d(TAG, "Memulai proses penyimpanan profil")

                // Pertahankan URL foto lama jika tidak ada foto baru.
                var newPhotoUrl = state.user?.photoUrl

                // =============================================
                // UPLOAD FOTO BARU KE CLOUDFLARE R2
                // =============================================

                val selectedPhoto = state.avatarPreview

                if (selectedPhoto != null) {

                    Log.d(TAG, "Foto baru terdeteksi: $selectedPhoto")

                    // Baca foto dari URI
                    val bytes = context.contentResolver
                        .openInputStream(selectedPhoto)
                        ?.use { inputStream ->
                            inputStream.readBytes()
                        }
                        ?: throw Exception(
                            "Gagal membaca file gambar"
                        )

                    // Validasi ukuran maksimal 2 MB
                    if (bytes.size > MAX_PHOTO_SIZE) {
                        throw Exception(
                            "Ukuran foto terlalu besar (maksimal 2 MB)"
                        )
                    }

                    // Upload ke Cloudflare R2
                    val uploadResult = userRepo.uploadAvatar(
                        uid = uid,
                        bytes = bytes
                    )

                    // Ambil URL foto dari hasil upload
                    newPhotoUrl = uploadResult.getOrElse { error ->
                        Log.e(TAG, "Upload avatar gagal", error)

                        throw Exception(
                            "Gagal mengunggah ke Cloudflare R2: ${error.message}"
                        )
                    }

                    Log.d(
                        TAG,
                        "Upload avatar berhasil: $newPhotoUrl"
                    )
                }

                // =============================================
                // UPDATE PROFIL KE FIRESTORE
                // =============================================

                Log.d(TAG, "Memperbarui data profil di Firestore")

                val result = userRepo.updateProfile(
                    uid = uid,
                    name = state.name.trim(),
                    nim = state.nim.trim(),
                    prodi = state.prodi.trim(),
                    email = state.email.trim(),
                    semester = state.semester.toIntOrNull()
                        ?: state.user?.semester
                        ?: 1,
                    photoUrl = newPhotoUrl
                )

                // Pastikan Firestore berhasil diperbarui
                result.getOrThrow()

                Log.d(TAG, "Profil berhasil diperbarui")

                // =============================================
                // UPDATE UI STATE
                // =============================================

                _uiState.update { currentState ->
                    currentState.copy(
                        isSaving = false,
                        photoUrl = newPhotoUrl,
                        avatarPreview = null,
                        nameError = null,
                        nimError = null,
                        prodiError = null,
                        semesterError = null,
                        generalError = null
                    )
                }

                // =============================================
                // KIRIM EVENT SUKSES
                // =============================================

                _events.send(EditProfileEvent.Success)

                _events.send(
                    EditProfileEvent.ShowMessage(
                        "Profil berhasil diperbarui"
                    )
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Gagal menyimpan profil: ${e.message}",
                    e
                )

                // =============================================
                // UPDATE UI STATE JIKA GAGAL
                // =============================================

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        generalError = e.message
                            ?: "Terjadi kesalahan saat menyimpan profil"
                    )
                }

                // =============================================
                // KIRIM EVENT ERROR
                // =============================================

                _events.send(
                    EditProfileEvent.ShowMessage(
                        e.message
                            ?: "Terjadi kesalahan saat menyimpan profil"
                    )
                )
            }
        }
    }
}
