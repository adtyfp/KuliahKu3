package com.kuliahku.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.kuliahku.R
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// PALET FORM (warna sama dengan tema yang sudah ada: light & dark)
// ---------------------------------------------------------------------------
class FormPalette(
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val error: Color,
    val segmentBg: Color
)

private val LightFormPalette = FormPalette(
    background = Color(0xFFFBFCFF),
    surface = Color.White,
    border = Color(0xFFE0E0E0),
    textPrimary = Color.Black,
    textSecondary = Color(0xFF808080),
    accent = Color(0xFF0F79DC),
    error = Color(0xFFC80000),
    segmentBg = Color(0xFFEEF1F8)
)

private val DarkFormPalette = FormPalette(
    background = Color(0xFF0E1220),
    surface = Color(0xFF171C2C),
    border = Color(0xFF2A3350),
    textPrimary = Color(0xFFF1F4FF),
    textSecondary = Color(0xFFA3ABC4),
    accent = Color(0xFF3B6FE8),
    error = Color(0xFFFF6B7A),
    segmentBg = Color(0xFF1B2134)
)

@Composable
fun formPalette(): FormPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DarkFormPalette else LightFormPalette

/** Padding horizontal konten form. Ilustrasi full-bleed membatalkan padding ini. */
private val FormHorizontalPadding: Dp = 24.dp

private val FieldShape = RoundedCornerShape(14.dp)
private val FieldHeight = 52.dp

/** Latar + border; border berubah warna saat fokus / error. */
fun Modifier.formFieldBox(p: FormPalette, focused: Boolean = false, hasError: Boolean = false): Modifier {
    val color = when {
        hasError -> p.error
        focused -> p.accent
        else -> p.border
    }
    return this
        .background(p.surface, FieldShape)
        .border(if (focused || hasError) 1.5.dp else 1.dp, color, FieldShape)
}

