package com.kuliahku.ui.tasks

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kuliahku.ui.components.PrimaryPillButton
import com.kuliahku.util.DateFormatters

@Composable
fun SubmitTaskScreen(
    navController: NavHostController,
    viewModel: SubmitTaskViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.onFilePicked(context, it) } }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SubmitTaskEvent.Submitted -> {
                    snackbar.showSnackbar("Tugas berhasil dikumpulkan.")
                    navController.popBackStack()
                }
                is SubmitTaskEvent.ShowMessage -> snackbar.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                }
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("📖", style = MaterialTheme.typography.displayLarge)
            }
            Spacer(Modifier.height(8.dp))
            Text("Kumpulkan Tugas", style = MaterialTheme.typography.headlineLarge)
            Text("Kumpulkan tugas mu sebelum deadline", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(20.dp))

            val task = state.task
            ReadOnlyField("Nama Tugas (dipanggil kembali)", task?.title ?: "-")
            Spacer(Modifier.height(12.dp))
            ReadOnlyField(
                "Deadline Tugas (dipanggil kembali)",
                task?.deadline?.toDate()?.let { DateFormatters.formatDateTime(it) } ?: "-"
            )
            Spacer(Modifier.height(12.dp))
            ReadOnlyField("Deskripsi Tugas (dipanggil kembali)", task?.description ?: "-")
            Spacer(Modifier.height(16.dp))

            // Kartu upload
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Upload file tugas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        state.pickedName.ifBlank { "PDF, DOCX, PPTX, Foto atau Video" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                picker.launch(arrayOf(
                                    "application/pdf",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                    "image/*",
                                    "video/*"
                                ))
                            },
                            enabled = !state.isUploading
                        ) { Text(if (state.pickedUri == null) "Pilih File" else "Ganti File") }
                        if (state.pickedUri != null) {
                            TextButton(onClick = viewModel::clearFile, enabled = !state.isUploading) { Text("Hapus") }
                        }
                    }

                    if (state.isUploading) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { state.uploadProgress }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(4.dp))
                        Text("${(state.uploadProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = viewModel::cancelUpload) { Text("Batal Upload") }
                    }
                }
            }

            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))
            PrimaryPillButton(
                text = if (state.isUploading) "Mengunggah..." else if (state.pickedUri == null) "Tandai Selesai" else "Submit",
                onClick = viewModel::submit,
                enabled = !state.isUploading
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReadOnlyField(label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}