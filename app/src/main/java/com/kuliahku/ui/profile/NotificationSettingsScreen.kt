package com.kuliahku.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.kuliahku.notification.NotificationPermissions

@Composable
fun NotificationSettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    var taskReminder by remember { mutableStateOf(true) }
    var eventReminder by remember { mutableStateOf(true) }
    var financeReminder by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
            }
            Text("Notifikasi", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(16.dp))

        SwitchRow("Pengingat Deadline Tugas", "H-1 dan 2 jam sebelum deadline", taskReminder) { taskReminder = it }
        Divider()
        SwitchRow("Pengingat Acara", "Sesuai Waktu Pengingat acara", eventReminder) { eventReminder = it }
        Divider()
        SwitchRow("Notifikasi Keuangan", "Deteksi otomatis transaksi dari notifikasi bank", financeReminder) { financeReminder = it }

        Spacer(Modifier.height(24.dp))
        Text("Izin Sistem", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { NotificationPermissions.openAppNotificationSettings(context) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) { Text("Buka Pengaturan Notifikasi Aplikasi") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { NotificationPermissions.openExactAlarmSettings(context) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) { Text("Izinkan Alarm Tepat Waktu") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { NotificationPermissions.openBatteryOptimizationSettings(context) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) { Text("Atur Pembatasan Baterai") }

        Spacer(Modifier.height(16.dp))
        Text(
            "Beberapa perangkat (Xiaomi, Oppo, Vivo) memiliki pembatasan baterai agresif yang dapat menunda notifikasi. Matikan optimasi baterai untuk KuliahKu.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}