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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
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
import com.kuliahku.ui.components.SearchBarWithFab
import com.kuliahku.ui.navigation.Routes
import com.kuliahku.ui.theme.cardShadow
import com.kuliahku.util.DateFormatters
import kotlin.math.abs

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// PALET
// Light = diukur langsung dari Tugas.png (440 x 956, 1 px = 1 dp).
// Dark  = #0E1220 (latar), #171C2C (kartu), #2A3350 (border), #F1F4FF (teks utama),
//         turunan lain diambil dari DarkPalette di HomeScreen.
// Mode gelap/terang dideteksi dari MaterialTheme.colorScheme.background (sama seperti Home).
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)



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
    
    BackHandler(enabled = state.selectedTasks.isNotEmpty()) {
        viewModel.clearSelection()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Spacer(Modifier.height(57.dp))

                // ── Header: tombol kembali bulat, judul + ringkasan ──
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
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
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            if (state.selectedTasks.isNotEmpty()) Icons.Default.Close else Icons.Default.ArrowBackIosNew,
                            contentDescription = if (state.selectedTasks.isNotEmpty()) "Batal Pilih" else "Kembali",
                            modifier = Modifier.size(if (state.selectedTasks.isNotEmpty()) 22.dp else 16.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = state.selectedTasks.isNotEmpty(),
                            transitionSpec = {
                                (fadeIn() + slideInVertically { it / 2 }).togetherWith(
                                    fadeOut() + slideOutVertically { -it / 2 })
                            },
                            label = "header_text"
                        ) { isSelected ->
                            Text(
                            if (isSelected) "${state.selectedTasks.size} Terpilih" else "Tugas",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 26.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (state.selectedTasks.isNotEmpty()) {
                        Text(
                            "Ketuk hapus atau edit",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "${state.filtered.size} tugas tersisa",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(
                        visible = state.selectedTasks.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (state.selectedTasks.size == 1) {
                                IconButton(
                                    onClick = {
                                        val taskId = state.selectedTasks.first()
                                        navController.navigate(Routes.editTask(taskId))
                                        viewModel.clearSelection()
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Tugas",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    viewModel.confirmDelete()
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Hapus Tugas",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

            Spacer(Modifier.height(23.dp))

            // ── Filter chips ──
            AnimatedEntry(delay = 200) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TaskFilter.entries.forEach { f ->
                        TaskFilterChip(
                            label = f.label,
                            selected = state.filter == f,
                            onClick = { viewModel.onFilterChange(f) }
                        )
                    }

                    val filterShape = CircleShape
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(filterShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), filterShape)
                            .clickable { viewModel.showFilterSheet(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.FilterAlt,
                            contentDescription = "Filter",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Daftar tugas ──
            Box(Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp)
                ) {
                    // â”€â”€ AI Advice Section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    if (state.isAiLoading || state.aiAdvice != null) {
                        item(key = "ai_advice", contentType = "ai_card") {
                            AnimatedEntry {
                                AiAdviceCard(
                                    advice = state.aiAdvice,
                                    isLoading = state.isAiLoading
                                )
                            }
                            Spacer(Modifier.height(15.dp))
                        }
                    }

                    items(
                        items = state.filtered,
                        key = { it.id },
                        contentType = { "task" }
                    ) { task ->
                        AnimatedEntry {
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

        // ── Search bar + FAB: Standardized ──
        AnimatedEntry(
            delay = 400,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 30.dp, end = 31.dp, bottom = 39.dp)
        ) {
            SearchBarWithFab(
                searchQuery = state.searchQuery,
                onSearchQueryChange = viewModel::onSearchChange,
                onFabClick = { navController.navigate(Routes.ADD_TASK) },
                placeholder = "Cari tugas..."
            )
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Hapus", color = MaterialTheme.colorScheme.onError)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.requestDeleteCancel() }) {
                        Text("Batal", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    }
}
}

@Composable
fun TaskFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = CircleShape
    val widthMod = chipWidth(label)?.let { Modifier.width(it) } ?: Modifier.widthIn(min = 59.dp)
    Box(
        modifier = widthMod
            .height(36.dp)
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .then(
                if (selected) Modifier else Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCardStyled(
    task: Task,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val now = remember { System.currentTimeMillis() }
    val dl = remember(task.deadline) { task.deadline?.toDate()?.time ?: 0L }
    val done = remember(task.statusEnum) { task.statusEnum == TaskStatus.DONE }
    val isLate = remember(done, dl, now) { !done && dl < now }

    val scale by animateFloatAsState(if (selected) 1.02f else 1f, label = "scale")

    val ringColor = remember(task.id) { RingColors[abs(task.id.hashCode()) % RingColors.size] }

    // UI mapping to MaterialTheme
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF171C2C) else Color.White
    val borderColor =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
            alpha = 0.2f
        )

    val shape = MaterialTheme.shapes.medium

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
        color = surfaceColor,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor
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
                    Box(
                        Modifier
                            .size(17.dp)
                            .clip(CircleShape)
                            .background(ringColor)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 20.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textPrimary,
                    textDecoration = if (done) TextDecoration.LineThrough else null
                )
                if (task.description.isNotBlank()) {
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    if (isLate) "Terlambat - ${
                        DateFormatters.formatFullDate(
                            task.deadline?.toDate() ?: java.util.Date()
                        )
                    }"
                    else DateFormatters.formatFullDate(task.deadline?.toDate() ?: java.util.Date()),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = if (isLate) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isLate) MaterialTheme.colorScheme.error else textSecondary,
                    maxLines = 1
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val priorityText = when (task.priorityEnum) {
                    TaskPriority.HIGH -> "High"
                    TaskPriority.MEDIUM -> "Medium"
                    TaskPriority.LOW -> "Low"
                }

                val priorityColors = when (task.priorityEnum) {
                    TaskPriority.HIGH -> Triple(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.error
                    )

                    TaskPriority.MEDIUM -> Triple(
                        Color(0xFFFFFBEB),
                        Color(0xFFD97706),
                        Color(0xFFFDE68A)
                    ) // Yellow fallback if not in theme
                    TaskPriority.LOW -> Triple(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.tertiary
                    )
                }

                TaskBadge(
                    text = priorityText,
                    containerColor = priorityColors.first,
                    contentColor = priorityColors.second,
                    borderColor = priorityColors.third,
                    modifier = Modifier.widthIn(min = 59.dp),
                    horizontalPadding = 7.dp
                )

                val (statusText, statusContainer, statusContent, statusBorder) = when {
                    done -> Quadruple(
                        "Selesai",
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.tertiary
                    )

                    isLate -> Quadruple(
                        "Terlambat",
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.error
                    )

                    else -> Quadruple(
                        "Belum Selesai",
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.error
                    )
                }

                TaskBadge(
                    text = statusText,
                    containerColor = statusContainer,
                    contentColor = statusContent,
                    borderColor = statusBorder,
                    modifier = Modifier.width(100.dp),
                    horizontalPadding = 0.dp
                )
            }
        }
    }
}

@Composable
fun TaskBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 7.dp
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 2.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AiAdviceCard(advice: String?, isLoading: Boolean) {
    val shape = MaterialTheme.shapes.medium
    val surfaceColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF171C2C) else Color.White
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow(shapeRadius = 16.dp),
        shape = shape,
        color = surfaceColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            } else {
                MarkdownText(
                    text = advice ?: "",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MarkdownText(
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
            if (currentLine.trimStart().startsWith("* ") || currentLine.trimStart()
                    .startsWith("- ")
            ) {
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
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = fontSize,
            lineHeight = lineHeight
        ),
        color = color,
        modifier = modifier
    )
}
