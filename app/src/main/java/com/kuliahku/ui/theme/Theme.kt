package com.kuliahku.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private fun getLightColorScheme(primaryColor: Color) = lightColorScheme(
    primary = primaryColor,
    onPrimary = Color.White,
    primaryContainer = primaryColor.copy(alpha = 0.12f),
    onPrimaryContainer = primaryColor,
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = RedMain,
    errorContainer = RedSoft,
    onError = Color.White,
    onErrorContainer = RedMain,
    outline = OnSurfaceVariant.copy(alpha = 0.5f),
    tertiary = GreenMain,
    tertiaryContainer = GreenSoft,
    onTertiary = Color.White,
    onTertiaryContainer = GreenMain
)

private fun getDarkColorScheme(primaryColor: Color) = darkColorScheme(
    primary = primaryColor,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1C2B55), // Standardized Dark Accent Container
    onPrimaryContainer = Color(0xFFF1F4FF),
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = RedMain,
    errorContainer = RedSoftDark,
    onError = Color.Black,
    onErrorContainer = RedMain,
    outline = OnSurfaceVariantDark.copy(alpha = 0.5f),
    tertiary = GreenMain,
    tertiaryContainer = GreenSoftDark,
    onTertiary = Color.Black,
    onTertiaryContainer = GreenMain
)

@Composable
fun KuliahkuTheme(
    themeMode: String = "system",
    primaryColorHex: String = "#0080FF",
    fontSizeMode: String = "medium",
    uiStyle: String = "soft",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    
    val primaryColor = try {
        Color(android.graphics.Color.parseColor(primaryColorHex))
    } catch (e: Exception) {
        Primary
    }

    val colorScheme = if (darkTheme) getDarkColorScheme(primaryColor) else getLightColorScheme(primaryColor)

    // Dynamic Typography based on fontSizeMode
    val sizeMultiplier = when (fontSizeMode) {
        "small" -> 0.85f
        "large" -> 1.15f
        else -> 1.0f
    }
    
    val baseTypography = Typography
    val dynamicTypography = Typography(
        displayLarge = baseTypography.displayLarge.copy(fontSize = baseTypography.displayLarge.fontSize * sizeMultiplier),
        headlineLarge = baseTypography.headlineLarge.copy(fontSize = baseTypography.headlineLarge.fontSize * sizeMultiplier),
        headlineMedium = baseTypography.headlineMedium.copy(fontSize = baseTypography.headlineMedium.fontSize * sizeMultiplier),
        titleLarge = baseTypography.titleLarge.copy(fontSize = baseTypography.titleLarge.fontSize * sizeMultiplier),
        titleMedium = baseTypography.titleMedium.copy(fontSize = baseTypography.titleMedium.fontSize * sizeMultiplier),
        bodyLarge = baseTypography.bodyLarge.copy(fontSize = baseTypography.bodyLarge.fontSize * sizeMultiplier),
        bodyMedium = baseTypography.bodyMedium.copy(fontSize = baseTypography.bodyMedium.fontSize * sizeMultiplier),
        labelLarge = baseTypography.labelLarge.copy(fontSize = baseTypography.labelLarge.fontSize * sizeMultiplier),
        labelMedium = baseTypography.labelMedium.copy(fontSize = baseTypography.labelMedium.fontSize * sizeMultiplier)
    )

    // Dynamic Shapes based on uiStyle
    val cornerSize = when (uiStyle) {
        "rounded" -> 24.dp
        "crisp" -> 4.dp
        else -> 12.dp // soft
    }
    val dynamicShapes = Shapes(
        extraSmall = RoundedCornerShape(cornerSize / 3),
        small = RoundedCornerShape(cornerSize / 2),
        medium = RoundedCornerShape(cornerSize),
        large = RoundedCornerShape(cornerSize * 1.5f),
        extraLarge = RoundedCornerShape(50.dp) // Keep pill for buttons
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = dynamicTypography,
        shapes = dynamicShapes,
        content = content
    )
}
