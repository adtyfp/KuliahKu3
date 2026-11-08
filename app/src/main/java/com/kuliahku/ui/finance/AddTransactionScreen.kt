package com.kuliahku.ui.finance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kuliahku.R
import com.kuliahku.data.model.TxType
import com.kuliahku.ui.components.*

@Composable
fun AddTransactionScreen(
    navController: NavHostController,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var expanded by remember { mutableStateOf<String?>(null) }
    val p = formPalette()

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                AddTransactionEvent.Saved -> {
                    snackbar.showSnackbar("Transaksi disimpan.")
                    navController.popBackStack()
                }
                is AddTransactionEvent.ShowMessage -> snackbar.showSnackbar(ev.message)
            }
        }
    }

    FormScreen(
        snackbarHostState = snackbar,
        p = p,
        buttonText = if (state.isSaving) "Menyimpan..." else "Simpan Transaksi",
        buttonEnabled = !state.isSaving,
        onButtonClick = { viewModel.save() }
    ) {
        FormHeader(
            title = "Catat Keuanganmu",
            subtitle = "Catat pemasukan dan pengeluaranmu dengan rapi",
            p = p,
            onBack = { navController.popBackStack() },
            illustration = R.drawable.ic_finance_add
        )

        FormSegmented(
            options = listOf("Pemasukan", "Pengeluaran"),
            selectedIndex = if (state.type == TxType.INCOME) 0 else 1,
            p = p
        ) { index ->
            viewModel.onTypeChange(if (index == 0) TxType.INCOME else TxType.EXPENSE)
        }

        FormSpacer()

        FormTextField(
            label = "Jumlah Uang",
            value = state.amountText,
            onValueChange = viewModel::onAmountChange,
            p = p,
            placeholder = "0",
            prefix = "Rp",
            keyboardType = KeyboardType.Number,
            error = state.amountError
        )

        FormSpacer()

        FormDropdownField(
            label = "Kategori",
            options = state.categories.map { it to it },
            selected = state.category.ifBlank { null },
            p = p,
            expanded = expanded == "category",
            onToggle = { expanded = if (expanded == "category") null else "category" },
            onSelect = { viewModel.onCategoryChange(it) },
            placeholder = "Pilih kategori",
            error = state.categoryError
        )

        FormSpacer()

        FormExpandableField(
            label = "Tanggal",
            value = state.dateLabel,
            placeholder = "Pilih tanggal",
            p = p,
            expanded = expanded == "date",
            onToggle = { expanded = if (expanded == "date") null else "date" },
            leadingIcon = Icons.Filled.DateRange
        ) {
            Box(Modifier.padding(horizontal = 8.dp)) {
                WheelDateTimePicker(
                    initialMillis = state.dateMillis,
                    onDateTimeChanged = viewModel::onDateChange,
                    textPrimary = p.textPrimary,
                    textSecondary = p.textSecondary
                )
            }
        }

        FormSpacer()

        FormTextField(
            label = "Catatan",
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            p = p,
            placeholder = "Tambahkan catatan",
            optional = true
        )
    }
}