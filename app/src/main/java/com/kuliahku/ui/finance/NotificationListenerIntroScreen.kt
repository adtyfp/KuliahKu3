package com.kuliahku.ui.finance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.kuliahku.notification.listener.FinanceNotificationListener
import com.kuliahku.ui.components.PrimaryPillButton

@Composable
fun NotificationListenerIntroScreen(navController: NavHostController) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
            }
            Text("Pencatatan Otomatis", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(16.dp))
        Text("Bagaimana ini bekerja?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "KuliahKu dapat membaca notifikasi dari aplikasi bank dan e-wallet untuk mencatat transaksi secara otomatis. Fitur ini menggunakan izin Android bernama \"Notification Access\".",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        Text("Aplikasi yang didukung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        listOf("m-BCA", "BRImo", "Livin' Mandiri", "DANA", "GoPay", "OVO", "ShopeePay").forEach {
            Text("• $it", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(16.dp))
        Text("Privasi & keamanan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "• Teks notifikasi mentah TIDAK disimpan.\n• Hanya jumlah, tanggal, dan merchant yang dicatat.\n• Anda dapat meninjau sebelum disimpan.\n• Aktifkan/nonaktifkan kapan saja di Pengaturan Android.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Catatan: Izin ini sensitif. Google Play mewajibkan penjelasan eksplisit dan penggunaan minimal saat review. Jangan gunakan untuk membaca notifikasi aplikasi lain.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(24.dp))
        PrimaryPillButton(
            text = if (FinanceNotificationListener.isEnabled(context)) "Izin Aktif — Buka Pengaturan" else "Aktifkan Izin",
            onClick = { FinanceNotificationListener.openSettings(context) }
        )
    }
}