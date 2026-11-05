package com.kuliahku.ui.scan

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.kuliahku.ui.theme.cardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kuliahku.ai.AiClient
import com.kuliahku.data.model.ChatMessage
import com.kuliahku.data.model.Flashcard
import com.kuliahku.data.model.JournalReference
import com.kuliahku.data.model.QuizQuestion
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.TextUnit
import java.text.NumberFormat
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// PALET — sama dengan layar lain (Home, Keuangan, Tugas, Kalender).
// Light : latar #FBFCFF, kartu putih, border #E0E0E0, aksen #0F79DC.
// Dark  : latar #0E1220, kartu #171C2C, border #2A3350, teks #F1F4FF, aksen #3B6FE8.
// ─────────────────────────────────────────────────────────────────────────────
private class ResultPalette(
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentText: Color,
    val accentSoft: Color,
    val success: Color,
    val successSoft: Color,
    val danger: Color,
    val dangerSoft: Color
)

private val LightResultPalette = ResultPalette(
    background = Color(0xFFFBFCFF),
    surface = Color.White,
    border = Color(0xFFE0E0E0),
    textPrimary = Color.Black,
    textSecondary = Color(0xFF767678),
    accent = Color(0xFF0F79DC),
    accentText = Color(0xFF0F79DC),
    accentSoft = Color(0xFFE3EEFB),
    success = Color(0xFF1E9E1E),
    successSoft = Color(0xFFDDF8DD),
    danger = Color(0xFFE02020),
    dangerSoft = Color(0xFFFFE3E3)
)

private val DarkResultPalette = ResultPalette(
    background = Color(0xFF0E1220),
    surface = Color(0xFF171C2C),
    border = Color(0xFF2A3350),
    textPrimary = Color(0xFFF1F4FF),
    textSecondary = Color(0xFFA3ABC4),
    accent = Color(0xFF3B6FE8),
    accentText = Color(0xFF7BA0FF),
    accentSoft = Color(0xFF1C2B55),
    success = Color(0xFF3DD083),
    successSoft = Color(0xFF14362A),
    danger = Color(0xFFFF6B7A),
    dangerSoft = Color(0xFF45202A)
)

@Composable
private fun resultPalette(): ResultPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DarkResultPalette else LightResultPalette

/** Kartu standar: radius 16, border 1 dp, bayangan lembut (sama dengan kartu tugas). */
@Composable
private fun Modifier.card(p: ResultPalette, radius: Dp = 16.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .cardShadow(shapeRadius = radius)
        .background(p.surface, shape)
        .border(1.dp, p.border, shape)
}

private fun tabLabel(t: ResultTab): String = when (t) {
    ResultTab.SUMMARY -> "Ringkasan"
    ResultTab.QUIZ -> "Kuis"
    ResultTab.FLASHCARD -> "Flashcard"
    ResultTab.CHAT -> "Chat"
    ResultTab.REFERENCE -> "Referensi"
    else -> t.name.lowercase().replaceFirstChar { it.uppercase() }
}

@Composable
fun MaterialResultScreen(
    navController: NavHostController,
    viewModel: MaterialResultViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val p = resultPalette()

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
        containerColor = p.background,
        contentColor = p.textPrimary
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // ── Header: chevron + judul materi ──────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 20.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ArrowBackIosNew, "Kembali", Modifier.size(24.dp), tint = p.textPrimary)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    state.material?.title ?: "Hasil Materi",
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Banner AI ───────────────────────────────────────────────────────
            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(p.accentSoft)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Info, null, tint = p.accentText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    AiClient.LABEL_AI,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = p.accentText
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Tab sebagai chip pill (gaya yang sama dengan filter Tugas) ─────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ResultTab.entries.forEach { t ->
                    TabChip(tabLabel(t), selected = state.tab == t, p = p) { viewModel.setTab(t) }
                }
            }

            if (state.isAiWorking) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = p.accent,
                        trackColor = p.border
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(state.aiProgress, fontSize = 12.sp, lineHeight = 16.sp, color = p.textSecondary)
                }
            } else {
                Spacer(Modifier.height(4.dp))
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (state.tab) {
                    ResultTab.SUMMARY -> SummaryTab(
                        summary = state.summary,
                        p = p,
                        onGenerate = viewModel::generateSummary,
                        onSaveNote = viewModel::saveAsNote
                    )
                    ResultTab.QUIZ -> QuizTab(
                        quiz = state.quiz,
                        answers = state.quizAnswers,
                        submitted = state.quizSubmitted,
                        score = state.quizScore,
                        p = p,
                        onSelect = viewModel::selectAnswer,
                        onSubmit = viewModel::submitQuiz,
                        onGenerate = viewModel::generateQuiz
                    )
                    ResultTab.FLASHCARD -> FlashcardTab(
                        cards = state.flashcards,
                        p = p,
                        onGenerate = viewModel::generateFlashcards
                    )
                    ResultTab.CHAT -> ChatTab(
                        messages = state.chatMessages,
                        input = state.chatInput,
                        p = p,
                        onInput = viewModel::onChatInput,
                        onSend = viewModel::sendChat
                    )
                    ResultTab.REFERENCE -> ReferenceTab(
                        references = state.suggestedReferences,
                        p = p,
                        onGenerate = viewModel::generateReferences
                    )
                }
            }
        }
    }
}

