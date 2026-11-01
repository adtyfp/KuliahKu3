package com.kuliahku.ui.scan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kuliahku.notification.NotificationChannels
import com.kuliahku.ui.components.AnimatedEntry
import com.kuliahku.ui.theme.*
import com.kuliahku.util.DocumentExtractor
import com.kuliahku.util.WebExtractor
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ---------------------------------------------------------------------------
// Palet Scan (Light = sesuai Scan.png, Dark = nilai yang sama dengan Beranda).
// Mode gelap dideteksi dari MaterialTheme.colorScheme.background.
// ---------------------------------------------------------------------------
private class ScanTone(val container: Color, val border: Color, val icon: Color)

private class ScanPalette(
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val headerSubtitle: Color,
    val cardSubtitle: Color,
    val arrow: Color,
    val shadowAlpha: Float,
    val upload: ScanTone,
    val youtube: ScanTone,
    val audio: ScanTone
)

private val ScanLight = ScanPalette(
    background = Color(0xFFF8F9FF),
    surface = Color.White,
    border = Color(0xFFD9D9DF),
    textPrimary = Color.Black,
    headerSubtitle = Color(0xFF7D7D7D),
    cardSubtitle = Color(0xFF464646),
    arrow = Color(0xFF6E6E6E),
    shadowAlpha = 0.10f,
    upload = ScanTone(Color(0xFFD6E0FC), Color(0xFF0000FF), Color(0xFF0000FF)),
    youtube = ScanTone(Color(0xFFFFCDCD), Color(0xFFFF0000), Color(0xFFFF0000)),
    audio = ScanTone(Color(0xFFC8F5C8), Color(0xFF1BA81B), Color(0xFF12A012))
)

private val ScanDark = ScanPalette(
    background = Color(0xFF0E1220),
    surface = Color(0xFF171C2C),
    border = Color(0xFF2A3350),
    textPrimary = Color(0xFFF1F4FF),
    headerSubtitle = Color(0xFF9AA3BF),
    cardSubtitle = Color(0xFFA3ABC4),
    arrow = Color(0xFF8B93AE),
    shadowAlpha = 0.45f,
    upload = ScanTone(Color(0xFF1C2755), Color(0xFF4C6FFF), Color(0xFF8FA8FF)),
    youtube = ScanTone(Color(0xFF3D1A20), Color(0xFFFF5C6C), Color(0xFFFF8A95)),
    audio = ScanTone(Color(0xFF11351F), Color(0xFF34C759), Color(0xFF6EE79A))
)

