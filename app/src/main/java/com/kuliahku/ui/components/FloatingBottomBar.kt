package com.kuliahku.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun rememberHomeIcon(): ImageVector = remember {
    ImageVector.Builder(
        name = "Home",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 3f)
        lineTo(4f, 10f)
        verticalLineTo(19f)
        arcTo(1f, 1f, 0f, false, false, 5f, 20f)
        horizontalLineTo(9f)
        verticalLineTo(14f)
        horizontalLineTo(15f)
        verticalLineTo(20f)
        horizontalLineTo(19f)
        arcTo(1f, 1f, 0f, false, false, 20f, 19f)
        verticalLineTo(10f)
        lineTo(12f, 3f)
        close()
    }.build()
}

@Composable
fun rememberScanIcon(): ImageVector = remember {
    ImageVector.Builder(
        name = "Scan",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Corners
        moveTo(3f, 8f)
        verticalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 5f, 3f)
        horizontalLineTo(8f)

        moveTo(16f, 3f)
        horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 21f, 5f)
        verticalLineTo(8f)

        moveTo(21f, 16f)
        verticalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 19f, 21f)
        horizontalLineTo(16f)

        moveTo(8f, 21f)
        horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 3f, 19f)
        verticalLineTo(16f)
    }.path(fill = SolidColor(Color.Black)) {
        // 4 dots
        moveTo(7f, 7f)
        horizontalLineTo(10f)
        verticalLineTo(10f)
        horizontalLineTo(7f)
        close()
        
        moveTo(14f, 7f)
        horizontalLineTo(17f)
        verticalLineTo(10f)
        horizontalLineTo(14f)
        close()
        
        moveTo(7f, 14f)
        horizontalLineTo(10f)
        verticalLineTo(17f)
        horizontalLineTo(7f)
        close()
        
        moveTo(14f, 14f)
        horizontalLineTo(17f)
        verticalLineTo(17f)
        horizontalLineTo(14f)
        close()
    }.build()
}

@Composable
fun rememberReferenceIcon(): ImageVector = remember {
    ImageVector.Builder(
        name = "Reference",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
        moveTo(18f, 2f)
        horizontalLineTo(6f)
        arcTo(2f, 2f, 0f, false, false, 4f, 4f)
        verticalLineTo(20f)
        arcTo(2f, 2f, 0f, false, false, 6f, 22f)
        horizontalLineTo(18f)
        arcTo(2f, 2f, 0f, false, false, 20f, 20f)
        verticalLineTo(4f)
        arcTo(2f, 2f, 0f, false, false, 18f, 2f)
        close()
        
        // Bookmark hole
        moveTo(14f, 2f)
        horizontalLineTo(8f)
        verticalLineTo(10f)
        lineTo(11f, 8.5f)
        lineTo(14f, 10f)
        verticalLineTo(2f)
        close()
    }.build()
}

@Composable
fun rememberProfileIcon(): ImageVector = remember {
    ImageVector.Builder(
        name = "Profile",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round
    ) {
        // Outer Circle
        moveTo(12f, 21f)
        arcTo(9f, 9f, 0f, true, true, 12f, 3f)
        arcTo(9f, 9f, 0f, true, true, 12f, 21f)
        close()
        
        // Head
        moveTo(12f, 11f)
        arcTo(3f, 3f, 0f, true, false, 12f, 5f)
        arcTo(3f, 3f, 0f, false, false, 12f, 11f)
        close()
        
        // Body
        moveTo(7f, 18f)
        arcTo(5f, 5f, 0f, false, true, 17f, 18f)
    }.build()
}

data class BottomNavItem(val route: String, val label: String, val icon: @Composable () -> ImageVector)

val bottomNavItems = listOf(
    BottomNavItem("home", "Beranda", { rememberHomeIcon() }),
    BottomNavItem("scan", "Scanning", { rememberScanIcon() }),
    BottomNavItem("reference", "Referensi", { rememberReferenceIcon() }),
    BottomNavItem("profile", "Profil", { rememberProfileIcon() })
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
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentRoute == item.route
                val icon = item.icon()
                val tint = if (selected) p.active else p.inactive
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) tint.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onNavigate(item.route) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = tint
                    )
                }
            }
        }
    }
}
