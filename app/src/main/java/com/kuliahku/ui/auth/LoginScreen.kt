package com.kuliahku.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kuliahku.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuliahku.ui.components.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val p = formPalette()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.NavigateToHome -> onLoginSuccess()
                is LoginEvent.ShowMessage -> snackbar.showSnackbar(event.message)
            }
        }
    }

    FormScreen(snackbarHostState = snackbar, p = p) {
        AuthHeader(
            title = "Selamat datang",
            prompt = "Belum punya akun?",
            actionText = "Registrasi",
            onAction = onNavigateToRegister,
            p = p,
            illustration = R.drawable.ic_login
        )

        AnimatedEntry(delay = 200) {
            FormTextField(
                label = "Email atau NIM",
                value = state.identifier,
                onValueChange = viewModel::onIdentifierChange,
                p = p,
                placeholder = "Masukkan email atau NIM",
                leadingIcon = Icons.Filled.Person,
                error = state.identifierError
            )
        }

        FormSpacer()

        AnimatedEntry(delay = 250) {
            FormTextField(
                label = "Password",
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                p = p,
                placeholder = "Masukkan password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                error = state.passwordError
            )
        }

        FormFieldError(state.generalError, p)

        Spacer(Modifier.height(24.dp))

        AnimatedEntry(delay = 300) {
            FormButton(
                text = if (state.isLoading) "Memuat..." else "Login",
                p = p,
                enabled = !state.isLoading,
                onClick = viewModel::login
            )
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = { viewModel.showResetDialog(true) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Lupa password kamu?", fontSize = 14.sp, color = p.accent)
        }
    }
}