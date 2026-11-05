package com.kuliahku.ui.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kuliahku.data.model.Task
import com.kuliahku.data.model.TaskPriority
import com.kuliahku.data.model.TaskStatus
import com.kuliahku.ui.components.AnimatedEntry
import com.kuliahku.ui.navigation.Routes
import com.kuliahku.ui.theme.cardShadow
import com.kuliahku.util.DateFormatters
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// PALET
// Light = diukur langsung dari Tugas.png (440 x 956, 1 px = 1 dp).
// Dark  = #0E1220 (latar), #171C2C (kartu), #2A3350 (border), #F1F4FF (teks utama),
//         turunan lain diambil dari DarkPalette di HomeScreen.
// Mode gelap/terang dideteksi dari MaterialTheme.colorScheme.background (sama seperti Home).
// ─────────────────────────────────────────────────────────────────────────────
private class BadgeTone(val bg: Color, val border: Color, val text: Color)

private class TaskPalette(
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val chipSelected: Color,
    val chipBorder: Color,
    val chipText: Color,
    val filterBg: Color,
    val filterBorder: Color,
    val filterIcon: Color,
    val searchBorder: Color,
    val fab: Color,
    val high: BadgeTone,
    val medium: BadgeTone,
    val low: BadgeTone,
    val actionRed: BadgeTone,   // "Selesaikan"
    val actionGreen: BadgeTone  // "Selesai"
)

private val LightTaskPalette = TaskPalette(
    background = Color(0xFFFBFCFF), // Senada dengan Beranda
    surface = Color.White,
    border = Color(0xFFE2E8F0), // Border halus slate
    textPrimary = Color(0xFF1E293B), // Teks utama dark slate
    textSecondary = Color(0xFF64748B), // Teks sekunder slate gray
    chipSelected = Color(0xFF3B82F6), // Biru modern
    chipBorder = Color(0xFFE2E8F0),
    chipText = Color(0xFF475569),
    filterBg = Color(0xFFF1F5F9),
    filterBorder = Color(0xFFCBD5E1),
    filterIcon = Color(0xFF64748B),
    searchBorder = Color(0xFFCBD5E1),
    fab = Color(0xFF2563EB),
    high = BadgeTone(Color(0xFFFEF2F2), Color(0xFFFECACA), Color(0xFFEF4444)),
    medium = BadgeTone(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFD97706)),
    low = BadgeTone(Color(0xFFF0FDF4), Color(0xFFBBF7D0), Color(0xFF16A34A)),
    actionRed = BadgeTone(Color(0xFFFEF2F2), Color(0xFFFCA5A5), Color(0xFFEF4444)),
    actionGreen = BadgeTone(Color(0xFFECFDF5), Color(0xFFA7F3D0), Color(0xFF10B981))
)

private val DarkTaskPalette = TaskPalette(
    background = Color(0xFF0E1220),
    surface = Color(0xFF171C2C),
    border = Color(0xFF2A3350),
    textPrimary = Color(0xFFF1F4FF),
    textSecondary = Color(0xFFA3ABC4),      // = Home textSecondary (dark)
    chipSelected = Color(0xFF3B6FE8),       // = Home ringMid (dark)
    chipBorder = Color(0xFF2A3350),
    chipText = Color(0xFFA3ABC4),
    filterBg = Color(0xFF1C2B55),           // = Home statTotal.container (dark)
    filterBorder = Color(0xFF4C6FFF),       // = Home menuTasks.border (dark)
    filterIcon = Color(0xFF7BA0FF),         // = Home statUpcoming.accent (dark)
    searchBorder = Color(0xFF2A3350),
    fab = Color(0xFF3B6FE8),
    high = BadgeTone(Color(0xFF45202A), Color(0xFFFF6B7A).copy(alpha = 0.5f), Color(0xFFFF6B7A)),
    medium = BadgeTone(Color(0xFF3A2A12), Color(0xFFFFB84D).copy(alpha = 0.5f), Color(0xFFFFB84D)),
    low = BadgeTone(Color(0xFF14362A), Color(0xFF3DD083).copy(alpha = 0.5f), Color(0xFF3DD083)),
    actionRed = BadgeTone(Color(0xFF45202A), Color(0xFFFF6B7A).copy(alpha = 0.5f), Color(0xFFFF6B7A)),
    actionGreen = BadgeTone(Color(0xFF14362A), Color(0xFF3DD083).copy(alpha = 0.5f), Color(0xFF3DD083))
)

