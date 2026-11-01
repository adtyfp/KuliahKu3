package com.kuliahku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
fun StatusActionButton(isDone: Boolean, onClick: () -> Unit) {
    val text = if (isDone) "Selesai" else "Selesaikan"
    val bg = if (isDone) ButtonSelesaiBg else ButtonSelesaikanBg
    val fg = if (isDone) SuccessText else DangerText

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .border(1.dp, fg, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(text, color = fg, fontWeight = FontWeight.SemiBold)
    }
}