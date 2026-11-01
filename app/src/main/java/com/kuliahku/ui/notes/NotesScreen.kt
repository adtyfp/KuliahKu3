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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kuliahku.data.model.Note
import com.kuliahku.ui.components.*
import com.kuliahku.ui.navigation.Routes

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
                is NotesEvent.ShowMessage -> snackbar.showSnackbar(event.message)
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
                .padding(horizontal = 16.dp)
        ) {
            // ── Header (sesuai Catatan.png): chevron di x=33..45, judul "Catatan" di x=61,
            //    keduanya berpusat di y=75. Tombol 36 dp mulai x=22 (= 16 padding + 6). ──
            AnimatedEntry(delay = 0) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 6.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "Catatan",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            AnimatedEntry(delay = 100) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        state.filter.label,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.showFilterMenu(true) }
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = viewModel::toggleLayout) {
                        Icon(
                            if (state.layout == NoteLayout.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = "Ubah tata letak"
                        )
                    }
                    IconButton(onClick = { viewModel.showFilterMenu(true) }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filter")
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
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 100.dp)
                                ) {
                                    items(state.filtered, key = { it.id }) { note ->
                                        NoteCard(note) { navController.navigate("note_edit/${note.id}") }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 100.dp)
                                ) {
                                    items(state.filtered, key = { it.id }) { note ->
                                        NoteRow(note) { navController.navigate("note_edit/${note.id}") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!state.isLoading && state.error == null) {
                AnimatedEntry(delay = 300) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
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
            ModalBottomSheet(onDismissRequest = { viewModel.showFilterMenu(false) }) {
                Column(Modifier.padding(20.dp)) {
                    Text("Filter", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    NoteFilter.entries.forEach { f ->
                        Row(
                            Modifier.fillMaxWidth().clickable { viewModel.setFilter(f); viewModel.showFilterMenu(false) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = state.filter == f, onClick = { viewModel.setFilter(f); viewModel.showFilterMenu(false) })
                            Text(f.label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 6.dp, // Ditingkatkan dari 2.dp agar lebih hidup
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
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
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun NoteRow(note: Note, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(4.dp, 40.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    note.title.ifBlank { "(Tanpa judul)" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}