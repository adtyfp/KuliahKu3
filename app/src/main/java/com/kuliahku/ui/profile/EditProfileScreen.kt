package com.kuliahku.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kuliahku.ui.components.PillTextField
import com.kuliahku.ui.components.PrimaryPillButton

@Composable
fun EditProfileScreen(
    navController: NavHostController,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditProfileEvent.Success -> {
                    navController.popBackStack()
                }
                is EditProfileEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onAvatarPicked(context, it) }
    }

    Scaffold(
        containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF0E1220) else Color(0xFFFBFCFF),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(48.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                }
                Text(
                    "Edit Profil",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = state.avatarPreview ?: state.photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { picker.launch("image/*") }
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { picker.launch("image/*") },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Ganti Foto")
            }
            Spacer(Modifier.height(24.dp))

            PillTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                placeholder = "Nama Lengkap",
                error = state.nameError
            )
            Spacer(Modifier.height(16.dp))
            PillTextField(
                value = state.nim,
                onValueChange = viewModel::onNimChange,
                placeholder = "NIM",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                error = state.nimError
            )
            Spacer(Modifier.height(16.dp))
            PillTextField(
                value = state.prodi,
                onValueChange = viewModel::onProdiChange,
                placeholder = "Program Studi",
                error = state.prodiError
            )
            Spacer(Modifier.height(16.dp))
            PillTextField(
                value = state.semester,
                onValueChange = viewModel::onSemesterChange,
                placeholder = "Semester",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                error = state.semesterError
            )

            state.generalError?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(40.dp))
            PrimaryPillButton(
                text = if (state.isSaving) "Menyimpan..." else "Simpan Perubahan",
                onClick = viewModel::save,
                enabled = !state.isSaving
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
