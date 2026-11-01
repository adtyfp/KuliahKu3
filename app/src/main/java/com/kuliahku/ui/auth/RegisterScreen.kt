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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { if (it is RegisterEvent.NavigateToHome) onRegisterSuccess() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        AnimatedEntry(delay = 50) {
            KuliahkuLogo(modifier = Modifier.height(32.dp))
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Ilustrasi Auth (Gunakan SVG yang sama dengan Login atau berbeda)
        AnimatedEntry(delay = 100) {
            Image(
                painter = painterResource(id = R.drawable.img_auth),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
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
                    .height(180.dp)
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        AnimatedEntry(delay = 150) {
            Column {
                Text(
                    text = "Registrasi",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sudah Punya Akun? ", style = MaterialTheme.typography.bodyMedium)
                    ClickableText(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
                                append("Login")
                            }
                        },
                        onClick = { onNavigateToLogin() }
                    )
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))

        AnimatedEntry(delay = 200) {
            PillTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                placeholder = "Nama Lengkap",
                error = state.nameError,
                modifier = Modifier.shadow(8.dp, MaterialTheme.shapes.extraLarge, ambientColor = Color.Black.copy(alpha = 0.1f))
            )
        }
        Spacer(Modifier.height(20.dp))
        AnimatedEntry(delay = 250) {
            PillTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = "Email",
                error = state.emailError,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                modifier = Modifier.shadow(8.dp, MaterialTheme.shapes.extraLarge, ambientColor = Color.Black.copy(alpha = 0.1f))
            )
        }
        Spacer(Modifier.height(20.dp))
        AnimatedEntry(delay = 300) {
            PillTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = "Password",
                isPassword = true,
                error = state.passwordError,
                modifier = Modifier.shadow(8.dp, MaterialTheme.shapes.extraLarge, ambientColor = Color.Black.copy(alpha = 0.1f))
            )
        }
        Spacer(Modifier.height(20.dp))
        AnimatedEntry(delay = 350) {
            PillTextField(
                value = state.nim,
                onValueChange = viewModel::onNimChange,
                placeholder = "NIM",
                error = state.nimError,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                modifier = Modifier.shadow(8.dp, MaterialTheme.shapes.extraLarge, ambientColor = Color.Black.copy(alpha = 0.1f))
            )
        }
        Spacer(Modifier.height(20.dp))
        AnimatedEntry(delay = 400) {
            PillTextField(
                value = state.semester,
                onValueChange = viewModel::onSemesterChange,
                placeholder = "Semester",
                error = state.semesterError,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                modifier = Modifier.shadow(8.dp, MaterialTheme.shapes.extraLarge, ambientColor = Color.Black.copy(alpha = 0.1f))
            )
        }
        Spacer(Modifier.height(32.dp))
        
        AnimatedEntry(delay = 450) {
            PrimaryPillButton(
                text = if (state.isLoading) "Memproses..." else "Registrasi",
                onClick = viewModel::register,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        AnimatedEntry(delay = 500) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                ClickableText(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("Lupa password kamu?")
                        }
                    },
                    onClick = { /* ... */ }
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}
