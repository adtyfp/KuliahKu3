package com.kuliahku.ui.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kuliahku.R
import com.kuliahku.data.model.EventRepeat
import com.kuliahku.data.model.EventType
import com.kuliahku.data.model.ReminderStyle
import com.kuliahku.ui.components.*

/** Desain tidak menampilkan pemilih jenis (Acara / Pengingat / Target). Ubah ke true untuk menampilkannya. */
private const val SHOW_TYPE_CHIPS = false

@Composable
fun AddEventScreen(
    navController: NavHostController,
    viewModel: AddEventViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    // Hanya satu bagian yang terbuka pada satu waktu.
    var expanded by remember { mutableStateOf<String?>(null) }
    val toggle: (String) -> Unit = { key -> expanded = if (expanded == key) null else key }
    val p = formPalette()

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                AddEventEvent.Saved -> {
                    snackbar.showSnackbar("Acara disimpan.")
                    navController.popBackStack()
                }
                is AddEventEvent.ShowMessage -> snackbar.showSnackbar(ev.message)
            }
        }
    }

    FormScreen(
        snackbarHostState = snackbar,
        p = p,
        buttonText = if (state.isSaving) "Menyimpan..." else "Simpan Acara",
        buttonEnabled = !state.isSaving,
        onButtonClick = { viewModel.save() }
    ) {
        FormHeader(
            title = if (state.isEdit) "Edit Acara" else "Tambah Acara",
            subtitle = "Atur jadwal dan pengingat kegiatanmu",
            p = p,
            onBack = { navController.popBackStack() },
            illustration = R.drawable.ic_calendar_add
        )

        FormTextField(
            label = "Judul Acara",
            value = state.title,
            onValueChange = viewModel::onTitleChange,
            p = p,
            placeholder = "Contoh: Kuis Basis Data",
            error = state.titleError
        )

        if (SHOW_TYPE_CHIPS) {
            FormSpacer()
            val types = listOf(EventType.ACARA, EventType.PENGINGAT, EventType.TARGET)
            FormSegmented(
                options = listOf("Acara", "Pengingat", "Target"),
                selectedIndex = types.indexOf(state.type).coerceAtLeast(0),
                p = p
            ) { viewModel.onTypeChange(types[it]) }
        }

        FormSpacer()
        FormSectionTitle("WAKTU", p)

        FormSwitchRow("Seharian", state.allDay, viewModel::onAllDayChange, p)

        FormSpacer()

        FormExpandableField(
            label = "Waktu Mulai",
            value = state.startLabel,
            placeholder = "Pilih waktu mulai",
            p = p,
            expanded = expanded == "start",
            onToggle = { toggle("start") },
            leadingIcon = Icons.Filled.Schedule
        ) {
            Box(Modifier.padding(horizontal = 8.dp)) {
                WheelDateTimePicker(
                    initialMillis = state.startMillis,
                    onDateTimeChanged = viewModel::onStartChange,
                    textPrimary = p.textPrimary,
                    textSecondary = p.textSecondary
                )
            }
        }

        FormSpacer()

        FormExpandableField(
            label = "Waktu Berakhir",
            value = state.endLabel,
            placeholder = "Pilih waktu berakhir",
            p = p,
            expanded = expanded == "end",
            onToggle = { toggle("end") },
            error = state.timeError,
            leadingIcon = Icons.Filled.Schedule
        ) {
            Box(Modifier.padding(horizontal = 8.dp)) {
                WheelDateTimePicker(
                    initialMillis = state.endMillis,
                    onDateTimeChanged = viewModel::onEndChange,
                    textPrimary = p.textPrimary,
                    textSecondary = p.textSecondary
                )
            }
        }

        FormSpacer()
        FormSectionTitle("PENGINGAT", p)

        FormDropdownField(
            label = "Pengulangan",
            options = listOf(
                "Tidak Pernah" to EventRepeat.NONE,
                "Harian" to EventRepeat.DAILY,
                "Mingguan" to EventRepeat.WEEKLY,
                "Bulanan" to EventRepeat.MONTHLY,
                "Tahunan" to EventRepeat.YEARLY
            ),
            selected = state.repeat,
            p = p,
            expanded = expanded == "repeat",
            onToggle = { toggle("repeat") },
            onSelect = { viewModel.onRepeatChange(it) }
        )

        FormSpacer()

        FormDropdownField(
            label = "Gaya Pengingat",
            options = listOf(
                "Spanduk" to ReminderStyle.BANNER,
                "Notifikasi + Suara" to ReminderStyle.NOTIFICATION,
                "Alarm" to ReminderStyle.ALARM
            ),
            selected = state.reminderStyle,
            p = p,
            expanded = expanded == "style",
            onToggle = { toggle("style") },
            onSelect = { viewModel.onStyleChange(it) }
        )

        FormSpacer()

        FormDropdownField(
            label = "Waktu Pengingat",
            options = listOf(
                "Tepat waktu" to 0,
                "5 Menit Sebelumnya" to 5,
                "10 Menit Sebelumnya" to 10,
                "15 Menit Sebelumnya" to 15,
                "30 Menit Sebelumnya" to 30,
                "1 Jam Sebelumnya" to 60,
                "1 Hari Sebelumnya" to 1440
            ),
            selected = state.reminderOffsetMinutes,
            p = p,
            expanded = expanded == "offset",
            onToggle = { toggle("offset") },
            onSelect = { viewModel.onOffsetChange(it) },
            placeholder = "Pilih waktu pengingat"
        )
    }
}