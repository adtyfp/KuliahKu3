package com.kuliahku.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kuliahku.R
import com.kuliahku.data.model.TaskPriority
import com.kuliahku.ui.components.KuliahkuContentDialog
import com.kuliahku.ui.components.WheelDateTimePicker
import com.kuliahku.ui.theme.BadgeHighBg
import com.kuliahku.ui.theme.BadgeLowBg
import com.kuliahku.ui.theme.BadgeMediumBg

// ─────────────────────────────────────────────────────────────────────────────
// PALET
// Light = diukur dari KumpulkanTugas.png (440 x 956, 1 px = 1 dp).
// Dark  = #0E1220 (latar), #171C2C (kolom/kartu), #2A3350 (border), #F1F4FF (teks utama),
//         placeholder & teks sekunder = abu kebiruan (sama dengan HomeScreen).
// ─────────────────────────────────────────────────────────────────────────────
private class FormPalette(
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val placeholder: Color,
    val button: Color,
    val shadowAlpha: Float
)

private val LightFormPalette = FormPalette(
    background = Color(0xFFFBFCFF),
    surface = Color.White,
    border = Color(0xFFE0E0E0),
    textPrimary = Color.Black,
    placeholder = Color(0xFF808080),
    button = Color(0xFF0F79DC),
    shadowAlpha = 0.12f
)

private val DarkFormPalette = FormPalette(
    background = Color(0xFF0E1220),
    surface = Color(0xFF171C2C),
    border = Color(0xFF2A3350),
    textPrimary = Color(0xFFF1F4FF),
    placeholder = Color(0xFFA3ABC4),
    button = Color(0xFF3B6FE8),
    shadowAlpha = 0.45f
)

