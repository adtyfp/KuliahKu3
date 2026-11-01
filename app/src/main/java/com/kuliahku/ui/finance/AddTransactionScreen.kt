package com.kuliahku.ui.finance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kuliahku.R
import com.kuliahku.data.model.TxType
import java.util.Calendar

import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ---------------------------------------------------------------------------
// Palet Catat Keuangan (Light = sesuai Catat_Keuangan.png, Dark = nilai yang sama dengan Beranda).
// Mode gelap dideteksi dari MaterialTheme.colorScheme.background.
// ---------------------------------------------------------------------------
private class AddPalette(
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val placeholder: Color,
    val chevron: Color,
    val segmentBg: Color,
    val segmentText: Color,
    val accent: Color,
    val error: Color,
    val decor: Color,
    val shadowAlpha: Float
)

private val AddLight = AddPalette(
    background = Color(0xFFF8F9FF),
    surface = Color.White,
    border = Color(0xFFE4E4E8),
    textPrimary = Color.Black,
    placeholder = Color(0xFF7A7A7A),
    chevron = Color(0xFF666666),
    segmentBg = Color(0xFFEEF1F8),
    segmentText = Color(0xFF666666),
    accent = Color(0xFF0F79DC),
    error = Color(0xFFC80000),
    decor = Color(0xFF808080),
    shadowAlpha = 0.10f
)

private val AddDark = AddPalette(
    background = Color(0xFF0E1220),
    surface = Color(0xFF171C2C),
    border = Color(0xFF2A3350),
    textPrimary = Color(0xFFF1F4FF),
    placeholder = Color(0xFF8B93AE),
    chevron = Color(0xFFA3ABC4),
    segmentBg = Color(0xFF1B2134),
    segmentText = Color(0xFFA3ABC4),
    accent = Color(0xFF0F79DC),
    error = Color(0xFFFF6B7A),
    decor = Color(0xFF5C6585),
    shadowAlpha = 0.45f
)

