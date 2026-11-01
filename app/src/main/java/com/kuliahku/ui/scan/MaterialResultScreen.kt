package com.kuliahku.ui.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kuliahku.ai.AiClient
import com.kuliahku.data.model.JournalReference
import com.kuliahku.ui.components.PrimaryPillButton
import com.kuliahku.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialResultScreen(
    navController: NavHostController,
    viewModel: MaterialResultViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                is ResultEvent.ShowMessage -> snackbar.showSnackbar(ev.message)
                ResultEvent.SavedNote -> { snackbar.showSnackbar("Catatan disimpan."); navController.popBackStack() }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(state.material?.title ?: "Hasil Materi", maxLines = 1) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Banner AI
            Surface(color = BlueSoft.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(AiClient.LABEL_AI, style = MaterialTheme.typography.labelMedium, color = Primary)
                }
            }

            // Tabs
            ScrollableTabRow(selectedTabIndex = state.tab.ordinal, edgePadding = 16.dp) {
                ResultTab.entries.forEach { t ->
                    Tab(
                        selected = state.tab == t,
                        onClick = { viewModel.setTab(t) },
                        text = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            if (state.isAiWorking) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(state.aiProgress, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium)
            }

            when (state.tab) {
                ResultTab.SUMMARY -> SummaryTab(
                    summary = state.summary,
                    onGenerate = viewModel::generateSummary,
                    onSaveNote = viewModel::saveAsNote
                )
                ResultTab.QUIZ -> QuizTab(
                    quiz = state.quiz,
                    answers = state.quizAnswers,
                    submitted = state.quizSubmitted,
                    score = state.quizScore,
                    onSelect = viewModel::selectAnswer,
                    onSubmit = viewModel::submitQuiz,
                    onGenerate = viewModel::generateQuiz
                )
                ResultTab.FLASHCARD -> FlashcardTab(
                    cards = state.flashcards,
                    onGenerate = viewModel::generateFlashcards
                )
                ResultTab.CHAT -> ChatTab(
                    messages = state.chatMessages,
                    input = state.chatInput,
                    onInput = viewModel::onChatInput,
                    onSend = viewModel::sendChat
                )
                ResultTab.REFERENCE -> ReferenceTab(
                    references = state.suggestedReferences,
                    onGenerate = viewModel::generateReferences
                )
            }
        }
    }
}

@Composable
private fun SummaryTab(summary: String?, onGenerate: () -> Unit, onSaveNote: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (summary.isNullOrBlank()) {
            Text("Belum ada ringkasan. Ketuk tombol di bawah untuk membuat ringkasan AI.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            PrimaryPillButton("Buat Ringkasan", onGenerate)
        } else {
            Text("Ringkasan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(summary, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            PrimaryPillButton("Simpan sebagai Catatan", onSaveNote)
        }
    }
}

@Composable
private fun QuizTab(
    quiz: List<com.kuliahku.data.model.QuizQuestion>,
    answers: Map<Int, Int>,
    submitted: Boolean,
    score: Int,
    onSelect: (Int, Int) -> Unit,
    onSubmit: () -> Unit,
    onGenerate: () -> Unit
) {
    if (quiz.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Belum ada kuis.")
            Spacer(Modifier.height(16.dp))
            PrimaryPillButton("Buat 10 Soal", onGenerate)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (submitted) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = GreenSoft, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Skor: $score / ${quiz.size} (${(score * 100 / quiz.size)}%)", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        val topics = quiz.filterIndexed { i, q -> answers[i] != q.correctIndex }.map { it.topic }.distinct()
                        if (topics.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Materi yang perlu dipelajari:", style = MaterialTheme.typography.titleMedium)
                            topics.forEach { Text("• $it") }
                        }
                    }
                }
            }
        }
        itemsIndexed(quiz) { i, q ->
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("${i + 1}. ${q.question}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    q.options.forEachIndexed { optIdx, opt ->
                        val selected = answers[i] == optIdx
                        val isCorrect = optIdx == q.correctIndex
                        val color = when {
                            !submitted && selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            submitted && isCorrect -> GreenSoft
                            submitted && selected && !isCorrect -> RedSoft
                            else -> Color.Transparent
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = color,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = !submitted) { onSelect(i, optIdx) }
                        ) {
                            Text(opt, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        if (!submitted) {
            item { PrimaryPillButton("Kumpulkan Jawaban", onSubmit, enabled = answers.size == quiz.size) }
        }
    }
}

@Composable
private fun FlashcardTab(cards: List<com.kuliahku.data.model.Flashcard>, onGenerate: () -> Unit) {
    if (cards.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Belum ada flashcard.")
            Spacer(Modifier.height(16.dp))
            PrimaryPillButton("Buat Flashcard", onGenerate)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(cards) { _, c ->
            var flipped by remember { mutableStateOf(false) }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (flipped) BlueSoft else MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().clickable { flipped = !flipped }
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(if (flipped) c.back else c.front, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(if (flipped) "Ketuk untuk kembali" else "Ketuk untuk melihat jawaban", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun ChatTab(
    messages: List<Pair<String, String>>,
    input: String,
    onInput: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(messages) { _, (role, msg) ->
                val isUser = role == "user"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isUser) Primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Text(
                            msg,
                            Modifier.padding(12.dp),
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = onInput, placeholder = { Text("Tanya materi...") }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.extraLarge)
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = onSend, enabled = input.isNotBlank()) { Icon(Icons.Filled.Send, "Kirim") }
        }
    }
}

@Composable
private fun ReferenceTab(references: List<JournalReference>, onGenerate: () -> Unit) {
    val context = LocalContext.current
    if (references.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Temukan referensi jurnal akademik yang relevan dengan materi ini.", textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            PrimaryPillButton("Cari Referensi AI", onGenerate)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Referensi Terkait", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("AI telah merangkum kata kunci dari materi dan mencari jurnal yang relevan.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
        }
        itemsIndexed(references) { _, ref ->
            ReferenceItem(ref = ref) {
                val url = ref.doi ?: "https://openalex.org/works/${ref.id}"
                val intent = CustomTabsIntent.Builder().build()
                intent.launchUrl(context, url.toUri())
            }
        }
    }
}

@Composable
private fun ReferenceItem(ref: JournalReference, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(ref.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(20.dp).clip(CircleShape).background(Primary), contentAlignment = Alignment.Center) {
                    Text(ref.firstAuthorInitial, color = Color.White, fontSize = 9.sp)
                }
                Spacer(Modifier.width(8.dp))
                Text(ref.firstAuthor + if (ref.authorCount > 1) " et al." else "", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Spacer(Modifier.weight(1f))
                Text(ref.year?.toString() ?: "-", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ThumbUp, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(Modifier.width(4.dp))
                Text(NumberFormat.getIntegerInstance(Locale("id", "ID")).format(ref.citedByCount), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(12.dp))
                if (ref.isOpenAccess) {
                    Surface(color = GreenSoft, shape = RoundedCornerShape(4.dp)) {
                        Text("Open Access", Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}
