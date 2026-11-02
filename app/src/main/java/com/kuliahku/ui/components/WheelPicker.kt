package com.kuliahku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun WheelDateTimePicker(
    initialMillis: Long,
    onDateTimeChanged: (Long) -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    val calendar = remember(initialMillis) {
        Calendar.getInstance().apply { timeInMillis = initialMillis }
    }

    // Generate dates around the current date
    val dates = remember {
        (-365..365).map { offset ->
            Calendar.getInstance().apply {
                timeInMillis = initialMillis
                add(Calendar.DAY_OF_YEAR, offset)
            }
        }
    }

    val hours = (0..23).toList()
    val minutes = (0..59).toList()

    val dateIndex = 365
    val hourIndex = calendar.get(Calendar.HOUR_OF_DAY)
    val minuteIndex = calendar.get(Calendar.MINUTE)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelPicker(
            items = dates.map { cal ->
                val month = SimpleDateFormat("MMM", Locale("id")).format(cal.time)
                val day = cal.get(Calendar.DAY_OF_MONTH)
                "$month $day"
            },
            selectedIndex = dateIndex,
            onItemSelected = { index ->
                val newCal = dates[index]
                calendar.set(Calendar.YEAR, newCal.get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, newCal.get(Calendar.MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, newCal.get(Calendar.DAY_OF_MONTH))
                onDateTimeChanged(calendar.timeInMillis)
            },
            modifier = Modifier.weight(1.5f),
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        WheelPicker(
            items = hours.map { it.toString().padStart(2, '0') },
            selectedIndex = hourIndex,
            onItemSelected = { index ->
                calendar.set(Calendar.HOUR_OF_DAY, hours[index])
                onDateTimeChanged(calendar.timeInMillis)
            },
            modifier = Modifier.weight(1f),
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        WheelPicker(
            items = minutes.map { it.toString().padStart(2, '0') },
            selectedIndex = minuteIndex,
            onItemSelected = { index ->
                calendar.set(Calendar.MINUTE, minutes[index])
                onDateTimeChanged(calendar.timeInMillis)
            },
            modifier = Modifier.weight(1f),
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )
    }
}

@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    textPrimary: Color,
    textSecondary: Color
) {
    val itemHeight = 40.dp
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val snappedIndex = listState.firstVisibleItemIndex
            if (snappedIndex in items.indices && snappedIndex != selectedIndex) {
                onItemSelected(snappedIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * 3)
            .padding(horizontal = 4.dp)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.2f to Color.Black,
                        0.8f to Color.Black,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Selection indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(textPrimary.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight)
        ) {
            itemsIndexed(items) { index, item ->
                val isSelected = listState.firstVisibleItemIndex == index
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = TextStyle(
                            fontSize = if (isSelected) 20.sp else 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) textPrimary else textSecondary.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    }
}
