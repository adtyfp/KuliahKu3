package com.kuliahku.ui.home

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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kuliahku.data.model.Task
import com.kuliahku.data.model.TaskStatus
import com.kuliahku.ui.components.AnimatedEntry
import com.kuliahku.ui.navigation.Routes
import com.kuliahku.util.DateFormatters
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ---------------------------------------------------------------------------
// Palet warna Beranda (Light = sesuai Beranda.png, Dark = turunan konsisten).
// Mode gelap/terang dideteksi dari MaterialTheme.colorScheme.background,
// sehingga mengikuti tema aplikasi (bukan hanya setelan sistem).
// Field gauge* dipakai oleh kartu progres (gauge setengah lingkaran) dan tetap
// berada di keluarga warna biru tema aplikasi.
// gaugeShade / gaugeShine = bayangan & kilap untuk efek 3D pada pita gauge.
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
    val statIconTotal: Color,
    val statIconUpcoming: Color,
    val statIconDone: Color,
    val statIconLate: Color,
    val shadowAlpha: Float,
    val gaugeHalo: Color,
    val gaugeTrack: Color,
    val gaugeLow: Color,
    val gaugeMid: Color,
    val gaugeHigh: Color,
    val gaugeTick: Color,
    val gaugeDot: Color,
    val gaugeShade: Color,
    val gaugeShine: Color,
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
    background = Color(0xFFF8FAFF), // Latar belakang putih kebiruan bersih
    progressCard = Color(0xFFF1F5F9), // Kartu progres biru sangat muda
    surface = Color.White,
    border = Color(0xFFE2E8F0), // Garis tepi halus (slate)
    textPrimary = Color(0xFF1E293B), // Teks utama (dark slate)
    textSecondary = Color(0xFF64748B), // Teks sekunder (slate gray)
    greeting = Color(0xFF475569),
    nim = Color(0xFF94A3B8),
    menuLabel = Color(0xFF334155),
    done = Color(0xFF10B981), // Emerald green
    checkOutline = Color(0xFFCBD5E1),
    statIconTotal = Color(0xFF2563EB),
    statIconUpcoming = Color(0xFF0EA5E9),
    statIconDone = Color(0xFF10B981),
    statIconLate = Color(0xFFEF4444),
    shadowAlpha = 0.08f, // Sedikit ditingkatkan agar lebih 'hidup'
    gaugeHalo = Color(0xFFDBEAFE),
    gaugeTrack = Color(0xFFE2EAF5), // sedikit lebih gelap dari kartu agar pita yang belum tercapai tetap terlihat
    gaugeLow = Color(0xFF93C5FD),
    gaugeMid = Color(0xFF60A5FA),
    gaugeHigh = Color(0xFF3B82F6),
    gaugeTick = Color(0xFF94A3B8),
    gaugeDot = Color.White,
    gaugeShade = Color(0x2E1E3A8A),
    gaugeShine = Color(0x70FFFFFF),
    statTotal = Tone(Color(0xFFEFF6FF), Color(0xFF2563EB)),
    statUpcoming = Tone(Color(0xFFF0F9FF), Color(0xFF0EA5E9)),
    statDone = Tone(Color(0xFFECFDF5), Color(0xFF10B981)),
    statLate = Tone(Color(0xFFFEF2F2), Color(0xFFEF4444)),
    menuTasks = MenuTone(Color(0xFFEEF2FF), Color(0xFF6366F1), Color(0xFF4F46E5)),
    menuNotes = MenuTone(Color(0xFFFFF7ED), Color(0xFFF97316), Color(0xFFEA580C)),
    menuCalendar = MenuTone(Color(0xFFF0FDF4), Color(0xFF22C55E), Color(0xFF16A34A)),
    menuFinance = MenuTone(Color(0xFFFFF1F2), Color(0xFFF43F5E), Color(0xFFE11D48))
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
    statIconTotal = Color(0xFF6C94FF),
    statIconUpcoming = Color(0xFF7BA0FF),
    statIconDone = Color(0xFF3DD083),
    statIconLate = Color(0xFFFF6B7A),
    shadowAlpha = 0.45f,
    gaugeHalo = Color(0xFF3B6FE8),
    gaugeTrack = Color(0xFF1F2A4A),
    gaugeLow = Color(0xFF2B4A9A),
    gaugeMid = Color(0xFF3B6FE8),
    gaugeHigh = Color(0xFF5E88F0),
    gaugeTick = Color(0xFF8B93AE),
    gaugeDot = Color(0xFFB8C8FF),
    gaugeShade = Color(0x66000000),
    gaugeShine = Color(0x2EFFFFFF),
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
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(58.dp))

        // ---------------- Header ----------------
        AnimatedEntry(delay = 100) {
            Row(
                Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Selamat ${state.greeting},",
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = p.greeting
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.user?.name ?: "Aditya Fajar P",
                            fontSize = 24.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = p.textPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("👋", fontSize = 22.sp)
                    }
                    Text(
                        state.user?.nim ?: "26552011092",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = p.nim
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Apa agenda kamu hari ini?",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = p.textSecondary
                    )
                }
                Box(
                    Modifier
                        .padding(top = 10.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { /* Inbox */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        rememberMailIcon(),
                        contentDescription = null,
                        tint = p.textPrimary,
                        modifier = Modifier.size(24.dp)
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
        AnimatedEntry(delay = 200) {
            val cardShape = RoundedCornerShape(28.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(442f / 268f)
                    .shadow(8.dp, cardShape, ambientColor = shadowColor, spotColor = shadowColor),
                shape = cardShape,
                color = p.progressCard,
                border = BorderStroke(2.dp, p.border)
            ) {
                val progress = state.taskProgress
                val tasksDone = state.tasksDone
                val tasksTotal = state.tasksTotal
                val motivation = state.motivation

                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val u = this.maxWidth.value / 442f

                    ProgressGauge(
                        progress = progress,
                        p = p,
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = (112f * u).dp, start = 12.dp, end = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${(progress * 100).toInt()}%",
                            fontSize = (52f * u).sp,
                            lineHeight = (56f * u).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = p.textPrimary
                        )
                        Spacer(Modifier.height((16f * u).dp))
                        Text(
                            "$tasksDone dari $tasksTotal tugas selesai",
                            fontSize = (20f * u).sp,
                            lineHeight = (28f * u).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = p.textPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Spacer(Modifier.height((4f * u).dp))
                        Text(
                            motivation.ifBlank { "Tetap semangat! Kamu pasti bisa" }.replace("\n", " "),
                            fontSize = (14f * u).sp,
                            lineHeight = (19f * u).sp,
                            fontWeight = FontWeight.Normal,
                            color = p.textSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(23.dp))

        // ---------------- Stats 2x2 ----------------
        AnimatedEntry(delay = 300) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    StatCardMini("Total Tugas", state.stats.total.toString(), p.statTotal.container, rememberClipboardListIcon(), p.statIconTotal, Modifier.weight(1f))
                    StatCardMini("On Progress", state.stats.upcoming.toString(), p.statUpcoming.container, rememberClockIcon(), p.statIconUpcoming, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    StatCardMini("Selesai", state.stats.done.toString(), p.statDone.container, rememberCheckCircleIcon(), p.statIconDone, Modifier.weight(1f))
                    StatCardMini("Terlambat", state.stats.overdue.toString(), p.statLate.container, rememberAlertCircleIcon(), p.statIconLate, Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        AnimatedEntry(delay = 350) {
            Text(
                "Menu Aksi",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                color = p.textPrimary
            )
        }
        Spacer(Modifier.height(14.dp))

        // ---------------- Menu Aksi ----------------
        AnimatedEntry(delay = 400) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MenuActionButton(rememberTasksIcon(), "Tugas", p.menuTasks.border, p.menuTasks.icon, containerColor = p.menuTasks.container) { navController.navigate(Routes.TASKS) }
                MenuActionButton(rememberNotesIcon(), "Catatan", p.menuNotes.border, p.menuNotes.icon, containerColor = p.menuNotes.container) { navController.navigate(Routes.NOTES) }
                MenuActionButton(rememberCalendarIcon(), "Kalender", p.menuCalendar.border, p.menuCalendar.icon, containerColor = p.menuCalendar.container) { navController.navigate(Routes.CALENDAR) }
                MenuActionButton(rememberFinanceIcon(), "Keuangan", p.menuFinance.border, p.menuFinance.icon, containerColor = p.menuFinance.container) { navController.navigate(Routes.FINANCE) }
            }
        }

        Spacer(Modifier.height(32.dp))
        AnimatedEntry(delay = 450) {
            Text(
                "Daftar Tugas",
                fontWeight = FontWeight.Bold,
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
 * Gauge setengah lingkaran seperti foto referensi, dengan sedikit efek 3D:
 *  1. halo lembut di belakang pita
 *  2. pita latar (bagian yang belum tercapai)
 *  3. bayangan jatuh, pita aktif bergradasi, dan pembayangan tabung
 *     (gelap di tepi, terang di tengah pita)
 *  4. tepi luar dan bagian bawah memudar ke warna kartu
 *  5. setengah lingkaran dalam (tepi tegas) + garis bayangan tipis di tepinya
 *  6. kilap tipis sepanjang pita, garis penanda, titik halus, dan jarum berbayangan
 * Koordinat dihitung dari ukuran 442 x 268 (1 satuan = u px).
 */
@Composable
private fun ProgressGauge(progress: Float, p: HomePalette, modifier: Modifier = Modifier) {
    val value = progress.coerceIn(0f, 1f)
    Canvas(modifier) {
        val u = size.width / 442f
        val cx = size.width / 2f
        val cy = 197f * u                 // garis dasar gauge
        val center = Offset(cx, cy)
        val rIn = 106f * u                // tepi dalam pita
        val rOut = 172f * u               // tepi luar pita
        val sweep = 180f * value
        val bg = p.progressCard
        val shade = p.gaugeShade
        val shine = p.gaugeShine
        val sectorTopLeft = Offset(cx - rOut, cy - rOut)
        val sectorSize = Size(rOut * 2f, rOut * 2f)
        val needleAngle = 180f + sweep

        clipRect(left = 0f, top = 0f, right = size.width, bottom = cy) {
            // 1. halo
            drawCircle(
                brush = Brush.radialGradient(
                    0.00f to Color.Transparent,
                    0.55f to Color.Transparent,
                    0.64f to p.gaugeHalo.copy(alpha = 0.20f),
                    0.86f to p.gaugeHalo.copy(alpha = 0.09f),
                    1.00f to Color.Transparent,
                    center = center,
                    radius = rOut * 1.12f
                ),
                radius = rOut * 1.12f,
                center = center
            )

            // 2. pita latar (setengah lingkaran penuh)
            drawArc(
                color = p.gaugeTrack.copy(alpha = 0.65f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = sectorTopLeft,
                size = sectorSize
            )

            if (value > 0f) {
                // 3a. bayangan jatuh di bawah pita aktif
                drawArc(
                    color = shade.copy(alpha = shade.alpha * 0.75f),
                    startAngle = 180f,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = sectorTopLeft + Offset(0f, 6f * u),
                    size = sectorSize
                )

                // 3b. pita aktif (gradasi sweep dari kiri ke posisi progres)
                val fEnd = (180f + sweep) / 360f
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to p.gaugeHigh.copy(alpha = 0.90f),
                        0.5f to p.gaugeLow.copy(alpha = 0.55f),
                        (0.5f + (fEnd - 0.5f) * 0.42f) to p.gaugeMid,
                        fEnd to p.gaugeHigh.copy(alpha = 0.90f),
                        1.0f to p.gaugeHigh.copy(alpha = 0.90f),
                        center = center
                    ),
                    startAngle = 180f,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = sectorTopLeft,
                    size = sectorSize
                )

                // 3c. pembayangan tabung: gelap di tepi dalam & luar, terang di sepertiga pita
                drawArc(
                    brush = Brush.radialGradient(
                        0.00f to Color.Transparent,
                        0.60f to Color.Transparent,
                        0.62f to shade,
                        0.72f to shade.copy(alpha = shade.alpha * 0.20f),
                        0.80f to shine,
                        0.89f to Color.Transparent,
                        0.96f to shade.copy(alpha = shade.alpha * 0.55f),
                        1.00f to shade.copy(alpha = shade.alpha * 0.90f),
                        center = center,
                        radius = rOut
                    ),
                    startAngle = 180f,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = sectorTopLeft,
                    size = sectorSize
                )
            }

            // 4a. tepi luar memudar ke warna kartu (dibuat tipis agar bentuk pita tetap tegas)
            drawCircle(
                brush = Brush.radialGradient(
                    0.000f to Color.Transparent,
                    0.800f to Color.Transparent,
                    0.893f to bg.copy(alpha = 0.30f),
                    0.950f to bg.copy(alpha = 0.80f),
                    1.000f to bg,
                    center = center,
                    radius = rOut * 1.12f
                ),
                radius = rOut * 1.12f,
                center = center
            )

            // 4b. bagian bawah (dekat garis dasar) memudar ke warna kartu
            val fadeH = 0.34f * rOut
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to bg,
                    startY = cy - fadeH,
                    endY = cy
                ),
                topLeft = Offset(0f, cy - fadeH),
                size = Size(size.width, fadeH)
            )

            // 5a. setengah lingkaran dalam
            drawCircle(color = bg, radius = rIn, center = center)
        }

        // 5b. garis bayangan tipis di tepi dalam pita (memberi kesan kedalaman)
        drawArc(
            color = shade.copy(alpha = shade.alpha * 0.55f),
            startAngle = 194f,
            sweepAngle = 152f,
            useCenter = false,
            topLeft = Offset(cx - (rIn + 2f * u), cy - (rIn + 2f * u)),
            size = Size((rIn + 2f * u) * 2f, (rIn + 2f * u) * 2f),
            style = Stroke(width = 4f * u)
        )

        // 6a. kilap tipis sepanjang pita aktif
        if (sweep > 30f) {
            val rs = rIn + 0.74f * (rOut - rIn)
            drawArc(
                color = shine,
                startAngle = 204f,
                sweepAngle = sweep - 30f,
                useCenter = false,
                topLeft = Offset(cx - rs, cy - rs),
                size = Size(rs * 2f, rs * 2f),
                style = Stroke(width = 2.6f * u, cap = StrokeCap.Round)
            )
        }

        // 6b. garis penanda tiap 15 derajat (kecuali dekat jarum)
        for (i in 1..11) {
            val deg = 180f + i * 15f
            if (kotlin.math.abs(deg - needleAngle) < 7f) continue
            val rad = deg * PI.toFloat() / 180f
            val r1 = rIn + 9f * u
            val r2 = rIn + 22f * u
            val active = deg <= needleAngle
            drawLine(
                color = p.gaugeTick.copy(alpha = if (active) 0.55f else 0.30f),
                start = Offset(cx + r1 * cos(rad), cy + r1 * sin(rad)),
                end = Offset(cx + r2 * cos(rad), cy + r2 * sin(rad)),
                strokeWidth = 1.6f * u,
                cap = StrokeCap.Round
            )
        }

        // 6c. titik halus: sedikit saja dan tersebar merata di sepanjang pita aktif
        //     (satu titik per irisan sudut, posisi acak tetap, bukan animasi)
        if (sweep > 20f) {
            val rnd = Random(42)
            val startDeg = 200f
            val endDeg = needleAngle - 6f
            val count = ((sweep / 180f) * 26f).toInt().coerceAtLeast(6)
            repeat(count) { i ->
                val deg = startDeg + (endDeg - startDeg) * (i + 0.15f + 0.70f * rnd.nextFloat()) / count
                val r = rIn + (rOut - rIn) * (0.40f + 0.50f * rnd.nextFloat())
                val rad = deg * PI.toFloat() / 180f
                drawCircle(
                    color = p.gaugeDot.copy(alpha = 0.45f + 0.40f * rnd.nextFloat()),
                    radius = (0.9f + 0.7f * rnd.nextFloat()) * u,
                    center = Offset(cx + r * cos(rad), cy + r * sin(rad))
                )
            }
        }

        // 6d. jarum (dengan bayangan tipis)
        if (value > 0f) {
            val rad = needleAngle * PI.toFloat() / 180f
            val r1 = rIn + 8f * u
            val r2 = rIn + 48f * u
            val s1 = Offset(cx + r1 * cos(rad), cy + r1 * sin(rad))
            val s2 = Offset(cx + r2 * cos(rad), cy + r2 * sin(rad))
            val off = Offset(1.5f * u, 2.5f * u)
            drawLine(
                color = Color.Black.copy(alpha = 0.18f),
                start = s1 + off,
                end = s2 + off,
                strokeWidth = 3.6f * u,
                cap = StrokeCap.Round
            )
            drawLine(
                color = p.textPrimary,
                start = s1,
                end = s2,
                strokeWidth = 3.6f * u,
                cap = StrokeCap.Round
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
                    fontWeight = FontWeight.Medium
                )
                Text(
                    value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
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
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
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
            .clickable(onClick = onToggle)
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
                    rememberCheckIcon(),
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

/** Ikon-ikon Modern & Minimalis (Lucide Style) */

@Composable
private fun rememberCheckIcon(): ImageVector = remember {
    ImageVector.Builder(name = "Check", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(20f, 6f); lineTo(9f, 17f); lineTo(4f, 12f)
        }
    }.build()
}

@Composable
private fun rememberMailIcon(): ImageVector = remember {
    ImageVector.Builder(name = "Mail", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(4f, 4f); horizontalLineTo(20f); quadTo(22f, 4f, 22f, 6f); verticalLineTo(18f); quadTo(22f, 20f, 20f, 20f); horizontalLineTo(4f); quadTo(2f, 20f, 2f, 18f); verticalLineTo(6f); quadTo(2f, 4f, 4f, 4f)
            moveTo(22f, 6f); lineTo(12f, 13f); lineTo(2f, 6f)
        }
    }.build()
}

@Composable
private fun rememberTasksIcon(): ImageVector = remember {
    ImageVector.Builder(name = "Tasks", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(8f, 2f); verticalLineTo(5f); moveTo(16f, 2f); verticalLineTo(5f)
            moveTo(3f, 10f); horizontalLineTo(21f)
            moveTo(3f, 6f); quadTo(3f, 4f, 5f, 4f); horizontalLineTo(19f); quadTo(21f, 4f, 21f, 6f); verticalLineTo(19f); quadTo(21f, 21f, 19f, 21f); horizontalLineTo(5f); quadTo(3f, 21f, 3f, 19f); close()
            moveTo(9f, 14f); lineTo(11f, 16f); lineTo(15f, 12f)
        }
    }.build()
}

@Composable
private fun rememberNotesIcon(): ImageVector = remember {
    ImageVector.Builder(name = "Notes", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(11f, 4f); horizontalLineTo(4f); quadTo(2f, 4f, 2f, 6f); verticalLineTo(20f); quadTo(2f, 22f, 4f, 22f); horizontalLineTo(18f); quadTo(20f, 22f, 20f, 20f); verticalLineTo(13f)
            moveTo(18.5f, 2.5f); quadTo(19.5f, 1.5f, 21.5f, 3.5f); quadTo(22.5f, 4.5f, 21.5f, 5.5f); lineTo(12f, 15f); lineTo(8f, 16f); lineTo(9f, 12f); close()
        }
    }.build()
}

@Composable
private fun rememberCalendarIcon(): ImageVector = remember {
    ImageVector.Builder(name = "Calendar", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(3f, 4f); horizontalLineTo(21f); quadTo(23f, 4f, 23f, 6f); verticalLineTo(20f); quadTo(23f, 22f, 21f, 22f); horizontalLineTo(3f); quadTo(1f, 22f, 1f, 20f); verticalLineTo(6f); quadTo(1f, 4f, 3f, 4f)
            moveTo(16f, 2f); verticalLineTo(6f); moveTo(8f, 2f); verticalLineTo(6f); moveTo(1f, 10f); horizontalLineTo(23f)
        }
    }.build()
}

@Composable
private fun rememberFinanceIcon(): ImageVector = remember {
    ImageVector.Builder(name = "Finance", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(21f, 12f); verticalLineTo(7f); quadTo(21f, 5f, 19f, 5f); horizontalLineTo(5f); quadTo(3f, 5f, 3f, 7f); verticalLineTo(17f); quadTo(3f, 19f, 5f, 19f); horizontalLineTo(19f); quadTo(21f, 19f, 21f, 17f); verticalLineTo(15f)
            moveTo(16f, 12f); horizontalLineTo(22f); verticalLineTo(15f); horizontalLineTo(16f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(19f, 13.5f)
            moveToRelative(-1.2f, 0f)
            arcTo(1.2f, 1.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 20.2f, 13.5f)
            arcTo(1.2f, 1.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 17.8f, 13.5f)
            close()
        }
    }.build()
}

@Composable
private fun rememberClipboardListIcon(): ImageVector = remember {
    ImageVector.Builder(name = "List", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(16f, 4f); horizontalLineTo(18f); quadTo(20f, 4f, 20f, 6f); verticalLineTo(20f); quadTo(20f, 22f, 18f, 22f); horizontalLineTo(6f); quadTo(4f, 22f, 4f, 20f); verticalLineTo(6f); quadTo(4f, 4f, 6f, 4f); horizontalLineTo(8f)
            moveTo(9f, 2f); horizontalLineTo(15f); verticalLineTo(6f); horizontalLineTo(9f); close()
            moveTo(8f, 11f); horizontalLineTo(8.01f); moveTo(8f, 16f); horizontalLineTo(8.01f); moveTo(12f, 11f); horizontalLineTo(16f); moveTo(12f, 16f); horizontalLineTo(16f)
        }
    }.build()
}

@Composable
private fun rememberClockIcon(): ImageVector = remember {
    ImageVector.Builder(name = "Clock", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(12f, 2f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 2f, 12f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 2f)
            moveTo(12f, 6f); verticalLineTo(12f); lineTo(16f, 14f)
        }
    }.build()
}

@Composable
private fun rememberCheckCircleIcon(): ImageVector = remember {
    ImageVector.Builder(name = "CheckCircle", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(12f, 2f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 2f, 12f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 2f)
            moveTo(9f, 12f); lineTo(11f, 14f); lineTo(15f, 10f)
        }
    }.build()
}

@Composable
private fun rememberAlertCircleIcon(): ImageVector = remember {
    ImageVector.Builder(name = "AlertCircle", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(12f, 2f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 2f, 12f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 2f)
            moveTo(12f, 8f); verticalLineTo(12f); moveTo(12f, 16f); horizontalLineTo(12.01f)
        }
    }.build()
}