package com.kuliahku.ui.finance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import com.kuliahku.ui.theme.cardShadow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kuliahku.data.model.TxType
import com.kuliahku.data.model.Transaction
import com.kuliahku.ui.components.AnimatedEntry
import com.kuliahku.ui.components.EmptyView
import com.kuliahku.ui.components.ErrorView
import com.kuliahku.ui.components.KuliahkuDialog
import com.kuliahku.ui.components.ShimmerBox
import com.kuliahku.ui.navigation.Routes
import com.kuliahku.ui.theme.*
import com.kuliahku.util.CurrencyFormatter
import com.kuliahku.util.DateFormatters
import com.kuliahku.util.MonthKey
import com.kuliahku.util.YearlyPoint
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Month Names for Yearly Chart
// ---------------------------------------------------------------------------
private val MonthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

@Composable
fun FinanceScreen(
    navController: NavHostController,
    viewModel: FinanceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    var lastDeleted by remember { mutableStateOf<Transaction?>(null) }
    var showLimitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                is FinanceEvent.ShowMessage -> {
                    val res = snackbar.showSnackbar(
                        ev.message,
                        actionLabel = if (lastDeleted != null) "Undo" else null
                    )
                    if (res == SnackbarResult.ActionPerformed) {
                        lastDeleted?.let { viewModel.undoDelete(it) }
                        lastDeleted = null
                    }
                }
                is FinanceEvent.Share -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = ev.mime
                        putExtra(android.content.Intent.EXTRA_STREAM, ev.uri)
                        putExtra(android.content.Intent.EXTRA_SUBJECT, ev.title)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, ev.title))
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                Spacer(Modifier.height(57.dp))

                // ── Header: tombol kembali bulat, judul + ringkasan ──
                AnimatedEntry(delay = 0) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.ArrowBackIosNew,
                                contentDescription = "Kembali",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Keuangan",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 26.sp,
                                    lineHeight = 32.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Kelola pemasukan & pengeluaran",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = { showLimitDialog = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    "Batas",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(
                                onClick = { navController.navigate(Routes.ADD_TRANSACTION) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    EditSquareIcon,
                                    "Catat",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.exportCsv(context) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    UploadTrayIcon,
                                    "Ekspor",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            when {
                state.isLoading -> {
                    item {
                        Column(Modifier.padding(horizontal = 22.dp)) {
                            ShimmerBox(Modifier.fillMaxWidth().height(284.dp))
                            Spacer(Modifier.height(12.dp))
                            ShimmerBox(Modifier.fillMaxWidth().height(80.dp))
                        }
                    }
                }
                state.error != null -> {
                    item {
                        ErrorView(message = state.error!!, onRetry = {})
                    }
                }
                else -> {
                    // ---------------- Ringkasan bulan ----------------
                    item {
                        AnimatedEntry(delay = 100) {
                            Column(Modifier.padding(horizontal = 20.dp)) {
                                DailyLimitProgress(
                                    dailyLimit = state.settings.dailyLimit,
                                    todayExpense = state.monthlySummary.todayExpense
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(284.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    WeeklyBarCard(
                                        monthCal = state.currentMonthCal,
                                        weekly = state.monthlySummary.weeklyExpense,
                                        onPrev = viewModel::previousMonth,
                                        onNext = viewModel::nextMonth,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        SummaryCard(
                                            title = "Total Pemasukan",
                                            value = CurrencyFormatter.format(state.monthlySummary.totalIncome),
                                            bg = MaterialTheme.colorScheme.tertiaryContainer,
                                            icon = MoneyBagPlusIcon,
                                            modifier = Modifier.weight(1f)
                                        )
                                        SummaryCard(
                                            title = "Total Pengeluaran",
                                            value = CurrencyFormatter.format(state.monthlySummary.totalExpense),
                                            bg = MaterialTheme.colorScheme.errorContainer,
                                            icon = DashedCoinIcon,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ---------------- AI Insight ----------------
                    if (state.isAiLoading || state.aiInsight != null) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            AnimatedEntry(delay = 150) {
                                Box(Modifier.padding(horizontal = 20.dp)) {
                                    AiInsightCard(
                                        insight = state.aiInsight,
                                        isLoading = state.isAiLoading
                                    )
                                }
                            }
                        }
                    }

                    // ---------------- Riwayat Transaksi ----------------
                    item {
                        Spacer(Modifier.height(16.dp))
                        AnimatedEntry(delay = 200) {
                            Column(Modifier.padding(horizontal = 20.dp)) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(start = 4.dp, end = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Riwayat Transaksi",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 20.sp,
                                            lineHeight = 28.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        "Lihat Semua",
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { navController.navigate("transactions_all") }
                                    )
                                }
                                Spacer(Modifier.height(6.5.dp))

                                if (state.latestTransactions.isEmpty()) {
                                    EmptyView(
                                        title = "Belum ada transaksi bulan ini",
                                        subtitle = "Catat pemasukan atau pengeluaran Anda."
                                    )
                                }
                            }
                        }
                    }

                    items(
                        items = state.latestTransactions,
                        key = { it.id },
                        contentType = { "transaction" }
                    ) { tx ->
                        AnimatedEntry {
                            Box(Modifier.padding(horizontal = 20.dp)) {
                                TransactionRow(
                                    tx = tx,
                                    onClick = {
                                        navController.navigate("transaction_detail/${tx.id}")
                                    },
                                    onLongClick = {
                                        lastDeleted = tx
                                        viewModel.requestDelete(tx)
                                    }
                                )
                            }
                        }
                    }

                    // ---------------- Catatan Pertahun ----------------
                    item {
                        Spacer(Modifier.height(21.5.dp))
                        AnimatedEntry(delay = 300) {
                            Column(Modifier.padding(horizontal = 20.dp)) {
                                Text(
                                    "Catatan Pertahun",
                                    modifier = Modifier.padding(start = 4.dp),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 20.sp,
                                        lineHeight = 28.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))

                                val cardShape = MaterialTheme.shapes.medium
                                val surfaceColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF171C2C) else Color.White
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .cardShadow(shapeRadius = 24.dp),
                                    shape = cardShape,
                                    color = surfaceColor,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Column {
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(start = 19.dp, end = 14.dp, top = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TabChip(
                                                "Chart",
                                                state.chartTab == ChartTab.CHART
                                            ) { viewModel.setChartTab(ChartTab.CHART) }
                                            TabChip(
                                                "Tabel",
                                                state.chartTab == ChartTab.TABLE
                                            ) { viewModel.setChartTab(ChartTab.TABLE) }
                                            if (state.chartTab == ChartTab.CHART) {
                                                Spacer(Modifier.weight(1f))
                                                Row(
                                                    Modifier
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        .padding(2.dp)
                                                ) {
                                                    TabChip(
                                                        "Pemasukan",
                                                        state.chartType == TxChartType.INCOME,
                                                        small = true
                                                    ) { viewModel.setChartType(TxChartType.INCOME) }
                                                    TabChip(
                                                        "Pengeluaran",
                                                        state.chartType == TxChartType.EXPENSE,
                                                        small = true
                                                    ) { viewModel.setChartType(TxChartType.EXPENSE) }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        when (state.chartTab) {
                                            ChartTab.CHART -> YearlyLineChart(
                                                points = state.yearlySummary.points,
                                                showIncome = state.chartType == TxChartType.INCOME,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(190.dp)
                                            )
                                            ChartTab.TABLE -> Box(
                                                Modifier.padding(
                                                    horizontal = 19.dp
                                                )
                                            ) {
                                                YearlyTable(state.yearlySummary.points)
                                            }
                                        }
                                        Spacer(Modifier.height(12.dp))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        }
    }

    if (state.pendingDelete != null) {
        KuliahkuDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = "Hapus Transaksi?",
            text = "Transaksi akan dihapus permanen.",
            icon = Icons.Default.Delete,
            iconColor = MaterialTheme.colorScheme.error,
            confirmText = "Hapus",
            confirmColor = MaterialTheme.colorScheme.error,
            onConfirm = viewModel::confirmDelete,
            dismissText = "Batal"
        )
    }

    if (showLimitDialog) {
        DailyLimitDialog(
            currentLimit = state.settings.dailyLimit,
            onDismiss = { showLimitDialog = false },
            onConfirm = {
                viewModel.updateDailyLimit(it)
                showLimitDialog = false
            }
        )
    }
}

@Composable
private fun DailyLimitProgress(
    dailyLimit: Long,
    todayExpense: Long
) {
    if (dailyLimit <= 0) return

    val progress = (todayExpense.toFloat() / dailyLimit).coerceIn(0f, 1f)
    val isOverLimit = todayExpense > dailyLimit
    val shape = MaterialTheme.shapes.medium
    val surfaceColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF171C2C) else Color.White

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow(shapeRadius = 24.dp),
        shape = shape,
        color = surfaceColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Batas Harian",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${CurrencyFormatter.formatCompact(todayExpense)} / ${CurrencyFormatter.formatCompact(dailyLimit)}",
                    fontSize = 12.sp,
                    color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            )
            if (isOverLimit) {
                Text(
                    "Peringatan: Pengeluaran melebihi batas!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DailyLimitDialog(
    currentLimit: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var limitText by remember { mutableStateOf(if (currentLimit > 0) currentLimit.toString() else "") }

    Dialog(onDismissRequest = onDismiss) {
        val surfaceColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF171C2C) else Color.White
        Surface(
            modifier = Modifier.cardShadow(shapeRadius = 28.dp),
            shape = RoundedCornerShape(28.dp),
            color = surfaceColor,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    "Atur Batas Harian",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Masukkan jumlah maksimal pengeluaran harian Anda.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) limitText = it },
                    label = { Text("Batas Harian (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(limitText.toLongOrNull() ?: 0L) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simpan", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiInsightCard(insight: String?, isLoading: Boolean) {
    val shape = MaterialTheme.shapes.medium
    val surfaceColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF171C2C) else Color.White
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow(shapeRadius = 24.dp),
        shape = shape,
        color = surfaceColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💡", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Smart Finance Insight",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(10.dp))
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            } else {
                MarkdownText(
                    text = insight ?: "",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun MarkdownText(
    text: String,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    color: Color,
    modifier: Modifier = Modifier
) {
    val annotatedString = buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            var currentLine = line
            // Handle List items
            if (currentLine.trimStart().startsWith("* ") || currentLine.trimStart().startsWith("- ")) {
                append("  • ")
                currentLine = currentLine.trimStart().removePrefix("* ").removePrefix("- ")
            }

            // Handle Bold
            val parts = currentLine.split("**")
            parts.forEachIndexed { partIndex, part ->
                if (partIndex % 2 == 1) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(part)
                    }
                } else {
                    // Remove raw markdown headings if they exist
                    val cleanedPart = part.replace(Regex("^#+\\s*"), "")
                    append(cleanedPart)
                }
            }
            if (index < lines.size - 1) append("\n")
        }
    }
    Text(
        text = annotatedString,
        fontSize = fontSize,
        lineHeight = lineHeight,
        color = color,
        modifier = modifier
    )
}

// ---------------------------------------------------------------------------
// Kartu bar chart mingguan (posisi dihitung dari lebar kartu agar responsif)
// ---------------------------------------------------------------------------
@Composable
private fun WeeklyBarCard(
    monthCal: Calendar,
    weekly: Map<String, Long>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buckets = MonthKey.BUCKETS
    val maxVal = weekly.values.maxOrNull() ?: 0L
    val axisMax = niceCeil(maxVal)
    val colorScheme = MaterialTheme.colorScheme
    val surfaceColor = if (colorScheme.background.luminance() < 0.5f) Color(0xFF171C2C) else Color.White

    Surface(
        modifier = modifier.cardShadow(shapeRadius = 24.dp),
        shape = MaterialTheme.shapes.medium,
        color = surfaceColor,
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f))
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val plotLeft = 30.dp
            val plotRight = maxWidth - 10.dp
            val slotW = (plotRight - plotLeft) / buckets.size
            val topY = 73.dp
            val baseY = 244.dp
            fun centerX(i: Int): Dp = plotLeft + slotW * (i + 0.5f)

            Text(
                monthCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id", "ID"))
                    ?.replaceFirstChar { it.uppercase() } ?: "",
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface
            )

            Canvas(Modifier.fillMaxSize()) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
                for (k in 1..4) {
                    val y = (baseY - (baseY - topY) * (k / 4f)).toPx()
                    drawLine(
                        color = colorScheme.outline.copy(alpha = 0.2f),
                        start = Offset(plotLeft.toPx(), y),
                        end = Offset(plotRight.toPx(), y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash
                    )
                }
                val barW = minOf(slotW * 0.55f, 16.dp).toPx()
                val minH = 8.dp.toPx()
                val full = (baseY - topY).toPx()
                buckets.forEachIndexed { i, bucket ->
                    val value = weekly[bucket] ?: 0L
                    val h = ((value.toFloat() / axisMax) * full).coerceIn(minH, full)
                    drawRoundRect(
                        color = colorScheme.primary,
                        topLeft = Offset(centerX(i).toPx() - barW / 2f, baseY.toPx() - h),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(barW / 2f)
                    )
                }
            }

            for (k in 1..4) {
                val y = baseY - (baseY - topY) * (k / 4f)
                Text(
                    formatK(axisMax * k / 4),
                    modifier = Modifier.offset(x = 6.dp, y = y - 7.dp),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Text(
                "0",
                modifier = Modifier.offset(x = 6.dp, y = 246.dp),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
            buckets.forEachIndexed { i, bucket ->
                Box(
                    Modifier.offset(x = centerX(i) - slotW / 2f, y = 246.dp).width(slotW),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        bucket,
                        modifier = Modifier.wrapContentWidth(unbounded = true),
                        fontSize = 9.sp,
                        lineHeight = 14.sp,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    bg: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val surfaceColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF171C2C) else Color.White
    val contentColor = if (bg == MaterialTheme.colorScheme.tertiaryContainer) MaterialTheme.colorScheme.tertiary
    else if (bg == MaterialTheme.colorScheme.errorContainer) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = surfaceColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = modifier.fillMaxWidth().cardShadow(shapeRadius = 24.dp)
    ) {
        Column(Modifier.padding(start = 21.dp, top = 27.dp)) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(2.dp))
            Text(
                title,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun TransactionRow(tx: Transaction, onClick: () -> Unit, onLongClick: () -> Unit) {
    val isIncome = tx.typeEnum == TxType.INCOME
    val shape = MaterialTheme.shapes.medium
    val surfaceColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF171C2C) else Color.White
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.5.dp)
            .height(68.dp)
            .cardShadow(shapeRadius = 22.dp),
        shape = shape,
        color = surfaceColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .padding(start = 19.dp, end = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isIncome) BanknotePlusIcon else BanknoteMinusIcon,
                contentDescription = null,
                tint = if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isIncome) "Uang Masuk" else "Uang Keluar",
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                tx.date?.toDate()?.let {
                    Text(
                        DateFormatters.formatDateTime(it),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                CurrencyFormatter.format(tx.amount),
                color = if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, small: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(if (small) 22.dp else 23.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = if (small) 10.dp else 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = if (small) 12.sp else 14.sp,
            lineHeight = if (small) 16.sp else 18.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else if (small) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun YearlyLineChart(
    points: List<YearlyPoint>,
    showIncome: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val data = remember(points) { points.sortedBy { it.monthIndex } }
    val values = data.map { if (showIncome) it.income else it.expense }
    val maxV = values.maxOrNull() ?: 0L
    val step = niceStep(maxV)
    val peakIndex = if (maxV > 0L) values.indexOf(maxV) else -1

    BoxWithConstraints(modifier) {
        val axisW = 48.dp
        val plotW = maxWidth - axisW - 16.dp
        val n = data.size.coerceAtLeast(1)
        val slotW = plotW / n
        val zeroY = 158.dp
        val topY = 28.dp
        val unit = zeroY - topY

        fun xOf(i: Int): Dp = axisW + slotW * (i + 0.5f)
        fun yOf(v: Long): Dp = zeroY - unit * (v.toFloat() / (2f * step))

        Canvas(Modifier.fillMaxSize()) {
            val left = axisW.toPx()
            val right = (axisW + plotW).toPx()
            listOf(step, 2 * step).forEach { level ->
                val y = yOf(level).toPx()
                drawLine(colorScheme.outline.copy(alpha = 0.2f), Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
            }

            if (values.isNotEmpty()) {
                val pts = values.indices.map { Offset(xOf(it).toPx(), yOf(values[it]).toPx()) }
                if (pts.size > 1) {
                    val path = Path().apply {
                        moveTo(pts[0].x, pts[0].y)
                        for (i in 1 until pts.size) {
                            val mx = (pts[i - 1].x + pts[i].x) / 2f
                            cubicTo(mx, pts[i - 1].y, mx, pts[i].y, pts[i].x, pts[i].y)
                        }
                    }
                    drawPath(
                        path,
                        color = if (showIncome) colorScheme.tertiary else colorScheme.error,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                if (peakIndex >= 0) {
                    val c = pts[peakIndex]
                    drawCircle(colorScheme.surface, radius = 5.dp.toPx(), center = c)
                    drawCircle(if (showIncome) colorScheme.tertiary else colorScheme.error, radius = 5.dp.toPx(), center = c, style = Stroke(width = 2.dp.toPx()))
                }
            }
        }

        listOf(2 * step, step, 0L).forEach { level ->
            Box(
                Modifier.offset(x = 0.dp, y = yOf(level) - 18.dp).width(axisW),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (level == 0L) "0" else formatK(level),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        data.forEachIndexed { i, pt ->
            if (slotW >= 22.dp || i % 2 == 0) {
                Box(
                    Modifier.offset(x = xOf(i) - slotW / 2f, y = zeroY + 8.dp).width(slotW),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        MonthNames[pt.monthIndex.coerceIn(0, 11)],
                        modifier = Modifier.wrapContentWidth(unbounded = true),
                        fontSize = 10.sp,
                        lineHeight = 16.sp,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }

        if (peakIndex >= 0) {
            Box(
                Modifier.offset(x = xOf(peakIndex) - 30.dp, y = yOf(values[peakIndex]) - 27.dp).width(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    NumberFormat.getIntegerInstance(Locale("id", "ID")).format(values[peakIndex] / 1000) + "k",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = if (showIncome) colorScheme.tertiary else colorScheme.error,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun YearlyTable(points: List<YearlyPoint>) {
    val colorScheme = MaterialTheme.colorScheme
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Bulan", Modifier.weight(0.6f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colorScheme.onSurface)
            Text("Masuk", Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colorScheme.onSurface)
            Text("Keluar", Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colorScheme.onSurface)
            Text("Selisih", Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colorScheme.onSurface)
        }
        points.forEach { pt ->
            val monthName = MonthNames[pt.monthIndex.coerceIn(0, 11)]
            val selisih = pt.income - pt.expense
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(monthName, Modifier.weight(0.6f), fontSize = 11.sp, color = colorScheme.onSurface)
                Text(CurrencyFormatter.formatCompact(pt.income), Modifier.weight(0.7f), fontSize = 11.sp, color = colorScheme.onSurface)
                Text(CurrencyFormatter.formatCompact(pt.expense), Modifier.weight(0.7f), fontSize = 11.sp, color = colorScheme.onSurface)
                Text(
                    CurrencyFormatter.formatCompact(selisih),
                    Modifier.weight(0.7f),
                    fontSize = 11.sp,
                    color = if (selisih >= 0) colorScheme.tertiary else colorScheme.error
                )
            }
        }
    }
}

private fun niceCeil(v: Long): Long {
    if (v <= 0L) return 100_000L
    var mag = 1L
    while (mag * 10 <= v) mag *= 10
    for (m in longArrayOf(1, 2, 4, 8, 10)) {
        if (mag * m >= v) return mag * m
    }
    return mag * 10
}

private fun niceStep(maxV: Long): Long {
    if (maxV <= 0L) return 500_000L
    val target = maxV / 2.0
    var mag = 1.0
    while (mag * 10 <= target) mag *= 10
    for (m in doubleArrayOf(1.0, 2.0, 5.0, 10.0)) {
        if (mag * m >= target) return (mag * m).toLong().coerceAtLeast(1000L)
    }
    return (mag * 10).toLong()
}

private fun formatK(v: Long): String = if (v >= 1000L) "${v / 1000}K" else v.toString()

private fun vec(name: String, build: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(build).build()

private fun ImageVector.Builder.stroked(d: String, width: Float) = addPath(
    pathData = addPathNodes(d),
    stroke = SolidColor(Color.Black),
    strokeLineWidth = width,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round
)

private fun ImageVector.Builder.filled(d: String, evenOdd: Boolean = false) = addPath(
    pathData = addPathNodes(d),
    pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
    fill = SolidColor(Color.Black)
)

private fun dashedCircle(cx: Float, cy: Float, r: Float, dashes: Int = 12): String {
    val sb = StringBuilder()
    val step = 360f / dashes
    val dash = step * 0.6f
    for (i in 0 until dashes) {
        val a0 = Math.toRadians((i * step).toDouble())
        val a1 = Math.toRadians((i * step + dash).toDouble())
        sb.append(
            String.format(
                Locale.US, "M%.2f %.2f A%.2f %.2f 0 0 1 %.2f %.2f ",
                cx + r * cos(a0), cy + r * sin(a0), r, r, cx + r * cos(a1), cy + r * sin(a1)
            )
        )
    }
    return sb.toString()
}

private val ChevronLeftIcon = Icons.AutoMirrored.Filled.ArrowBack

private val EditSquareIcon = vec("EditSquare") {
    stroked(
        "M11 4H5a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h13a2 2 0 0 0 2-2v-6 " +
                "M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z",
        2.2f
    )
}

private val UploadTrayIcon = vec("UploadTray") {
    stroked("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4 M17 8l-5-5-5 5 M12 3v12", 2.2f)
}

private val MoneyBagPlusIcon = vec("MoneyBagPlus") {
    stroked(
        "M9 3h6 M9 3l1.7 3.6 M15 3l-1.7 3.6 " +
                "M10.7 6.6C7 8.5 5 11.5 5 14.5 5 17.8 7.5 20 11 20 " +
                "M13.3 6.6C16.5 8.3 18.5 11 18.5 13 " +
                "M11 10.5v6.5 M12.9 11.7c0-.8-.8-1.2-1.9-1.2s-1.9.5-1.9 1.3.9 1.2 1.9 1.5 1.9.7 1.9 1.5-.8 1.3-1.9 1.3-1.9-.4-1.9-1.2 " +
                "M19 15a3.3 3.3 0 1 0 0 6.6 3.3 3.3 0 1 0 0-6.6z M19 16.7v3.2 M17.4 18.3h3.2",
        2f
    )
}

private val DashedCoinIcon = vec("DashedCoin") {
    stroked(dashedCircle(12f, 12f, 9.5f), 2f)
    stroked(
        "M12 6.5v11 M14.6 9.4c0-1.1-1.2-1.7-2.6-1.7s-2.6.7-2.6 1.9 1.2 1.6 2.6 2 2.6.8 2.6 2-1.2 1.9-2.6 1.9-2.6-.7-2.6-1.8",
        2f
    )
}

private const val BanknoteBody =
    "M2 5h16a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z " +
            "M10 8.2a2.8 2.8 0 1 0 0 5.6 2.8 2.8 0 1 0 0-5.6z " +
            "M10 9.7a1.3 1.3 0 1 0 0 2.6 1.3 1.3 0 1 0 0-2.6z"

val BanknotePlusIcon = vec("BanknotePlus") {
    filled(BanknoteBody, evenOdd = true)
    filled("M18.5 15h2v2.5H23v2h-2.5V22h-2v-2.5H16v-2h2.5z")
}

val BanknoteMinusIcon = vec("BanknoteMinus") {
    filled(BanknoteBody, evenOdd = true)
    filled("M16 17.5h7v2h-7z")
}

private fun FinanceViewModel.exportCsv(context: android.content.Context) {
    com.kuliahku.util.FinanceExport.buildCsv(
        context = context,
        transactions = uiState.value.yearTransactions
    )
}
