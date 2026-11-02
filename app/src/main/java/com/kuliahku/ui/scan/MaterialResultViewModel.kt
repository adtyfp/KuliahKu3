
package com.kuliahku.ui.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.kuliahku.ai.AiRepository
import com.kuliahku.ai.AiResult
import com.kuliahku.data.model.Flashcard
import com.kuliahku.data.model.JournalReference
import com.kuliahku.data.model.Material
import com.kuliahku.data.model.Note
import com.kuliahku.data.model.NoteSource
import com.kuliahku.data.model.QuizQuestion
import com.kuliahku.data.repository.JournalRepository
import com.kuliahku.data.repository.MaterialRepository
import com.kuliahku.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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

enum class ResultTab {
    SUMMARY,
    QUIZ,
    FLASHCARD,
    CHAT,
    REFERENCE
}

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
        val id = savedState.get<String>("materialId").orEmpty()

        _uiState.update {
            it.copy(materialId = id)
        }

        if (id.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "ID materi tidak ditemukan."
                )
            }
        } else {
            viewModelScope.launch {
                try {
                    materialRepo.observe(id).collect { material ->
                        _uiState.update {
                            it.copy(
                                material = material,
                                summary = material?.summary,
                                isLoading = false,
                                error = if (material == null) {
                                    "Materi tidak ditemukan."
                                } else {
                                    null
                                }
                            )
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Gagal memuat materi."
                        )
                    }
                }
            }
        }
    }


    fun setTab(tab: ResultTab) {
        _uiState.update {
            it.copy(tab = tab)
        }
    }

    // =====================================================
    // GENERATE SUMMARY
    // =====================================================

    fun generateSummary() {
        val material = _uiState.value.material

        if (material == null) {
            showMessage("Data materi belum tersedia.")
            return
        }

        val text = material.extractedText

        if (text.isNullOrBlank()) {
            if (material.sourceType.equals("youtube", ignoreCase = true)) {
                showMessage(
                    "Teks atau transkrip video YouTube belum tersedia. " +
                            "Ekstrak transkrip video terlebih dahulu agar Gemini dapat merangkumnya."
                )
            } else {
                showMessage("Materi tidak memiliki teks untuk diringkas.")
            }
            return
        }

        runAi("Merangkum materi...") {
            when (val result = ai.summarize(text)) {
                is AiResult.Success -> {
                    _uiState.update {
                        it.copy(summary = result.data)
                    }

                    materialRepo.updateSummary(
                        _uiState.value.materialId,
                        result.data
                    )
                }

                is AiResult.Timeout -> {
                    showMessage("AI timeout. Silakan coba lagi.")
                }

                is AiResult.Error -> {
                    showMessage(result.message)
                }
            }
        }
    }

    // =====================================================
    // GENERATE QUIZ
    // =====================================================

    fun generateQuiz() {
        val text = _uiState.value.material?.extractedText

        if (text.isNullOrBlank()) {
            showMessage("Teks materi belum tersedia untuk membuat kuis.")
            return
        }

        runAi("Membuat kuis...") {
            when (val result = ai.generateQuiz(text, 10)) {
                is AiResult.Success -> {
                    if (result.data.isEmpty()) {
                        showMessage("AI tidak menghasilkan soal. Coba lagi.")
                    } else {
                        _uiState.update {
                            it.copy(
                                quiz = result.data,
                                quizAnswers = emptyMap(),
                                quizSubmitted = false,
                                quizScore = 0
                            )
                        }
                    }
                }

                is AiResult.Timeout -> {
                    showMessage("AI timeout. Silakan coba lagi.")
                }

                is AiResult.Error -> {
                    showMessage(result.message)
                }
            }
        }
    }

    // =====================================================
    // GENERATE FLASHCARDS
    // =====================================================

    fun generateFlashcards() {
        val text = _uiState.value.material?.extractedText

        if (text.isNullOrBlank()) {
            showMessage("Teks materi belum tersedia untuk membuat flashcard.")
            return
        }

        runAi("Membuat flashcard...") {
            when (val result = ai.generateFlashcards(text, 12)) {
                is AiResult.Success -> {
                    if (result.data.isEmpty()) {
                        showMessage("AI tidak menghasilkan flashcard. Coba lagi.")
                    } else {
                        _uiState.update {
                            it.copy(flashcards = result.data)
                        }
                    }
                }

                is AiResult.Timeout -> {
                    showMessage("AI timeout. Silakan coba lagi.")
                }

                is AiResult.Error -> {
                    showMessage(result.message)
                }
            }
        }
    }

    // =====================================================
    // QUIZ ANSWERS
    // =====================================================

    fun selectAnswer(
        questionIndex: Int,
        answerIndex: Int
    ) {
        val state = _uiState.value

        if (state.quizSubmitted) return
        if (questionIndex !in state.quiz.indices) return
        if (answerIndex !in state.quiz[questionIndex].options.indices) return

        _uiState.update {
            it.copy(
                quizAnswers = it.quizAnswers + (
                        questionIndex to answerIndex
                        )
            )
        }
    }

    fun submitQuiz() {
        val state = _uiState.value

        if (state.quiz.isEmpty()) {
            showMessage("Belum ada soal kuis.")
            return
        }

        if (state.quizSubmitted) return

        val score = state.quiz.indices.count { index ->
            state.quizAnswers[index] == state.quiz[index].correctIndex
        }

        _uiState.update {
            it.copy(
                quizSubmitted = true,
                quizScore = score
            )
        }

        viewModelScope.launch {
            try {
                materialRepo.saveQuizAttempt(
                    attemptId = System.currentTimeMillis().toString(),
                    data = mapOf(
                        "materialId" to state.materialId,
                        "score" to score,
                        "total" to state.quiz.size,
                        "answers" to state.quiz.indices.map { index ->
                            state.quizAnswers[index] ?: -1
                        },
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage(
                    e.message ?: "Gagal menyimpan hasil kuis."
                )
            }
        }
    }

    // =====================================================
    // CHAT AI
    // =====================================================

    fun onChatInput(value: String) {
        _uiState.update {
            it.copy(chatInput = value)
        }
    }

    fun sendChat() {
        val state = _uiState.value
        val question = state.chatInput.trim()
        val text = state.material?.extractedText

        if (question.isBlank()) return

        if (text.isNullOrBlank()) {
            showMessage("Teks materi belum tersedia untuk chat AI.")
            return
        }

        _uiState.update {
            it.copy(
                chatInput = "",
                chatMessages = it.chatMessages + ("user" to question)
            )
        }

        runAi("Menjawab pertanyaan...") {
            when (val result = ai.ask(text, question)) {
                is AiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            chatMessages = it.chatMessages + (
                                    "ai" to result.data
                                    )
                        )
                    }
                }

                is AiResult.Timeout -> {
                    showMessage("AI timeout. Silakan kirim pertanyaan lagi.")
                }

                is AiResult.Error -> {
                    showMessage(result.message)
                }
            }
        }
    }

    // =====================================================
    // JOURNAL REFERENCES
    // =====================================================

    fun generateReferences() {
        val text = _uiState.value.material?.extractedText

        if (text.isNullOrBlank()) {
            showMessage("Teks materi belum tersedia untuk mencari referensi.")
            return
        }

        runAi("Mencari referensi jurnal...") {
            when (val result = ai.suggestReferences(text)) {
                is AiResult.Success -> {
                    val keywords = result.data
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .take(5)

                    if (keywords.isEmpty()) {
                        showMessage("AI tidak menemukan kata kunci referensi.")
                        return@runAi
                    }

                    val allResults = mutableListOf<JournalReference>()

                    keywords.forEach { keyword ->
                        val searchResult = journalRepo.search(
                            query = keyword,
                            field = null,
                            yearFrom = null,
                            yearTo = null,
                            onlyOpenAccess = false,
                            sort = "cited_by_count:desc",
                            page = 1,
                            perPage = 3
                        )

                        searchResult.onSuccess { response ->
                            allResults.addAll(response.references)
                        }
                    }

                    val uniqueResults = allResults.distinctBy {
                        it.doi?.takeIf { doi -> doi.isNotBlank() }
                            ?: it.id
                    }

                    _uiState.update {
                        it.copy(suggestedReferences = uniqueResults)
                    }

                    if (uniqueResults.isEmpty()) {
                        showMessage("Referensi jurnal belum ditemukan.")
                    }
                }

                is AiResult.Timeout -> {
                    showMessage("Pencarian referensi timeout. Coba lagi.")
                }

                is AiResult.Error -> {
                    showMessage(result.message)
                }
            }
        }
    }

    // =====================================================
    // SAVE AS NOTE
    // =====================================================

    fun saveAsNote() {
        val state = _uiState.value
        val material = state.material

        if (material == null) {
            showMessage("Data materi belum tersedia.")
            return
        }

        val content = buildString {
            if (!state.summary.isNullOrBlank()) {
                appendLine("RINGKASAN:")
                appendLine(state.summary)
                appendLine()
            }

            if (state.quiz.isNotEmpty()) {
                appendLine("QUIZ (${state.quiz.size} soal):")

                state.quiz.forEachIndexed { index, question ->
                    appendLine("${index + 1}. ${question.question}")

                    question.options.forEach { option ->
                        appendLine("   $option")
                    }

                    appendLine(
                        "   Jawaban: ${
                            question.options.getOrNull(
                                question.correctIndex
                            ) ?: "-"
                        }"
                    )

                    appendLine()
                }
            }

            if (state.flashcards.isNotEmpty()) {
                appendLine("FLASHCARDS:")

                state.flashcards.forEachIndexed { index, card ->
                    appendLine("${index + 1}. ${card.front}")
                    appendLine("   ${card.back}")
                    appendLine()
                }
            }
        }.trim()

        if (content.isBlank()) {
            showMessage(
                "Belum ada ringkasan, kuis, atau flashcard untuk disimpan."
            )
            return
        }

        viewModelScope.launch {
            try {
                val note = Note(
                    title = material.title.ifBlank {
                        "Catatan Materi"
                    },
                    content = content,
                    source = NoteSource.SCAN.value,
                    materialId = material.id
                )

                noteRepo.upsert(note)
                    .onSuccess {
                        _events.send(ResultEvent.SavedNote)
                    }
                    .onFailure {
                        showMessage(
                            it.message ?: "Gagal menyimpan catatan."
                        )
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage(
                    e.message ?: "Terjadi kesalahan saat menyimpan catatan."
                )
            }
        }
    }

    // =====================================================
    // AI PROCESS HANDLER
    // =====================================================

    private fun runAi(
        progressText: String,
        block: suspend () -> Unit
    ) {
        if (_uiState.value.isAiWorking) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAiWorking = true,
                    aiProgress = progressText,
                    error = null
                )
            }

            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage(
                    e.message ?: "Terjadi kesalahan saat menghubungi AI."
                )
            } finally {
                _uiState.update {
                    it.copy(
                        isAiWorking = false,
                        aiProgress = ""
                    )
                }
            }
        }
    }

    // =====================================================
    // MESSAGE HELPER
    // =====================================================

    private fun showMessage(message: String) {
        viewModelScope.launch {
            _events.send(ResultEvent.ShowMessage(message))
        }
    }
}
