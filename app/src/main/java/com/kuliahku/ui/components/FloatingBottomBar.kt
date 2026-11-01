package com.kuliahku.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem("home", "Beranda", Icons.Filled.Home),
    BottomNavItem("scan", "Scanning", Icons.Filled.QrCodeScanner),
    BottomNavItem("reference", "Referensi", Icons.Filled.Book),
    BottomNavItem("profile", "Profil", Icons.Filled.AccountCircle)
)

private class BottomBarPalette(
    val surface: Color,
    val border: Color?,
    val inactive: Color,
    val active: Color
)

private val BarLight = BottomBarPalette(
    surface = Color.White,
    border = null,
    inactive = Color(0xFF4A4A4A),
    active = Color(0xFF0F79DC)
)

private val BarDark = BottomBarPalette(
    surface = Color(0xFF171C2C),
    border = Color(0xFF2A3350),
    inactive = Color(0xFFA3ABC4),
    active = Color(0xFF5AA2FF)
)

@Composable
fun FloatingBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) BarDark else BarLight

    // Ukuran sama seperti versi sebelumnya: margin 20dp/16dp, ikon 24dp, padding item 12dp/4dp.
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        shape = RoundedCornerShape(50),
        shadowElevation = 8.dp,
        color = p.surface,
        border = p.border?.let { BorderStroke(1.dp, it) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentRoute == item.route
                val tint = if (selected) p.active else p.inactive
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigate(item.route) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = tint
                    )
                }
            }
        }
    }
}