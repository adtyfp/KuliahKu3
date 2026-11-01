package com.kuliahku.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kuliahku.ui.components.AnimatedEntry
import com.kuliahku.ui.navigation.Routes

// ---------------------------------------------------------------------------
// Palet Profil (Light = sesuai Profile.png, Dark = nilai yang sama dengan Beranda).
// Mode gelap dideteksi dari MaterialTheme.colorScheme.background.
// ---------------------------------------------------------------------------
private class ProfilePalette(
    val background: Color,
    val header: Color,
    val decor: Color,
    val decorMuted: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val divider: Color,
    val logoutBg: Color,
    val logoutText: Color,
    val shadowAlpha: Float
)

private val ProfileLight = ProfilePalette(
    background = Color(0xFFF8F9FF),
    header = Color(0xFF0080FF),
    decor = Color(0xFF0B2A5E),
    decorMuted = Color(0xFF8FA0BA),
    surface = Color.White,
    border = Color(0xFFD9D9DF),
    textPrimary = Color.Black,
    divider = Color(0xFFA0A0A0),
    logoutBg = Color(0xFFECEDF1),
    logoutText = Color(0xFF666A73),
    shadowAlpha = 0.10f
)

private val ProfileDark = ProfilePalette(
    background = Color(0xFF0E1220),
    header = Color(0xFF133C8A),
    decor = Color(0xFF0A2459),
    decorMuted = Color(0xFF4A6AA8),
    surface = Color(0xFF171C2C),
    border = Color(0xFF2A3350),
    textPrimary = Color(0xFFF1F4FF),
    divider = Color(0xFF2E3652),
    logoutBg = Color(0xFF1B2134),
    logoutText = Color(0xFFA3ABC4),
    shadowAlpha = 0.45f
)

@Composable
private fun profilePalette(): ProfilePalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) ProfileDark else ProfileLight

