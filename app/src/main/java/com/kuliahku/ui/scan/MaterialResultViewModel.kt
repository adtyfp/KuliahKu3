package com.kuliahku.ui.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuliahku.ai.AiRepository
import com.kuliahku.ai.AiResult
import com.kuliahku.data.model.Flashcard
import com.kuliahku.data.model.Material
import com.kuliahku.data.model.QuizAttempt
import com.kuliahku.data.model.QuizQuestion
import com.kuliahku.data.repository.MaterialRepository
import com.kuliahku.data.repository.NoteRepository
import com.kuliahku.data.repository.JournalRepository
import com.kuliahku.data.model.JournalReference
import com.kuliahku.data.model.Note
import com.kuliahku.data.model.NoteSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultUiState(
    val materialId: String = "",
    val material: Material? = null,
    val isLoading: Boolean = true,
    val isAiWorking: Boolean = false,
    val aiProgress: String = "",
    val error: String? = null,
    val summary: String? = null,
    val quiz: List<QuizQuestion> = emptyList(),
    val flashcards: List<Flashcard> = emptyList(),
    val quizAnswers: Map<Int, Int> = emptyMap(),
    val quizSubmitted: Boolean = false,
    val quizScore: Int = 0,
    val chatMessages: List<Pair<String, String>> = emptyList(),
    val chatInput: String = "",
    val suggestedReferences: List<JournalReference> = emptyList(),
    val tab: ResultTab = ResultTab.SUMMARY
)

enum class ResultTab { SUMMARY, QUIZ, FLASHCARD, CHAT, REFERENCE }

sealed interface ResultEvent {
    data class ShowMessage(val message: String) : ResultEvent
    data object SavedNote : ResultEvent
}

