package com.kuliahku.ui.calendar

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kuliahku.data.model.EventType
import com.kuliahku.data.repository.EventRepository
import com.kuliahku.ui.components.AnimatedEntry
import com.kuliahku.ui.components.KuliahkuDialog
import com.kuliahku.ui.navigation.Routes
import com.kuliahku.util.DateFormatters
import java.util.Calendar

// ─────────────────────────────────────────────────────────────────────────────
// PALET
// Light = diukur dari Kalender.png (440 x 1004, 1 px = 1 dp).
// Dark  = #0E1220 (latar), #2A3350 (garis/border), #F1F4FF (teks utama),
//         warna aksen diambil dari DarkPalette di HomeScreen.
// `holiday` = warna merah untuk hari Minggu & hari libur nasional.
// ─────────────────────────────────────────────────────────────────────────────
private class CalPalette(
    val background: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val mutedDay: Color,
    val divider: Color,
    val selected: Color,
    val selectedText: Color,
    val accent: Color,
    val plusButton: Color,
    val agendaIcon: Color,
    val holiday: Color
)

private val LightCalPalette = CalPalette(
    background = Color(0xFFFBFCFF),
    textPrimary = Color.Black,
    textSecondary = Color(0xFF767678),
    mutedDay = Color(0xFFB4B6BC),
    divider = Color(0xFFBCBDBF),
    selected = Color(0xFF6BACEA),
    selectedText = Color.White,
    accent = Color(0xFF0F79DC),
    plusButton = Color(0xFF0F79DC),
    agendaIcon = Color(0xFF0A5EAD),
    holiday = Color(0xFFE53935)
)

private val DarkCalPalette = CalPalette(
    background = Color(0xFF0E1220),
    textPrimary = Color(0xFFF1F4FF),
    textSecondary = Color(0xFFA3ABC4),
    mutedDay = Color(0xFF555E7C),
    divider = Color(0xFF2A3350),
    selected = Color(0xFF3B6FE8),
    selectedText = Color.White,
    accent = Color(0xFF7BA0FF),
    plusButton = Color(0xFF3B6FE8),
    agendaIcon = Color(0xFF7BA0FF),
    holiday = Color(0xFFFF6B7A)
)

@Composable
private fun calPalette(): CalPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DarkCalPalette else LightCalPalette

/** Desain menampilkan titik penanda hari yang punya agenda. */
private const val SHOW_ENTRY_DOTS = true

// ─────────────────────────────────────────────────────────────────────────────
// HARI LIBUR NASIONAL (bukan cuti bersama)
// Sumber: SKB 3 Menteri – Setneg
//   2026: No. 1497/2025, 2/2025, 5/2025 (ditandatangani 19 Sep 2025)
//   2027: No. 1205/2026, 3/2026, 2/2026 (ditandatangani 15 Sep 2026)
// Tanggal Idulfitri/Iduladha ditetapkan resmi lewat Keputusan Menteri Agama; bila ada
// perubahan, cukup ubah tanggalnya di sini.
//
// Untuk tahun yang belum punya data SKB (mis. 2025 atau 2028), hanya libur bertanggal
// tetap yang ditandai. Tambahkan tahun baru dengan menambah satu entri di `bySkb`.
// Kunci tanggal berformat "MM-dd".
// ─────────────────────────────────────────────────────────────────────────────
private object NationalHolidays {

    private val fixedEveryYear = mapOf(
        "01-01" to "Tahun Baru Masehi",
        "05-01" to "Hari Buruh Internasional",
        "06-01" to "Hari Lahir Pancasila",
        "08-17" to "Proklamasi Kemerdekaan",
        "12-25" to "Kelahiran Yesus Kristus (Natal)"
    )

