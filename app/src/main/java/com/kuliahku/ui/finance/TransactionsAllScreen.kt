package com.kuliahku.ui.finance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kuliahku.ui.components.EmptyView
import com.kuliahku.ui.components.ErrorView
import com.kuliahku.ui.components.ShimmerBox
import com.kuliahku.util.CurrencyFormatter
import com.kuliahku.util.DateFormatters
import java.util.Calendar
import java.util.Locale

// ---------------------------------------------------------------------------
// Palet (sama dengan layar Keuangan: Light #F8F9FF, Dark #0E1220).
// Mode gelap dideteksi dari MaterialTheme.colorScheme.background.
// ---------------------------------------------------------------------------
private class AllTxPalette(
    val background: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val greenBg: Color,
    val redBg: Color
)

private val AllTxLight = AllTxPalette(
    background = Color(0xFFF8F9FF),
    textPrimary = Color.Black,
    textMuted = Color(0xFF767678),
    greenBg = Color(0xFFC2FFC4),
    redBg = Color(0xFFFFB1B1)
)

private val AllTxDark = AllTxPalette(
    background = Color(0xFF0E1220),
    textPrimary = Color(0xFFF1F4FF),
    textMuted = Color(0xFFA3ABC4),
    greenBg = Color(0xFF14362A),
    redBg = Color(0xFF45202A)
)

@Composable
private fun allTxPalette(): AllTxPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) AllTxDark else AllTxLight

@Composable
fun TransactionsAllScreen(
    navController: NavHostController,
    viewModel: FinanceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val p = allTxPalette()
    val listPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 24.dp)

    // Kelompokkan per hari (urutan kemunculan dipertahankan). Kunci 0 = tanpa tanggal.
    val groups = remember(state.monthTransactions) {
        state.monthTransactions.groupBy { tx ->
            tx.date?.toDate()?.let { d ->
                val c = Calendar.getInstance().apply { time = d }
                c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
            } ?: 0
        }
    }

    Scaffold(containerColor = p.background) { padding ->
        // padding dari Scaffold sudah memuat inset status bar, jadi spacer atas dibuat kecil.
        Column(Modifier.fillMaxSize().padding(padding)) {
            Spacer(Modifier.height(8.dp))

            // ---------------- Header ----------------
            Row(
                Modifier.fillMaxWidth().height(48.dp).padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(ChevronLeftIcon, "Kembali", tint = p.textPrimary, modifier = Modifier.size(26.dp))
                }
                Text(
                    "Semua Transaksi",
                    fontSize = 24.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.textPrimary
                )
            }

            when {
                state.isLoading -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                    contentPadding = listPadding
                ) {
                    items(6) { ShimmerBox(Modifier.fillMaxWidth().height(68.dp)) }
                }
                state.error != null -> ErrorView(message = state.error!!, onRetry = {})
                state.monthTransactions.isEmpty() -> EmptyView(
                    title = "Belum ada transaksi",
                    subtitle = "Catat pemasukan atau pengeluaran Anda."
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listPadding
                ) {
                    // Bulan berjalan + jumlah transaksi
                    item(key = "summary") {
                        val monthName = state.currentMonthCal
                            .getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id", "ID"))
                            ?.replaceFirstChar { it.uppercase() } ?: ""
                        val year = state.currentMonthCal.get(Calendar.YEAR)
                        Column {
                            Text(
                                "$monthName $year  ·  ${state.monthTransactions.size} transaksi",
                                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = p.textMuted
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MonthTotalCard(
                                    label = "Pemasukan",
                                    value = CurrencyFormatter.format(state.monthlySummary.totalIncome),
                                    bg = p.greenBg,
                                    p = p,
                                    modifier = Modifier.weight(1f)
                                )
                                MonthTotalCard(
                                    label = "Pengeluaran",
                                    value = CurrencyFormatter.format(state.monthlySummary.totalExpense),
                                    bg = p.redBg,
                                    p = p,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    groups.forEach { (dayKey, list) ->
                        item(key = "header_$dayKey") {
                            val date = list.first().date?.toDate()
                            Text(
                                if (date != null) DateFormatters.formatDayLong(date) else "Tanpa tanggal",
                                modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 0.dp),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = p.textMuted
                            )
                        }
                        items(list, key = { it.id }) { tx ->
                            TransactionRow(
                                tx = tx,
                                onClick = { navController.navigate("transaction_detail/${tx.id}") },
                                onLongClick = { viewModel.requestDelete(tx) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Kartu total pemasukan / pengeluaran bulan berjalan (warna sama dengan layar Keuangan). */
@Composable
private fun MonthTotalCard(
    label: String,
    value: String,
    bg: Color,
    p: AllTxPalette,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = bg
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(label, fontSize = 12.sp, lineHeight = 16.sp, color = p.textMuted)
            Text(
                value,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                color = p.textPrimary,
                maxLines = 1
            )
        }
    }
}

private fun vec(name: String, build: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(build).build()

private fun ImageVector.Builder.stroked(d: String, width: Float) = addPath(
    pathData = addPathNodes(d),
    stroke = SolidColor(Color.Black),
    strokeLineWidth = width,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round
)

private val ChevronLeftIcon = vec("ChevronLeft") { stroked("M15 4L7 12l8 8", 2.2f) }