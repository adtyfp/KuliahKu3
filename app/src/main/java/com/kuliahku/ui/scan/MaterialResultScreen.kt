package com.kuliahku.ui.scan

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kuliahku.ai.AiClient
import com.kuliahku.data.model.ChatMessage
import com.kuliahku.data.model.Flashcard
import com.kuliahku.data.model.JournalReference
import com.kuliahku.data.model.QuizQuestion
import com.kuliahku.ui.theme.cardShadow
import java.text.NumberFormat
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// PALET
// ─────────────────────────────────────────────────────────────────────────────
private class ResultPalette(
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentEnd: Color,
    val accentText: Color,
    val accentSoft: Color,
    val success: Color,
    val successSoft: Color,
    val danger: Color,
    val dangerSoft: Color
) {
    val accentBrush: Brush get() = Brush.linearGradient(listOf(accent, accentEnd))
}

private val LightResultPalette = ResultPalette(
    background = Color(0xFFFBFCFF),
    surface = Color.White,
    border = Color(0xFFE0E0E0),
    textPrimary = Color.Black,
    textSecondary = Color(0xFF767678),
    accent = Color(0xFF0F79DC),
    accentEnd = Color(0xFF4AA3F0),
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
    accentEnd = Color(0xFF6A8DFF),
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

// Ukuran layar yang dibagikan ke semua tab
private val LocalHPad = compositionLocalOf { 20.dp }
private val MaxContentWidth = 840.dp

/** Kartu standar: radius 16, border 1 dp, bayangan lembut. */
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

private fun tabIcon(t: ResultTab): ImageVector = when (t) {
    ResultTab.SUMMARY -> Icons.Filled.AutoAwesome
    ResultTab.QUIZ -> Icons.Filled.Quiz
    ResultTab.FLASHCARD -> Icons.Filled.Layers
    ResultTab.CHAT -> Icons.Filled.QuestionAnswer
    ResultTab.REFERENCE -> Icons.Filled.Bookmark
    else -> Icons.Filled.Info
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Memaksa mode keyboard "resize" selama layar ini tampil, lalu mengembalikan
 * mode semula saat layar ditutup. Ini mencegah jendela ter-PAN ke atas
 * (penyebab text field "terbang" dan judul menabrak status bar).
 */
@Composable
private fun ForceAdjustResize() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        val previous = window?.attributes?.softInputMode
        @Suppress("DEPRECATION")
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        onDispose {
            if (window != null && previous != null) window.setSoftInputMode(previous)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LAYAR UTAMA
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MaterialResultScreen(
    navController: NavHostController,
    viewModel: MaterialResultViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val p = resultPalette()

    // Catat jumlah pesan saat pertama kali data dimuat untuk membedakan riwayat vs pesan baru
    var initialMessagesSize by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(state.chatMessages) {
        if (initialMessagesSize == null && state.chatMessages.isNotEmpty()) {
            initialMessagesSize = state.chatMessages.size
        }
    }

    ForceAdjustResize()

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
        contentColor = p.textPrimary,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
                // Padding system bars sudah dipakai di atas → tandai terpakai
                // supaya imePadding() hanya menambah selisih tinggi keyboard.
                .consumeWindowInsets(padding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            val hPad = if (maxWidth < 360.dp) 16.dp else 20.dp
            val isChat = state.tab == ResultTab.CHAT

            CompositionLocalProvider(LocalHPad provides hPad) {
                Column(Modifier.fillMaxHeight().widthIn(max = MaxContentWidth).fillMaxWidth()) {
                    // ── Header ──────────────────────────────────────────────────
                    Row(
                        Modifier.fillMaxWidth().padding(start = hPad, end = hPad, top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(p.surface)
                                .border(1.dp, p.border, CircleShape)
                                .clickable { navController.popBackStack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ArrowBackIosNew, "Kembali", Modifier.size(18.dp), tint = p.textPrimary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                state.material?.title ?: "Hasil Materi",
                                fontSize = 20.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = p.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Belajar dengan bantuan AI",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = p.textSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Banner AI (disembunyikan di tab Chat agar ruang chat lebih lega) ──
                    AnimatedVisibility(
                        visible = !isChat,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Row(
                                Modifier
                                    .padding(horizontal = hPad)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(p.accentSoft)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Info, null, tint = p.accentText, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    AiClient.LABEL_AI,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = p.accentText
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    // ── Tab chip dengan ikon ─────────────────────────────────────
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = hPad),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ResultTab.entries.forEach { t ->
                            TabChip(tabLabel(t), tabIcon(t), selected = state.tab == t, p = p) { viewModel.setTab(t) }
                        }
                    }

                    if (state.isAiWorking && !isChat) {
                        Column(Modifier.padding(horizontal = hPad, vertical = 12.dp)) {
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
                                initialSize = initialMessagesSize,
                                input = state.chatInput,
                                isTyping = state.isAiWorking,
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
    }
}

// ── Komponen umum ─────────────────────────────────────────────────────────────

@Composable
private fun TabChip(label: String, icon: ImageVector, selected: Boolean, p: ResultPalette, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .height(38.dp)
            .clip(shape)
            .background(if (selected) p.accent else p.surface)
            .then(if (selected) Modifier else Modifier.border(1.dp, p.border, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = if (selected) Color.White else p.textSecondary)
        Spacer(Modifier.width(6.dp))
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

/** Tombol pill gradien 52 dp. */
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
            .then(
                if (enabled) Modifier.background(p.accentBrush)
                else Modifier.background(p.accent.copy(alpha = 0.35f))
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

/** Keadaan kosong: ikon di lingkaran lembut, judul, pesan, lalu tombol aksi. */
@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    buttonText: String,
    p: ResultPalette,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(p.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(60.dp).clip(CircleShape).background(p.accentBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(title, fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, color = p.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, fontSize = 14.sp, lineHeight = 21.sp, color = p.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        ActionButton(buttonText, onClick, p, Modifier.widthIn(max = 300.dp))
    }
}

// ── Ringkasan ─────────────────────────────────────────────────────────────────

@Composable
private fun SummaryTab(summary: String?, p: ResultPalette, onGenerate: () -> Unit, onSaveNote: () -> Unit) {
    val hPad = LocalHPad.current
    val clipboardManager = LocalClipboardManager.current
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
            title = "Belum ada ringkasan",
            message = "Ketuk tombol di bawah untuk membuat ringkasan materi dengan AI.",
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
            .padding(horizontal = hPad, vertical = 12.dp)
    ) {
        Column(Modifier.fillMaxWidth().card(p, 20.dp).padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(p.accentSoft),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.AutoAwesome, null, tint = p.accentText, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Ringkasan",
                    fontSize = 18.sp, lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold, color = p.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(p.accentSoft)
                        .clickable { onGenerate() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerasi Ringkasan",
                        tint = p.accentText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isCopied) p.successSoft else p.accentSoft)
                        .clickable {
                            clipboardManager.setText(AnnotatedString(summary))
                            isCopied = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                        contentDescription = "Salin Ringkasan",
                        tint = if (isCopied) p.success else p.accentText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = p.border)
            Spacer(Modifier.height(14.dp))
            MarkdownText(summary, fontSize = 15.sp, lineHeight = 24.sp, color = p.textPrimary)
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
    val hPad = LocalHPad.current
    if (quiz.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Quiz,
            title = "Belum ada kuis",
            message = "Uji pemahamanmu dengan soal pilihan ganda yang dibuat dari materi ini.",
            buttonText = "Buat 10 Soal",
            p = p,
            onClick = onGenerate
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = hPad, end = hPad, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (submitted) {
            item {
                val percent = score * 100 / quiz.size
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(p.accentBrush)
                        .padding(20.dp)
                ) {
                    Text("Skor Kamu", fontSize = 13.sp, lineHeight = 18.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$score / ${quiz.size}", fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Text("$percent%", fontSize = 18.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.9f))
                    }
                    val topics = quiz.filterIndexed { i, q -> answers[i] != q.correctIndex }.map { it.topic }.distinct()
                    if (topics.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Materi yang perlu dipelajari",
                            fontSize = 14.sp, lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold, color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        topics.forEach {
                            Text("• $it", fontSize = 14.sp, lineHeight = 22.sp, color = Color.White.copy(alpha = 0.95f))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onGenerate,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Buat Kuis Baru", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        } else {
            item {
                val progress = answers.size / quiz.size.toFloat()
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Progres",
                            fontSize = 12.sp, lineHeight = 16.sp,
                            color = p.textSecondary, modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${answers.size}/${quiz.size}",
                            fontSize = 12.sp, lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold, color = p.accentText
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = p.accent,
                        trackColor = p.border
                    )
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
                        modifier = Modifier.weight(1f).padding(top = 3.dp)
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
                        OptionRow(
                            letter = ('A' + optIdx).toString(),
                            text = opt, optState = optState, p = p, enabled = !submitted
                        ) { onSelect(i, optIdx) }
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
private fun OptionRow(
    letter: String,
    text: String,
    optState: OptionState,
    p: ResultPalette,
    enabled: Boolean,
    onClick: () -> Unit
) {
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
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .then(
                    when (optState) {
                        OptionState.IDLE -> Modifier.border(1.dp, p.border, CircleShape)
                        OptionState.SELECTED -> Modifier.background(p.accent)
                        OptionState.CORRECT -> Modifier.background(p.success)
                        OptionState.WRONG -> Modifier.background(p.danger)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (optState) {
                OptionState.CORRECT -> Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                OptionState.WRONG -> Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                OptionState.SELECTED -> Text(letter, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                OptionState.IDLE -> Text(letter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = p.textSecondary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 14.sp, lineHeight = 21.sp, color = p.textPrimary, modifier = Modifier.weight(1f))
    }
}

// ── Flashcard ─────────────────────────────────────────────────────────────────

@Composable
private fun FlashcardTab(cards: List<Flashcard>, p: ResultPalette, onGenerate: () -> Unit) {
    val hPad = LocalHPad.current
    if (cards.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Layers,
            title = "Belum ada flashcard",
            message = "Buat kartu belajar untuk menghafal konsep penting dari materi ini.",
            buttonText = "Buat Flashcard",
            p = p,
            onClick = onGenerate
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        contentPadding = PaddingValues(start = hPad, end = hPad, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Ketuk kartu untuk membalik", fontSize = 12.sp, lineHeight = 16.sp, color = p.textSecondary)
        }
        items(cards.size) { index ->
            FlashcardItem(index = index, total = cards.size, card = cards[index], p = p)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedButton(
                onClick = onGenerate,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, p.border)
            ) {
                Icon(Icons.Default.Refresh, null, tint = p.textSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Regenerasi Flashcard", color = p.textSecondary)
            }
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
            .heightIn(min = 190.dp)
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
            FlashcardSide("PERTANYAAN", card.front, "${index + 1} / $total", false, p)
        } else {
            Box(Modifier.graphicsLayer { rotationY = 180f }) {
                FlashcardSide("JAWABAN", card.back, "${index + 1} / $total", true, p)
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
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isBack) p.surface else p.accentSoft)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(label, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, color = p.accentText)
            }
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

private val ChatSuggestions = listOf(
    "Jelaskan konsep utama materi ini",
    "Buatkan contoh soal",
    "Apa poin yang paling penting?"
)

@Composable
private fun ChatTab(
    messages: List<ChatMessage>,
    initialSize: Int?,
    input: String,
    isTyping: Boolean,
    p: ResultPalette,
    onInput: (String) -> Unit,
    onSend: () -> Unit
) {
    val hPad = LocalHPad.current
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val canSend = input.isNotBlank()

    // Scroll ke pesan terakhir saat ada pesan baru, indikator mengetik, atau keyboard muncul.
    LaunchedEffect(messages.size, isTyping, imeVisible) {
        val lastIndex = messages.size - 1 + if (isTyping) 1 else 0
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    // TIDAK ada imePadding() di sini — sudah ditangani sekali di root layar.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxBubble = maxWidth * 0.82f

        Column(Modifier.fillMaxSize()) {
            if (messages.isEmpty()) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = hPad, vertical = 16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Saat keyboard terbuka ikon disembunyikan agar saran tetap muat.
                    if (!imeVisible) {
                        Box(
                            Modifier.size(64.dp).clip(CircleShape).background(p.accentSoft),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.QuestionAnswer, null, tint = p.accentText, modifier = Modifier.size(28.dp)) }
                        Spacer(Modifier.height(14.dp))
                    }
                    Text(
                        "Yuk, tanya apa saja soal materi ini!",
                        fontSize = 16.sp, lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold, color = p.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    ChatSuggestions.forEach { s ->
                        val shape = RoundedCornerShape(16.dp)
                        Box(
                            Modifier
                                .padding(vertical = 6.dp)
                                .widthIn(max = 420.dp)
                                .fillMaxWidth()
                                .clip(shape)
                                .background(p.surface)
                                .border(1.dp, p.border, shape)
                                .clickable { onInput(s) }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.TipsAndUpdates,
                                    null,
                                    tint = p.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(s, fontSize = 14.sp, lineHeight = 20.sp, color = p.textPrimary)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = hPad, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        count = messages.size,
                        key = { i ->
                            val m = messages[i]
                            "${m.role}_${m.timestamp?.seconds ?: i}_${m.content.hashCode()}"
                        }
                    ) { index ->
                        val msg = messages[index]
                        // Animasikan hanya jika pesan baru (index >= initialSize) dan milik AI
                        val shouldAnimate = initialSize != null && index >= initialSize && msg.role == "ai"
                        ChatBubble(
                            msg = msg,
                            p = p,
                            maxBubbleWidth = maxBubble,
                            animate = shouldAnimate
                        )
                    }
                    if (isTyping) {
                        item(key = "typing_indicator") { TypingBubble(p) }
                    }
                }
            }

            // Kolom input — selalu menempel tepat di atas keyboard.
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = hPad, end = hPad, top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val fieldShape = RoundedCornerShape(26.dp)
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .clip(fieldShape)
                        .background(p.surface)
                        .border(1.dp, p.border, fieldShape)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = onInput,
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp, lineHeight = 22.sp, color = p.textPrimary
                        ),
                        cursorBrush = SolidColor(p.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (input.isEmpty()) {
                                Text("Tanya apa saja...", fontSize = 15.sp, lineHeight = 22.sp, color = p.textSecondary)
                            }
                            inner()
                        }
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .then(
                            if (canSend) Modifier.background(p.accentBrush)
                            else Modifier.background(p.accent.copy(alpha = 0.35f))
                        )
                        .clickable(enabled = canSend, onClick = onSend),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Kirim", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage, p: ResultPalette, maxBubbleWidth: Dp, animate: Boolean) {
    val isUser = msg.role == "user"
    val content = msg.content
    
    // Gunakan rememberSaveable agar teks yang sudah ditampilkan tidak hilang saat recomposition.
    // Inisialisasi dengan teks kosong hanya jika pesan perlu dianimasikan.
    var displayedText by rememberSaveable(content, animate) { 
        mutableStateOf(if (animate) "" else content) 
    }

    LaunchedEffect(content, animate) {
        // Hanya jalankan animasi mengetik jika flag animate aktif dan teks masih kosong
        if (animate && displayedText.isEmpty() && content.isNotEmpty()) {
            val words = content.split(" ")
            val sb = StringBuilder()
            for (i in words.indices) {
                sb.append(words[i])
                if (i < words.size - 1) sb.append(" ")
                displayedText = sb.toString()
                kotlinx.coroutines.delay(30)
            }
        } else {
            displayedText = content
        }
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            AiAvatar(p)
            Spacer(Modifier.width(8.dp))
        }
        val shape = RoundedCornerShape(
            topStart = 18.dp, topEnd = 18.dp,
            bottomStart = if (isUser) 18.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 18.dp
        )
        Box(
            Modifier
                .widthIn(max = maxBubbleWidth)
                .clip(shape)
                .then(if (isUser) Modifier.background(p.accentBrush) else Modifier.background(p.surface))
                .then(if (isUser) Modifier else Modifier.border(1.dp, p.border, shape))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (isUser) {
                Text(displayedText, fontSize = 14.sp, lineHeight = 21.sp, color = Color.White)
            } else {
                MarkdownText(displayedText, fontSize = 14.sp, lineHeight = 21.sp, color = p.textPrimary)
            }
        }
    }
}

@Composable
private fun AiAvatar(p: ResultPalette) {
    Box(
        Modifier.size(28.dp).clip(CircleShape).background(p.accentSoft),
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Filled.AutoAwesome, null, tint = p.accentText, modifier = Modifier.size(16.dp)) }
}

@Composable
private fun TypingBubble(p: ResultPalette) {
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        AiAvatar(p)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .clip(shape)
                .background(p.surface)
                .border(1.dp, p.border, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(p.textSecondary.copy(alpha = dot1Alpha)))
                Box(Modifier.size(6.dp).clip(CircleShape).background(p.textSecondary.copy(alpha = dot2Alpha)))
                Box(Modifier.size(6.dp).clip(CircleShape).background(p.textSecondary.copy(alpha = dot3Alpha)))
            }
        }
    }
}

// ── Referensi ─────────────────────────────────────────────────────────────────

@Composable
private fun ReferenceTab(references: List<JournalReference>, p: ResultPalette, onGenerate: () -> Unit) {
    val context = LocalContext.current
    val hPad = LocalHPad.current
    if (references.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Search,
            title = "Belum ada referensi",
            message = "Temukan referensi jurnal akademik yang relevan dengan materi ini.",
            buttonText = "Cari Referensi AI",
            p = p,
            onClick = onGenerate
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        contentPadding = PaddingValues(start = hPad, end = hPad, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Referensi Terkait",
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onGenerate) {
                        Icon(Icons.Default.Refresh, "Cari Ulang", tint = p.textSecondary)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "AI telah merangkum kata kunci dari materi dan mencari jurnal yang relevan.",
                    fontSize = 12.sp, lineHeight = 18.sp, color = p.textSecondary
                )
            }
        }
        items(references.size) { idx ->
            val ref = references[idx]
            ReferenceItem(ref = ref, p = p) {
                val urlString = ref.doi ?: "https://openalex.org/works/${ref.id}"
                try {
                    val url = urlString.toUri()
                    if (url.scheme != null && url.host != null) {
                        CustomTabsIntent.Builder().build().launchUrl(context, url)
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
            maxLines = 3, overflow = TextOverflow.Ellipsis
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
            Icon(Icons.Filled.FormatQuote, null, modifier = Modifier.size(16.dp), tint = p.textSecondary)
            Spacer(Modifier.width(4.dp))
            Text(
                NumberFormat.getIntegerInstance(Locale("id", "ID")).format(ref.citedByCount) + " sitasi",
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

// ── Markdown sederhana ────────────────────────────────────────────────────────

@Composable
private fun MarkdownText(
    text: String,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    color: Color,
    modifier: Modifier = Modifier
) {
    val annotatedString = remember(text) {
        buildAnnotatedString {
            val lines = text.split("\n")
            lines.forEachIndexed { index, rawLine ->
                var line = rawLine
                val trimmed = line.trimStart()
                val indent = line.length - trimmed.length
                var headingLevel = 0

                when {
                    trimmed.startsWith("### ") -> { headingLevel = 3; line = trimmed.removePrefix("### ") }
                    trimmed.startsWith("## ") -> { headingLevel = 2; line = trimmed.removePrefix("## ") }
                    trimmed.startsWith("# ") -> { headingLevel = 1; line = trimmed.removePrefix("# ") }
                    trimmed.startsWith("* ") || trimmed.startsWith("- ") -> {
                        append(if (indent >= 2) "      ◦ " else "  • ")
                        line = trimmed.drop(2)
                    }
                }

                val parts = line.split("**")
                parts.forEachIndexed { partIndex, part ->
                    val bold = partIndex % 2 == 1
                    when {
                        headingLevel > 0 -> withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = when (headingLevel) {
                                    1 -> fontSize * 1.25f
                                    2 -> fontSize * 1.15f
                                    else -> fontSize * 1.05f
                                }
                            )
                        ) { append(part) }
                        bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) }
                        else -> append(part)
                    }
                }
                if (index < lines.size - 1) append("\n")
            }
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