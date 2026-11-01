package com.kuliahku.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController

@Composable
fun AppearanceScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null) }
            Text("Tampilan", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(24.dp))
        Text("Pilih tema aplikasi:", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        listOf("Terang", "Gelap", "Ikuti Sistem").forEach { option ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                RadioButton(selected = false, onClick = { /* simpan ke DataStore */ })
                Spacer(Modifier.width(8.dp))
                Text(option)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Perubahan tema akan diterapkan di FASE berikutnya melalui DataStore.", style = MaterialTheme.typography.bodySmall)
    }
}