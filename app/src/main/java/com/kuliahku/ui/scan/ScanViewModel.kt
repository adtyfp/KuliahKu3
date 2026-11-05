package com.kuliahku.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuliahku.data.model.Material
import com.kuliahku.data.model.MaterialSourceType
import com.kuliahku.data.repository.MaterialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanUiState(
    val isProcessing: Boolean = false,
    val error: String? = null,
    val lastMaterialId: String? = null,
    val history: List<Material> = emptyList()
)

sealed interface ScanEvent {
    data class NavigateToResult(val materialId: String) : ScanEvent
    data class ShowMessage(val message: String) : ScanEvent
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repo: MaterialRepository,
    private val aiRepo: com.kuliahku.ai.AiRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState = _uiState.asStateFlow()
    private val _events = Channel<ScanEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeHistory()
    }

    private fun observeHistory() {
        android.util.Log.d("ScanViewModel", "Memulai observasi riwayat...")
        viewModelScope.launch {
            repo.observeAll().collect { materials ->
                android.util.Log.d("ScanViewModel", "Menerima ${materials.size} materi dari repo")
                _uiState.update { it.copy(history = materials) }
            }
        }
    }

    fun createFromText(title: String, text: String, sourceType: MaterialSourceType = MaterialSourceType.TEXT) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            
            // 1. Generate summary using Gemini
            val aiResult = aiRepo.summarize(text)
            val summary = when (aiResult) {
                is com.kuliahku.ai.AiResult.Success -> aiResult.data
                is com.kuliahku.ai.AiResult.Error -> aiResult.message
                else -> "Gagal merangkum materi secara otomatis."
            }

            val material = Material(
                title = title.ifBlank { "Materi Teks" },
                sourceType = sourceType.value,
                extractedText = text,
                summary = summary
            )

            repo.create(material)
                .onSuccess { id ->
                    _uiState.update { it.copy(isProcessing = false, lastMaterialId = id) }
                    _events.send(ScanEvent.NavigateToResult(id))
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isProcessing = false, error = e.message) }
                    _events.send(ScanEvent.ShowMessage(e.message ?: "Gagal menyimpan."))
                }
        }
    }

    fun createFromYoutube(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            
            val transcriptResult = com.kuliahku.util.YoutubeExtractor.extractTranscript(url)
            
            val materialText = transcriptResult.getOrNull() ?: "URL: $url"
            
            // Jika transcript gagal, kita tetap coba minta Gemini analisis URL (beberapa video didukung langsung)
            // Jika transcript ada, kita kirim transcriptnya agar lebih akurat.
            val prompt = if (transcriptResult.isSuccess) {
                "Ini adalah transkrip dari video YouTube. Tolong buatkan ringkasan materi kuliah yang sangat lengkap, poin-poin penting, dan daftar istilah sulit beserta penjelasannya dalam Bahasa Indonesia:\n\n${transcriptResult.getOrThrow()}"
            } else {
                "Analisis video YouTube ini: $url. Berikan ringkasan materi yang sangat lengkap dalam Bahasa Indonesia."
            }

            val aiResult = aiRepo.summarize(prompt)
            val summary = when (aiResult) {
                is com.kuliahku.ai.AiResult.Success -> aiResult.data
                else -> "Gagal menganalisis video secara otomatis."
            }

            val material = Material(
                title = "Materi YouTube",
                sourceType = MaterialSourceType.YOUTUBE.value,
                sourceRef = url,
                extractedText = materialText,
                summary = summary
            )
            repo.create(material)
                .onSuccess { id ->
                    _uiState.update { it.copy(isProcessing = false) }
                    _events.send(ScanEvent.NavigateToResult(id))
                }
                .onFailure { e -> 
                    _uiState.update { it.copy(isProcessing = false, error = e.message) }
                    _events.send(ScanEvent.ShowMessage("Gagal menyimpan materi: ${e.message}"))
                }
        }
    }

    fun createFromWeb(url: String, text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            
            val aiResult = aiRepo.summarize(text)
            val summary = when (aiResult) {
                is com.kuliahku.ai.AiResult.Success -> aiResult.data
                else -> "Gagal merangkum konten web secara otomatis."
            }

            val material = Material(
                title = "Materi Web",
                sourceType = MaterialSourceType.WEB.value,
                sourceRef = url,
                extractedText = text,
                summary = summary
            )
            repo.create(material)
                .onSuccess { id -> 
                    _uiState.update { it.copy(isProcessing = false) }
                    _events.send(ScanEvent.NavigateToResult(id)) 
                }
                .onFailure { e -> _uiState.update { it.copy(isProcessing = false, error = e.message) } }
        }
    }

    fun createFromPhoto(ocrText: String) {
        createFromText("Materi Foto (OCR)", ocrText, MaterialSourceType.PHOTO)
    }

    fun createFromAudio(audioUri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            
            try {
                val inputStream = context.contentResolver.openInputStream(audioUri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Gagal membaca file audio")
                val mimeType = context.contentResolver.getType(audioUri) ?: "audio/mpeg"
                
                val aiResult = aiRepo.summarizeAudio(bytes, mimeType)
                val summary = when (aiResult) {
                    is com.kuliahku.ai.AiResult.Success -> aiResult.data
                    else -> "Gagal merangkum audio secara otomatis."
                }

                val material = Material(
                    title = "Materi Audio (${java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())})",
                    sourceType = MaterialSourceType.AUDIO.value,
                    summary = summary
                )
                
                repo.create(material)
                    .onSuccess { id ->
                        _uiState.update { it.copy(isProcessing = false) }
                        _events.send(ScanEvent.NavigateToResult(id))
                    }
                    .onFailure { e -> _uiState.update { it.copy(isProcessing = false, error = e.message) } }
                    
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = e.message) }
                _events.send(ScanEvent.ShowMessage("Gagal memproses audio: ${e.message}"))
            }
        }
    }
}