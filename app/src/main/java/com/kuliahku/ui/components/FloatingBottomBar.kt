package com.kuliahku.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuliahku.ui.theme.cardShadow

private fun roundRectPath(x: Float, y: Float, w: Float, h: Float, r: Float): String =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r $r" +
            "v${h - 2 * r}a$r $r 0 0 1 -$r $r" +
            "h-${w - 2 * r}a$r $r 0 0 1 -$r -$r" +
            "v-${h - 2 * r}a$r $r 0 0 1 $r -${r}z"

private fun ImageVector.Builder.solid(d: String): ImageVector.Builder =
    addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))

private fun ImageVector.Builder.outline(d: String, width: Float): ImageVector.Builder =
    addPath(
        pathData = addPathNodes(d),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    )

private fun navIcon(name: String, build: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(build).build()


@Composable
fun rememberHomeIcon(): ImageVector = remember {
    navIcon("Home") {
        addPath(
            pathData = addPathNodes("M12 3.3L21.3 11.4H19V20.6H14.9V16H9.1V20.6H5V11.4H2.7Z"),
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineJoin = StrokeJoin.Round
        )
    }
}

@Composable
fun rememberScanIcon(): ImageVector = remember {
    navIcon("Scan") {
        outline(
            "M3.2 8.4V6.2a3 3 0 0 1 3-3h2.2 " +
                    "M15.6 3.2h2.2a3 3 0 0 1 3 3v2.2 " +
                    "M20.8 15.6v2.2a3 3 0 0 1-3 3h-2.2 " +
                    "M8.4 20.8H6.2a3 3 0 0 1-3-3v-2.2",
            width = 2.4f
        )
        solid(
            roundRectPath(6.8f, 6.8f, 4.0f, 4.0f, 1.0f) +
                    roundRectPath(13.2f, 6.8f, 4.0f, 4.0f, 1.0f) +
                    roundRectPath(6.8f, 13.2f, 4.0f, 4.0f, 1.0f) +
                    roundRectPath(13.2f, 13.2f, 2.0f, 2.0f, 0.5f) +
                    roundRectPath(15.6f, 15.6f, 1.8f, 1.8f, 0.5f) +
                    roundRectPath(15.6f, 13.2f, 1.6f, 1.6f, 0.4f)
        )
    }
}

@Composable
fun rememberReferenceIcon(): ImageVector = remember {
    navIcon("Reference") {
        solid(
            "M6.4 2.2H8.2V10.8L10.4 9L12.6 10.8V2.2H17.6" +
                    "a2.2 2.2 0 0 1 2.2 2.2V19.6a2.2 2.2 0 0 1-2.2 2.2H6.4" +
                    "a2.2 2.2 0 0 1-2.2-2.2V4.4a2.2 2.2 0 0 1 2.2-2.2Z"
        )
    }
}

@Composable
fun rememberProfileIcon(): ImageVector = remember {
    navIcon("Profile") {
        outline("M21.4 12a9.4 9.4 0 1 1-18.8 0a9.4 9.4 0 1 1 18.8 0z", width = 2.2f)
        solid("M8.8 9.7a3.2 3.2 0 1 0 6.4 0a3.2 3.2 0 1 0-6.4 0z")
        solid("M5.35 18.65C6.5 15.8 9 14.1 12 14.1C15 14.1 17.5 15.8 18.65 18.65A9.4 9.4 0 0 1 5.35 18.65Z")
    }
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
    
    // State untuk mendeteksi indeks secara instan
    val selectedIndex = remember(currentRoute) {
        val index = bottomNavItems.indexOfFirst { it.route == currentRoute }
        if (index == -1) 0 else index
    }

    // Animasi indeks (fractional) yang sangat halus menggunakan GPU-bound property
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessLow // Memberikan kesan 'premium' dan fluid
        ),
        label = "pillIndex"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp) // Bar dibuat lebih lebar
            .cardShadow(shapeRadius = 30.dp, blurRadius = 35.dp, offsetY = 12.dp),
        shape = RoundedCornerShape(30.dp),
        color = p.surface,
        border = p.border?.let { BorderStroke(1.dp, it) }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp) 
                .height(74.dp) // Tinggi ditingkatkan agar lebih lega
        ) {
            val itemWidth = maxWidth / bottomNavItems.size
            
            // Background Pill (Sliding dengan GPU Acceleration)
            Box(
                modifier = Modifier
                    .width(itemWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = animatedIndex * size.width
                    }
                    .padding(0.dp) // Area biru meluas maksimal ke samping
                    .clip(RoundedCornerShape(24.dp))
                    .background(p.active.copy(alpha = 0.12f))
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    
                    val tint by animateColorAsState(
                        targetValue = if (isSelected) p.active else p.inactive,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "navTint"
                    )

                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "navScale"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { 
                                if (!isSelected) onNavigate(item.route) 
                            }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        verticalArrangement = Arrangement.Center
                    ) {
                        val icon = item.icon()
                        Icon(
                            icon,
                            contentDescription = item.label,
                            tint = tint,
                            modifier = Modifier.size(26.dp) // Ukuran ikon diperbesar sedikit
                        )
                        Spacer(modifier = Modifier.height(4.dp)) // Jarak antar ikon & teks
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = tint,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
