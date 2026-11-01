package com.kuliahku.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kuliahku.data.model.Task
import com.kuliahku.data.model.TaskStatus
import com.kuliahku.ui.components.*
import com.kuliahku.ui.components.AnimatedEntry
import com.kuliahku.ui.navigation.Routes
import com.kuliahku.ui.theme.*
import com.kuliahku.util.DateFormatters
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Palet warna Beranda (Light = sesuai Beranda.png, Dark = turunan konsisten).
// Mode gelap/terang dideteksi dari MaterialTheme.colorScheme.background,
// sehingga mengikuti tema aplikasi (bukan hanya setelan sistem).
// ---------------------------------------------------------------------------
private class Tone(val container: Color, val accent: Color)
private class MenuTone(val container: Color, val border: Color, val icon: Color)

private class HomePalette(
    val background: Color,
    val progressCard: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val greeting: Color,
    val nim: Color,
    val menuLabel: Color,
    val done: Color,
    val checkOutline: Color,
    val shadowAlpha: Float,
    val ringTrack: Color,
    val ringStart: Color,
    val ringMid: Color,
    val ringEnd: Color,
    val statTotal: Tone,
    val statUpcoming: Tone,
    val statDone: Tone,
    val statLate: Tone,
    val menuTasks: MenuTone,
    val menuNotes: MenuTone,
    val menuCalendar: MenuTone,
    val menuFinance: MenuTone
)

private val LightPalette = HomePalette(
    background = Color(0xFFFBFCFF),
    progressCard = Color(0xFFF3F6FF),
    surface = Color.White,
    border = Color(0xFFD3D3DA),
    textPrimary = Color.Black,
    textSecondary = Color(0xFF6E6E6E),
    greeting = Color(0xFF7D7D7D),
    nim = Color(0xFF8A8A8A),
    menuLabel = Color(0xFF555555),
    done = Color(0xFF21A121),
    checkOutline = Color(0xFF6E6E6E),
    shadowAlpha = 0.10f,
    ringTrack = Color(0xFFE6EAF8),
    ringStart = Color(0xFFC7D5F7),
    ringMid = Color(0xFF0A2A9E),
    ringEnd = Color(0xFF1A66E0),
    statTotal = Tone(Color(0xFFD3DDF8), Color(0xFF0000FF)),
    statUpcoming = Tone(Color(0xFFDCE6F8), Color(0xFF1A2FD6)),
    statDone = Tone(Color(0xFFC6FAC6), Color(0xFF1E9E1E)),
    statLate = Tone(Color(0xFFFFB0B0), Color(0xFFE02020)),
    menuTasks = MenuTone(Color(0xFFD9DEFA), Color(0xFF1010E8), Color(0xFF1B1BA8)),
    menuNotes = MenuTone(Color(0xFFFFE8C2), Color(0xFFF08C00), Color(0xFFE08A10)),
    menuCalendar = MenuTone(Color(0xFFCDF7CD), Color(0xFF1BA81B), Color(0xFF148A14)),
    menuFinance = MenuTone(Color(0xFFFFB9B9), Color(0xFFD81B1B), Color(0xFFD01818))
)

private val DarkPalette = HomePalette(
    background = Color(0xFF0E1220),
    progressCard = Color(0xFF182036),
    surface = Color(0xFF171C2C),
    border = Color(0xFF2A3350),
    textPrimary = Color(0xFFF1F4FF),
    textSecondary = Color(0xFFA3ABC4),
    greeting = Color(0xFF9AA3BF),
    nim = Color(0xFF7F88A6),
    menuLabel = Color(0xFFB4BCD4),
    done = Color(0xFF2FBF71),
    checkOutline = Color(0xFF8B93AE),
    shadowAlpha = 0.45f,
    ringTrack = Color(0xFF263050),
    ringStart = Color(0xFF2B4A9A),
    ringMid = Color(0xFF3B6FE8),
    ringEnd = Color(0xFF7DB2FF),
    statTotal = Tone(Color(0xFF1C2B55), Color(0xFF6C94FF)),
    statUpcoming = Tone(Color(0xFF1B2A4A), Color(0xFF7BA0FF)),
    statDone = Tone(Color(0xFF14362A), Color(0xFF3DD083)),
    statLate = Tone(Color(0xFF45202A), Color(0xFFFF6B7A)),
    menuTasks = MenuTone(Color(0xFF1C2755), Color(0xFF4C6FFF), Color(0xFF8FA8FF)),
    menuNotes = MenuTone(Color(0xFF3A2A12), Color(0xFFF59E0B), Color(0xFFFFB84D)),
    menuCalendar = MenuTone(Color(0xFF11351F), Color(0xFF34C759), Color(0xFF6EE79A)),
    menuFinance = MenuTone(Color(0xFF3D1A20), Color(0xFFFF5C6C), Color(0xFFFF8A95))
)

