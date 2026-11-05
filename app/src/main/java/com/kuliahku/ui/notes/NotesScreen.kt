package com.kuliahku.ui.notes

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kuliahku.data.model.Note
import com.kuliahku.ui.components.*
import com.kuliahku.ui.navigation.Routes
import com.kuliahku.ui.theme.cardShadow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    navController: NavHostController,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NotesEvent.ShowMessage -> {
                    val result = snackbar.showSnackbar(
                        message = event.message,
                        actionLabel = if (event.message.contains("dihapus")) "Urungkan" else null,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete()
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 0.dp)
        ) {
            // ── Header (sesuai Catatan.png): chevron di x=33..45, judul "Catatan" di x=61,
            //    keduanya berpusat di y=75. Tombol 36 dp mulai x=22 (= 16 padding + 6). ──
            AnimatedEntry(delay = 0) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, top = 16.dp, end = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.selectedNoteIds.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearSelection() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Batal Pilih",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${state.selectedNoteIds.size} dipilih",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = { viewModel.deleteSelected() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.ArrowBackIosNew,
                                contentDescription = "Kembali",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Catatan",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 24.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))

            AnimatedEntry(delay = 100) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        state.filter.label,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.showFilterMenu(true) }
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = viewModel::toggleLayout,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            if (state.layout == NoteLayout.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = "Ubah tata letak",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.showFilterMenu(true) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "Filter",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            AnimatedEntry(delay = 200, modifier = Modifier.weight(1f)) {
                Box(Modifier.fillMaxSize()) {
                    when {
                        state.isLoading -> LazyVerticalGrid(
                            GridCells.Fixed(2),
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(6) { ShimmerBox(Modifier.fillMaxWidth().height(160.dp)) }
                        }
                        state.error != null -> ErrorView(message = state.error!!, onRetry = {})
                        state.filtered.isEmpty() -> EmptyView(
                            title = "Belum ada catatan",
                            subtitle = "Ketuk tombol pensil di bawah untuk mulai menulis."
                        )
                        else -> {
                            if (state.layout == NoteLayout.GRID) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 100.dp)
                                ) {
                                    items(state.filtered, key = { it.id }) { note ->
                                        val isSelected = state.selectedNoteIds.contains(note.id)
                                        NoteCard(
                                            note = note,
                                            isSelected = isSelected,
                                            onLongClick = { viewModel.toggleSelection(note.id) },
                                            onClick = {
                                                if (state.selectedNoteIds.isNotEmpty()) {
                                                    viewModel.toggleSelection(note.id)
                                                } else {
                                                    navController.navigate("note_edit/${note.id}")
                                                }
                                            }
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 100.dp)
                                ) {
                                    items(state.filtered, key = { it.id }) { note ->
                                        val isSelected = state.selectedNoteIds.contains(note.id)
                                        NoteRow(
                                            note = note,
                                            isSelected = isSelected,
                                            onLongClick = { viewModel.toggleSelection(note.id) },
                                            onClick = {
                                                if (state.selectedNoteIds.isNotEmpty()) {
                                                    viewModel.toggleSelection(note.id)
                                                } else {
                                                    navController.navigate("note_edit/${note.id}")
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!state.isLoading && state.error == null) {
                AnimatedEntry(delay = 300) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                        SearchBarWithFab(
                            searchQuery = state.searchQuery,
                            onSearchQueryChange = viewModel::onSearch,
                            onFabClick = { navController.navigate("note_edit/new") },
                            placeholder = "Cari catatan"
                        )
                    }
                }
            }
        }

        if (state.showFilterMenu) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.showFilterMenu(false) },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Text(
                        "Urutkan & Filter",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(20.dp))
                    
                    NoteFilter.entries.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { f ->
                                val isSelected = state.filter == f
                                Surface(
                                    onClick = { 
                                        viewModel.setFilter(f)
                                        viewModel.showFilterMenu(false)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.weight(1f).height(50.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            f.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: Note,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp) // Mengurangi padding agar margin total (content + card) menjadi 8dp
            .zIndex(if (isSelected) 1f else 0f),
        contentAlignment = Alignment.TopEnd
    ) {
        Surface(
            shape = shape,
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .cardShadow(shapeRadius = 20.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin.Center
                }
        ) {
            Column(
                modifier = Modifier
                    .clip(shape) // Memastikan ripple/hover mengikuti lengkungan 20.dp
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                    .padding(16.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            RoundedCornerShape(50)
                        )
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    note.title.ifBlank { "(Tanpa judul)" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    note.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteRow(
    note: Note,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rowScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .zIndex(if (isSelected) 1f else 0f),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            shape = shape,
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .cardShadow(shapeRadius = 20.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin.Center
                }
        ) {
            Row(
                modifier = Modifier
                    .clip(shape)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(4.dp, 36.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            RoundedCornerShape(50)
                        )
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        note.title.ifBlank { "(Tanpa judul)" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
