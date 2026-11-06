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
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
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
// PALET — selaras dengan layar Home, Keuangan, Tugas, Kalender.
// Ditambah warna kategori (tugas = amber, jadwal = biru, keuangan = hijau)
// supaya jenis notifikasi bisa dikenali sekilas.
// ─────────────────────────────────────────────────────────────────────────────
private data class NotifPalette(
    val background: Color,
    val surface: Color,
    val unreadSurface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentText: Color,
    val accentSoft: Color,
    // kategori tugas
    val taskTint: Color,
    val taskSoft: Color,
    // kategori keuangan
    val financeTint: Color,
    val financeSoft: Color,
    // kategori umum
    val neutralTint: Color,
    val neutralSoft: Color
)

private val LightNotifPalette = NotifPalette(
    background = Color(0xFFFBFCFF),
    surface = Color.White,
    unreadSurface = Color(0xFFF3F8FF),
    border = Color(0xFFE6E8EE),
    textPrimary = Color(0xFF0F1320),
    textSecondary = Color(0xFF6B7080),
    accent = Color(0xFF0F79DC),
    accentText = Color(0xFF0F79DC),
    accentSoft = Color(0xFFE3EEFB),
    taskTint = Color(0xFFB45309),
    taskSoft = Color(0xFFFEF1D6),
    financeTint = Color(0xFF15803D),
    financeSoft = Color(0xFFDDF5E5),
    neutralTint = Color(0xFF5B6275),
    neutralSoft = Color(0xFFEDEFF4)
)

private val DarkNotifPalette = NotifPalette(
    background = Color(0xFF0E1220),
    surface = Color(0xFF171C2C),
    unreadSurface = Color(0xFF1A2240),
    border = Color(0xFF2A3350),
    textPrimary = Color(0xFFF1F4FF),
    textSecondary = Color(0xFFA3ABC4),
    accent = Color(0xFF3B6FE8),
    accentText = Color(0xFF7BA0FF),
    accentSoft = Color(0xFF1C2B55),
    taskTint = Color(0xFFFBBF5A),
    taskSoft = Color(0xFF3A2D12),
    financeTint = Color(0xFF5BD68A),
    financeSoft = Color(0xFF13321F),
    neutralTint = Color(0xFFB4BBD3),
    neutralSoft = Color(0xFF232A40)
)


/** Gaya kategori notifikasi (ikon, label, warna) mengikuti tujuan deep link. */
private data class CategoryStyle(
    val icon: ImageVector,
    val label: String,
    val tint: Color,
    val soft: Color
)

private fun categoryFor(link: String?, p: NotifPalette): CategoryStyle = when {
    link == null -> CategoryStyle(Icons.Outlined.NotificationsNone, "Info", p.neutralTint, p.neutralSoft)
    link.startsWith("kuliahku://task") -> CategoryStyle(Icons.Outlined.Assignment, "Tugas", p.taskTint, p.taskSoft)
    link.startsWith("kuliahku://event") || link == "kuliahku://calendar" ->
        CategoryStyle(Icons.Outlined.CalendarToday, "Jadwal", p.accentText, p.accentSoft)
    link == "kuliahku://finance" -> CategoryStyle(Icons.Outlined.Payments, "Keuangan", p.financeTint, p.financeSoft)
    else -> CategoryStyle(Icons.Outlined.NotificationsNone, "Info", p.neutralTint, p.neutralSoft)
}

@Composable
fun NotificationListScreen(
    navController: NavHostController,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val p = remember(isDark) { if (isDark) DarkNotifPalette else LightNotifPalette }
    val unreadCount = remember(notifications) { notifications.count { !it.read } }

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

            // ── Header: tombol kembali bulat, judul + ringkasan, aksi "Tandai Dibaca" ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(p.surface, CircleShape)
                        .border(1.dp, p.border, CircleShape)
                ) {
                    Icon(Icons.Filled.ArrowBackIosNew, "Kembali", Modifier.size(16.dp), tint = p.textPrimary)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Notifikasi",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = p.textPrimary
                    )
                    Text(
                        if (unreadCount > 0) "$unreadCount belum dibaca" else "Semua sudah dibaca",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = p.textSecondary
                    )
                }
                if (unreadCount > 0) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(p.accentSoft)
                            .clickable { viewModel.markAllAsRead() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.DoneAll, null, Modifier.size(16.dp), tint = p.accentText)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Tandai Dibaca",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = p.accentText
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            if (notifications.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 40.dp).padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Lingkaran berlapis agar empty state terasa lebih hidup
                    Box(
                        Modifier.size(112.dp).clip(CircleShape).background(p.accentSoft.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.size(80.dp).clip(CircleShape).background(p.accentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.NotificationsNone, null, Modifier.size(36.dp), tint = p.accentText)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Belum ada notifikasi",
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Pengingat tugas, jadwal, dan keuangan akan muncul di sini.",
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = p.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = notifications,
                        key = { it.id },
                        contentType = { "notification" }
                    ) { notif ->
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

// Kartu notifikasi:
// - Ikon kategori berwarna dalam kotak rounded (tugas / jadwal / keuangan / info).
// - Belum dibaca = latar sedikit kebiruan, border beraksen, judul tebal, titik biru.
// - Sudah dibaca = kartu netral dengan teks lebih tenang.
// - Baris bawah: label kategori di kiri, waktu di kanan.
@Composable
private fun NotificationItem(
    notification: AppNotification,
    p: NotifPalette,
    onClick: () -> Unit
) {
    val unread = !notification.read
    val shape = remember { RoundedCornerShape(18.dp) }
    val category = remember(notification.deepLink, p) { categoryFor(notification.deepLink, p) }
    val formattedDate = remember(notification.createdAt) {
        DateFormatters.formatDateTime(notification.createdAt?.toDate() ?: java.util.Date())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow(shapeRadius = 18.dp)
            .background(if (unread) p.unreadSurface else p.surface, shape)
            .border(1.dp, if (unread) p.accent.copy(alpha = 0.40f) else p.border, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(category.soft),
            contentAlignment = Alignment.Center
        ) {
            Icon(category.icon, null, Modifier.size(22.dp), tint = category.tint)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    notification.title,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Medium,
                    color = p.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (unread) {
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .padding(top = 6.dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(p.accent)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                notification.body,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = if (unread) p.textPrimary.copy(alpha = 0.78f) else p.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Label kategori
                Text(
                    category.label,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = category.tint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(category.soft)
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                )
                Spacer(Modifier.weight(1f))
                // Waktu
                Icon(
                    Icons.Outlined.Schedule,
                    null,
                    Modifier.size(12.dp),
                    tint = p.textSecondary.copy(alpha = 0.8f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    formattedDate,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = p.textSecondary.copy(alpha = 0.8f)
                )
            }
        }
    }
}