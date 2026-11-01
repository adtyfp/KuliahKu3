package com.kuliahku.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

@Composable
fun AnimatedEntry(
    modifier: Modifier = Modifier,
    delay: Int = 0,
    duration: Int = 800,
    content: @Composable () -> Unit
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (delay > 0) {
            delay(delay.toLong())
        }
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(duration, easing = EaseOutQuart)
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = animatedProgress.value
            translationY = (1f - animatedProgress.value) * 60f // Slide up effect
        }
    ) {
        content()
    }
}
