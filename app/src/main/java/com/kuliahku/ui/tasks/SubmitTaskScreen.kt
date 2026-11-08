package com.kuliahku.ui.tasks

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kuliahku.data.model.TaskStatus
import com.kuliahku.ui.components.*
import com.kuliahku.util.DateFormatters

@Composable
fun SubmitTaskScreen(
    navController: NavHostController,
    viewModel: SubmitTaskViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val p = formPalette()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.onFilePicked(context, it) } }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SubmitTaskEvent.Submitted -> navController.popBackStack()
                is SubmitTaskEvent.ShowMessage -> snackbar.showSnackbar(event.message)
            }
        }
    }

    val task = state.task
    val isDone = task?.statusEnum == TaskStatus.DONE

    FormScreen(
        snackbarHostState = snackbar,
        p = p,
        buttonText = if (isDone) null else when {
            state.isUploading -> "Mengunggah..."
            state.pickedUri == null -> "Tandai Selesai"
            else -> "Kumpulkan"
        },
        buttonEnabled = !state.isUploading,
        onButtonClick = { viewModel.submit() }
    ) {
        FormHeader(
            title = "Kumpulkan Tugas",
            subtitle = "Kumpulkan tugasmu sebelum deadline",
            p = p,
            onBack = { navController.popBackStack() },
            art = TaskArt
        )

        FormReadOnlyField("Nama Tugas", task?.title ?: "-", p)
        FormSpacer()
        FormReadOnlyField(
            "Deadline",
            task?.deadline?.toDate()?.let { DateFormatters.formatDateTime(it) } ?: "-",
            p
        )
        FormSpacer()
        FormReadOnlyField("Deskripsi", task?.description?.ifBlank { "-" } ?: "-", p, minHeight = 96.dp)

        FormSpacer()
        FormSectionTitle("BERKAS TUGAS", p)

        // Kartu upload (garis putus-putus)
        val dashColor = if (state.pickedUri != null) p.accent else p.border
        Column(
            Modifier
                .fillMaxWidth()
                .background(p.surface, RoundedCornerShape(14.dp))
                .drawBehind {
                    drawRoundRect(
                        color = dashColor,
                        cornerRadius = CornerRadius(14.dp.toPx()),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
                        )
                    )
                }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.CloudUpload, null, tint = p.accent, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                "Upload file tugas",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = p.textPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                state.pickedName.ifBlank { "PDF, DOCX, PPTX, Foto atau Video" },
                fontSize = 13.sp,
                color = p.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        picker.launch(
                            arrayOf(
                                "application/pdf",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                "image/*",
                                "video/*"
                            )
                        )
                    },
                    enabled = !state.isUploading,
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (state.pickedUri == null) "Pilih File" else "Ganti File", color = p.accent) }
                if (state.pickedUri != null) {
                    TextButton(onClick = viewModel::clearFile, enabled = !state.isUploading) {
                        Text("Hapus", color = p.error)
                    }
                }
            }

            if (state.isUploading) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { state.uploadProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = p.accent,
                    trackColor = p.border
                )
                Spacer(Modifier.height(4.dp))
                Text("${(state.uploadProgress * 100).toInt()}%", fontSize = 12.sp, color = p.textSecondary)
                TextButton(onClick = viewModel::cancelUpload) { Text("Batal Upload", color = p.error) }
            }
        }

        FormFieldError(state.error, p)

        if (isDone) {
            FormSpacer()
            Text(
                "Tugas ini telah selesai dikumpulkan.",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(p.accent.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = p.accent,
                textAlign = TextAlign.Center
            )
        }
    }
}