package com.kuliahku.ui.reference

import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kuliahku.ui.theme.cardShadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kuliahku.data.model.JournalReference
import com.kuliahku.ui.components.*
import com.kuliahku.ui.theme.Primary
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

// ---------------------------------------------------------------------------
// Reference Constants
// ---------------------------------------------------------------------------
/** Format angka dengan pemisah ribuan titik, mis. 34.573 (sesuai desain). */
private fun formatNumber(n: Int): String =
    NumberFormat.getIntegerInstance(Locale("id", "ID")).format(n)

private fun relativeTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        days < 1 -> "Hari ini"
        days < 7 -> "$days Hari yang lalu"
        days < 30 -> "${days / 7} Minggu yang lalu"
        days < 365 -> "${days / 30} Bulan yang lalu"
        else -> "${days / 365} Tahun yang lalu"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceScreen(
    navController: NavHostController,
    viewModel: ReferenceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                is ReferenceEvent.ShowMessage -> snackbar.showSnackbar(ev.message)
                is ReferenceEvent.OpenUrl -> {
                    val intent = CustomTabsIntent.Builder().build()
                    intent.launchUrl(context, ev.url.toUri())
                }
            }
        }
    }

    // Infinite scroll
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.references.size - 3 && state.hasMore && !state.isLoadingMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(57.dp))

            // ---------------- Header: Standardized ----------------
            AnimatedEntry(delay = 0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Referensi",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 26.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Cari & simpan referensi jurnal",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // ---------------- Search bar + filter ----------------
            AnimatedEntry(delay = 100) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReferenceSearchBar(
                        query = state.searchQuery,
                        onQueryChange = viewModel::onSearchChange,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    val filterShape = CircleShape
                    Box(
                        modifier = Modifier
                            .size(57.dp)
                            .clip(filterShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), filterShape)
                            .clickable { viewModel.showFilterSheet(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.FilterAlt,
                            contentDescription = "Filter",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---------------- Chip kategori ----------------
            AnimatedEntry(delay = 200) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReferenceFilter.entries.forEach { f ->
                        ReferenceChip(
                            label = f.label,
                            selected = state.selectedFilter == f,
                            onClick = { viewModel.setFilter(f) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(17.dp))
            AnimatedEntry(delay = 300) {
                Column {
                    Box(
                        Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    )
                    Spacer(Modifier.height(19.dp))

                    when {
                        state.isLoading -> LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp)
                        ) {
                            items(4) { ShimmerBox(Modifier.fillMaxWidth().height(169.dp)) }
                        }
                        state.error != null && state.references.isEmpty() -> ErrorView(
                            message = state.error!!,
                            onRetry = { viewModel.onSearchChange(state.searchQuery) }
                        )
                        state.references.isEmpty() -> EmptyView(
                            title = "Tidak ada jurnal ditemukan",
                            subtitle = "Coba kata kunci atau filter lain."
                        )
                        else -> LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp)
                        ) {
                            items(state.references, key = { it.id }) { ref ->
                                ReferenceCard(
                                    ref = ref,
                                    isBookmarked = state.bookmarkedIds.contains(ref.id),
                                    onClick = { viewModel.openReference(ref) },
                                    onBookmark = { viewModel.toggleBookmark(ref) }
                                )
                            }
                            if (state.isLoadingMore) {
                                item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                            }
                        }
                    }
                }
            }
        }

        if (state.showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.showFilterSheet(false) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                FilterSheetContent(
                    state = state,
                    onYearRange = { from, to -> viewModel.setYearRange(from, to) },
                    onOpenAccess = { viewModel.setOnlyOpenAccess(it) },
                    onSort = { viewModel.setSort(it) }
                )
            }
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp))
    }
}

