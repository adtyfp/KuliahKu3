package com.kuliahku.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuliahku.R
import com.kuliahku.ui.components.AnimatedEntry
import com.kuliahku.ui.components.KuliahkuLogo
import com.kuliahku.ui.theme.cardShadow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun WelcomeScreen(
    onStart: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedEntry(delay = 100) {
                KuliahkuLogo(modifier = Modifier.height(32.dp))
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Ilustrasi full width (mentok kiri-kanan), tanpa padding dari parent
            AnimatedEntry(delay = 200) {
                Image(
                    painter = painterResource(id = R.drawable.img_welcome),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                AnimatedEntry(delay = 300) {
                    Text(
                        text = "Siap Untuk Memulai Hari\nHari Mu?",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedEntry(delay = 400) {
                    Text(
                        text = "Setiap langkah kecil hari ini, adalah bagian dari masa depan yang sedang kamu bangun.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        lineHeight = 24.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedEntry(delay = 500) {
                SwipeToStartButton(
                    onStart = onStart,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun SwipeToStartButton(
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val trackHeight = 58.dp
    val knobSize = 50.dp
    val knobPadding = 4.dp

    val knobSizePx = with(density) { knobSize.toPx() }
    val knobPaddingPx = with(density) { knobPadding.toPx() }

    val dragOffset = remember { Animatable(0f) }
    var completed by remember { mutableStateOf(false) }

    // Animasi kedip halus untuk teks petunjuk
    val transition = rememberInfiniteTransition(label = "hint")
    val hintPulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintPulse"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .cardShadow(shapeRadius = 29.dp, blurRadius = 15.dp, offsetY = 4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }
        val maxDrag = (trackWidthPx - knobSizePx - knobPaddingPx * 2).coerceAtLeast(1f)
        val progress = (dragOffset.value / maxDrag).coerceIn(0f, 1f)

        // Jalur yang terisi di belakang knob
        val fillWidth = with(density) { (dragOffset.value + knobSizePx + knobPaddingPx * 2).toDp() }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(fillWidth)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Teks petunjuk (memudar saat digeser)
        val hintAlpha = if (progress > 0.6f) 1f else hintPulse * (1f - progress)
        val hintColor = if (progress > 0.6f) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            text = "Ayo Kita Mulai",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = hintColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(hintAlpha)
        )

        // Knob yang bisa digeser
        Box(
            modifier = Modifier
                .padding(start = knobPadding)
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .size(knobSize)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(maxDrag, completed) {
                    if (completed) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (dragOffset.value >= maxDrag * 0.85f) {
                                    completed = true
                                    dragOffset.animateTo(
                                        maxDrag,
                                        spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                    onStart()
                                } else {
                                    dragOffset.animateTo(
                                        0f,
                                        spring(stiffness = Spring.StiffnessLow)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { dragOffset.animateTo(0f) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                dragOffset.snapTo(
                                    (dragOffset.value + dragAmount).coerceIn(0f, maxDrag)
                                )
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Geser untuk mulai",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer { translationX = progress * 4.dp.toPx() }
            )
        }
    }
}