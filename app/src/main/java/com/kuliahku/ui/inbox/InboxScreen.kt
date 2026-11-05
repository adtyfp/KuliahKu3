package com.kuliahku.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kuliahku.data.model.AppNotification
import com.kuliahku.ui.components.AnimatedEntry
import com.kuliahku.ui.components.EmptyView
import com.kuliahku.ui.components.ErrorView
import com.kuliahku.ui.components.ShimmerBox
import com.kuliahku.ui.navigation.Routes
import com.kuliahku.ui.theme.cardShadow
import com.kuliahku.util.DateFormatters

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun InboxScreen(
    navController: NavHostController,
    viewModel: InboxViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        AnimatedEntry(delay = 100) {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                }
                Text("Notifikasi", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = viewModel::markAllRead) { Text("Tandai semua") }
            }
        }
        Spacer(Modifier.height(8.dp))

        when {
            state.isLoading -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(5) { ShimmerBox(Modifier.fillMaxWidth().height(80.dp)) }
            }
            state.error != null -> ErrorView(message = state.error!!, onRetry = {})
            state.items.isEmpty() -> AnimatedEntry(delay = 200) {
                EmptyView(
                    title = "Belum ada notifikasi",
                    subtitle = "Pengingat tugas dan acara akan muncul di sini."
                )
            }
            else -> AnimatedEntry(delay = 300) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        NotificationItem(
                            item = item,
                            onClick = {
                                viewModel.markRead(item.id)
                                item.deepLink?.let { link ->
                                    when {
                                        link.startsWith("kuliahku://task/") -> navController.navigate(
                                            Routes.submitTask(link.removePrefix("kuliahku://task/"))
                                        )
                                        link.startsWith("kuliahku://event/") -> {
                                            val today = com.kuliahku.data.repository.EventRepository
                                                .keyOf(java.util.Calendar.getInstance())
                                            navController.navigate("add_event?editId=${link.removePrefix("kuliahku://event/")}&date=$today")
                                        }
                                    }
                                }
                            },
                            onDelete = { viewModel.delete(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(item: AppNotification, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (item.read) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow(shapeRadius = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.read) Color.Transparent else MaterialTheme.colorScheme.primary
                    )
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(item.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                item.createdAt?.toDate()?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(DateFormatters.formatDateTime(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, "Hapus") }
        }
    }
}