// ---------------------------------------------------------------------------
// KERANGKA HALAMAN: konten scroll + tombol aksi menempel di bawah
// ---------------------------------------------------------------------------
@Composable
fun FormScreen(
    snackbarHostState: SnackbarHostState,
    p: FormPalette,
    buttonText: String? = null,
    buttonEnabled: Boolean = true,
    onButtonClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = p.background,
        contentColor = p.textPrimary,
        bottomBar = {
            if (buttonText != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(p.background)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    FormButton(buttonText, p, enabled = buttonEnabled, onClick = onButtonClick)
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FormHorizontalPadding)
        ) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Header seragam: tombol kembali, ilustrasi (opsional), judul, subjudul (opsional). */
@Composable
fun FormHeader(
    title: String,
    p: FormPalette,
    onBack: () -> Unit,
    subtitle: String? = null,
    @DrawableRes illustration: Int? = null,
    art: FullBleedArt? = null
) {
    Spacer(Modifier.height(8.dp))
    if (art != null) {
        // Ilustrasi di tengah, tombol kembali menumpuk di kiri atas (hemat tinggi layar).
        Box(Modifier.fillMaxWidth()) {
            FullBleedIllustration(art, Modifier.align(Alignment.TopCenter))
            FormBackButton(p, onBack, Modifier.align(Alignment.TopStart))
        }
    } else {
        FormBackButton(p, onBack)
        if (illustration != null) {
            Image(
                painter = painterResource(id = illustration),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(top = 8.dp)
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(title, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)
    if (subtitle != null) {
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 14.sp, lineHeight = 20.sp, color = p.textSecondary)
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun FormBackButton(p: FormPalette, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(p.surface, CircleShape)
            .border(1.dp, p.border, CircleShape)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = p.textPrimary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun FormSectionTitle(text: String, p: FormPalette) {
    Text(
        text,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = p.textSecondary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun FormSpacer() = Spacer(Modifier.height(16.dp))

@Composable
fun FormButton(text: String, p: FormPalette, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(FieldShape)
            .background(p.accent.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

// ---------------------------------------------------------------------------
// LABEL & ERROR
// ---------------------------------------------------------------------------
@Composable
private fun FieldLabel(label: String, optional: Boolean, p: FormPalette) {
    Row(Modifier.padding(start = 2.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = p.textPrimary)
        if (optional) {
            Text("  (opsional)", fontSize = 12.sp, lineHeight = 20.sp, color = p.textSecondary)
        }
    }
}

@Composable
fun FormFieldError(message: String?, p: FormPalette) {
    if (!message.isNullOrBlank()) {
        Text(
            message,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = p.error,
            modifier = Modifier.padding(start = 2.dp, top = 6.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// TEXT FIELD (label di atas, kotak rounded, border fokus)
// ---------------------------------------------------------------------------
@Composable
fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    p: FormPalette,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: ImageVector? = null,
    prefix: String? = null,
    isPassword: Boolean = false,
    optional: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label, optional, p)
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (singleLine) Modifier.height(FieldHeight) else Modifier.heightIn(min = 120.dp))
                .formFieldBox(p, focused, error != null)
                .padding(horizontal = 16.dp, vertical = if (singleLine) 0.dp else 14.dp),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = p.textSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
            }
            if (prefix != null) {
                Text(prefix, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = p.textSecondary)
                Spacer(Modifier.width(8.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = p.textPrimary
                ),
                cursorBrush = SolidColor(p.accent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                    capitalization = if (isPassword) KeyboardCapitalization.None else KeyboardCapitalization.Sentences
                ),
                visualTransformation = if (isPassword && !showPassword) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Box(contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, fontSize = 15.sp, lineHeight = 22.sp, color = p.textSecondary)
                        }
                        inner()
                    }
                }
            )
            if (isPassword) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (showPassword) "Sembunyikan password" else "Tampilkan password",
                    tint = p.textSecondary,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable { showPassword = !showPassword }
                )
            }
        }
        FormFieldError(error, p)
    }
}

/** Field hanya-baca dengan gaya yang sama. */
@Composable
fun FormReadOnlyField(label: String, value: String, p: FormPalette, minHeight: androidx.compose.ui.unit.Dp = FieldHeight) {
    Column(Modifier.fillMaxWidth()) {
        FieldLabel(label, false, p)
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .formFieldBox(p)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(value, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, color = p.textPrimary)
        }
    }
}

// ---------------------------------------------------------------------------
// FIELD YANG BISA DIBUKA (dropdown / date picker inline)
// ---------------------------------------------------------------------------
@Composable
fun FormExpandableField(
    label: String,
    value: String,
    placeholder: String,
    p: FormPalette,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    leadingIcon: ImageVector? = null,
    optional: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label, optional, p)
        Column(
            Modifier
                .fillMaxWidth()
                .animateContentSize(tween(250, easing = FastOutSlowInEasing))
                .formFieldBox(p, expanded, error != null)
                .clip(FieldShape)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(FieldHeight)
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, null, tint = p.textSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    value.ifBlank { placeholder },
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (value.isBlank()) p.textSecondary else p.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = p.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(250)),
                exit = shrinkVertically(tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(250))
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(p.border))
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), content = content)
                }
            }
        }
        FormFieldError(error, p)
    }
}