@Composable
private fun ReferenceSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(57.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
            .padding(start = 16.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        "Cari Jurnal...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun ReferenceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .widthIn(min = 59.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(MaterialTheme.colorScheme.primary)
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ReferenceCard(
    ref: JournalReference,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onBookmark: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Surface(
        shape = shape,
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(169.dp)
            .cardShadow(shapeRadius = 24.dp)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.fillMaxSize()) {
            // Thumbnail / Image area
            Box(
                Modifier
                    .width(130.dp)
                    .fillMaxHeight()
                    .background(colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!ref.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(ref.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Journal Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    // GENERATED COVER
                    val hash = ref.title.hashCode()
                    val colors = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE), Color(0xFF43E97B), Color(0xFF38F9D7), Color(0xFFFA709A), Color(0xFFFEE140))
                    val color1 = colors[Math.abs(hash) % colors.size]
                    val color2 = colors[Math.abs(hash + 1) % colors.size]
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(color1, color2))),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = ref.field?.uppercase() ?: "JOURNAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                        
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .width(4.dp)
                                .align(Alignment.CenterStart)
                                .background(Color.Black.copy(alpha = 0.1f))
                        )
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.padding(start = 15.dp, top = 17.dp, end = 15.dp)) {
                    Text(
                        ref.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(22.dp).clip(CircleShape).background(colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(ref.firstAuthorInitial, color = colorScheme.onPrimary, fontSize = 10.sp, lineHeight = 12.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            ref.firstAuthor + if (ref.authorCount > 1) " et al." else "",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ThumbUp, null, modifier = Modifier.size(18.dp), tint = colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(5.dp))
                        Text(formatNumber(ref.citedByCount), fontSize = 12.sp, lineHeight = 16.sp, color = colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(16.dp))
                        Icon(rememberViewStatIcon(), null, modifier = Modifier.size(20.dp), tint = colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (ref.isOpenAccess) "OA" else "Tertutup",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        ref.publishedAtMillis?.let { relativeTime(it) } ?: "-",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 18.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBookmark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/** Ikon statistik (frame grafik + mata) untuk baris metrik kartu. */
@Composable
private fun rememberViewStatIcon(): ImageVector = remember {
    ImageVector.Builder(
        name = "ViewStat",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // frame
            moveTo(5f, 4f); horizontalLineTo(19f)
            quadTo(21.5f, 4f, 21.5f, 6.5f); verticalLineTo(12f)
            moveTo(2.5f, 6.5f); quadTo(2.5f, 4f, 5f, 4f)
            moveTo(2.5f, 6.5f); verticalLineTo(14f)
            quadTo(2.5f, 16.5f, 5f, 16.5f); horizontalLineTo(11f)
            // garis grafik
            moveTo(5.5f, 13f); lineTo(9.5f, 9f); lineTo(12.5f, 11.5f); lineTo(17f, 7f)
            // mata
            moveTo(13.5f, 18f); quadTo(17.5f, 12.5f, 21.5f, 18f)
            quadTo(17.5f, 23.5f, 13.5f, 18f); close()
            moveTo(17.5f, 18f); lineTo(17.51f, 18f)
        }
    }.build()
}

@Composable
private fun FilterSheetContent(
    state: ReferenceUiState,
    onYearRange: (Int?, Int?) -> Unit,
    onOpenAccess: (Boolean) -> Unit,
    onSort: (SortOrder) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(Modifier.padding(24.dp)) {
        AnimatedEntry(delay = 0, duration = 250) {
            Text("Urutkan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
        }
        Spacer(Modifier.height(16.dp))
        
        AnimatedEntry(delay = 50, duration = 250) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortOrder.entries.take(2).forEach { s ->
                        FilterChipModern(
                            label = s.label,
                            selected = state.sortOrder == s,
                            modifier = Modifier.weight(1f),
                            onClick = { onSort(s) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortOrder.entries.drop(2).forEach { s ->
                        FilterChipModern(
                            label = s.label,
                            selected = state.sortOrder == s,
                            modifier = Modifier.weight(1f),
                            onClick = { onSort(s) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        AnimatedEntry(delay = 100, duration = 250) {
            Text("Tahun Terbit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
        }
        Spacer(Modifier.height(16.dp))
        
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val ranges = listOf(
            null to null,
            (currentYear - 3) to currentYear,
            (currentYear - 5) to currentYear,
            (currentYear - 10) to currentYear
        )
        
        AnimatedEntry(delay = 150, duration = 250) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ranges.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { (from, to) ->
                            val label = when {
                                from == null -> "Semua"
                                else -> "$from-$to"
                            }
                            FilterChipModern(
                                label = label,
                                selected = state.yearFrom == from && state.yearTo == to,
                                modifier = Modifier.weight(1f),
                                onClick = { onYearRange(from, to) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        AnimatedEntry(delay = 200, duration = 250) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Hanya Akses Terbuka", fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                        Text("Tampilkan jurnal yang bisa dibaca penuh", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.onlyOpenAccess, 
                        onCheckedChange = onOpenAccess,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colorScheme.primary
                        )
                    )
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun FilterChipModern(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colorScheme.primary else colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) null else BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier.height(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) colorScheme.onPrimary else colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}