@HiltViewModel
class MaterialResultViewModel @Inject constructor(
    private val materialRepo: MaterialRepository,
    private val noteRepo: NoteRepository,
    private val journalRepo: JournalRepository,
    private val ai: AiRepository,
    savedState: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState = _uiState.asStateFlow()
    private val _events = Channel<ResultEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        val id: String = savedState.get<String>("materialId") ?: ""
        _uiState.update { it.copy(materialId = id) }
        viewModelScope.launch {
            materialRepo.observe(id).collect { m ->
                _uiState.update { it.copy(material = m, summary = m?.summary, isLoading = false) }
            }
        }
    }

    fun setTab(tab: ResultTab) { _uiState.update { it.copy(tab = tab) } }

    fun generateSummary() {
        val text = _uiState.value.material?.extractedText
        if (text.isNullOrBlank()) {
            // Kalau YouTube, kirim URL ke model (didukung langsung)
            if (_uiState.value.material?.sourceType == "youtube") {
                generateSummaryFromYoutube()
            } else {
                viewModelScope.launch { _events.send(ResultEvent.ShowMessage("Materi tidak memiliki teks untuk diringkas.")) }
            }
            return
        }
        runAi("Merangkum materi...") {
            when (val r = ai.summarize(text)) {
                is AiResult.Success -> {
                    _uiState.update { it.copy(summary = r.data) }
                    materialRepo.updateSummary(_uiState.value.materialId, r.data)
                }
                is AiResult.Timeout -> _events.send(ResultEvent.ShowMessage("AI timeout. Coba lagi."))
                is AiResult.Error -> _events.send(ResultEvent.ShowMessage(r.message))
            }
        }
    }

    private fun generateSummaryFromYoutube() {
        // Firestore simpan URL, kita kirim lewat prompt khusus (model bisa proses URL YouTube)
        val url = _uiState.value.material?.sourceRef ?: return
        runAi("Menganalisis YouTube...") {
            // Panggil model dengan URL sebagai bagian dari prompt
            // Implementasi: gunakan client.get().generateContent dengan prompt berisi URL
            when (val r = ai.summarize("Analisis video berikut: $url")) {
                is AiResult.Success -> {
                    _uiState.update { it.copy(summary = r.data) }
                    materialRepo.updateSummary(_uiState.value.materialId, r.data)
                }
                is AiResult.Timeout -> _events.send(ResultEvent.ShowMessage("AI timeout. Coba lagi."))
                is AiResult.Error -> _events.send(ResultEvent.ShowMessage(r.message))
            }
        }
    }

    fun generateQuiz() {
        val text = _uiState.value.material?.extractedText ?: return
        runAi("Membuat kuis...") {
            when (val r = ai.generateQuiz(text, 10)) {
                is AiResult.Success -> _uiState.update { it.copy(quiz = r.data, quizAnswers = emptyMap(), quizSubmitted = false) }
                is AiResult.Timeout -> _events.send(ResultEvent.ShowMessage("AI timeout."))
                is AiResult.Error -> _events.send(ResultEvent.ShowMessage(r.message))
            }
        }
    }

    fun generateFlashcards() {
        val text = _uiState.value.material?.extractedText ?: return
        runAi("Membuat flashcard...") {
            when (val r = ai.generateFlashcards(text, 12)) {
                is AiResult.Success -> _uiState.update { it.copy(flashcards = r.data) }
                is AiResult.Timeout -> _events.send(ResultEvent.ShowMessage("AI timeout."))
                is AiResult.Error -> _events.send(ResultEvent.ShowMessage(r.message))
            }
        }
    }

    fun selectAnswer(questionIndex: Int, answerIndex: Int) {
        _uiState.update { it.copy(quizAnswers = it.quizAnswers + (questionIndex to answerIndex)) }
    }

    fun submitQuiz() {
        val s = _uiState.value
        val score = s.quiz.indices.count { i -> s.quizAnswers[i] == s.quiz[i].correctIndex }
        _uiState.update { it.copy(quizSubmitted = true, quizScore = score) }
        // Simpan attempt
        viewModelScope.launch {
            materialRepo.saveQuizAttempt(
                attemptId = System.currentTimeMillis().toString(),
                data = mapOf(
                    "materialId" to s.materialId,
                    "score" to score,
                    "total" to s.quiz.size,
                    "answers" to s.quizAnswers.map { it.value },
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
        }
    }

    fun sendChat() {
        val q = _uiState.value.chatInput.trim()
        if (q.isBlank()) return
        val text = _uiState.value.material?.extractedText ?: return
        _uiState.update {
            it.copy(chatInput = "", chatMessages = it.chatMessages + ("user" to q))
        }
        runAi("Menjawab...") {
            when (val r = ai.ask(text, q)) {
                is AiResult.Success -> _uiState.update {
                    it.copy(chatMessages = it.chatMessages + ("ai" to r.data))
                }
                is AiResult.Timeout -> _events.send(ResultEvent.ShowMessage("AI timeout."))
                is AiResult.Error -> _events.send(ResultEvent.ShowMessage(r.message))
            }
        }
    }

    fun onChatInput(v: String) { _uiState.update { it.copy(chatInput = v) } }

    fun generateReferences() {
        val text = _uiState.value.material?.extractedText
        if (text.isNullOrBlank()) return
        runAi("Mencari referensi jurnal...") {
            when (val r = ai.suggestReferences(text)) {
                is AiResult.Success -> {
                    val keywords = r.data
                    val allResults = mutableListOf<JournalReference>()
                    // Ambil 2 teratas untuk setiap keyword untuk keberagaman
                    keywords.forEach { kw ->
                        journalRepo.search(
                            query = kw,
                            field = null,
                            yearFrom = null,
                            yearTo = null,
                            onlyOpenAccess = false,
                            sort = "cited_by_count:desc",
                            page = 1,
                            perPage = 3
                        ).onSuccess { res ->
                            allResults.addAll(res.references)
                        }
                    }
                    _uiState.update { it.copy(suggestedReferences = allResults.distinctBy { it.doi ?: it.id }) }
                }
                is AiResult.Timeout -> _events.send(ResultEvent.ShowMessage("AI timeout."))
                is AiResult.Error -> _events.send(ResultEvent.ShowMessage(r.message))
            }
        }
    }

    fun saveAsNote() {
        val s = _uiState.value
        val m = s.material ?: return
        val content = buildString {
            if (!s.summary.isNullOrBlank()) appendLine("RINGKASAN:").appendLine(s.summary).appendLine()
            if (s.quiz.isNotEmpty()) {
                appendLine("QUIZ (${s.quiz.size} soal):")
                s.quiz.forEachIndexed { i, q ->
                    appendLine("${i + 1}. ${q.question}")
                    q.options.forEach { appendLine("   $it") }
                    appendLine("   Jawaban: ${q.options.getOrNull(q.correctIndex) ?: "-"}")
                }
            }
        }
        viewModelScope.launch {
            val note = Note(
                title = m.title.ifBlank { "Catatan Materi" },
                content = content,
                source = NoteSource.SCAN.value,
                materialId = m.id
            )
            noteRepo.upsert(note)
                .onSuccess { _events.send(ResultEvent.SavedNote) }
                .onFailure { _events.send(ResultEvent.ShowMessage(it.message ?: "Gagal menyimpan catatan.")) }
        }
    }

    private fun runAi(progressText: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiWorking = true, aiProgress = progressText) }
            try { block() } finally { _uiState.update { it.copy(isAiWorking = false, aiProgress = "") } }
        }
    }
}