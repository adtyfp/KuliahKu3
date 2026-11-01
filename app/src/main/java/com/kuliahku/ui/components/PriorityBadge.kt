package com.kuliahku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuliahku.ui.theme.*

@Composable
fun PriorityBadge(priority: String) {
    val (bg, text) = when (priority.lowercase()) {
        "high" -> BadgeHighBg to DangerText
        "medium" -> BadgeMediumBg to WarningText
        else -> BadgeLowBg to SuccessText
    }
    Text(
        text = priority.replaceFirstChar { it.uppercase() },
        color = text,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}