package com.kuliahku.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kuliahku.ui.theme.cardShadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kuliahku.data.model.AppNotification
import com.kuliahku.ui.components.AnimatedEntry
import com.kuliahku.ui.navigation.Routes
import com.kuliahku.util.DateFormatters

private fun handleDeepLink(navController: NavHostController, link: String) {
    when {
        link.startsWith("kuliahku://task/") -> {
            val id = link.removePrefix("kuliahku://task/")
            navController.navigate(Routes.submitTask(id))
        }
        link.startsWith("kuliahku://event/") -> {
            val id = link.removePrefix("kuliahku://event/")
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            navController.navigate("add_event?editId=$id&date=$today")
        }
        link == "kuliahku://finance" -> {
            navController.navigate(Routes.FINANCE)
        }
        link == "kuliahku://calendar" -> {
            navController.navigate(Routes.CALENDAR)
        }
        link == "kuliahku://tasks" -> {
            navController.navigate(Routes.TASKS)
        }
        else -> {
            // Default fallback if route is just a string in Routes
            val route = link.removePrefix("kuliahku://")
            try { navController.navigate(route) } catch (_: Exception) {}
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PALET — sama dengan layar Home, Keuangan, Tugas, Kalender.
// Light : latar #FBFCFF, kartu putih, border #E0E0E0, aksen #0F79DC.
// Dark  : latar #0E1220, kartu #171C2C, border #2A3350, teks #F1F4FF, aksen #3B6FE8.
// ─────────────────────────────────────────────────────────────────────────────
private class NotifPalette(
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentText: Color,
    val accentSoft: Color
)

private val LightNotifPalette = NotifPalette(
    background = Color(0xFFFBFCFF),
    surface = Color.White,
    border = Color(0xFFE0E0E0),
    textPrimary = Color.Black,
    textSecondary = Color(0xFF767678),
    accent = Color(0xFF0F79DC),
    accentText = Color(0xFF0F79DC),
    accentSoft = Color(0xFFE3EEFB)
)

private val DarkNotifPalette = NotifPalette(
    background = Color(0xFF0E1220),
    surface = Color(0xFF171C2C),
    border = Color(0xFF2A3350),
    textPrimary = Color(0xFFF1F4FF),
    textSecondary = Color(0xFFA3ABC4),
    accent = Color(0xFF3B6FE8),
    accentText = Color(0xFF7BA0FF),
    accentSoft = Color(0xFF1C2B55)
)

@Composable
private fun notifPalette(): NotifPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DarkNotifPalette else LightNotifPalette

/** Ikon notifikasi mengikuti tujuan deep link (ikon yang sama dengan menu di Beranda). */
private fun iconFor(link: String?): ImageVector = when {
    link == null -> Icons.Outlined.NotificationsNone
    link.startsWith("kuliahku://task") -> Icons.Outlined.Assignment
    link.startsWith("kuliahku://event") || link == "kuliahku://calendar" -> Icons.Outlined.CalendarToday
    link == "kuliahku://finance" -> Icons.Outlined.Payments
    else -> Icons.Outlined.NotificationsNone
}

@Composable
fun NotificationListScreen(
    navController: NavHostController,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val p = notifPalette()

    Scaffold(
        containerColor = p.background,
        contentColor = p.textPrimary,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Spacer(Modifier.height(57.dp))

            // ── Header (ikon tengah y=75, judul x=60) — sama dengan layar lain ──
            Row(
                Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ArrowBackIosNew, "Kembali", Modifier.size(24.dp), tint = p.textPrimary)
                }
                Spacer(Modifier.width(2.dp))
                Text(
                    "Notifikasi",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 26.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = p.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (notifications.any { !it.read }) {
                    Text(
                        "Tandai Dibaca",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = p.accentText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.markAllAsRead() }
                            .padding(horizontal = 6.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (notifications.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 40.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(p.accentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.NotificationsNone, null, Modifier.size(32.dp), tint = p.accentText)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Belum ada notifikasi",
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = p.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 13.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        AnimatedEntry {
                            NotificationItem(
                                notification = notif,
                                p = p,
                                onClick = {
                                    viewModel.markAsRead(notif.id)
                                    notif.deepLink?.let { link ->
                                        handleDeepLink(navController, link)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Kartu notifikasi: radius 16, border 1 dp, bayangan lembut (sama dengan kartu tugas).
// Belum dibaca = border beraksen + judul tebal + titik biru.
@Composable
private fun NotificationItem(
    notification: AppNotification,
    p: NotifPalette,
    onClick: () -> Unit
) {
    val unread = !notification.read
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow(shapeRadius = 16.dp)
            .background(p.surface, shape)
            .border(1.dp, if (unread) p.accent.copy(alpha = 0.45f) else p.border, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(p.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconFor(notification.deepLink), null, Modifier.size(22.dp), tint = p.accentText)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    notification.title,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Medium,
                    color = p.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (unread) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .padding(top = 7.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(p.accent)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                notification.body,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = p.textSecondary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                DateFormatters.formatDateTime(notification.createdAt?.toDate() ?: java.util.Date()),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = p.textSecondary.copy(alpha = 0.8f)
            )
        }
    }
}