@Composable
private fun homePalette(): HomePalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DarkPalette else LightPalette

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val p = homePalette()
    val shadowColor = Color.Black.copy(alpha = p.shadowAlpha)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(58.dp))

        // ---------------- Header ----------------
        AnimatedEntry(delay = 100) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 11.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Selamat ${state.greeting},",
                        fontSize = 20.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.greeting
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.user?.name ?: "Aditya Fajar P",
                            fontSize = 24.sp,
                            lineHeight = 31.sp,
                            fontWeight = FontWeight.Bold,
                            color = p.textPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("👋", fontSize = 24.sp)
                    }
                    Text(
                        state.user?.nim ?: "26552011092",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.nim
                    )
                    Text(
                        "Sekarang ada agenda apa hari ini",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.textPrimary
                    )
                }
                Box(
                    Modifier
                        .padding(top = 10.dp, end = 13.dp)
                        .size(32.dp)
                        .clickable { /* Inbox */ }
                ) {
                    Icon(
                        Icons.Filled.MailOutline,
                        contentDescription = null,
                        tint = p.textPrimary,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(13.dp)
                            .border(2.dp, p.background, CircleShape)
                            .padding(2.dp)
                            .background(p.textPrimary, CircleShape)
                    )
                }
            }
        }

        Spacer(Modifier.height(13.dp))

        // ---------------- Progres Harianku ----------------
        AnimatedEntry(delay = 200) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(204.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp), ambientColor = shadowColor, spotColor = shadowColor),
                shape = RoundedCornerShape(28.dp),
                color = p.progressCard,
                border = BorderStroke(2.dp, p.border)
            ) {
                Column(Modifier.padding(start = 27.dp, end = 17.dp, top = 16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Progres Tugasku",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            color = p.textPrimary
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = p.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.height(11.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            GradientProgressRing(
                                progress = state.taskProgress,
                                ringSize = 122.dp,
                                strokeWidth = 17.dp,
                                trackColor = p.ringTrack,
                                startColor = p.ringStart,
                                midColor = p.ringMid,
                                endColor = p.ringEnd
                            )
                            Text(
                                "${(state.taskProgress * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = p.textPrimary
                            )
                        }
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${state.tasksDone} dari ${state.tasksTotal} tugas selesai",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.textPrimary
                            )
                            Spacer(Modifier.height(9.dp))
                            Text(
                                state.motivation.ifBlank { "Tetap semangat!\nKamu pasti bisa" },
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = p.textSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(23.dp))

        // ---------------- Stats 2x2 ----------------
        AnimatedEntry(delay = 300) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    StatCardMini("Total Tugas", state.stats.total.toString(), p.statTotal.container, Icons.Filled.CheckCircle, p.statTotal.accent, Modifier.weight(1f))
                    StatCardMini("Belum Selesai", state.stats.upcoming.toString(), p.statUpcoming.container, Icons.Outlined.AccessTime, p.statUpcoming.accent, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    StatCardMini("Selesai", state.stats.done.toString(), p.statDone.container, Icons.Filled.CheckCircle, p.statDone.accent, Modifier.weight(1f))
                    StatCardMini("Terlambat", state.stats.overdue.toString(), p.statLate.container, Icons.Outlined.Timer, p.statLate.accent, Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(13.dp))
        AnimatedEntry(delay = 350) {
            Text(
                "Menu Aksi",
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                color = p.textPrimary
            )
        }
        Spacer(Modifier.height(7.dp))

        // ---------------- Menu Aksi ----------------
        AnimatedEntry(delay = 400) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MenuActionButton(rememberClipboardPlusIcon(), "Tugas", p.menuTasks.border, p.menuTasks.icon, containerColor = p.menuTasks.container) { navController.navigate(Routes.TASKS) }
                MenuActionButton(Icons.Outlined.EditNote, "Catatan", p.menuNotes.border, p.menuNotes.icon, containerColor = p.menuNotes.container) { navController.navigate(Routes.NOTES) }
                MenuActionButton(Icons.Outlined.CalendarMonth, "Kalender", p.menuCalendar.border, p.menuCalendar.icon, containerColor = p.menuCalendar.container) { navController.navigate(Routes.CALENDAR) }
                MenuActionButton(Icons.Outlined.AccountBalanceWallet, "Keuangan", p.menuFinance.border, p.menuFinance.icon, containerColor = p.menuFinance.container) { navController.navigate(Routes.FINANCE) }
            }
        }

        Spacer(Modifier.height(22.dp))
        AnimatedEntry(delay = 450) {
            Text(
                "Daftar Tugas",
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                color = p.textPrimary
            )
        }
        Spacer(Modifier.height(11.dp))

        // ---------------- Daftar Tugas ----------------
        AnimatedEntry(delay = 500) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(28.dp), ambientColor = shadowColor, spotColor = shadowColor)
                    .clickable { navController.navigate(Routes.TASKS) },
                shape = RoundedCornerShape(28.dp),
                color = p.surface,
                border = BorderStroke(1.dp, p.border)
            ) {
                Column(Modifier.padding(vertical = 12.dp)) {
                    if (state.tasks.isEmpty()) {
                        Text("Tidak ada tugas", Modifier.padding(20.dp), color = p.textSecondary)
                    } else {
                        state.tasks.forEach { task ->
                            TaskRowItem(task, onToggle = { navController.navigate(Routes.TASKS) })
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(120.dp))
    }
}

/**
 * Ring progres dengan gradasi sweep (start -> mid -> end) sepanjang busur
 * progres, ujung membulat, dan track di belakangnya.
 */
@Composable
private fun GradientProgressRing(
    progress: Float,
    ringSize: Dp,
    strokeWidth: Dp,
    trackColor: Color,
    startColor: Color,
    midColor: Color,
    endColor: Color
) {
    val value = progress.coerceIn(0f, 1f)
    Canvas(Modifier.size(ringSize)) {
        val stroke = strokeWidth.toPx()
        val diameter = size.minDimension - stroke
        val topLeft = Offset(stroke / 2f, stroke / 2f)
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke)
        )

        if (value > 0f) {
            // Mulai dari jam 12, searah jarum jam
            rotate(degrees = -90f, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to startColor,
                        (value * 0.55f) to midColor,
                        value to endColor,
                        1f to endColor,
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f * value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            // Ujung awal membulat dengan warna awal gradasi (hindari wrap warna)
            drawCircle(
                color = startColor,
                radius = stroke / 2f,
                center = Offset(center.x, stroke / 2f)
            )
        }
    }
}

