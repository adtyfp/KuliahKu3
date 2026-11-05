package com.kuliahku.ui.finance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
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
import com.kuliahku.ui.components.KuliahkuContentDialog
import com.kuliahku.ui.components.WheelDateTimePicker
import com.kuliahku.ui.theme.cardShadow
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
    val decor: Color
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
    decor = Color(0xFF808080)
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
    decor = Color(0xFF5C6585)
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
    var expandedDate by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }
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
                val categoryCorner by animateDpAsState(
                    targetValue = if (expandedCategory) 25.dp else 26.dp,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    label = "categoryCorner"
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(250, easing = FastOutSlowInEasing))
                        .cardShadow(shapeRadius = categoryCorner)
                        .background(p.surface, RoundedCornerShape(categoryCorner))
                        .border(1.dp, if (state.categoryError != null) p.error else p.border, RoundedCornerShape(categoryCorner))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(49.dp)
                            .clickable {
                                expandedCategory = !expandedCategory
                                if (expandedCategory) expandedDate = false
                            }
                            .padding(start = 26.dp, end = 27.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            state.category.ifBlank { "Kategori" },
                            modifier = Modifier.weight(1f),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.category.isBlank()) p.placeholder else p.textPrimary,
                            maxLines = 1
                        )
                        Icon(
                            if (expandedCategory) Icons.Filled.ExpandLess else Icons.Filled.UnfoldMore,
                            contentDescription = null,
                            tint = p.chevron,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    AnimatedVisibility(
                        visible = expandedCategory,
                        enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(250)),
                        exit = shrinkVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(250))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            state.categories.forEach { c ->
                                val isSelected = state.category == c
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onCategoryChange(c)
                                            expandedCategory = false
                                        }
                                        .padding(horizontal = 26.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        c,
                                        fontSize = 15.sp,
                                        color = if (isSelected) p.accent else p.textPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
                FieldError(state.categoryError, p)

                Spacer(Modifier.height(17.dp))

                // ---------------- Tanggal ----------------
                val dateCorner by animateDpAsState(
                    targetValue = if (expandedDate) 25.dp else 26.dp,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    label = "dateCorner"
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(250, easing = FastOutSlowInEasing))
                        .cardShadow(shapeRadius = dateCorner)
                        .background(p.surface, RoundedCornerShape(dateCorner))
                        .border(1.dp, p.border, RoundedCornerShape(dateCorner))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(49.dp)
                            .clickable {
                                expandedDate = !expandedDate
                                if (expandedDate) expandedCategory = false
                            }
                            .padding(start = 26.dp, end = 27.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            state.dateLabel.ifBlank { "Tanggal" },
                            modifier = Modifier.weight(1f),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.dateLabel.isBlank()) p.placeholder else p.textPrimary,
                            maxLines = 1
                        )
                        Icon(
                            if (expandedDate) Icons.Filled.ExpandLess else Icons.Filled.UnfoldMore,
                            contentDescription = null,
                            tint = p.chevron,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    AnimatedVisibility(
                        visible = expandedDate,
                        enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(250)),
                        exit = shrinkVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(250))
                    ) {
                        Box(Modifier.padding(bottom = 12.dp)) {
                            WheelDateTimePicker(
                                initialMillis = state.dateMillis,
                                onDateTimeChanged = viewModel::onDateChange,
                                textPrimary = p.textPrimary,
                                textSecondary = p.placeholder
                            )
                        }
                    }
                }

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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(49.dp)
            .cardShadow(shapeRadius = 24.5.dp),
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