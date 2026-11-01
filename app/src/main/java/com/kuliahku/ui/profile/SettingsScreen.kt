package com.kuliahku.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuliahku.ui.components.PillTextField
import com.kuliahku.ui.components.PrimaryPillButton

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null) }
            Text("Pengaturan", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(24.dp))
        Text("Ganti Password", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        PillTextField(state.currentPassword, viewModel::onCurrentPasswordChange, "Password Lama", isPassword = true)
        Spacer(Modifier.height(12.dp))
        PillTextField(state.newPassword, viewModel::onNewPasswordChange, "Password Baru", isPassword = true)
        Spacer(Modifier.height(12.dp))
        PrimaryPillButton(
            text = if (state.isChanging) "Menyimpan..." else "Ganti Password",
            onClick = viewModel::changePassword,
            enabled = !state.isChanging
        )

        Spacer(Modifier.height(32.dp))
        Text("Zona Berbahaya", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { viewModel.showDeleteDialog(true) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) { Text("Hapus Akun", color = MaterialTheme.colorScheme.error) }

        state.message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog(false) },
            title = { Text("Hapus Akun?") },
            text = {
                Column {
                    Text("Semua data (tugas, catatan, keuangan, dll) akan dihapus permanen. Masukkan password untuk konfirmasi.")
                    Spacer(Modifier.height(12.dp))
                    PillTextField(state.deletePassword, viewModel::onDeletePasswordChange, "Password", isPassword = true)
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::deleteAccount) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { viewModel.showDeleteDialog(false) }) { Text("Batal") } }
        )
    }
}