package com.kuliahku.ui.notes

import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController

// ─────────────────────────────────────────────────────────────────────────────
// Editor catatan bergaya Google Keep: layar polos tanpa kotak/border, judul besar,
// isi catatan memenuhi layar, tersimpan otomatis saat kembali.
// Light = putih seperti Keep. Dark = #0E1220 (latar), #F1F4FF (teks), abu kebiruan (hint),
// sama dengan palet gelap layar lain.
// ─────────────────────────────────────────────────────────────────────────────
private class KeepPalette(
    val background: Color,
    val textPrimary: Color,
    val hint: Color,
    val status: Color
)

private val LightKeepPalette = KeepPalette(
    background = Color.White,
    textPrimary = Color(0xFF202124),
    hint = Color(0xFF80868B),
    status = Color(0xFF5F6368)
)

private val DarkKeepPalette = KeepPalette(
    background = Color(0xFF0E1220),
    textPrimary = Color(0xFFF1F4FF),
    hint = Color(0xFFA3ABC4),
    status = Color(0xFFA3ABC4)
)

@Composable
private fun keepPalette(): KeepPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DarkKeepPalette else LightKeepPalette

@Composable
fun NoteEditorScreen(
    navController: NavHostController,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val p = keepPalette()
    val focusManager = LocalFocusManager.current
    val titleFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                NoteEditorEvent.Saved -> { navController.popBackStack(); snackbar.showSnackbar("Catatan disimpan.") }
                is NoteEditorEvent.ShowMessage -> snackbar.showSnackbar(ev.message)
            }
        }
    }

    // Catatan baru: langsung fokus ke judul (seperti Keep) agar keyboard muncul.
    LaunchedEffect(state.isNew) {
        if (state.isNew) runCatching { titleFocus.requestFocus() }
    }

    // Tombol kembali sistem juga menyimpan otomatis, sama seperti tombol panah di atas.
    val saveAndExit: () -> Unit = { viewModel.save(silent = true) { navController.popBackStack() } }
    BackHandler(onBack = saveAndExit)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = p.background,
        contentColor = p.textPrimary
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // Bilah atas: panah kembali di kiri, indikator simpan di kanan
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 20.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = saveAndExit) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = p.textPrimary)
                }
                Spacer(Modifier.weight(1f))
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = p.status
                    )
                } else if (state.lastSavedAt != null) {
                    Text(
                        "Tersimpan",
                        fontSize = 12.sp,
                        color = p.status
                    )
                }
            }

            // Judul: tanpa border, 22sp
            BasicTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Medium,
                    color = p.textPrimary
                ),
                cursorBrush = SolidColor(p.textPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { contentFocus.requestFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .focusRequester(titleFocus),
                decorationBox = { inner ->
                    Box {
                        if (state.title.isEmpty()) {
                            Text(
                                "Judul",
                                fontSize = 22.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.Medium,
                                color = p.hint
                            )
                        }
                        inner()
                    }
                }
            )

            // Isi catatan: memenuhi sisa layar, tanpa border
            BasicTextField(
                value = state.content,
                onValueChange = viewModel::onContentChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = p.textPrimary
                ),
                cursorBrush = SolidColor(p.textPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .focusRequester(contentFocus),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize()) {
                        if (state.content.isEmpty()) {
                            Text(
                                "Catatan",
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = p.hint
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
}