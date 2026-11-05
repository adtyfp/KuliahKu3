package com.kuliahku.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kuliahku.R
import com.kuliahku.data.model.EventRepeat
import com.kuliahku.data.model.EventType
import com.kuliahku.data.model.ReminderStyle
import com.kuliahku.ui.theme.BadgeHighBg
import com.kuliahku.ui.theme.BadgeLowBg
import com.kuliahku.ui.theme.BadgeMediumBg
import com.kuliahku.ui.theme.cardShadow
import com.kuliahku.ui.components.WheelDateTimePicker
import java.util.Calendar

// ─────────────────────────────────────────────────────────────────────────────
// PALET
// Light = diukur dari Tambah_Kalender.png (440 x 956, 1 px = 1 dp).
// Dark  = #0E1220 (latar), #171C2C (kartu), #2A3350 (border), #F1F4FF (teks utama),
//         teks sekunder abu kebiruan (sama dengan HomeScreen).
// ─────────────────────────────────────────────────────────────────────────────
private class EventPalette(
    val background: Color,
    val surface: Color,
    val border: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val chevron: Color,
    val switchOff: Color,
    val switchOn: Color,
    val button: Color
)

private val LightEventPalette = EventPalette(
    background = Color(0xFFFBFCFF),
    surface = Color.White,
    border = Color(0xFFE0E0E0),
    divider = Color(0xFFA8A8A8),
    textPrimary = Color.Black,
    textSecondary = Color(0xFF808080),
    chevron = Color(0xFF7D7D7D),
    switchOff = Color(0xFFE7E7E7),
    switchOn = Color(0xFF0F79DC),
    button = Color(0xFF0F79DC)
)

private val DarkEventPalette = EventPalette(
    background = Color(0xFF0E1220),
    surface = Color(0xFF171C2C),
    border = Color(0xFF2A3350),
    divider = Color(0xFF2A3350),
    textPrimary = Color(0xFFF1F4FF),
    textSecondary = Color(0xFFA3ABC4),
    chevron = Color(0xFFA3ABC4),
    switchOff = Color(0xFF2A3350),
    switchOn = Color(0xFF3B6FE8),
    button = Color(0xFF3B6FE8)
)

@Composable
private fun eventPalette(): EventPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DarkEventPalette else LightEventPalette

/**
 * Desain TIDAK menampilkan chip jenis (Acara / Pengingat / Target).
 * Default false = mengikuti desain. Ubah ke true untuk mengembalikan pemilih jenis acara.
 */