@Composable
fun FormOptionItem(text: String, selected: Boolean, p: FormPalette, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) p.accent else p.textPrimary
        )
        if (selected) {
            Icon(Icons.Filled.Check, null, tint = p.accent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun <T> FormDropdownField(
    label: String,
    options: List<Pair<String, T>>,
    selected: T?,
    p: FormPalette,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Pilih",
    error: String? = null,
    leadingIcon: ImageVector? = null
) {
    val current = options.firstOrNull { it.second == selected }?.first ?: ""
    FormExpandableField(
        label = label,
        value = current,
        placeholder = placeholder,
        p = p,
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier,
        error = error,
        leadingIcon = leadingIcon
    ) {
        options.forEach { (text, v) ->
            FormOptionItem(text, v == selected, p) {
                onSelect(v)
                onToggle()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// SEGMENTED CONTROL & SWITCH
// ---------------------------------------------------------------------------
@Composable
fun FormSegmented(options: List<String>, selectedIndex: Int, p: FormPalette, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(FieldShape)
            .background(p.segmentBg)
            .padding(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (sel) p.accent else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (sel) Color.White else p.textSecondary
                )
            }
        }
    }
}

@Composable
fun FormSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, p: FormPalette) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(FieldHeight)
            .formFieldBox(p)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = p.textPrimary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = p.accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = p.border,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

// ---------------------------------------------------------------------------
// HEADER AUTH (Login & Registrasi): logo, ilustrasi, judul, tautan pindah halaman
// ---------------------------------------------------------------------------
@Composable
fun AuthHeader(
    title: String,
    prompt: String,
    actionText: String,
    onAction: () -> Unit,
    p: FormPalette,
    art: FullBleedArt? = null,
    @DrawableRes illustration: Int? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        AnimatedEntry(delay = 50) {
            KuliahkuLogo(modifier = Modifier.height(32.dp))
        }
        Spacer(Modifier.height(8.dp))
        if (art != null) {
            FullBleedIllustration(art)
        } else if (illustration != null) {
            // Ilustrasi biasa (di dalam padding form), tinggi tetap.
            AnimatedEntry(delay = 100) {
                Image(
                    painter = painterResource(id = illustration),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        AnimatedEntry(delay = 150) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    fontSize = 28.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(prompt, fontSize = 14.sp, lineHeight = 20.sp, color = p.textSecondary)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        actionText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onAction)
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

// ---------------------------------------------------------------------------
// ILUSTRASI FULL-BLEED (mentok kiri-kanan layar)
// ---------------------------------------------------------------------------
/**
 * Area ilustrasi yang ditampilkan, dalam satuan viewport vector (1500 x 1500).
 * Ruang kosong di sekitar gambar dipotong sehingga gambar memenuhi lebar layar.
 * Kalau ada bagian gambar yang terpotong / terlalu longgar, ubah angka crop di sini.
 */
class FullBleedArt(
    @DrawableRes val res: Int,
    val cropLeft: Float,
    val cropTop: Float,
    val cropWidth: Float,
    val cropHeight: Float,
    val viewport: Float = 1500f,
    /** 1f = mentok kiri-kanan layar. < 1f = lebar sebagian layar, di tengah. */
    val widthFraction: Float = 1f
)

val LoginArt = FullBleedArt(R.drawable.ic_login, 55f, 370f, 1375f, 760f)
val AuthArt = FullBleedArt(R.drawable.img_auth, 55f, 370f, 1375f, 760f)
val TaskArt = FullBleedArt(R.drawable.img_task_add, 290f, 315f, 1035f, 875f, widthFraction = 0.54f)

@Composable
fun FullBleedIllustration(art: FullBleedArt, modifier: Modifier = Modifier) {
    val sized = if (art.widthFraction >= 1f) {
        // Lebarkan sampai tepi layar dengan membatalkan padding horizontal form.
        modifier
            .layout { measurable, constraints ->
                val pad = FormHorizontalPadding.roundToPx()
                val placeable = measurable.measure(Constraints.fixedWidth(constraints.maxWidth + pad * 2))
                layout(constraints.maxWidth, placeable.height) { placeable.place(-pad, 0) }
            }
            .fillMaxWidth()
    } else {
        modifier.fillMaxWidth(art.widthFraction)
    }
    BoxWithConstraints(
        sized
            .aspectRatio(art.cropWidth / art.cropHeight)
            .clipToBounds()
    ) {
        val scale = maxWidth / art.cropWidth
        Image(
            painter = painterResource(id = art.res),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .size(scale * art.viewport, scale * art.viewport)
                .offset(-(scale * art.cropLeft), -(scale * art.cropTop))
        )
    }
}