    private val bySkb: Map<Int, Map<String, String>> = mapOf(
        2026 to mapOf(
            "01-01" to "Tahun Baru 2026 Masehi",
            "01-16" to "Isra Mikraj Nabi Muhammad SAW",
            "02-17" to "Tahun Baru Imlek 2577 Kongzili",
            "03-19" to "Hari Suci Nyepi (Tahun Baru Saka 1948)",
            "03-21" to "Idulfitri 1447 H",
            "03-22" to "Idulfitri 1447 H",
            "04-03" to "Wafat Yesus Kristus",
            "04-05" to "Kebangkitan Yesus Kristus (Paskah)",
            "05-01" to "Hari Buruh Internasional",
            "05-14" to "Kenaikan Yesus Kristus",
            "05-27" to "Iduladha 1447 H",
            "05-31" to "Hari Raya Waisak 2570 BE",
            "06-01" to "Hari Lahir Pancasila",
            "06-16" to "Tahun Baru Islam 1448 H",
            "08-17" to "Proklamasi Kemerdekaan",
            "08-25" to "Maulid Nabi Muhammad SAW",
            "12-25" to "Kelahiran Yesus Kristus (Natal)"
        ),
        2027 to mapOf(
            "01-01" to "Tahun Baru 2027 Masehi",
            "01-05" to "Isra Mikraj Nabi Muhammad SAW",
            "02-06" to "Tahun Baru Imlek 2578 Kongzili",
            "03-08" to "Hari Suci Nyepi (Tahun Baru Saka 1949)",
            "03-10" to "Idulfitri 1448 H",
            "03-11" to "Idulfitri 1448 H",
            "03-26" to "Wafat Yesus Kristus",
            "03-28" to "Kebangkitan Yesus Kristus (Paskah)",
            "05-01" to "Hari Buruh Internasional",
            "05-06" to "Kenaikan Yesus Kristus",
            "05-17" to "Iduladha 1448 H",
            "05-20" to "Hari Raya Waisak 2571 BE",
            "06-01" to "Hari Lahir Pancasila",
            "06-06" to "Tahun Baru Islam 1449 H",
            "08-15" to "Maulid Nabi Muhammad SAW",
            "08-17" to "Proklamasi Kemerdekaan",
            "12-25" to "Kelahiran Yesus Kristus (Natal)",
            "12-26" to "Isra Mikraj Nabi Muhammad SAW"
        )
    )

    /** Nama libur nasional pada tanggal [cal], atau null bila bukan libur nasional. */
    fun nameOf(cal: Calendar): String? {
        val year = cal.get(Calendar.YEAR)
        val key = String.format(
            java.util.Locale.US, "%02d-%02d",
            cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
        )
        return (bySkb[year] ?: fixedEveryYear)[key]
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    navController: NavHostController,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val p = calPalette()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CalendarEvent.ShowMessage -> snackbar.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = p.background,
        contentColor = p.textPrimary,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(57.dp))