@Composable
private fun formPalette(): FormPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DarkFormPalette else LightFormPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    navController: NavHostController,
    viewModel: AddEditTaskViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var expandedDeadline by remember { mutableStateOf(false) }
    var expandedPriority by remember { mutableStateOf(false) }
    val p = formPalette()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AddEditTaskEvent.Saved -> { navController.popBackStack(); snackbar.showSnackbar("Tugas disimpan.") }
                is AddEditTaskEvent.ShowMessage -> snackbar.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = p.background,
        contentColor = p.textPrimary
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
                    .padding(start = 25.dp, end = 22.dp)
            ) {
                Spacer(Modifier.height(57.dp))

                // ── Header + ilustrasi (panah tengah y=75; ilustrasi 230x171 di y=65..236) ──
                Box(Modifier.fillMaxWidth().height(179.dp)) {
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
                        painter = painterResource(id = R.drawable.img_task_add),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = 4.dp, y = 8.dp)
                            .size(width = 230.dp, height = 171.dp)
                    )
                }

                // ── Judul (glyph y=252..284) ──
                Spacer(Modifier.height(12.dp))
                Text(
                    "Tambah Tugas",
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.textPrimary
                )
                Spacer(Modifier.height(44.dp))

                // ── Nama Tugas: pill 51 dp ──
                FormField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    placeholder = "Nama Tugas",
                    p = p,
                    height = 51.dp,
                    shape = CircleShape,
                    singleLine = true
                )
                FieldError(state.titleError)
                Spacer(Modifier.height(15.dp))

                // ── Deadline: pill 51 dp, membuka inline wheel picker ──
                val deadlineCorner by animateDpAsState(
                    targetValue = if (expandedDeadline) 25.dp else 26.dp,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    label = "deadlineCorner"
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(250, easing = FastOutSlowInEasing))
                        .formSurface(p, RoundedCornerShape(deadlineCorner))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(51.dp)
                            .clickable {
                                expandedDeadline = !expandedDeadline
                                if (expandedDeadline) expandedPriority = false
                            }
                            .padding(horizontal = 26.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hasDeadline = state.deadlineLabel.isNotBlank()
                        Text(
                            if (hasDeadline) state.deadlineLabel else "Deadline Tugas",
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontWeight = if (hasDeadline) FontWeight.Medium else FontWeight.SemiBold,
                            color = if (hasDeadline) p.textPrimary else p.placeholder,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (expandedDeadline) Icons.Filled.ExpandLess else Icons.Filled.UnfoldMore,
                            contentDescription = null,
                            tint = p.placeholder,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    AnimatedVisibility(
                        visible = expandedDeadline,
                        enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(250)),
                        exit = shrinkVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(250))
                    ) {
                        Box(Modifier.padding(bottom = 12.dp)) {
                            WheelDateTimePicker(
                                initialMillis = state.deadlineMillis ?: System.currentTimeMillis(),
                                onDateTimeChanged = viewModel::onDeadlineChange,
                                textPrimary = p.textPrimary,
                                textSecondary = p.placeholder
                            )
                        }
                    }
                }
                FieldError(state.deadlineError)
                Spacer(Modifier.height(15.dp))

                // ── Prioritas: pill 51 dp, Dropdown ──
                val priorityCorner by animateDpAsState(
                    targetValue = if (expandedPriority) 25.dp else 26.dp,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    label = "priorityCorner"
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(250, easing = FastOutSlowInEasing))
                        .formSurface(p, RoundedCornerShape(priorityCorner))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(51.dp)
                            .clickable {
                                expandedPriority = !expandedPriority
                                if (expandedPriority) expandedDeadline = false
                            }
                            .padding(horizontal = 26.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Prioritas",
                                fontSize = 12.sp,
                                color = p.placeholder,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                when (state.priority) {
                                    TaskPriority.LOW -> "Rendah"
                                    TaskPriority.MEDIUM -> "Sedang"
                                    TaskPriority.HIGH -> "Tinggi"
                                },
                                fontSize = 15.sp,
                                color = p.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            if (expandedPriority) Icons.Filled.ExpandLess else Icons.Filled.UnfoldMore,
                            contentDescription = null,
                            tint = p.placeholder,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    AnimatedVisibility(
                        visible = expandedPriority,
                        enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(250)),
                        exit = shrinkVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(250))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            listOf(
                                "Rendah" to TaskPriority.LOW,
                                "Sedang" to TaskPriority.MEDIUM,
                                "Tinggi" to TaskPriority.HIGH
                            ).forEach { (lbl, pr) ->
                                val isSelected = state.priority == pr
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onPriorityChange(pr)
                                            expandedPriority = false
                                        }
                                        .padding(horizontal = 26.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        lbl,
                                        fontSize = 15.sp,
                                        color = if (isSelected) p.button else p.textPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(15.dp))

                // ── Deskripsi: kotak 149 dp, radius 30 ──
                FormField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    placeholder = "Deskripsi Tugas",
                    p = p,
                    height = 149.dp,
                    shape = RoundedCornerShape(30.dp),
                    singleLine = false
                )
                Spacer(Modifier.height(32.dp))
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

/** Bayangan lembut + latar + border 1 dp, sama untuk semua kolom pada desain. */
private fun Modifier.formSurface(p: FormPalette, shape: Shape): Modifier =
    this
        .shadow(
            elevation = 6.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = p.shadowAlpha),
            spotColor = Color.Black.copy(alpha = p.shadowAlpha)
        )
        .background(p.surface, shape)
        .border(1.dp, p.border, shape)

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    p: FormPalette,
    height: Dp,
    shape: Shape,
    singleLine: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .formSurface(p, shape)
            .padding(horizontal = 26.dp, vertical = if (singleLine) 0.dp else 20.dp),
        contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = p.placeholder,
                maxLines = 1
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                color = p.textPrimary
            ),
            cursorBrush = SolidColor(p.textPrimary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FieldError(message: String?) {
    message?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
    }
}

@Composable
private fun PriorityChip(label: String, selected: Boolean, bg: Color, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (selected) bg else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}