@Composable
private fun addPalette(): AddPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) AddDark else AddLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavHostController,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingCal by remember { mutableStateOf<Calendar?>(null) }
    val p = addPalette()

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                AddTransactionEvent.Saved -> { snackbar.showSnackbar("Transaksi disimpan."); navController.popBackStack() }
                is AddTransactionEvent.ShowMessage -> snackbar.showSnackbar(ev.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = p.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .imePadding()
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 25.dp)
            ) {
                // ---------------- Header + ilustrasi ----------------
                Box(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .offset(x = (-10).dp, y = 12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(ChevronLeftIcon, "Kembali", tint = p.textPrimary, modifier = Modifier.size(24.dp))
                    }
                    FinanceIllustration(
                        decorTint = p.decor,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 18.dp)
                            .fillMaxWidth(0.61f)
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "Catat Keuanganmu",
                    fontSize = 32.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.textPrimary
                )
                Spacer(Modifier.height(16.dp))

                // Pilihan jenis transaksi (Pemasukan / Pengeluaran)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(p.segmentBg)
                        .padding(3.dp)
                ) {
                    TypeChip("Pemasukan", state.type == TxType.INCOME, p) { viewModel.onTypeChange(TxType.INCOME) }
                    TypeChip("Pengeluaran", state.type == TxType.EXPENSE, p) { viewModel.onTypeChange(TxType.EXPENSE) }
                }

                Spacer(Modifier.height(16.dp))

                // ---------------- Jumlah Uang ----------------
                PillInput(
                    value = state.amountText,
                    onValueChange = viewModel::onAmountChange,
                    placeholder = "Jumlah Uang",
                    palette = p,
                    keyboardType = KeyboardType.Number,
                    hasError = state.amountError != null
                )
                FieldError(state.amountError, p)

                Spacer(Modifier.height(17.dp))

                // ---------------- Kategori ----------------
                var expanded by remember { mutableStateOf(false) }
                var fieldWidth by remember { mutableStateOf(0.dp) }
                val density = LocalDensity.current
                Box(Modifier.fillMaxWidth().onSizeChanged { fieldWidth = with(density) { it.width.toDp() } }) {
                    PillPicker(
                        text = state.category,
                        placeholder = "Kategori",
                        palette = p,
                        hasError = state.categoryError != null,
                        trailing = {
                            Icon(ChevronDownIcon, null, tint = p.chevron, modifier = Modifier.size(24.dp))
                        },
                        onClick = { expanded = true }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(fieldWidth)
                    ) {
                        state.categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = { viewModel.onCategoryChange(c); expanded = false }
                            )
                        }
                    }
                }
                FieldError(state.categoryError, p)

                Spacer(Modifier.height(17.dp))

                // ---------------- Tanggal ----------------
                PillPicker(
                    text = state.dateLabel,
                    placeholder = "Tanggal",
                    palette = p,
                    onClick = { showDatePicker = true }
                )

                Spacer(Modifier.height(17.dp))

                // ---------------- Catatan (opsional) ----------------
                PillInput(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    placeholder = "Catatan (opsional)",
                    palette = p
                )

                Spacer(Modifier.height(24.dp))
            }

            // ---------------- Submit ----------------
            val canSubmit = !state.isSaving
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 25.dp, end = 25.dp, top = 12.dp, bottom = 24.dp)
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(if (canSubmit) p.accent else p.accent.copy(alpha = 0.5f))
                    .clickable(enabled = canSubmit) { viewModel.save() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (state.isSaving) "Menyimpan..." else "Submit",
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = state.dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let {
                        val c = Calendar.getInstance().apply { timeInMillis = it }
                        pendingCal = c
                        showDatePicker = false
                        showTimePicker = true
                    }
                }) { Text("Lanjut") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } }
        ) { DatePicker(state = dpState) }
    }

    if (showTimePicker) {
        val tpState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pilih Waktu") },
            text = { TimePicker(state = tpState) },
            confirmButton = {
                TextButton(onClick = {
                    val c = pendingCal ?: Calendar.getInstance()
                    c.set(Calendar.HOUR_OF_DAY, tpState.hour)
                    c.set(Calendar.MINUTE, tpState.minute)
                    c.set(Calendar.SECOND, 0)
                    viewModel.onDateChange(c.timeInMillis)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Batal") } }
        )
    }
}
@Composable
private fun FinanceIllustration(decorTint: Color, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(1253f / 890f)) {
        Image(
            painter = painterResource(id = R.drawable.ic_finance_add),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

// ---------------------------------------------------------------------------
// Komponen field berbentuk pill (tinggi 49dp, border tipis, bayangan lembut)
// ---------------------------------------------------------------------------
@Composable
private fun PillContainer(
    palette: AddPalette,
    hasError: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val shadowColor = Color.Black.copy(alpha = palette.shadowAlpha)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(49.dp)
            .shadow(6.dp, CircleShape, ambientColor = shadowColor, spotColor = shadowColor),
        shape = CircleShape,
        color = palette.surface,
        border = BorderStroke(1.dp, if (hasError) palette.error else palette.border)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(start = 26.dp, end = 27.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun PillInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    palette: AddPalette,
    keyboardType: KeyboardType = KeyboardType.Text,
    hasError: Boolean = false
) {
    PillContainer(palette, hasError) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            ),
            cursorBrush = SolidColor(palette.textPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.placeholder
                        )
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun PillPicker(
    text: String,
    placeholder: String,
    palette: AddPalette,
    hasError: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    PillContainer(palette, hasError, onClick = onClick) {
        Text(
            text.ifBlank { placeholder },
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (text.isBlank()) palette.placeholder else palette.textPrimary,
            maxLines = 1
        )
        trailing?.invoke()
    }
}

@Composable
private fun FieldError(message: String?, palette: AddPalette) {
    if (!message.isNullOrBlank()) {
        Text(
            message,
            modifier = Modifier.padding(start = 26.dp, top = 4.dp),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = palette.error
        )
    }
}

@Composable
private fun RowScope.TypeChip(label: String, selected: Boolean, palette: AddPalette, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(CircleShape)
            .background(if (selected) palette.accent else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else palette.segmentText
        )
    }
}

// ---------------------------------------------------------------------------
// Ikon vektor kecil (chevron kembali & dropdown)
// ---------------------------------------------------------------------------
private fun strokeIcon(name: String, d: String, width: Float): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(
        pathData = addPathNodes(d),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()

private val ChevronLeftIcon = strokeIcon("ChevronLeft", "M15 4L7 12l8 8", 2.2f)
private val ChevronDownIcon = strokeIcon("ChevronDown", "M5 8.5l7 7 7-7", 2.4f)