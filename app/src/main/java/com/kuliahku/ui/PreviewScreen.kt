package com.kuliahku.ui.preview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kuliahku.ui.components.*
import com.kuliahku.ui.theme.*

@Composable
fun ComponentPreviewScreen() {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Design System Preview", style = MaterialTheme.typography.headlineMedium)
        KuliahkuLogo()
        PillTextField("", {}, "Email/NIM", leadingIcon = { Icon(Icons.Filled.Email, null) })
        PillTextField("password", {}, "Password", isPassword = true)
        PrimaryPillButton("Login", {})
        SwipeStartButton({})
        FilterChipRow(listOf("All", "Hari ini", "Akan datang", "Selesai"), 0, {})
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriorityBadge("High")
            PriorityBadge("Medium")
            PriorityBadge("Low")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusActionButton(false) {}
            StatusActionButton(true) {}
        }
        ProgressRing(0.75f)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Icons.Filled.CheckCircle, BlueSoft, "Total Tugas", "12", Modifier.weight(1f))
            StatCard(Icons.Filled.Schedule, BlueSoft, "Akan datang", "4", Modifier.weight(1f))
        }
        ShimmerBox(Modifier.fillMaxWidth().height(80.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun ComponentPreviewLight() {
    KuliahkuTheme(darkTheme = false) { ComponentPreviewScreen() }
}

@Preview(showBackground = true, uiMode = 0x20)
@Composable
fun ComponentPreviewDark() {
    KuliahkuTheme(darkTheme = true) { ComponentPreviewScreen() }
}