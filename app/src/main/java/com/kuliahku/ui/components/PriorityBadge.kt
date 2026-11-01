package com.kuliahku.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class BadgeTone(val bg: Color, val border: Color, val text: Color)

private val LightTones = mapOf(
    "high" to BadgeTone(Color(0xFFFEF2F2), Color(0xFFFECACA), Color(0xFFEF4444)),
    "medium" to BadgeTone(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFD97706)),
    "low" to BadgeTone(Color(0xFFF0FDF4), Color(0xFFBBF7D0), Color(0xFF16A34A))
)

private val DarkTones = mapOf(
    "high" to BadgeTone(Color(0xFF45202A), Color(0xFFFF6B7A).copy(alpha = 0.5f), Color(0xFFFF6B7A)),
    "medium" to BadgeTone(Color(0xFF3A2A12), Color(0xFFFFB84D).copy(alpha = 0.5f), Color(0xFFFFB84D)),
    "low" to BadgeTone(Color(0xFF14362A), Color(0xFF3DD083).copy(alpha = 0.5f), Color(0xFF3DD083))
)

@Composable
fun PriorityBadge(priority: String) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tone = (if (isDark) DarkTones else LightTones)[priority.lowercase()] ?: LightTones["low"]!!

    Surface(
        shape = RoundedCornerShape(50),
        color = tone.bg,
        border = BorderStroke(1.dp, tone.border)
    ) {
        Text(
            text = priority.replaceFirstChar { it.uppercase() },
            color = tone.text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            textAlign = TextAlign.Center
        )
    }
}
