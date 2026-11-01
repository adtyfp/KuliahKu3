package com.kuliahku.ui.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    val shadowAlpha: Float,
    val high: BadgeTone,
    val medium: BadgeTone,
    val low: BadgeTone,
    val actionRed: BadgeTone,   // "Selesaikan"
    val actionGreen: BadgeTone  // "Selesai"
)

private val LightTaskPalette = TaskPalette(
    background = Color(0xFFFBFCFF),
    surface = Color.White,
    border = Color(0xFFDEDEDE),
    textPrimary = Color.Black,
    textSecondary = Color(0xFF999999),
    chipSelected = Color(0xFF0F79DC),
    chipBorder = Color(0xFFDBDBDB),
    chipText = Color(0xFF737373),
    filterBg = Color(0xFF7BBFFF),
    filterBorder = Color(0xFF00A1FF),
    filterIcon = Color(0xFF0073DF),
    searchBorder = Color(0xFFB3B3B3),
    fab = Color(0xFF0F79DC),
    shadowAlpha = 0.08f,
    high = BadgeTone(Color(0xFFFFD8D8), Color(0xFFFF8282), Color(0xFFFF0000)),
    medium = BadgeTone(Color(0xFFFFFBC9), Color(0xFFFFDB79), Color(0xFFBE8100)),
    low = BadgeTone(Color(0xFFD5FFE0), Color(0xFFA5FF86), Color(0xFF00AE03)),
    actionRed = BadgeTone(Color(0xFFF8CDCD), Color(0xFFFB7B7B), Color(0xFFFF0303)),
    actionGreen = BadgeTone(Color(0xFFC3FFD2), Color(0xFF97E47E), Color(0xFF096F00))
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
    shadowAlpha = 0.45f,                    // = Home shadowAlpha (dark)
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
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.ArrowBackIosNew,
                            contentDescription = "Kembali",
                            modifier = Modifier.size(24.dp),
                            tint = p.textPrimary
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "Tugas",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = p.textPrimary
                    )
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
                            Icons.Outlined.FilterAlt,
                            contentDescription = "Filter",
                            tint = p.filterIcon,
                            modifier = Modifier.size(26.dp)
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
                        items(state.filtered, key = { it.id }) { task ->
                            TaskCardStyled(
                                task = task,
                                onClick = { navController.navigate(Routes.submitTask(task.id)) }
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
                        .shadow(
                            6.dp, CircleShape,
                            ambientColor = Color.Black.copy(alpha = p.shadowAlpha),
                            spotColor = Color.Black.copy(alpha = p.shadowAlpha * 1.5f)
                        )
                        .clip(CircleShape)
                        .background(p.fab)
                        .clickable { navController.navigate(Routes.ADD_TASK) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Tambah tugas",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
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
            fontSize = 16.sp,
            lineHeight = 24.sp,
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
            .padding(start = 14.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = p.textPrimary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                color = p.textPrimary
            ),
            cursorBrush = SolidColor(p.textPrimary),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TaskCardStyled(task: Task, onClick: () -> Unit) {
    val p = taskPalette()
    val now = System.currentTimeMillis()
    val dl = task.deadline?.toDate()?.time ?: 0L
    val isLate = task.statusEnum == TaskStatus.TODO && dl < now
    val done = task.statusEnum == TaskStatus.DONE
    
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
            .height(88.dp)
            .shadow(
                4.dp, shape,
                ambientColor = Color.Black.copy(alpha = p.shadowAlpha),
                spotColor = Color.Black.copy(alpha = p.shadowAlpha)
            )
            .clickable(onClick = onClick),
        shape = shape,
        color = p.surface,
        border = BorderStroke(1.dp, p.border)
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
                    fontWeight = FontWeight.SemiBold,
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
    modifier: Modifier,
    horizontalPadding: Dp
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(24.dp)
            .clip(shape)
            .background(tone.bg)
            .border(1.dp, tone.border, shape)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = tone.text,
            maxLines = 1,
            softWrap = false
        )
    }
}
