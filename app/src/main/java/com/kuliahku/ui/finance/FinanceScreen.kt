package com.kuliahku.ui.finance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
// Palet Keuangan (Light = sesuai Keuangan.png, Dark = nilai yang sama dengan Beranda).
// Mode gelap dideteksi dari MaterialTheme.colorScheme.background.
// ---------------------------------------------------------------------------
private class FinPalette(
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textGray: Color,
    val accent: Color,
    val barCardBg: Color,
    val barColor: Color,
    val barGrid: Color,
    val greenBg: Color,
    val redBg: Color,
    val tabSelected: Color,
    val segmentBg: Color,
    val chartGrid: Color,
    val chartLine: Color,
    val chartLabel: Color,
    val peakLabel: Color,
    val income: Color,
    val expense: Color,
    val shadowAlpha: Float
)

private val FinLight = FinPalette(
    background = Color(0xFFF8FAFF), // Senada dengan Beranda
    surface = Color.White,
    border = Color(0xFFE2E8F0), // Border halus slate
    textPrimary = Color(0xFF1E293B), // Dark slate
    textGray = Color(0xFF64748B), // Slate gray
    accent = Color(0xFF2563EB), // Blue 600
    barCardBg = Color(0xFFEFF6FF),
    barColor = Color(0xFF3B82F6),
    barGrid = Color(0xFFDBEAFE),
    greenBg = Color(0xFFECFDF5),
    redBg = Color(0xFFFEF2F2),
    tabSelected = Color(0xFFDBEAFE),
    segmentBg = Color(0xFFF1F5F9),
    chartGrid = Color(0xFFCBD5E1),
    chartLine = Color(0xFF2DCB6B),
    chartLabel = Color(0xFF666666),
    peakLabel = Color(0xFF3A8F5A),
    income = Color(0xFF0AAA0A),
    expense = Color(0xFFC80000),
    shadowAlpha = 0.10f
)

private val FinDark = FinPalette(
    background = Color(0xFF0E1220),
    surface = Color(0xFF171C2C),
    border = Color(0xFF2A3350),
    textPrimary = Color(0xFFF1F4FF),
    textGray = Color(0xFFA3ABC4),
    accent = Color(0xFF5AA2FF),
    barCardBg = Color(0xFF1C2755),
    barColor = Color(0xFF5B7CFF),
    barGrid = Color(0xFF35457A),
    greenBg = Color(0xFF14362A),
    redBg = Color(0xFF45202A),
    tabSelected = Color(0xFF2B5C9E),
    segmentBg = Color(0xFF1B2134),
    chartGrid = Color(0xFF2E3652),
    chartLine = Color(0xFF3DD083),
    chartLabel = Color(0xFFA3ABC4),
    peakLabel = Color(0xFF6EE79A),
    income = Color(0xFF3DD083),
    expense = Color(0xFFFF6B7A),
    shadowAlpha = 0.45f
)

