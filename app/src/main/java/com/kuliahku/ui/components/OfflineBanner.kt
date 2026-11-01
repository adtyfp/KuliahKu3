package com.kuliahku.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kuliahku.ui.theme.WarningText

@Composable
fun OfflineBanner(isOffline: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = isOffline, modifier = modifier) {
        Surface(
            color = WarningText.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CloudOff, null, tint = WarningText, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Anda sedang offline. Perubahan akan disinkronkan saat online.",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarningText
                )
            }
        }
    }
}