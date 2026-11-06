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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    // Tidak ada horizontal padding di root, supaya ilustrasi bisa full-bleed.
    // Padding 24dp hanya diberikan ke bagian teks & form.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ===== HEADER: Ilustrasi full width + logo menumpuk di tengah atas =====
        AnimatedEntry(delay = 100) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                // Ilustrasi (sudah berisi ikon-ikon melayang) mentok kiri-kanan,
                // tinggi mengikuti rasio asli gambar agar tidak gepeng/terpotong.
                Image(
                    painter = painterResource(id = R.drawable.ic_login),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )

                // Logo di tengah atas, sejajar dengan ikon sparkle di referensi
                KuliahkuLogo(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .height(32.dp)
                )
            }
        }

        // ===== JUDUL =====
        AnimatedEntry(delay = 200) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Selamat datang",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Belum Punya Akun? ", style = MaterialTheme.typography.bodyMedium)
                    ClickableText(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) { append("Registrasi") }
                        },
                        onClick = { onNavigateToRegister() }
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ===== FORM =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedEntry(delay = 300) {
                PillTextField(
                    value = state.identifier,
                    onValueChange = viewModel::onIdentifierChange,
                    placeholder = "Email/NIM",
                    error = state.identifierError
                )
            }

            Spacer(Modifier.height(16.dp))

            AnimatedEntry(delay = 400) {
                PillTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    placeholder = "Password",
                    isPassword = true,
                    error = state.passwordError
                )
            }

            state.generalError?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(28.dp))

            AnimatedEntry(delay = 500) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PrimaryPillButton(
                        text = if (state.isLoading) "Memuat..." else "Login",
                        onClick = viewModel::login,
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    ClickableText(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            ) { append("Lupa password kamu?") }
                        },
                        onClick = { viewModel.showResetDialog(true) }
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}