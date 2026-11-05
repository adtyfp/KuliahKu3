package com.kuliahku.ui.finance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kuliahku.ui.theme.cardShadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kuliahku.data.model.Transaction
import com.kuliahku.data.model.TxType
import com.kuliahku.ui.components.ErrorView
import com.kuliahku.ui.components.ShimmerBox
import com.kuliahku.util.CurrencyFormatter
import com.kuliahku.util.DateFormatters

// ---------------------------------------------------------------------------
// Palet (sama dengan layar Keuangan: Light #F8F9FF, Dark #0E1220).
// Mode gelap dideteksi dari MaterialTheme.colorScheme.background.
// ---------------------------------------------------------------------------
private class DetailPalette(
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val divider: Color,
    val income: Color,
    val expense: Color,
    val incomeBg: Color,
    val expenseBg: Color
)

private val DetailLight = DetailPalette(
    background = Color(0xFFF8F9FF),
    surface = Color.White,
    border = Color(0xFFE0E0E5),
    textPrimary = Color.Black,
    textMuted = Color(0xFF767678),
    divider = Color(0xFFECECF1),
    income = Color(0xFF0AAA0A),
    expense = Color(0xFFC80000),
    incomeBg = Color(0xFFC2FFC4),
    expenseBg = Color(0xFFFFB1B1)
)

private val DetailDark = DetailPalette(
    background = Color(0xFF0E1220),
    surface = Color(0xFF171C2C),
    border = Color(0xFF2A3350),
    textPrimary = Color(0xFFF1F4FF),
    textMuted = Color(0xFFA3ABC4),
    divider = Color(0xFF232B45),
    income = Color(0xFF3DD083),
    expense = Color(0xFFFF6B7A),
    incomeBg = Color(0xFF14362A),
    expenseBg = Color(0xFF45202A)
)

@Composable
private fun detailPalette(): DetailPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DetailDark else DetailLight

@Composable
fun TransactionDetailScreen(
    navController: NavHostController,
    txId: String,
    viewModel: FinanceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val transaction = remember(state.yearTransactions, txId) {
        state.yearTransactions.find { it.id == txId }
    }
    val p = detailPalette()

    Scaffold(containerColor = p.background) { padding ->
        // padding dari Scaffold sudah memuat inset status bar, jadi spacer atas dibuat kecil.
        Column(Modifier.fillMaxSize().padding(padding)) {
            Spacer(Modifier.height(8.dp))
            DetailHeader(title = "Detail Transaksi", onBack = { navController.popBackStack() }, p = p)

            when {
                transaction != null -> Box(Modifier.weight(1f)) {
                    TransactionDetailContent(transaction, PaddingValues(0.dp))
                }
                state.isLoading -> Box(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ShimmerBox(Modifier.fillMaxWidth().height(400.dp))
                }
                else -> ErrorView(message = "Transaksi tidak ditemukan", onRetry = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun DetailHeader(title: String, onBack: () -> Unit, p: DetailPalette) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(ChevronLeftIcon, "Kembali", tint = p.textPrimary, modifier = Modifier.size(26.dp))
        }
        Text(
            title,
            fontSize = 24.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.SemiBold,
            color = p.textPrimary
        )
    }
}

@Composable
fun TransactionDetailContent(tx: Transaction, padding: PaddingValues) {
    val p = detailPalette()
    val isIncome = tx.typeEnum == TxType.INCOME
    val accent = if (isIncome) p.income else p.expense
    val accentBg = if (isIncome) p.incomeBg else p.expenseBg
    val cardShape = RoundedCornerShape(24.dp)
    val note = tx.description?.takeIf { it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // ---------------- Ringkasan: ikon, status, nominal ----------------
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .cardShadow(shapeRadius = 24.dp),
                shape = cardShape,
                color = p.surface,
                border = BorderStroke(1.dp, p.border)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(accentBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isIncome) DetailBanknotePlus else DetailBanknoteMinus,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (isIncome) "Pemasukan Berhasil" else "Pengeluaran Berhasil",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = p.textMuted
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        (if (isIncome) "+ " else "- ") + CurrencyFormatter.format(tx.amount),
                        fontSize = 32.sp,
                        lineHeight = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---------------- Rincian ----------------
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .cardShadow(shapeRadius = 24.dp),
                shape = cardShape,
                color = p.surface,
                border = BorderStroke(1.dp, p.border)
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    DetailRow(
                        label = "Tipe Transaksi",
                        value = if (isIncome) "Uang Masuk" else "Uang Keluar",
                        valueColor = accent
                    )
                    DetailDivider(p)
                    DetailRow(label = "Kategori", value = tx.category.ifBlank { "-" })
                    DetailDivider(p)
                    DetailRow(label = "Waktu", value = tx.date?.toDate()?.let { DateFormatters.formatDateTime(it) } ?: "-")
                    DetailDivider(p)
                    DetailRow(label = "Sumber", value = tx.source ?: "Manual")

                    if (note != null) {
                        DetailDivider(p)
                        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                            Text(
                                "Catatan",
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = p.textMuted
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                note,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = p.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // ---------------- ID transaksi (selalu di bawah layar) ----------------
        Text(
            text = "ID Transaksi: ${tx.id.takeLast(12).uppercase()}",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = p.textMuted.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp)
        )
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color? = null) {
    val p = detailPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 14.sp, lineHeight = 20.sp, color = p.textMuted)
        Spacer(Modifier.width(16.dp))
        Text(
            value,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor ?: p.textPrimary,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun DetailDivider(p: DetailPalette) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
}

// ---------------------------------------------------------------------------
// Ikon vektor (gaya sama dengan ikon transaksi di layar Keuangan)
// ---------------------------------------------------------------------------
private const val DetailBanknoteBody =
    "M2 5h16a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z " +
            "M10 8.2a2.8 2.8 0 1 0 0 5.6 2.8 2.8 0 1 0 0-5.6z " +
            "M10 9.7a1.3 1.3 0 1 0 0 2.6 1.3 1.3 0 1 0 0-2.6z"

private fun banknoteIcon(name: String, badge: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(
            pathData = addPathNodes(DetailBanknoteBody),
            pathFillType = PathFillType.EvenOdd,
            fill = SolidColor(Color.Black)
        )
        .addPath(pathData = addPathNodes(badge), fill = SolidColor(Color.Black))
        .build()

private val DetailBanknotePlus = banknoteIcon("BanknotePlus", "M18.5 15h2v2.5H23v2h-2.5V22h-2v-2.5H16v-2h2.5z")
private val DetailBanknoteMinus = banknoteIcon("BanknoteMinus", "M16 17.5h7v2h-7z")

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