// ── Komponen umum ─────────────────────────────────────────────────────────────

@Composable
private fun TabChip(label: String, selected: Boolean, p: ResultPalette, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) p.accent else p.surface)
            .then(if (selected) Modifier else Modifier.border(1.dp, p.border, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else p.textSecondary,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Tombol pill 52 dp, sama dengan tombol Submit di layar form. */
@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    p: ResultPalette,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(p.accent.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

/** Keadaan kosong: ikon di lingkaran lembut, pesan, lalu tombol aksi. */
@Composable
private fun EmptyState(
    icon: ImageVector,
    message: String,
    buttonText: String,
    p: ResultPalette,
    onClick: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(p.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = p.accentText, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            message,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = p.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        ActionButton(buttonText, onClick, p, Modifier.widthIn(max = 280.dp))
    }
}

// ── Ringkasan ─────────────────────────────────────────────────────────────────

@Composable
private fun SummaryTab(summary: String?, p: ResultPalette, onGenerate: () -> Unit, onSaveNote: () -> Unit) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            kotlinx.coroutines.delay(2000)
            isCopied = false
        }
    }

    if (summary.isNullOrBlank()) {
        EmptyState(
            icon = Icons.Filled.AutoAwesome,
            message = "Belum ada ringkasan. Ketuk tombol di bawah untuk membuat ringkasan AI.",
            buttonText = "Buat Ringkasan",
            p = p,
            onClick = onGenerate
        )
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(Modifier.fillMaxWidth().card(p, 20.dp).padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(p.accentSoft),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.AutoAwesome, null, tint = p.accentText, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(10.dp))
                Text("Ringkasan", fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(summary))
                        isCopied = true
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                        contentDescription = "Salin Ringkasan",
                        tint = if (isCopied) p.success else p.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            MarkdownText(
                summary,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = p.textPrimary
            )
        }
        Spacer(Modifier.height(20.dp))
        ActionButton("Simpan sebagai Catatan", onSaveNote, p)
        Spacer(Modifier.height(24.dp))
    }
}

// ── Kuis ──────────────────────────────────────────────────────────────────────

private enum class OptionState { IDLE, SELECTED, CORRECT, WRONG }

@Composable
private fun QuizTab(
    quiz: List<QuizQuestion>,
    answers: Map<Int, Int>,
    submitted: Boolean,
    score: Int,
    p: ResultPalette,
    onSelect: (Int, Int) -> Unit,
    onSubmit: () -> Unit,
    onGenerate: () -> Unit
) {
    if (quiz.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Quiz,
            message = "Belum ada kuis.",
            buttonText = "Buat 10 Soal",
            p = p,
            onClick = onGenerate
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (submitted) {
            item {
                val percent = score * 100 / quiz.size
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(p.accentSoft)
                        .padding(20.dp)
                ) {
                    Text("Skor Kamu", fontSize = 13.sp, lineHeight = 18.sp, color = p.accentText, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$score / ${quiz.size}", fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)
                        Spacer(Modifier.width(10.dp))
                        Text("$percent%", fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, color = p.accentText)
                    }
                    val topics = quiz.filterIndexed { i, q -> answers[i] != q.correctIndex }.map { it.topic }.distinct()
                    if (topics.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Materi yang perlu dipelajari",
                            fontSize = 14.sp, lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold, color = p.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        topics.forEach {
                            Text("• $it", fontSize = 14.sp, lineHeight = 22.sp, color = p.textPrimary)
                        }
                    }
                }
            }
        }
        itemsIndexed(quiz) { i, q ->
            Column(Modifier.fillMaxWidth().card(p).padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(p.accentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${i + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = p.accentText)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        q.question,
                        fontSize = 15.sp, lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold, color = p.textPrimary,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    q.options.forEachIndexed { optIdx, opt ->
                        val selected = answers[i] == optIdx
                        val isCorrect = optIdx == q.correctIndex
                        val optState = when {
                            submitted && isCorrect -> OptionState.CORRECT
                            submitted && selected -> OptionState.WRONG
                            !submitted && selected -> OptionState.SELECTED
                            else -> OptionState.IDLE
                        }
                        OptionRow(opt, optState, p, enabled = !submitted) { onSelect(i, optIdx) }
                    }
                }
            }
        }
        if (!submitted) {
            item {
                Column {
                    Text(
                        "${answers.size} dari ${quiz.size} soal terjawab",
                        fontSize = 12.sp, lineHeight = 16.sp, color = p.textSecondary,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                    )
                    ActionButton("Kumpulkan Jawaban", onSubmit, p, enabled = answers.size == quiz.size)
                }
            }
        }
    }
}

