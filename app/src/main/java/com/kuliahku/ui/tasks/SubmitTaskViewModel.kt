package com.kuliahku.ui.tasks

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuliahku.data.model.Task
import com.kuliahku.data.repository.TaskRepository
import com.kuliahku.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubmitTaskUiState(
    val taskId: String = "",
    val task: Task? = null,
    val isLoading: Boolean = true,
    val pickedUri: Uri? = null,
    val pickedName: String = "",
    val pickedSize: Long = 0L,
    val pickedMime: String = "",
    val uploadProgress: Float = 0f,
    val isUploading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

sealed interface SubmitTaskEvent {
    data object Submitted : SubmitTaskEvent
    data class ShowMessage(val message: String) : SubmitTaskEvent
}

@HiltViewModel
class SubmitTaskViewModel @Inject constructor(
    private val repo: TaskRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubmitTaskUiState())
    val uiState: StateFlow<SubmitTaskUiState> = _uiState.asStateFlow()

    private val _events = Channel<SubmitTaskEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var uploadJob: Job? = null

    init {
        val id: String = savedState.get<String>("taskId") ?: ""
        _uiState.update { it.copy(taskId = id) }
        viewModelScope.launch {
            repo.observeTask(id)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { task -> _uiState.update { it.copy(task = task, isLoading = false) } }
        }
    }

    fun onFilePicked(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val cr = context.contentResolver
                val mime = cr.getType(uri) ?: "application/octet-stream"
                val name = queryName(context, uri) ?: "file"
                val size = querySize(context, uri)
                val error = validate(mime, size)
                if (error != null) {
                    _uiState.update { it.copy(error = error) }
                    return@launch
                }
                _uiState.update {
                    it.copy(pickedUri = uri, pickedName = name, pickedSize = size, pickedMime = mime, error = null)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Gagal memuat file: ${e.message}") }
            }
        }
    }

    fun clearFile() {
        _uiState.update { it.copy(pickedUri = null, pickedName = "", pickedSize = 0L, pickedMime = "", uploadProgress = 0f) }
    }

    fun cancelUpload() {
        uploadJob?.cancel()
        _uiState.update { it.copy(isUploading = false, uploadProgress = 0f, message = "Upload dibatalkan.") }
    }

    fun submit() {
        val s = _uiState.value
        val uri = s.pickedUri

        if (uri == null) {
            // Upload opsional, tandai langsung selesai
            viewModelScope.launch {
                _uiState.update { it.copy(isUploading = true, error = null) }
                repo.setTaskStatus(s.taskId, true)
                    .onSuccess { _events.send(SubmitTaskEvent.Submitted) }
                    .onFailure { e ->
                        _uiState.update { it.copy(isUploading = false, error = e.message ?: "Gagal submit.") }
                    }
            }
            return
        }

        uploadJob = viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadProgress = 0f, error = null) }
            val res = repo.uploadSubmission(uri, s.pickedMime, s.pickedName) { progress ->
                _uiState.update { it.copy(uploadProgress = progress) }
            }
            res.onSuccess { (path, type) ->
                repo.markSubmitted(s.taskId, path, type)
                    .onSuccess { _events.send(SubmitTaskEvent.Submitted) }
                    .onFailure { _events.send(SubmitTaskEvent.ShowMessage(it.message ?: "Gagal menandai selesai.")) }
            }.onFailure { e ->
                _uiState.update { it.copy(isUploading = false, error = e.message ?: "Upload gagal.") }
                _events.send(SubmitTaskEvent.ShowMessage("Upload gagal."))
            }
        }
    }

    private fun validate(mime: String, size: Long): String? {
        val allowed = listOf(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        )
        val isImage = mime.startsWith("image/")
        val isVideo = mime.startsWith("video/")
        val isDoc = mime in allowed
        if (!isImage && !isVideo && !isDoc) return "Tipe file tidak didukung. Gunakan PDF, DOCX, PPTX, gambar, atau video."
        val maxDoc = 10L * 1024 * 1024
        val maxMedia = 50L * 1024 * 1024
        if (isDoc && size > maxDoc) return "Ukuran dokumen maksimal 10 MB."
        if ((isImage || isVideo) && size > maxMedia) return "Ukuran media maksimal 50 MB."
        return null
    }

    private fun queryName(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && it.moveToFirst()) return it.getString(idx)
        }
        return null
    }

    private fun querySize(context: Context, uri: Uri): Long {
        val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return 0L
        cursor.use {
            val idx = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (idx >= 0 && it.moveToFirst()) return it.getLong(idx)
        }
        return 0L
    }
}