            // ── Header (ikon tengah y=75, judul x=60) ──────────────────────────
            AnimatedEntry(delay = 0) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.ArrowBackIosNew, "Kembali", Modifier.size(22.dp), tint = p.textPrimary)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Kalender",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = p.textPrimary
                    )
                }
            }

            Spacer(Modifier.height(25.dp))

            // ── Bulan & tahun + panah (nama bulan x=38, panah tengah x=340 / 383) ──
            AnimatedEntry(delay = 100) {
                Row(
                    Modifier.fillMaxWidth().padding(end = 37.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f).padding(start = 38.dp)) {
                        Text(
                            state.currentMonth.getDisplayName(Calendar.MONTH, Calendar.LONG, java.util.Locale("id", "ID"))
                                ?.replaceFirstChar { it.uppercase() } ?: "",
                            fontSize = 24.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = p.plusButton
                        )
                        Text(
                            state.currentMonth.get(Calendar.YEAR).toString(),
                            fontSize = 14.sp,
                            lineHeight = 16.sp,
                            color = p.textSecondary
                        )
                    }
                    IconButton(onClick = viewModel::previousMonth, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.ChevronLeft, "Sebelumnya", Modifier.size(32.dp), tint = p.textPrimary)
                    }
                    Spacer(Modifier.width(3.dp))
                    IconButton(onClick = viewModel::nextMonth, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.ChevronRight, "Berikutnya", Modifier.size(32.dp), tint = p.textPrimary)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Nama hari (tengah y=186) + grid tanpa kartu (kolom 58,5 dp mulai x=16) ──
            // Huruf pertama (M = Minggu) berwarna merah.
            AnimatedEntry(delay = 200) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 14.dp)) {
                        listOf("M", "S", "S", "R", "K", "J", "S").forEachIndexed { index, d ->
                            Text(
                                d,
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = if (index == 0) p.holiday else p.textPrimary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    
                    // Garis mendatar di bawah nama hari (seperti divider agenda)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(1.dp)
                            .background(p.divider.copy(alpha = 0.5f))
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Animasi bergeser saat ganti bulan
                    val monthTime = state.currentMonth.timeInMillis
                    AnimatedContent(
                        targetState = monthTime,
                        transitionSpec = {
                            if (targetState > initialState) {
                                // Geser ke kiri (bulan depan)
                                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                            } else {
                                // Geser ke kanan (bulan sebelumnya)
                                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                            }
                        },
                        label = "MonthSlide"
                    ) { _ ->
                        MonthGrid(
                            month = state.currentMonth,
                            selectedDate = state.selectedDate,
                            daysWithEntries = state.daysWithEntries,
                            p = p,
                            modifier = Modifier.padding(start = 16.dp, end = 14.dp),
                            onSelect = { viewModel.selectDate(it) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(17.dp))

            // ── Tanggal terpilih + tombol tambah (36 dp, radius 10, x=372..408) ──
            AnimatedEntry(delay = 300) {
                Column {
                    val selectedKey = EventRepository.keyOf(state.selectedDate)
                    val todayKey = EventRepository.keyOf(Calendar.getInstance())
                    val dateText = DateFormatters.formatDayLong(state.selectedDate.time)
                    val holidayName = NationalHolidays.nameOf(state.selectedDate)
                    Row(
                        Modifier.fillMaxWidth().padding(start = 31.dp, end = 32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(dateText) }
                                    if (selectedKey == todayKey) append(", hari ini")
                                },
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = p.textPrimary
                            )
                            // Nama hari libur (hanya libur nasional)
                            if (holidayName != null) {
                                Text(
                                    holidayName,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = p.holiday
                                )
                            }
                        }
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(p.plusButton)
                                .clickable {
                                    val key = EventRepository.keyOf(state.selectedDate)
                                    navController.navigate("add_event?date=$key")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, "Tambah", Modifier.size(30.dp), tint = Color.White)
                        }
                    }

                    Spacer(Modifier.height(11.dp))

                    // ── Daftar agenda: garis 2 dp (x=29..412), pitch item 111 ───────────────
                    Column(Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(1.5.dp).background(p.divider.copy(alpha = 0.5f)))
                        
                        if (state.selectedDayEntries.isEmpty()) {
                            Text(
                                "Tidak ada agenda pada tanggal ini.",
                                fontSize = 14.sp,
                                color = p.textSecondary,
                                modifier = Modifier.padding(start = 38.dp, top = 16.dp, bottom = 16.dp)
                            )
                        } else {
                            state.selectedDayEntries.forEach { entry ->
                                when (entry) {
                                    is CalendarEntry.Acara -> AgendaItem(
                                        icon = when (entry.event.typeEnum) {
                                            EventType.ACARA -> Icons.Outlined.CalendarToday
                                            EventType.PENGINGAT -> Icons.Outlined.NotificationsNone
                                            EventType.TARGET -> Icons.Outlined.PieChart
                                        },
                                        showDot = entry.event.typeEnum == EventType.PENGINGAT,
                                        typeLabel = when (entry.event.typeEnum) {
                                            EventType.ACARA -> "Acara"
                                            EventType.PENGINGAT -> "Mengingatkan"
                                            EventType.TARGET -> "Target"
                                        },
                                        title = entry.title,
                                        timeRange = "${DateFormatters.formatTime(java.util.Date(entry.startMillis))} - ${DateFormatters.formatTime(java.util.Date(entry.endMillis))} WIB",
                                        p = p,
                                        onClick = {
                                            val key = EventRepository.keyOf(state.selectedDate)
                                            navController.navigate("add_event?editId=${entry.event.id}&date=$key")
                                        },
                                        onLongClick = {
                                            val key = EventRepository.keyOf(state.selectedDate)
                                            viewModel.requestDelete(entry.event, key)
                                        }
                                    )
                                    is CalendarEntry.Tugas -> AgendaItem(
                                        icon = Icons.Outlined.RadioButtonUnchecked,
                                        showDot = false,
                                        typeLabel = "Tugas",
                                        title = entry.title,
                                        timeRange = "${DateFormatters.formatTime(java.util.Date(entry.startMillis))} WIB",
                                        p = p,
                                        onClick = { navController.navigate(Routes.submitTask(entry.task.id)) },
                                        onLongClick = { }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (state.showRepeatDeleteDialog) {
        KuliahkuDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = "Hapus Acara Berulang",
            text = "Acara ini berulang. Hapus hanya kemunculan ini, atau seluruh rangkaian acara ke depannya?",
            icon = Icons.Default.DeleteSweep,
            iconColor = MaterialTheme.colorScheme.error,
            confirmText = "Semua",
            confirmColor = MaterialTheme.colorScheme.error,
            onConfirm = viewModel::deleteAllOccurrences,
            dismissText = "Hanya Ini",
            onDismiss = viewModel::deleteOnlyThis
        )
    } else if (state.pendingDeleteEvent != null) {
        KuliahkuDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = "Hapus Acara?",
            text = "Acara ini akan dihapus secara permanen dari kalender Anda.",
            icon = Icons.Default.WarningAmber,
            iconColor = MaterialTheme.colorScheme.error,
            confirmText = "Hapus",
            confirmColor = MaterialTheme.colorScheme.error,
            onConfirm = viewModel::deleteAllOccurrences,
            dismissText = "Batal",
            onDismiss = viewModel::cancelDelete
        )
    }
}

// Baris tanggal setinggi 53 dp; lingkaran terpilih berdiameter 36 dp; angka 2 digit.
// Warna angka: terpilih (putih) > Minggu/libur nasional (merah) > hari ini (aksen) > bulan lain (redup) > biasa.
@Composable
private fun MonthGrid(
    month: Calendar,
    selectedDate: Calendar,
    daysWithEntries: Set<String>,
    p: CalPalette,
    modifier: Modifier = Modifier,
    onSelect: (Calendar) -> Unit
) {
    val firstDay = (month.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Minggu
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val prevMonth = (month.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
    val prevDays = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

    val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7
    val cells = mutableListOf<Pair<Calendar, Boolean>>()

    for (i in 0 until totalCells) {
        val dayIndex = i - firstDayOfWeek + 1
        val cal = (month.clone() as Calendar)
        if (dayIndex < 1) {
            cal.add(Calendar.MONTH, -1)
            cal.set(Calendar.DAY_OF_MONTH, prevDays + dayIndex)
            cells.add(cal to false)
        } else if (dayIndex > daysInMonth) {
            cal.add(Calendar.MONTH, 1)
            cal.set(Calendar.DAY_OF_MONTH, dayIndex - daysInMonth)
            cells.add(cal to false)
        } else {
            cal.set(Calendar.DAY_OF_MONTH, dayIndex)
            cells.add(cal to true)
        }
    }

    val todayKey = EventRepository.keyOf(Calendar.getInstance())
    val selectedKey = EventRepository.keyOf(selectedDate)

    Column(modifier) {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { (cal, isCurrentMonth) ->
                    val key = EventRepository.keyOf(cal)
                    val isSelected = key == selectedKey
                    val isToday = key == todayKey
                    val hasEntries = daysWithEntries.contains(key)
                    val isRedDay = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ||
                            NationalHolidays.nameOf(cal) != null

                    Box(
                        Modifier
                            .weight(1f)
                            .height(53.dp)
                            .clickable { onSelect(cal) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) p.selected else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "%02d".format(cal.get(Calendar.DAY_OF_MONTH)),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                // Tanggal Bold
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isSelected -> p.selectedText
                                    isRedDay && isCurrentMonth -> p.holiday
                                    isRedDay -> p.holiday.copy(alpha = 0.45f)
                                    isToday -> p.accent
                                    !isCurrentMonth -> p.mutedDay
                                    else -> p.textPrimary
                                }
                            )
                        }
                        // Titik penanda agenda (tetap muncul saat terpilih)
                        if (SHOW_ENTRY_DOTS && hasEntries) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 4.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) p.selectedText else p.accent)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Item agenda: garis 2 dp di atas, ikon 30 dp di x=38, teks mulai x=85,
// label tipe 14sp / judul 16sp bold / jam 13sp semibold, tinggi item 111 dp.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AgendaItem(
    icon: ImageVector,
    showDot: Boolean,
    typeLabel: String,
    title: String,
    timeRange: String,
    p: CalPalette,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 6.dp)
            .height(111.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, p.divider.copy(alpha = 0.5f))
    ) {
        Row(
            Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).background(p.accent.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = p.agendaIcon, modifier = Modifier.size(24.dp))
                if (showDot) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(10.dp)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .background(p.accent, CircleShape)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    typeLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = p.textSecondary
                )
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    timeRange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.textSecondary
                )
            }
        }
    }
}