@Composable
private fun taskPalette(): TaskPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DarkTaskPalette else LightTaskPalette

/** Warna cincin radio pada desain (7 warna pastel, bukan warna prioritas). */
private val RingColors = listOf(
    Color(0xFF7CFF65), Color(0xFFFF6363), Color(0xFF7663FF), Color(0xFFC46BFB),
    Color(0xFFE4E67B), Color(0xFF63B9FF), Color(0xFFFF7878)
)

/** Lebar chip filter hasil ukur desain; label lain memakai lebar konten. */
private fun chipWidth(label: String): Dp? = when (label) {
    "All" -> 59.dp
    "Hari ini" -> 84.dp
    "Akan datang" -> 115.dp
    "Selesai" -> 82.dp
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    navController: NavHostController,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val p = taskPalette()

    BackHandler(enabled = state.selectedTasks.isNotEmpty()) {
        viewModel.clearSelection()
    }

    Box(modifier = Modifier.fillMaxSize().background(p.background)) {
        // Margin kiri 14, kanan 13 (kartu & chip pada desain membentang x = 14..426)
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 13.dp)
        ) {
            Spacer(Modifier.height(57.dp))

            // ── Header (ikon tengah y=75, judul x=60) ──────────────────────────
            AnimatedEntry(delay = 100) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (state.selectedTasks.isNotEmpty()) {
                                viewModel.clearSelection()
                            } else {
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (state.selectedTasks.isNotEmpty()) Icons.Default.Close else rememberBackIcon(),
                            contentDescription = if (state.selectedTasks.isNotEmpty()) "Batal Pilih" else "Kembali",
                            modifier = Modifier.size(24.dp),
                            tint = p.textPrimary
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                    
                    AnimatedContent(
                        targetState = state.selectedTasks.isNotEmpty(),
                        transitionSpec = {
                            (fadeIn() + slideInVertically { it / 2 }).togetherWith(fadeOut() + slideOutVertically { -it / 2 })
                        },
                        label = "header_text"
                    ) { isSelected ->
                        Text(
                            if (isSelected) "${state.selectedTasks.size} Terpilih" else "Tugas",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 24.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = p.textPrimary
                        )
                    }

                    AnimatedVisibility(
                        visible = state.selectedTasks.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.selectedTasks.size == 1) {
                                IconButton(
                                    onClick = {
                                        val taskId = state.selectedTasks.first()
                                        navController.navigate(Routes.editTask(taskId))
                                        viewModel.clearSelection()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Tugas",
                                        tint = p.chipSelected
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    viewModel.confirmDelete()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Hapus Tugas",
                                    tint = Color.Red
                                )
                            }
                        }
                    }

                    if (state.selectedTasks.isEmpty()) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(23.dp))

            // ── Filter chips (tinggi 36, jarak 9) + tombol filter 36x36 ───────────
            AnimatedEntry(delay = 200) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TaskFilter.entries.forEach { f ->
                        TaskFilterChip(
                            label = f.label,
                            selected = state.filter == f,
                            p = p,
                            onClick = { viewModel.onFilterChange(f) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(p.filterBg)
                            .border(1.dp, p.filterBorder, RoundedCornerShape(10.dp))
                            .clickable { viewModel.showFilterSheet(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            rememberFilterIcon(),
                            contentDescription = "Filter",
                            tint = p.filterIcon,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Daftar tugas: kartu 88 dp, jarak 5 dp ─────────────────────────────
            Box(Modifier.weight(1f)) {
                AnimatedEntry(delay = 300, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        // ── AI Advice Section ─────────────────────────────
                        if (state.isAiLoading || state.aiAdvice != null) {
                            item(key = "ai_advice") {
                                AiAdviceCard(
                                    advice = state.aiAdvice,
                                    isLoading = state.isAiLoading,
                                    p = p
                                )
                                Spacer(Modifier.height(15.dp))
                            }
                        }

                        items(state.filtered, key = { it.id }) { task ->
                            TaskCardStyled(
                                task = task,
                                selected = state.selectedTasks.contains(task.id),
                                onClick = {
                                    if (state.selectedTasks.isNotEmpty()) {
                                        viewModel.selectTask(task.id)
                                    } else {
                                        navController.navigate(Routes.submitTask(task.id))
                                    }
                                },
                                onLongClick = {
                                    viewModel.selectTask(task.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── Search bar (30..326) + FAB (352..409), tinggi 57, dasar y=917 ────────
        AnimatedEntry(
            delay = 400,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 30.dp, end = 31.dp, bottom = 39.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TaskSearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchChange,
                    p = p,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(25.dp))
                Box(
                    modifier = Modifier
                        .size(57.dp)
                        .cardShadow(shapeRadius = 28.5.dp)
                        .clip(CircleShape)
                        .background(p.fab)
                        .clickable { navController.navigate(Routes.ADD_TASK) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        rememberPlusIcon(),
                        contentDescription = "Tambah tugas",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        // ── Delete Confirmation Dialog ──
        if (state.pendingDelete != null) {
            AlertDialog(
                onDismissRequest = { viewModel.requestDeleteCancel() },
                title = { Text("Hapus Tugas", fontWeight = FontWeight.Bold) },
                text = { Text("Apakah Anda yakin ingin menghapus tugas \"${state.pendingDelete?.title}\"?") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmDelete() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Hapus", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.requestDeleteCancel() }) {
                        Text("Batal", color = p.textPrimary)
                    }
                }
            )
        }
    }
}

@Composable
private fun TaskFilterChip(label: String, selected: Boolean, p: TaskPalette, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val widthMod = chipWidth(label)?.let { Modifier.width(it) } ?: Modifier.widthIn(min = 59.dp)
    Box(
        modifier = widthMod
            .height(36.dp)
            .clip(shape)
            .background(if (selected) p.chipSelected else p.surface)
            .then(if (selected) Modifier else Modifier.border(1.dp, p.chipBorder, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            color = if (selected) Color.White else p.chipText
        )
    }
}

@Composable
private fun TaskSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    p: TaskPalette,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(57.dp)
            .clip(CircleShape)
            .background(p.surface)
            .border(1.dp, p.searchBorder, CircleShape)
            .padding(start = 16.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            rememberSearchIcon(),
            contentDescription = null,
            tint = p.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = p.textPrimary
            ),
            cursorBrush = SolidColor(p.textPrimary),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        "Cari tugas...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = p.textSecondary
                    )
                }
                innerTextField()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCardStyled(task: Task, selected: Boolean = false, onClick: () -> Unit, onLongClick: () -> Unit) {
    val p = taskPalette()
    val now = System.currentTimeMillis()
    val dl = task.deadline?.toDate()?.time ?: 0L
    val isLate = task.statusEnum == TaskStatus.TODO && dl < now
    val done = task.statusEnum == TaskStatus.DONE

    val scale by animateFloatAsState(if (selected) 1.02f else 1f, label = "scale")
    
    val ringColor = RingColors[abs(task.id.hashCode()) % RingColors.size]
    val priorityTone = when (task.priorityEnum) {
        TaskPriority.HIGH -> p.high
        TaskPriority.MEDIUM -> p.medium
        TaskPriority.LOW -> p.low
    }
    
    // Status UI logic
    val (statusText, statusTone) = when {
        done -> "Selesai" to p.actionGreen
        isLate -> "Terlambat" to p.actionRed
        else -> "Belum Selesai" to p.actionRed
    }
    
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(88.dp)
            .cardShadow(shapeRadius = 16.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        color = p.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) p.chipSelected else p.border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 21.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox ring (non-clickable as requested)
            Box(
                modifier = Modifier
                    .size(27.dp)
                    .clip(CircleShape)
                    .border(2.dp, ringColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Box(Modifier.size(17.dp).clip(CircleShape).background(ringColor))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = p.textPrimary,
                    textDecoration = if (done) TextDecoration.LineThrough else null
                )
                if (task.description.isNotBlank()) {
                    Text(
                        task.description,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = p.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    if (isLate) "Terlambat" else DateFormatters.formatFullDate(task.deadline?.toDate() ?: java.util.Date()),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = if (isLate) Color.Red else p.textSecondary,
                    maxLines = 1
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TaskBadge(
                    text = when (task.priorityEnum) {
                        TaskPriority.HIGH -> "High"
                        TaskPriority.MEDIUM -> "Medium"
                        TaskPriority.LOW -> "Low"
                    },
                    tone = priorityTone,
                    modifier = Modifier.widthIn(min = 59.dp),
                    horizontalPadding = 7.dp
                )

                TaskBadge(
                    text = statusText,
                    tone = statusTone,
                    modifier = Modifier.width(100.dp),
                    horizontalPadding = 0.dp
                )
            }
        }
    }
}

@Composable
private fun TaskBadge(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 7.dp
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = tone.bg,
        border = BorderStroke(1.dp, tone.border)
    ) {
        Text(
            text = text,
            color = tone.text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 2.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AiAdviceCard(advice: String?, isLoading: Boolean, p: TaskPalette) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow(shapeRadius = 16.dp),
        shape = shape,
        color = p.surface,
        border = BorderStroke(1.dp, p.border)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✨", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "AI Smart Recommendation",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.chipSelected
                )
            }
            Spacer(Modifier.height(8.dp))
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                    color = p.chipSelected,
                    trackColor = p.border
                )
            } else {
                MarkdownText(
                    text = advice ?: "",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = p.textPrimary
                )
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
            // Handle List items
            if (currentLine.trimStart().startsWith("* ") || currentLine.trimStart().startsWith("- ")) {
                append("  • ")
                currentLine = currentLine.trimStart().removePrefix("* ").removePrefix("- ")
            }

            // Handle Bold
            val parts = currentLine.split("**")
            parts.forEachIndexed { partIndex, part ->
                if (partIndex % 2 == 1) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(part)
                    }
                } else {
                    // Remove raw markdown headings if they exist
                    val cleanedPart = part.replace(Regex("^#+\\s*"), "")
                    append(cleanedPart)
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

@Composable
private fun rememberBackIcon(): ImageVector = remember {
    ImageVector.Builder(name = "Back", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(15f, 18f); lineTo(9f, 12f); lineTo(15f, 6f)
        }
    }.build()
}

@Composable
private fun rememberFilterIcon(): ImageVector = remember {
    ImageVector.Builder(name = "Filter", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(22f, 3f); horizontalLineTo(2f); lineTo(10f, 12.46f); verticalLineTo(19f); lineTo(14f, 21f); verticalLineTo(12.46f); lineTo(22f, 3f); close()
        }
    }.build()
}

@Composable
private fun rememberSearchIcon(): ImageVector = remember {
    ImageVector.Builder(name = "Search", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(11f, 19f)
            arcTo(8f, 8f, 0f, true, true, 11f, 3f)
            arcTo(8f, 8f, 0f, true, true, 11f, 19f)
            moveTo(21f, 21f); lineTo(16.65f, 16.65f)
        }
    }.build()
}

@Composable
private fun rememberPlusIcon(): ImageVector = remember {
    ImageVector.Builder(name = "Plus", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(12f, 5f); verticalLineTo(19f)
            moveTo(5f, 12f); horizontalLineTo(19f)
        }
    }.build()
}