@Composable
private fun OptionRow(text: String, optState: OptionState, p: ResultPalette, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val container = when (optState) {
        OptionState.IDLE -> Color.Transparent
        OptionState.SELECTED -> p.accentSoft
        OptionState.CORRECT -> p.successSoft
        OptionState.WRONG -> p.dangerSoft
    }
    val stroke = when (optState) {
        OptionState.IDLE -> p.border
        OptionState.SELECTED -> p.accent
        OptionState.CORRECT -> p.success
        OptionState.WRONG -> p.danger
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            .border(BorderStroke(1.dp, stroke), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indikator bulat: kosong / terisi / centang / silang
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .then(
                    when (optState) {
                        OptionState.IDLE -> Modifier.border(1.5.dp, p.border, CircleShape)
                        OptionState.SELECTED -> Modifier.background(p.accent)
                        OptionState.CORRECT -> Modifier.background(p.success)
                        OptionState.WRONG -> Modifier.background(p.danger)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (optState) {
                OptionState.SELECTED -> Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                OptionState.CORRECT -> Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                OptionState.WRONG -> Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                OptionState.IDLE -> {}
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 14.sp, lineHeight = 21.sp, color = p.textPrimary)
    }
}

// ── Flashcard ─────────────────────────────────────────────────────────────────

@Composable
private fun FlashcardTab(cards: List<Flashcard>, p: ResultPalette, onGenerate: () -> Unit) {
    if (cards.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Layers,
            message = "Belum ada flashcard.",
            buttonText = "Buat Flashcard",
            p = p,
            onClick = onGenerate
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(cards) { index, c ->
            FlashcardItem(index = index, total = cards.size, card = c, p = p)
        }
    }
}

@Composable
private fun FlashcardItem(index: Int, total: Int, card: Flashcard, p: ResultPalette) {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "flashcardFlip"
    )

    val shape = RoundedCornerShape(20.dp)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .cardShadow(shapeRadius = 20.dp)
            .background(if (rotation > 90f) p.accentSoft else p.surface, shape)
            .border(1.dp, if (rotation > 90f) p.accent.copy(alpha = 0.5f) else p.border, shape)
            .clip(shape)
            .clickable { flipped = !flipped },
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            // Front Side
            FlashcardSide(
                label = "PERTANYAAN",
                content = card.front,
                pageInfo = "${index + 1} / $total",
                isBack = false,
                p = p
            )
        } else {
            // Back Side (flipped again to face forward)
            Box(Modifier.graphicsLayer { rotationY = 180f }) {
                FlashcardSide(
                    label = "JAWABAN",
                    content = card.back,
                    pageInfo = "${index + 1} / $total",
                    isBack = true,
                    p = p
                )
            }
        }
    }
}

@Composable
private fun FlashcardSide(
    label: String,
    content: String,
    pageInfo: String,
    isBack: Boolean,
    p: ResultPalette
) {
    Column(Modifier.padding(20.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                fontSize = 11.sp, lineHeight = 14.sp,
                fontWeight = FontWeight.Bold, color = p.accentText
            )
            Spacer(Modifier.weight(1f))
            Text(pageInfo, fontSize = 12.sp, lineHeight = 16.sp, color = p.textSecondary)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            content,
            fontSize = 16.sp, lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold, color = p.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        Spacer(Modifier.height(14.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.SwapHoriz, null, tint = p.textSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (isBack) "Ketuk untuk kembali" else "Ketuk untuk melihat jawaban",
                fontSize = 12.sp, lineHeight = 16.sp, color = p.textSecondary
            )
        }
    }
}

// ── Chat ──────────────────────────────────────────────────────────────────────

