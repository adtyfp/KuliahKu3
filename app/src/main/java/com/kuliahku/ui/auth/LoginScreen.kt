package com.kuliahku.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.layout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.shadow
import com.kuliahku.R
import com.kuliahku.ui.components.AnimatedEntry
import com.kuliahku.ui.components.KuliahkuLogo
import com.kuliahku.ui.components.PillTextField
import com.kuliahku.ui.components.PrimaryPillButton

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.NavigateToHome -> onLoginSuccess()
                is LoginEvent.ShowMessage -> { /* Bisa tampilkan snackbar */ }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        AnimatedEntry(delay = 100) {
            KuliahkuLogo(modifier = Modifier.height(32.dp))
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Ilustrasi Login dibuat Full Width (mentok kanan kiri)
        AnimatedEntry(delay = 200) {
            Image(
                painter = painterResource(id = R.drawable.img_auth),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            constraints.copy(
                                maxWidth = constraints.maxWidth + 48.dp.roundToPx()
                            )
                        )
                        layout(placeable.width, placeable.height) {
                            placeable.place(-24.dp.roundToPx(), 0)
                        }
                    }
                    .height(250.dp)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        AnimatedEntry(delay = 300) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Selamat datang",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Belum Punya Akun? ", style = MaterialTheme.typography.bodyMedium)
                    ClickableText(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
                                append("Registrasi")
                            }
                        },
                        onClick = { onNavigateToRegister() }
                    )
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))

        AnimatedEntry(delay = 400) {
            PillTextField(
                value = state.identifier,
                onValueChange = viewModel::onIdentifierChange,
                placeholder = "Email/NIM",
                error = state.identifierError,
                modifier = Modifier.shadow(8.dp, MaterialTheme.shapes.extraLarge, ambientColor = Color.Black.copy(alpha = 0.1f))
            )
        }
        
        Spacer(Modifier.height(20.dp))
        
        AnimatedEntry(delay = 500) {
            PillTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = "Password",
                isPassword = true,
                error = state.passwordError,
                modifier = Modifier.shadow(8.dp, MaterialTheme.shapes.extraLarge, ambientColor = Color.Black.copy(alpha = 0.1f))
            )
        }

        state.generalError?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(32.dp))
        
        AnimatedEntry(delay = 600) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PrimaryPillButton(
                    text = if (state.isLoading) "Memuat..." else "Login",
                    onClick = viewModel::login,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                )

                Spacer(Modifier.height(16.dp))

                ClickableText(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))) {
                            append("Lupa password kamu?")
                        }
                    },
                    onClick = { viewModel.showResetDialog(true) }
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}