private const val SHOW_TYPE_CHIPS = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    navController: NavHostController,
    viewModel: AddEventViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var expandedStart by remember { mutableStateOf(false) }
    var expandedEnd by remember { mutableStateOf(false) }
    var expandedRepeat by remember { mutableStateOf(false) }
    var expandedStyle by remember { mutableStateOf(false) }
    var expandedOffset by remember { mutableStateOf(false) }
    val p = eventPalette()

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                AddEventEvent.Saved -> { snackbar.showSnackbar("Acara disimpan."); navController.popBackStack() }
                is AddEventEvent.ShowMessage -> snackbar.showSnackbar(ev.message)
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
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 25.dp, end = 22.dp)
            ) {
                Spacer(Modifier.height(57.dp))

                // ── Header + ilustrasi (panah tengah y=75; ilustrasi 182x148 di y=74..221) ──
                Box(Modifier.fillMaxWidth().height(164.dp)) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(36.dp).offset(x = (-3).dp)
                    ) {
                        Icon(
                            Icons.Filled.ArrowBackIosNew,
                            contentDescription = "Kembali",
                            modifier = Modifier.size(24.dp),
                            tint = p.textPrimary
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.ic_calendar_add),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = 3.dp, y = 17.dp)
                            .size(width = 182.dp, height = 148.dp)
                    )
                }

                // ── Judul (baseline y=277) ──
                Spacer(Modifier.height(27.dp))
                Text(
                    if (state.isEdit) "Edit Acara" else "Tambah Acara",
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.textPrimary
                )
                Spacer(Modifier.height(14.dp))

                // ── Judul Acara: pill 51 dp ──
                TitleField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    placeholder = "Judul Acara",
                    p = p
                )
                state.titleError?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                if (SHOW_TYPE_CHIPS) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EventTypeChip("Acara", state.type == EventType.ACARA, BadgeMediumBg) { viewModel.onTypeChange(EventType.ACARA) }
                        EventTypeChip("Pengingat", state.type == EventType.PENGINGAT, BadgeHighBg) { viewModel.onTypeChange(EventType.PENGINGAT) }
                        EventTypeChip("Target", state.type == EventType.TARGET, BadgeLowBg) { viewModel.onTypeChange(EventType.TARGET) }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Kartu 1: Seharian + waktu (x=25..417, radius 20) ──
                Column(
                    Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(250, easing = FastOutSlowInEasing))
                        .cardSurface(p, RoundedCornerShape(20.dp))
                        .padding(vertical = 9.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).padding(start = 26.dp, end = 27.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Seharian",
                            Modifier.weight(1f),
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = p.textPrimary
                        )
                        MiniSwitch(checked = state.allDay, onCheckedChange = viewModel::onAllDayChange, p = p)
                    }
                    RowDivider(p)
                    FieldRow(
                        label = "Waktu Mulai", value = state.startLabel, p = p,
                        startPadding = 26.dp, chevronEnd = 27.dp,
                        isExpanded = expandedStart
                    ) {
                        expandedStart = !expandedStart
                        if (expandedStart) {
                            expandedEnd = false
                            expandedRepeat = false
                            expandedStyle = false
                            expandedOffset = false
                        }
                    }
                    AnimatedVisibility(
                        visible = expandedStart,
                        enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(250)),
                        exit = shrinkVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(250))
                    ) {
                        WheelDateTimePicker(
                            initialMillis = state.startMillis,
                            onDateTimeChanged = viewModel::onStartChange,
                            textPrimary = p.textPrimary,
                            textSecondary = p.textSecondary
                        )
                    }
                    RowDivider(p)
                    FieldRow(
                        label = "Waktu Berakhir", value = state.endLabel, p = p,
                        startPadding = 26.dp, chevronEnd = 27.dp,
                        isExpanded = expandedEnd
                    ) {
                        expandedEnd = !expandedEnd
                        if (expandedEnd) {
                            expandedStart = false
                            expandedRepeat = false
                            expandedStyle = false
                            expandedOffset = false
                        }
                    }
                    AnimatedVisibility(
                        visible = expandedEnd,
                        enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(250)),
                        exit = shrinkVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(250))
                    ) {
                        WheelDateTimePicker(
                            initialMillis = state.endMillis,
                            onDateTimeChanged = viewModel::onEndChange,
                            textPrimary = p.textPrimary,
                            textSecondary = p.textSecondary
                        )
                    }
                    state.timeError?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 26.dp, top = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Kartu 2: Pengulangan, Gaya Pengingat, Waktu Pengingat ──
                Column(
                    Modifier
                        .fillMaxWidth()
                        .cardSurface(p, RoundedCornerShape(20.dp))
                        .padding(top = 4.dp, bottom = 6.dp)
                ) {
                    DropdownField(
                        label = "Pengulangan",
                        value = when (state.repeat) {
                            EventRepeat.NONE -> "Tidak Pernah"
                            EventRepeat.DAILY -> "Harian"
                            EventRepeat.WEEKLY -> "Mingguan"
                            EventRepeat.MONTHLY -> "Bulanan"
                            EventRepeat.YEARLY -> "Tahunan"
                        },
                        options = listOf(
                            "Tidak Pernah" to EventRepeat.NONE,
                            "Harian" to EventRepeat.DAILY,
                            "Mingguan" to EventRepeat.WEEKLY,
                            "Bulanan" to EventRepeat.MONTHLY,
                            "Tahunan" to EventRepeat.YEARLY
                        ),
                        p = p,
                        expanded = expandedRepeat,
                        onExpandToggle = {
                            expandedRepeat = !expandedRepeat
                            if (expandedRepeat) {
                                expandedStart = false
                                expandedEnd = false
                                expandedStyle = false
                                expandedOffset = false
                            }
                        },
                        onSelect = { viewModel.onRepeatChange(it) }
                    )
                    RowDivider(p)
                    DropdownField(
                        label = "Gaya Pengingat",
                        value = when (state.reminderStyle) {
                            ReminderStyle.BANNER -> "Spanduk"
                            ReminderStyle.NOTIFICATION -> "Notifikasi + Suara"
                            ReminderStyle.ALARM -> "Alarm"
                        },
                        options = listOf(
                            "Spanduk" to ReminderStyle.BANNER,
                            "Notifikasi + Suara" to ReminderStyle.NOTIFICATION,
                            "Alarm" to ReminderStyle.ALARM
                        ),
                        p = p,
                        expanded = expandedStyle,
                        onExpandToggle = {
                            expandedStyle = !expandedStyle
                            if (expandedStyle) {
                                expandedStart = false
                                expandedEnd = false
                                expandedRepeat = false
                                expandedOffset = false
                            }
                        },
                        onSelect = { viewModel.onStyleChange(it) }
                    )
                    RowDivider(p)
                    DropdownField(
                        label = "Waktu Pengingat",
                        value = offsetLabel(state.reminderOffsetMinutes),
                        options = listOf(
                            "Tepat waktu" to 0,
                            "5 Menit Sebelumnya" to 5,
                            "10 Menit Sebelumnya" to 10,
                            "15 Menit Sebelumnya" to 15,
                            "30 Menit Sebelumnya" to 30,
                            "1 Jam Sebelumnya" to 60,
                            "1 Hari Sebelumnya" to 1440
                        ),
                        p = p,
                        expanded = expandedOffset,
                        onExpandToggle = {
                            expandedOffset = !expandedOffset
                            if (expandedOffset) {
                                expandedStart = false
                                expandedEnd = false
                                expandedRepeat = false
                                expandedStyle = false
                            }
                        },
                        onSelect = { viewModel.onOffsetChange(it) }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Tombol Submit: x=25..413, tinggi 52, dasar y=913 (bawah 43) ──
            val enabled = !state.isSaving
            Box(
                modifier = Modifier
                    .padding(start = 25.dp, end = 27.dp, bottom = 43.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(p.button.copy(alpha = if (enabled) 1f else 0.5f), CircleShape)
                    .clickable(enabled = enabled, onClick = viewModel::save),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (state.isSaving) "Menyimpan..." else "Submit",
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

/** Latar + border 1 dp + bayangan lembut, sama untuk kolom judul dan kedua kartu. */
@Composable
private fun Modifier.cardSurface(p: EventPalette, shape: Shape, cornerRadius: Dp = 20.dp): Modifier =
    this
        .cardShadow(shapeRadius = cornerRadius)
        .background(p.surface, shape)
        .border(1.dp, p.border, shape)

@Composable
private fun TitleField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    p: EventPalette
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(51.dp)
            .cardSurface(p, CircleShape, cornerRadius = 26.dp)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Pada desain placeholder berwarna hitam (sama dengan teks).
        if (value.isEmpty()) {
            Text(
                placeholder,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                color = p.textPrimary,
                maxLines = 1
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                color = p.textPrimary
            ),
            cursorBrush = SolidColor(p.textPrimary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Garis pemisah 1 dp di dalam kartu (x=44..397 pada desain). */
@Composable
private fun RowDivider(p: EventPalette) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 19.dp, end = 20.dp)
            .height(1.dp)
            .background(p.divider)
    )
}

/** Switch mini 35 x 19 dp (knob 14 dp) sesuai desain. */
@Composable
private fun MiniSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, p: EventPalette) {
    Box(
        modifier = Modifier
            .size(width = 35.dp, height = 19.dp)
            .clip(CircleShape)
            .background(if (checked) p.switchOn else p.switchOff)
            .clickable { onCheckedChange(!checked) }
            .padding(2.5.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
private fun EventTypeChip(label: String, selected: Boolean, bg: Color, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (selected) bg else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

// Baris waktu: tinggi 54, judul 16sp medium (lh 20) + nilai 12sp semibold (lh 16).
@Composable
private fun FieldRow(
    label: String,
    value: String,
    p: EventPalette,
    startPadding: Dp,
    chevronEnd: Dp,
    isExpanded: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(onClick = onClick)
            .padding(start = startPadding, end = chevronEnd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowLabel(label, value, p, Modifier.weight(1f))
        Icon(
            if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.UnfoldMore,
            contentDescription = null,
            tint = p.chevron,
            modifier = Modifier.padding(end = 5.dp).size(24.dp)
        )
    }
}

@Composable
private fun RowLabel(label: String, value: String, p: EventPalette, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            color = p.textPrimary
        )
        Text(
            value,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = p.textSecondary
        )
    }
}

// Baris dropdown kartu 2: tinggi 52, teks mulai x=49 (padding 24), chevron tengah x=370.
@Composable
private fun <T> DropdownField(
    label: String,
    value: String,
    options: List<Pair<String, T>>,
    p: EventPalette,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onSelect: (T) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(250, easing = FastOutSlowInEasing))) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable { onExpandToggle() }
                .padding(start = 24.dp, end = 30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowLabel(label, value, p, Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = p.chevron,
                modifier = Modifier.size(34.dp)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(250)),
            exit = shrinkVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(250))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                options.forEach { (lbl, v) ->
                    val isSelected = lbl == value
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(v); onExpandToggle() }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = lbl,
                            fontSize = 15.sp,
                            color = if (isSelected) p.button else p.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

private fun offsetLabel(minutes: Int?): String = when (minutes) {
    null -> "Spanduk"
    0 -> "Tepat waktu"
    5 -> "5 Menit Sebelumnya"
    10 -> "10 Menit Sebelumnya"
    15 -> "15 Menit Sebelumnya"
    30 -> "30 Menit Sebelumnya"
    60 -> "1 Jam Sebelumnya"
    1440 -> "1 Hari Sebelumnya"
    else -> "$minutes menit"
}