@Composable
private fun scanPalette(): ScanPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) ScanDark else ScanLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    navController: NavHostController,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val p = scanPalette()

    var showTextDialog by remember { mutableStateOf(false) }
    var showYoutubeDialog by remember { mutableStateOf(false) }
    var showWebDialog by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSourceSheet by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val mime = context.contentResolver.getType(it) ?: ""
            scope.launch {
                val res = DocumentExtractor.extract(context, it, mime)
                res.onSuccess { text ->
                    viewModel.createFromText("Materi ${mime.substringAfterLast('/').uppercase()}", text)
                }.onFailure { e ->
                    snackbar.showSnackbar(e.message ?: "Gagal ekstraksi file.")
                }
            }
        }
    }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.createFromAudio(it, context) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ScanEvent.NavigateToResult -> navController.navigate("material_result/${event.materialId}")
                is ScanEvent.ShowMessage -> snackbar.showSnackbar(event.message)
            }
        }
    }

    if (state.isProcessing) {
        ModalBottomSheet(onDismissRequest = {}) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    "Sedang memproses materi dengan AI...",
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    Box(Modifier.fillMaxSize().background(p.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
        ) {
            Spacer(Modifier.height(68.dp))

            // ---------------- Header ----------------
            AnimatedEntry(delay = 100) {
                Column {
                    Text(
                        "Scanning Materi",
                        modifier = Modifier.padding(start = 4.dp),
                        fontSize = 31.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.textPrimary
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Pindai materi dosen kamu di sini untuk dijadikan\ncatatan",
                        modifier = Modifier.padding(start = 4.dp),
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.headerSubtitle
                    )
                }
            }
            Spacer(Modifier.height(17.dp))

            // ---------------- Kartu ----------------
            AnimatedEntry(delay = 200) {
                ScanCard(
                    icon = Icons.Filled.UploadFile,
                    tone = p.upload,
                    title = "Upload Materi",
                    subtitle = "PDF, DOCX, PPTX, atau Tempelkan teks",
                    onClick = { showSourceSheet = true }
                )
            }
            Spacer(Modifier.height(35.dp))
            AnimatedEntry(delay = 300) {
                ScanCard(
                    icon = rememberYoutubeIcon(),
                    tone = p.youtube,
                    title = "Link Youtube",
                    subtitle = "Youtube atau website",
                    onClick = { showYoutubeDialog = true }
                )
            }
            Spacer(Modifier.height(35.dp))
            AnimatedEntry(delay = 400) {
                ScanCard(
                    icon = Icons.Filled.Mic,
                    tone = p.audio,
                    title = "Audio",
                    subtitle = "Rekam kelas atau unggah file",
                    onClick = { showAudioSheet = true }
                )
            }

            Spacer(Modifier.height(120.dp))
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp))
    }

    if (showSourceSheet) {
        ModalBottomSheet(onDismissRequest = { showSourceSheet = false }) {
            Column(Modifier.padding(20.dp)) {
                Text("Sumber Materi", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                ListItem(
                    headlineContent = { Text("Pilih File") },
                    supportingContent = { Text("PDF, DOCX, PPTX") },
                    leadingContent = { Icon(Icons.Filled.UploadFile, null) },
                    modifier = Modifier.clickable {
                        showSourceSheet = false
                        filePicker.launch(arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.openxmlformats-officedocument.presentationml.presentation"))
                    }
                )
                ListItem(
                    headlineContent = { Text("Tempel Teks") },
                    supportingContent = { Text("Salin teks dari sumber lain") },
                    leadingContent = { Icon(Icons.Filled.ContentPaste, null) },
                    modifier = Modifier.clickable {
                        showSourceSheet = false
                        showTextDialog = true
                    }
                )
                ListItem(
                    headlineContent = { Text("Ambil Foto (OCR)") },
                    supportingContent = { Text("Pindai teks dari buku/kertas") },
                    leadingContent = { Icon(Icons.Filled.PhotoCamera, null) },
                    modifier = Modifier.clickable {
                        showSourceSheet = false
                        scope.launch {
                            snackbar.showSnackbar("Fitur OCR segera hadir.")
                        }
                    }
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showTextDialog) {
        TextPasteDialog(
            onDismiss = { showTextDialog = false },
            onSubmit = { title, text ->
                showTextDialog = false
                viewModel.createFromText(title, text)
            }
        )
    }

    if (showYoutubeDialog) {
        UrlInputDialog(
            title = "Link YouTube / Website",
            placeholder = "https://...",
            onDismiss = { showYoutubeDialog = false },
            onSubmit = { url ->
                showYoutubeDialog = false
                if (url.contains("youtube.com") || url.contains("youtu.be")) {
                    viewModel.createFromYoutube(url)
                } else {
                    scope.launch {
                        WebExtractor.fetch(url).onSuccess { text ->
                            viewModel.createFromWeb(url, text)
                        }.onFailure { e ->
                            snackbar.showSnackbar("Gagal mengambil konten web: ${e.message}")
                        }
                    }
                }
            }
        )
    }

    if (showAudioSheet) {
        ModalBottomSheet(onDismissRequest = { showAudioSheet = false }) {
            Column(Modifier.padding(20.dp)) {
                Text("Sumber Audio", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                ListItem(
                    headlineContent = { Text("Rekam Sekarang") },
                    supportingContent = { Text("Maksimal 15 menit") },
                    leadingContent = { Icon(Icons.Filled.Mic, null) },
                    modifier = Modifier.clickable {
                        showAudioSheet = false
                        navController.navigate("record_audio")
                    }
                )
                ListItem(
                    headlineContent = { Text("Unggah File Audio") },
                    supportingContent = { Text("MP3, M4A, WAV, maks 50 MB") },
                    leadingContent = { Icon(Icons.Filled.AudioFile, null) },
                    modifier = Modifier.clickable {
                        showAudioSheet = false
                        audioPicker.launch(arrayOf("audio/*"))
                    }
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ScanCard(
    icon: ImageVector,
    tone: ScanTone,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val p = scanPalette()
    val cardShape = RoundedCornerShape(30.dp)
    val iconShape = RoundedCornerShape(12.dp)
    val shadowColor = Color.Black.copy(alpha = p.shadowAlpha)
    val glow = tone.border.copy(alpha = if (p.shadowAlpha > 0.2f) 0.30f else 0.40f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(179.dp)
            .shadow(8.dp, cardShape, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(cardShape)
            .clickable(onClick = onClick),
        shape = cardShape,
        color = p.surface,
        border = BorderStroke(1.dp, p.border)
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.padding(start = 26.dp, top = 19.dp)) {
                Box(
                    modifier = Modifier
                        .size(39.dp)
                        .shadow(6.dp, iconShape, ambientColor = glow, spotColor = glow)
                        .clip(iconShape)
                        .background(tone.container)
                        .border(1.dp, tone.border, iconShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tone.icon, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(15.dp))
                Column(Modifier.padding(start = 20.dp)) {
                    Text(
                        title,
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.textPrimary
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        subtitle,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = p.cardSubtitle
                    )
                }
            }
            Icon(
                Icons.Filled.ArrowOutward,
                contentDescription = null,
                tint = p.arrow,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 21.dp, end = 21.dp)
                    .size(20.dp)
            )
        }
    }
}

/** Logo YouTube (persegi membulat + segitiga play berlubang) untuk kartu "Link Youtube". */
@Composable
private fun rememberYoutubeIcon(): ImageVector = remember {
    ImageVector.Builder(
        name = "YoutubeLogo",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            moveTo(6f, 5f); horizontalLineTo(18f)
            quadTo(22f, 5f, 22f, 9f); verticalLineTo(15f)
            quadTo(22f, 19f, 18f, 19f); horizontalLineTo(6f)
            quadTo(2f, 19f, 2f, 15f); verticalLineTo(9f)
            quadTo(2f, 5f, 6f, 5f); close()
            moveTo(10f, 9f); lineTo(10f, 15f); lineTo(15.2f, 12f); close()
        }
    }.build()
}

@Composable
private fun TextPasteDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tempel Materi") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, placeholder = { Text("Judul materi (opsional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text("Tempelkan teks materi...") }, modifier = Modifier.fillMaxWidth().height(200.dp))
            }
        },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onSubmit(title, text) }, enabled = text.isNotBlank()) { Text("Proses") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun UrlInputDialog(title: String, placeholder: String, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = url, onValueChange = { url = it }, placeholder = { Text(placeholder) }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { if (url.isNotBlank()) onSubmit(url.trim()) }, enabled = url.isNotBlank()) { Text("Proses") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}