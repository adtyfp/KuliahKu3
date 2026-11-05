package com.kuliahku.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kuliahku.ui.components.KuliahkuContentDialog
import com.kuliahku.ui.components.PillTextField
import com.kuliahku.ui.components.PrimaryPillButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Privasi & Keamanan", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Ganti Password", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            PillTextField(state.currentPassword, viewModel::onCurrentPasswordChange, "Password Lama", isPassword = true)
            Spacer(Modifier.height(12.dp))
            PillTextField(state.newPassword, viewModel::onNewPasswordChange, "Password Baru", isPassword = true)
            Spacer(Modifier.height(12.dp))
            PrimaryPillButton(
                text = if (state.isChanging) "Menyimpan..." else "Ganti Password",
                onClick = viewModel::changePassword,
                enabled = !state.isChanging,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))
            Text("Zona Berbahaya", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { viewModel.showDeleteDialog(true) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Hapus Akun") }

            state.message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = if (it.contains("berhasil")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        }
    }

    if (state.showDeleteDialog) {
        KuliahkuContentDialog(
            onDismissRequest = { viewModel.showDeleteDialog(false) },
            title = "Hapus Akun?",
            icon = Icons.Default.Warning,
            iconColor = MaterialTheme.colorScheme.error,
            confirmText = "Hapus",
            onConfirm = viewModel::deleteAccount,
            dismissText = "Batal"
        ) {
            Column {
                Text("Semua data (tugas, catatan, keuangan, dll) akan dihapus permanen. Masukkan password untuk konfirmasi.")
                Spacer(Modifier.height(12.dp))
                PillTextField(state.deletePassword, viewModel::onDeletePasswordChange, "Password", isPassword = true)
            }
        }
    }
}
