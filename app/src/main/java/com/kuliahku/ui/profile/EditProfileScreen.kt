package com.kuliahku.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.kuliahku.ui.components.*

@Composable
fun EditProfileScreen(
    navController: NavHostController,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val p = formPalette()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditProfileEvent.Success -> navController.popBackStack()
                is EditProfileEvent.ShowMessage -> snackbar.showSnackbar(event.message)
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onAvatarPicked(context, it) }
    }

    FormScreen(
        snackbarHostState = snackbar,
        p = p,
        buttonText = if (state.isSaving) "Menyimpan..." else "Simpan Perubahan",
        buttonEnabled = !state.isSaving,
        onButtonClick = { viewModel.save() }
    ) {
        FormHeader(
            title = "Edit Profil",
            subtitle = "Perbarui data dirimu",
            p = p,
            onBack = { navController.popBackStack() }
        )

        // Avatar
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(112.dp)
                .clickable { picker.launch("image/*") }
        ) {
            AsyncImage(
                model = state.avatarPreview ?: state.photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(p.segmentBg)
                    .border(1.dp, p.border, CircleShape)
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(p.accent)
                    .border(2.dp, p.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CameraAlt, "Ganti foto", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        TextButton(
            onClick = { picker.launch("image/*") },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Ganti Foto", fontSize = 14.sp, color = p.accent)
        }

        Spacer(Modifier.height(16.dp))

        FormTextField(
            label = "Nama Lengkap",
            value = state.name,
            onValueChange = viewModel::onNameChange,
            p = p,
            placeholder = "Masukkan nama lengkap",
            leadingIcon = Icons.Filled.Person,
            error = state.nameError
        )
        FormSpacer()
        FormTextField(
            label = "NIM",
            value = state.nim,
            onValueChange = viewModel::onNimChange,
            p = p,
            placeholder = "Masukkan NIM",
            leadingIcon = Icons.Filled.Badge,
            keyboardType = KeyboardType.Number,
            error = state.nimError
        )
        FormSpacer()
        FormTextField(
            label = "Program Studi",
            value = state.prodi,
            onValueChange = viewModel::onProdiChange,
            p = p,
            placeholder = "Masukkan program studi",
            leadingIcon = Icons.Filled.School,
            error = state.prodiError
        )
        FormSpacer()
        FormTextField(
            label = "Semester",
            value = state.semester,
            onValueChange = viewModel::onSemesterChange,
            p = p,
            placeholder = "Masukkan semester",
            leadingIcon = Icons.Filled.DateRange,
            keyboardType = KeyboardType.Number,
            error = state.semesterError
        )

        FormFieldError(state.generalError, p)
    }
}