@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val p = profilePalette()
    val shadowColor = Color.Black.copy(alpha = p.shadowAlpha)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProfileEvent.NavigateToLogin -> navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
                is ProfileEvent.ShowMessage -> { }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ---------------- Header biru ----------------
        AnimatedEntry(delay = 100) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(p.header, RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            ) {
                // Dekorasi
                StarsDecor(Modifier.align(Alignment.TopStart).padding(start = 92.dp, top = 22.dp), p.decor)
                PlanetDecor(Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 103.dp), p.decor)
                Icon(
                    Icons.Outlined.Lightbulb, null, tint = p.decor,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 73.dp, end = 85.dp).size(56.dp)
                )
                Icon(
                    Icons.Outlined.BarChart, null, tint = p.decorMuted,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 79.dp, end = 22.dp).size(46.dp)
                )
                Icon(
                    Icons.Outlined.ViewInAr, null, tint = p.decor,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 173.dp, end = 50.dp).size(48.dp)
                )

                // Foto + identitas
                Column(Modifier.fillMaxSize().padding(start = 46.dp, top = 66.dp)) {
                    Box {
                        AsyncImage(
                            model = state.user?.photoUrl,
                            contentDescription = "Foto profil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(125.dp).clip(CircleShape).background(Color.White)
                        )
                        IconButton(
                            onClick = { navController.navigate(Routes.EDIT_PROFILE) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, p.border, CircleShape)
                        ) {
                            Icon(Icons.Outlined.EditNote, null, tint = p.header, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(
                        state.user?.name ?: "Aditya Fajar Pratama",
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        state.user?.prodi ?: "Teknik informatika",
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Text(
                        state.user?.nim ?: "26552011092",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(Modifier.height(19.dp))

        // ---------------- Kartu menu ----------------
        AnimatedEntry(delay = 200) {
            val cardShape = RoundedCornerShape(24.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(341.dp)
                    .shadow(6.dp, cardShape, ambientColor = shadowColor, spotColor = shadowColor),
                shape = cardShape,
                color = p.surface,
                border = BorderStroke(1.dp, p.border)
            ) {
                Column(Modifier.padding(top = 12.dp)) {
                    ProfileMenuItem(
                        icon = { MenuIcon(Icons.Outlined.Payments, p.textPrimary) },
                        label = "Keuangan",
                        palette = p
                    ) { navController.navigate(Routes.FINANCE) }
                    ProfileMenuItem(
                        icon = { NotificationIcon(p.textPrimary, p.surface) },
                        label = "Notifikasi",
                        palette = p
                    ) { }
                    ProfileMenuItem(
                        icon = { MenuIcon(Icons.Outlined.EditNote, p.textPrimary) },
                        label = "Tampilan",
                        palette = p
                    ) { }
                    ProfileMenuItem(
                        icon = { MenuIcon(Icons.Outlined.Hearing, p.textPrimary) },
                        label = "Bantuan & Dukungan",
                        palette = p
                    ) { }
                    ProfileMenuItem(
                        icon = { MenuIcon(Icons.Outlined.Settings, p.textPrimary) },
                        label = "Pengaturan",
                        palette = p
                    ) { }
                }
            }
        }

        Spacer(Modifier.height(19.dp))

        // ---------------- Logout ----------------
        AnimatedEntry(delay = 300) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 23.dp)
                    .height(50.dp)
                    .clip(CircleShape)
                    .clickable { viewModel.showLogoutDialog(true) },
                color = p.logoutBg,
                shape = CircleShape
            ) {
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = p.logoutText,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Logout",
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = p.logoutText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(120.dp)) // ruang untuk bottom bar
    }

    if (state.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showLogoutDialog(false) },
            title = { Text("Logout") },
            text = { Text("Yakin ingin keluar dari akun ini?") },
            confirmButton = {
                TextButton(onClick = { viewModel.showLogoutDialog(false); viewModel.logout() }) { Text("Logout") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showLogoutDialog(false) }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: @Composable () -> Unit,
    label: String,
    palette: ProfilePalette,
    onClick: () -> Unit
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(55.dp)
                .clickable(onClick = onClick)
                .padding(start = 37.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(Modifier.width(20.dp))
            Text(label, fontSize = 16.sp, lineHeight = 22.sp, color = palette.textPrimary)
        }
        Box(
            Modifier
                .padding(start = 31.dp, end = 33.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider)
        )
    }
}

@Composable
private fun MenuIcon(icon: ImageVector, tint: Color) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
}

/** Amplop dengan titik notifikasi (menu "Notifikasi"). */
@Composable
private fun NotificationIcon(tint: Color, cutoutColor: Color) {
    Box(Modifier.size(28.dp)) {
        Icon(Icons.Outlined.MailOutline, contentDescription = null, tint = tint, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 1.dp, y = (-1).dp)
                .size(11.dp)
                .border(2.dp, cutoutColor, CircleShape)
                .padding(2.dp)
                .background(tint, CircleShape)
        )
    }
}

/** Dua bintang 4 sudut di kiri atas header. */
@Composable
private fun StarsDecor(modifier: Modifier, color: Color) {
    Canvas(modifier.size(50.dp)) {
        fun star(cx: Float, cy: Float, r: Float) {
            val ri = r * 0.28f
            val path = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + ri, cy - ri)
                lineTo(cx + r, cy)
                lineTo(cx + ri, cy + ri)
                lineTo(cx, cy + r)
                lineTo(cx - ri, cy + ri)
                lineTo(cx - r, cy)
                lineTo(cx - ri, cy - ri)
                close()
            }
            drawPath(path, color)
        }
        star(19.dp.toPx(), 24.dp.toPx(), 11.dp.toPx())
        star(41.dp.toPx(), 14.dp.toPx(), 8.dp.toPx())
    }
}

/** Planet bergaris dengan cincin miring di sisi kiri header. */
@Composable
private fun PlanetDecor(modifier: Modifier, color: Color) {
    Canvas(modifier.size(32.dp)) {
        val stroke = 1.6.dp.toPx()
        drawCircle(color, radius = 8.dp.toPx(), center = center, style = Stroke(stroke))
        rotate(degrees = -22f, pivot = center) {
            drawOval(
                color = color,
                topLeft = Offset(center.x - 15.dp.toPx(), center.y - 4.5.dp.toPx()),
                size = Size(30.dp.toPx(), 9.dp.toPx()),
                style = Stroke(stroke)
            )
        }
    }
}