@Composable
fun StatCardMini(label: String, value: String, bgColor: Color, icon: ImageVector, iconColor: Color, modifier: Modifier) {
    val p = homePalette()
    val shape = RoundedCornerShape(25.dp)
    val shadowColor = Color.Black.copy(alpha = p.shadowAlpha * 0.8f)
    Surface(
        modifier = modifier
            .height(70.dp)
            .shadow(6.dp, shape, ambientColor = shadowColor, spotColor = shadowColor),
        shape = shape,
        color = p.surface,
        border = BorderStroke(1.dp, p.border)
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(50.dp).clip(RoundedCornerShape(18.dp)).background(bgColor), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column {
                Text(
                    label,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = p.textSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    value,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    color = p.textPrimary
                )
            }
        }
    }
}

@Composable
fun MenuActionButton(
    icon: ImageVector,
    label: String,
    borderColor: Color,
    iconColor: Color,
    containerColor: Color = borderColor.copy(alpha = 0.2f),
    onClick: () -> Unit
) {
    val p = homePalette()
    val shape = RoundedCornerShape(25.dp)
    val glow = borderColor.copy(alpha = if (p.shadowAlpha > 0.2f) 0.25f else 0.3f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .shadow(6.dp, shape, ambientColor = glow, spotColor = glow)
                .clip(shape)
                .background(containerColor)
                .border(1.dp, borderColor, shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(9.dp))
        Text(
            label,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = p.menuLabel,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TaskRowItem(task: Task, onToggle: () -> Unit) {
    val p = homePalette()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 19.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val done = task.statusEnum == TaskStatus.DONE
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .then(
                    if (done) Modifier.background(p.done)
                    else Modifier.border(2.dp, p.checkOutline, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                task.title,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                color = p.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                DateFormatters.formatTime(task.deadline?.toDate() ?: java.util.Date()),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = p.textSecondary
            )
        }
    }
}

/** Ikon clipboard + tanda plus (outline) untuk menu "Tugas". */
@Composable
private fun rememberClipboardPlusIcon(): ImageVector = remember {
    ImageVector.Builder(
        name = "ClipboardPlus",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // badan clipboard
            moveTo(8f, 4f); horizontalLineTo(6f)
            quadTo(4f, 4f, 4f, 6f); verticalLineTo(20f)
            quadTo(4f, 22f, 6f, 22f); horizontalLineTo(18f)
            quadTo(20f, 22f, 20f, 20f); verticalLineTo(6f)
            quadTo(20f, 4f, 18f, 4f); horizontalLineTo(16f)
            // penjepit
            moveTo(9f, 2f); horizontalLineTo(15f); verticalLineTo(6f); horizontalLineTo(9f); close()
            // plus
            moveTo(12f, 11f); verticalLineTo(18f)
            moveTo(8.5f, 14.5f); horizontalLineTo(15.5f)
        }
    }.build()
}