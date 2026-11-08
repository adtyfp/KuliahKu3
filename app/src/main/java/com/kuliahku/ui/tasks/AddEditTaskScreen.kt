package com.kuliahku.ui.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kuliahku.data.model.TaskPriority
import com.kuliahku.ui.components.*

@Composable
fun AddEditTaskScreen(
    navController: NavHostController,
    viewModel: AddEditTaskViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var expanded by remember { mutableStateOf<String?>(null) }
    val toggle: (String) -> Unit = { key -> expanded = if (expanded == key) null else key }
    val initialNow = remember { System.currentTimeMillis() }
    val p = formPalette()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AddEditTaskEvent.Saved -> navController.popBackStack()
                is AddEditTaskEvent.ShowMessage -> snackbar.showSnackbar(event.message)
            }
        }
    }

    FormScreen(
        snackbarHostState = snackbar,
        p = p,
        buttonText = if (state.isSaving) "Menyimpan..." else "Simpan Tugas",
        buttonEnabled = !state.isSaving,
        onButtonClick = { viewModel.save() }
    ) {
        FormHeader(
            title = "Tambah Tugas",
            subtitle = "Catat tugasmu agar tidak terlewat deadline",
            p = p,
            onBack = { navController.popBackStack() },
            art = TaskArt
        )

        FormTextField(
            label = "Nama Tugas",
            value = state.title,
            onValueChange = viewModel::onTitleChange,
            p = p,
            placeholder = "Contoh: Laporan Praktikum",
            error = state.titleError
        )

        FormSpacer()

        FormExpandableField(
            label = "Deadline",
            value = state.deadlineLabel,
            placeholder = "Pilih tanggal & waktu",
            p = p,
            expanded = expanded == "deadline",
            onToggle = { toggle("deadline") },
            error = state.deadlineError,
            leadingIcon = Icons.Filled.DateRange
        ) {
            Box(Modifier.padding(horizontal = 8.dp)) {
                WheelDateTimePicker(
                    initialMillis = state.deadlineMillis ?: initialNow,
                    onDateTimeChanged = viewModel::onDeadlineChange,
                    textPrimary = p.textPrimary,
                    textSecondary = p.textSecondary
                )
            }
        }

        FormSpacer()

        FormDropdownField(
            label = "Prioritas",
            options = listOf(
                "Rendah" to TaskPriority.LOW,
                "Sedang" to TaskPriority.MEDIUM,
                "Tinggi" to TaskPriority.HIGH
            ),
            selected = state.priority,
            p = p,
            expanded = expanded == "priority",
            onToggle = { toggle("priority") },
            onSelect = { viewModel.onPriorityChange(it) }
        )

        FormSpacer()

        FormTextField(
            label = "Deskripsi",
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            p = p,
            placeholder = "Tuliskan detail tugas",
            singleLine = false,
            optional = true
        )
    }
}