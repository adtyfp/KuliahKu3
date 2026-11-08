package com.kuliahku.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuliahku.ui.components.*

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val p = formPalette()

    LaunchedEffect(Unit) {
        viewModel.events.collect { if (it is RegisterEvent.NavigateToHome) onRegisterSuccess() }
    }

    FormScreen(snackbarHostState = snackbar, p = p) {
        AuthHeader(
            title = "Registrasi",
            prompt = "Sudah punya akun?",
            actionText = "Login",
            onAction = onNavigateToLogin,
            p = p,
            art = AuthArt
        )

        AnimatedEntry(delay = 200) {
            FormTextField(
                label = "Nama Lengkap",
                value = state.name,
                onValueChange = viewModel::onNameChange,
                p = p,
                placeholder = "Masukkan nama lengkap",
                leadingIcon = Icons.Filled.Person,
                error = state.nameError
            )
        }

        FormSpacer()

        AnimatedEntry(delay = 250) {
            FormTextField(
                label = "Email",
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                p = p,
                placeholder = "nama@email.com",
                leadingIcon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email,
                error = state.emailError
            )
        }

        FormSpacer()

        AnimatedEntry(delay = 300) {
            FormTextField(
                label = "Password",
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                p = p,
                placeholder = "Buat password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                error = state.passwordError
            )
        }

        FormSpacer()

        AnimatedEntry(delay = 350) {
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
        }

        FormSpacer()

        AnimatedEntry(delay = 400) {
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
        }

        FormSpacer()

        AnimatedEntry(delay = 425) {
            FormDropdownField(
                label = "Program Studi",
                options = state.prodiOptions.map { it to it },
                selected = state.prodi,
                p = p,
                expanded = state.isProdiExpanded,
                onToggle = viewModel::toggleProdiDropdown,
                onSelect = viewModel::onProdiChange,
                placeholder = "Pilih Program Studi",
                error = state.prodiError
            )
        }

        Spacer(Modifier.height(28.dp))

        if (state.generalError != null) {
            Text(
                text = state.generalError!!,
                color = p.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }

        AnimatedEntry(delay = 450) {
            FormButton(
                text = if (state.isLoading) "Memproses..." else "Registrasi",
                p = p,
                enabled = !state.isLoading,
                onClick = viewModel::register
            )
        }
    }
}