@Composable
private fun ChatTab(
    messages: List<ChatMessage>,
    input: String,
    p: ResultPalette,
    onInput: (String) -> Unit,
    onSend: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        if (messages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Tanyakan apa saja tentang materi ini.",
                    fontSize = 14.sp, lineHeight = 21.sp, color = p.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(messages) { _, msg ->
                    val isUser = msg.role == "user"
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        val shape = RoundedCornerShape(
                            topStart = 18.dp, topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                        Box(
                            Modifier
                                .widthIn(max = 280.dp)
                                .clip(shape)
                                .background(if (isUser) p.accent else p.surface)
                                .then(if (isUser) Modifier else Modifier.border(1.dp, p.border, shape))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            if (isUser) {
                                Text(
                                    msg.content,
                                    fontSize = 14.sp, lineHeight = 21.sp,
                                    color = Color.White
                                )
                            } else {
                                MarkdownText(
                                    msg.content,
                                    fontSize = 14.sp, lineHeight = 21.sp,
                                    color = p.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Kolom input pill + tombol kirim bulat (gaya sama dengan search bar + FAB di Tugas)
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(p.surface)
                    .border(1.dp, p.border, RoundedCornerShape(26.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (input.isEmpty()) {
                    Text("Tanya materi...", fontSize = 15.sp, lineHeight = 22.sp, color = p.textSecondary)
                }
                BasicTextField(
                    value = input,
                    onValueChange = onInput,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp, lineHeight = 22.sp, color = p.textPrimary
                    ),
                    cursorBrush = SolidColor(p.textPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.width(10.dp))
            val canSend = input.isNotBlank()
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(p.accent.copy(alpha = if (canSend) 1f else 0.4f))
                    .clickable(enabled = canSend, onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Kirim", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ── Referensi ─────────────────────────────────────────────────────────────────

@Composable
private fun ReferenceTab(references: List<JournalReference>, p: ResultPalette, onGenerate: () -> Unit) {
    val context = LocalContext.current
    if (references.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Search,
            message = "Temukan referensi jurnal akademik yang relevan dengan materi ini.",
            buttonText = "Cari Referensi AI",
            p = p,
            onClick = onGenerate
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text("Referensi Terkait", fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                Spacer(Modifier.height(2.dp))
                Text(
                    "AI telah merangkum kata kunci dari materi dan mencari jurnal yang relevan.",
                    fontSize = 12.sp, lineHeight = 18.sp, color = p.textSecondary
                )
            }
        }
        itemsIndexed(references) { _, ref ->
            ReferenceItem(ref = ref, p = p) {
                val urlString = ref.doi ?: "https://openalex.org/works/${ref.id}"
                try {
                    val url = urlString.toUri()
                    if (url.scheme != null && url.host != null) {
                        val intent = CustomTabsIntent.Builder().build()
                        intent.launchUrl(context, url)
                    } else {
                        android.widget.Toast.makeText(context, "URL tidak valid", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Tidak dapat membuka link", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
private fun ReferenceItem(ref: JournalReference, p: ResultPalette, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .card(p)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            ref.title,
            fontSize = 15.sp, lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold, color = p.textPrimary,
            maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(p.accent),
                contentAlignment = Alignment.Center
            ) {
                Text(ref.firstAuthorInitial, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                ref.firstAuthor + if (ref.authorCount > 1) " et al." else "",
                fontSize = 13.sp, lineHeight = 18.sp, color = p.textSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(ref.year?.toString() ?: "-", fontSize = 13.sp, lineHeight = 18.sp, color = p.textSecondary)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ThumbUp, null, modifier = Modifier.size(14.dp), tint = p.textSecondary)
            Spacer(Modifier.width(6.dp))
            Text(
                NumberFormat.getIntegerInstance(Locale("id", "ID")).format(ref.citedByCount),
                fontSize = 12.sp, lineHeight = 16.sp, color = p.textSecondary
            )
            Spacer(Modifier.width(12.dp))
            if (ref.isOpenAccess) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(p.successSoft)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("Open Access", fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, color = p.success)
                }
            }
        }
    }
}

@Composable
private fun MarkdownText(
    text: String,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    color: Color,
    modifier: Modifier = Modifier
) {
    val annotatedString = buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            var currentLine = line
            if (currentLine.trimStart().startsWith("* ") || currentLine.trimStart().startsWith("- ")) {
                append("  • ")
                currentLine = currentLine.trimStart().removePrefix("* ").removePrefix("- ")
            }

            val parts = currentLine.split("**")
            parts.forEachIndexed { partIndex, part ->
                if (partIndex % 2 == 1) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(part)
                    }
                } else {
                    append(part)
                }
            }
            if (index < lines.size - 1) append("\n")
        }
    }
    Text(
        text = annotatedString,
        fontSize = fontSize,
        lineHeight = lineHeight,
        color = color,
        modifier = modifier
    )
}