@Composable
private fun finPalette(): FinPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) FinDark else FinLight

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
    val p = finPalette()

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
        containerColor = p.background
    ) { padding ->
        // padding dari Scaffold sudah memuat inset status bar, jadi spacer atas dibuat kecil.
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ---------------- Header ----------------
            AnimatedEntry(delay = 0) {
                Row(
                    Modifier.fillMaxWidth().height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Area sentuh 40dp, ikon 24dp. Offset menyejajarkan ikon dengan tepi konten.
                    Box(
                        Modifier
                            .offset(x = (-10).dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(ChevronLeftIcon, "Kembali", tint = p.textPrimary, modifier = Modifier.size(24.dp))
                    }
                    Text(
                        "Keuangan",
                        modifier = Modifier.offset(x = (-10).dp),
                        fontSize = 24.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = p.textPrimary
                    )
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.offset(x = 11.dp)) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).clickable { navController.navigate(Routes.ADD_TRANSACTION) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(EditSquareIcon, "Catat", tint = p.textPrimary, modifier = Modifier.size(24.dp))
                        }
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).clickable { viewModel.exportCsv(context) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(UploadTrayIcon, "Ekspor", tint = p.textPrimary, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when {
                state.isLoading -> {
                    ShimmerBox(Modifier.fillMaxWidth().height(284.dp))
                    Spacer(Modifier.height(12.dp))
                    ShimmerBox(Modifier.fillMaxWidth().height(80.dp))
                }
                state.error != null -> ErrorView(message = state.error!!, onRetry = {})
                else -> {
                    // ---------------- Ringkasan bulan ----------------
                    AnimatedEntry(delay = 100) {
                        Row(
                            Modifier.fillMaxWidth().height(284.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WeeklyBarCard(
                                monthCal = state.currentMonthCal,
                                weekly = state.monthlySummary.weeklyExpense,
                                onPrev = viewModel::previousMonth,
                                onNext = viewModel::nextMonth,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            Column(
                                Modifier.weight(1f).fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SummaryCard(
                                    title = "Total Pemasukan",
                                    value = CurrencyFormatter.format(state.monthlySummary.totalIncome),
                                    bg = p.greenBg,
                                    icon = MoneyBagPlusIcon,
                                    modifier = Modifier.weight(1f)
                                )
                                SummaryCard(
                                    title = "Total Pengeluaran",
                                    value = CurrencyFormatter.format(state.monthlySummary.totalExpense),
                                    bg = p.redBg,
                                    icon = DashedCoinIcon,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // ---------------- AI Insight ----------------
                    if (state.isAiLoading || state.aiInsight != null) {
                        Spacer(Modifier.height(16.dp))
                        AnimatedEntry(delay = 150) {
                            AiInsightCard(
                                insight = state.aiInsight,
                                isLoading = state.isAiLoading,
                                p = p
                            )
                        }
                    }

                    // ---------------- Riwayat Transaksi ----------------
                    Spacer(Modifier.height(16.dp))
                    AnimatedEntry(delay = 200) {
                        Column {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 4.dp, end = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Riwayat Transaksi",
                                    fontSize = 20.sp,
                                    lineHeight = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = p.textPrimary
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "Lihat Semua",
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = p.accent,
                                    modifier = Modifier.clickable { navController.navigate("transactions_all") }
                                )
                            }
                            Spacer(Modifier.height(6.5.dp))

                            if (state.latestTransactions.isEmpty()) {
                                EmptyView(
                                    title = "Belum ada transaksi bulan ini",
                                    subtitle = "Catat pemasukan atau pengeluaran Anda."
                                )
                            } else {
                                state.latestTransactions.forEach { tx ->
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
                    }

                    // ---------------- Catatan Pertahun ----------------
                    Spacer(Modifier.height(21.5.dp))
                    AnimatedEntry(delay = 300) {
                        Column {
                            Text(
                                "Catatan Pertahun",
                                modifier = Modifier.padding(start = 4.dp),
                                fontSize = 20.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.textPrimary
                            )
                            Spacer(Modifier.height(8.dp))

                            val cardShape = RoundedCornerShape(24.dp)
                            val shadowColor = Color.Black.copy(alpha = p.shadowAlpha)
                            Surface(
                                shape = cardShape,
                                color = p.surface,
                                border = BorderStroke(1.dp, p.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(6.dp, cardShape, ambientColor = shadowColor, spotColor = shadowColor)
                            ) {
                                Column {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(start = 19.dp, end = 14.dp, top = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TabChip("Chart", state.chartTab == ChartTab.CHART) { viewModel.setChartTab(ChartTab.CHART) }
                                        TabChip("Tabel", state.chartTab == ChartTab.TABLE) { viewModel.setChartTab(ChartTab.TABLE) }
                                        if (state.chartTab == ChartTab.CHART) {
                                            Spacer(Modifier.weight(1f))
                                            // Pilihan jenis data: kontrol segmented kecil di kanan
                                            Row(
                                                Modifier
                                                    .clip(CircleShape)
                                                    .background(p.segmentBg)
                                                    .padding(2.dp)
                                            ) {
                                                TabChip("Pemasukan", state.chartType == TxChartType.INCOME, small = true) { viewModel.setChartType(TxChartType.INCOME) }
                                                TabChip("Pengeluaran", state.chartType == TxChartType.EXPENSE, small = true) { viewModel.setChartType(TxChartType.EXPENSE) }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    when (state.chartTab) {
                                        ChartTab.CHART -> YearlyLineChart(
                                            points = state.yearlySummary.points,
                                            showIncome = state.chartType == TxChartType.INCOME,
                                            modifier = Modifier.fillMaxWidth().height(190.dp)
                                        )
                                        ChartTab.TABLE -> Box(Modifier.padding(horizontal = 19.dp)) {
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
}

@Composable
private fun AiInsightCard(insight: String?, isLoading: Boolean, p: FinPalette) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                6.dp, shape,
                ambientColor = Color.Black.copy(alpha = p.shadowAlpha),
                spotColor = Color.Black.copy(alpha = p.shadowAlpha)
            ),
        shape = shape,
        color = p.surface,
        border = BorderStroke(1.dp, p.border)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💡", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Smart Finance Insight",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.accent
                )
            }
            Spacer(Modifier.height(10.dp))
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                    color = p.accent,
                    trackColor = p.border
                )
            } else {
                Text(
                    text = insight ?: "",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = p.textPrimary
                )
            }
        }
    }
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
    val p = finPalette()
    val buckets = MonthKey.BUCKETS
    val maxVal = weekly.values.maxOrNull() ?: 0L
    val axisMax = niceCeil(maxVal)

    Surface(
        modifier = modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(24.dp),
            ambientColor = Color.Black.copy(alpha = p.shadowAlpha),
            spotColor = Color.Black.copy(alpha = p.shadowAlpha)
        ),
        shape = RoundedCornerShape(24.dp),
        color = p.barCardBg,
        border = BorderStroke(1.dp, p.border.copy(alpha = 0.5f))
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
                color = p.textPrimary
            )

            Canvas(Modifier.fillMaxSize()) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
                for (k in 1..4) {
                    val y = (baseY - (baseY - topY) * (k / 4f)).toPx()
                    drawLine(
                        color = p.barGrid,
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
                        color = p.barColor,
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
                    color = p.textGray
                )
            }
            Text(
                "0",
                modifier = Modifier.offset(x = 6.dp, y = 246.dp),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = p.textGray
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
                        color = p.textGray,
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
    val p = finPalette()
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = bg,
        border = BorderStroke(1.dp, p.border.copy(alpha = 0.5f)),
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(start = 21.dp, top = 27.dp)) {
            Icon(icon, contentDescription = null, tint = p.textPrimary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(2.dp))
            Text(
                title,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = p.textGray
            )
            Text(
                value,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                color = p.textPrimary
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Baris transaksi
// ---------------------------------------------------------------------------
@Composable
fun TransactionRow(tx: Transaction, onClick: () -> Unit, onLongClick: () -> Unit) {
    val p = finPalette()
    val isIncome = tx.typeEnum == TxType.INCOME
    val shape = RoundedCornerShape(22.dp)
    val shadowColor = Color.Black.copy(alpha = p.shadowAlpha)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.5.dp)
            .height(68.dp)
            .shadow(6.dp, shape, ambientColor = shadowColor, spotColor = shadowColor),
        shape = shape,
        color = p.surface,
        border = BorderStroke(1.dp, p.border)
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
                tint = if (isIncome) p.income else p.expense,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isIncome) "Uang Masuk" else "Uang Keluar",
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.textPrimary
                )
                tx.date?.toDate()?.let {
                    Text(
                        DateFormatters.formatDateTime(it),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = p.textPrimary
                    )
                }
            }
            // CurrencyFormatter.format sudah menyertakan awalan "Rp." (dipakai juga di kartu ringkasan)
            Text(
                CurrencyFormatter.format(tx.amount),
                color = if (isIncome) p.income else p.expense,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, small: Boolean = false, onClick: () -> Unit) {
    val p = finPalette()
    Box(
        modifier = Modifier
            .height(if (small) 22.dp else 23.dp)
            .clip(CircleShape)
            .background(if (selected) p.tabSelected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = if (small) 10.dp else 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = if (small) 12.sp else 14.sp,
            lineHeight = if (small) 16.sp else 18.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else if (small) p.textGray else p.textPrimary,
            maxLines = 1,
            softWrap = false
        )
    }
}

// ---------------------------------------------------------------------------
// Line chart tahunan - fleksibel:
//  * menampilkan semua bulan yang ada pada data tahun tersebut (Jan - Des)
//  * lebar slot menyesuaikan lebar kartu dan jumlah titik
//  * skala sumbu Y dihitung otomatis dari nilai tertinggi
// ---------------------------------------------------------------------------
@Composable
private fun YearlyLineChart(
    points: List<YearlyPoint>,
    showIncome: Boolean,
    modifier: Modifier = Modifier
) {
    val p = finPalette()
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
        val unit = zeroY - topY // tinggi untuk 2 x step (gridline paling atas)

        fun xOf(i: Int): Dp = axisW + slotW * (i + 0.5f)
        fun yOf(v: Long): Dp = zeroY - unit * (v.toFloat() / (2f * step))

        Canvas(Modifier.fillMaxSize()) {
            val left = axisW.toPx()
            val right = (axisW + plotW).toPx()
            listOf(step, 2 * step).forEach { level ->
                val y = yOf(level).toPx()
                drawLine(p.chartGrid, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
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
                        color = p.chartLine,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                if (peakIndex >= 0) {
                    val c = pts[peakIndex]
                    drawCircle(p.surface, radius = 5.dp.toPx(), center = c)
                    drawCircle(p.chartLine, radius = 5.dp.toPx(), center = c, style = Stroke(width = 2.dp.toPx()))
                }
            }
        }

        // Label sumbu Y
        listOf(2 * step, step, 0L).forEach { level ->
            Box(
                Modifier.offset(x = 0.dp, y = yOf(level) - 18.dp).width(axisW),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (level == 0L) "0" else formatK(level),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = p.chartLabel,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Label sumbu X (jika slot terlalu sempit, tampilkan selang-seling)
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
                        color = p.chartLabel,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }

        // Label puncak
        if (peakIndex >= 0) {
            Box(
                Modifier.offset(x = xOf(peakIndex) - 30.dp, y = yOf(values[peakIndex]) - 27.dp).width(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    NumberFormat.getIntegerInstance(Locale("id", "ID")).format(values[peakIndex] / 1000) + "k",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = p.peakLabel,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun YearlyTable(points: List<YearlyPoint>) {
    val p = finPalette()
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Bulan", Modifier.weight(0.6f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = p.textPrimary)
            Text("Masuk", Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = p.textPrimary)
            Text("Keluar", Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = p.textPrimary)
            Text("Selisih", Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = p.textPrimary)
        }
        points.forEach { pt ->
            val monthName = MonthNames[pt.monthIndex.coerceIn(0, 11)]
            val selisih = pt.income - pt.expense
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(monthName, Modifier.weight(0.6f), fontSize = 11.sp, color = p.textPrimary)
                Text(CurrencyFormatter.formatCompact(pt.income), Modifier.weight(0.7f), fontSize = 11.sp, color = p.textPrimary)
                Text(CurrencyFormatter.formatCompact(pt.expense), Modifier.weight(0.7f), fontSize = 11.sp, color = p.textPrimary)
                Text(
                    CurrencyFormatter.formatCompact(selisih),
                    Modifier.weight(0.7f),
                    fontSize = 11.sp,
                    color = if (selisih >= 0) p.income else p.expense
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Helper skala & format
// ---------------------------------------------------------------------------

/** Batas sumbu bar chart: kelipatan 1/2/4/8 x 10^n supaya 4 gridline bernilai bulat. */
private fun niceCeil(v: Long): Long {
    if (v <= 0L) return 100_000L
    var mag = 1L
    while (mag * 10 <= v) mag *= 10
    for (m in longArrayOf(1, 2, 4, 8, 10)) {
        if (mag * m >= v) return mag * m
    }
    return mag * 10
}

/** Step gridline line chart: bilangan 1/2/5 x 10^n terkecil dengan 2 x step >= nilai maksimum. */
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

// ---------------------------------------------------------------------------
// Ikon vektor (digambar mengikuti desain, ukuran tampil 24dp dengan garis 2dp)
// ---------------------------------------------------------------------------
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

private val ChevronLeftIcon = vec("ChevronLeft") { stroked("M15 18l-6-6 6-6", 2.2f) }

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

// Ekstensi ViewModel untuk ekspor (dipindah agar file ini ringkas)
private fun FinanceViewModel.exportCsv(context: android.content.Context) {
    // detail ada di FinanceExport.kt
    com.kuliahku.util.FinanceExport.buildCsv(
        context = context,
        transactions = uiState.value.yearTransactions
    )
}