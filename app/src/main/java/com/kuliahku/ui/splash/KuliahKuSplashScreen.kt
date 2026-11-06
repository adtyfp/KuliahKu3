package com.kuliahku.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.tan

private val EaseBook = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val EaseText = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/** Durasi total splash sebelum pindah layar (ms). Shimmer selesai di ~2900ms. */
private const val TOTAL_DURATION_MS = 3600L

/**
 * Splash screen animasi KuliahKu.
 *
 * Semua warna diambil dari [MaterialTheme] aplikasi (bukan dari pengaturan sistem),
 * sehingga otomatis mengikuti mode terang/gelap yang dipakai aplikasi, sama seperti
 * halaman Login. Pastikan splash dipanggil di dalam tema aplikasi (KuliahkuTheme).
 *
 * Koordinat logo memakai viewBox 320x120, lalu di-scale mengikuti lebar layar.
 */
@Composable
fun KuliahKuSplashScreen(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit
) {
    val currentOnFinished by rememberUpdatedState(onFinished)

    // ---- Warna dari tema aplikasi ----
    val colors = MaterialTheme.colorScheme
    val backgroundColor = colors.background
    val brand = colors.primary
    val taglineColor = colors.onSurfaceVariant
    val isDark = backgroundColor.luminance() < 0.5f
    val bookGradient = listOf(
        lerp(brand, Color.Black, 0.2f),
        brand,
        lerp(brand, Color.White, 0.15f)
    )

    val glowScale = remember { Animatable(0.5f) }
    val glowAlpha = remember { Animatable(0f) }
    val bookProgress = remember { Animatable(0f) }
    val textProgress = remember { Animatable(0f) }
    val shimmerX = remember { Animatable(-160f) }
    val breath = remember { Animatable(0f) }
    val footerAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Glow: scale 0.5 -> 1.1, alpha 0 -> 0.8 -> 0.45 (2.2s)
        launch { glowScale.animateTo(1.1f, tween(2200, easing = EaseText)) }
        launch {
            glowAlpha.animateTo(0.45f, keyframes {
                durationMillis = 2200
                0f at 0
                0.8f at 1100
                0.45f at 2200
            })
        }
        // Garis buku tergambar (delay 0.25s, durasi 1.3s)
        launch { bookProgress.animateTo(1f, tween(1300, 250, EaseBook)) }
        // Teks muncul (delay 0.65s, durasi 1.1s)
        launch { textProgress.animateTo(1f, tween(1100, 650, EaseText)) }
        // Kilau menyapu logo (delay 1.6s, durasi 1.3s)
        launch { shimmerX.animateTo(380f, tween(1300, 1600, FastOutSlowInEasing)) }
        // Tagline + titik loading muncul (delay 2.1s, fade 1s)
        launch { footerAlpha.animateTo(1f, tween(1000, 2100)) }
        // Efek "napas" tanpa henti (mulai 2.4s)
        launch {
            delay(2400)
            breath.animateTo(
                1f,
                infiniteRepeatable(tween(2250, easing = FastOutSlowInEasing), RepeatMode.Reverse)
            )
        }

        delay(TOTAL_DURATION_MS)
        currentOnFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow
        Canvas(
            modifier = Modifier
                .size(288.dp)
                .graphicsLayer {
                    scaleX = glowScale.value
                    scaleY = glowScale.value
                    alpha = glowAlpha.value
                }
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        brand.copy(alpha = if (isDark) 0.35f else 0.22f),
                        brand.copy(alpha = 0f)
                    )
                )
            )
        }

        LogoCanvas(
            brand = brand,
            bookGradient = bookGradient,
            bookProgress = { bookProgress.value },
            textProgress = { textProgress.value },
            shimmerX = { shimmerX.value },
            breath = { breath.value },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .widthIn(max = 360.dp)
                .aspectRatio(320f / 120f)
        )

        // Footer: tagline + 3 titik loading
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .graphicsLayer { alpha = footerAlpha.value },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TEMAN SETIA PERKULIAHAN",
                color = taglineColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BouncingDot(color = brand, delayMs = 0)
                BouncingDot(color = brand, delayMs = 150)
                BouncingDot(color = brand, delayMs = 300)
            }
        }
    }
}

@Composable
private fun LogoCanvas(
    brand: Color,
    bookGradient: List<Color>,
    bookProgress: () -> Float,
    textProgress: () -> Float,
    shimmerX: () -> Float,
    breath: () -> Float,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier.graphicsLayer {
            // Offscreen: wajib agar BlendMode.SrcAtop hanya mengenai piksel logo
            compositingStrategy = CompositingStrategy.Offscreen
            val b = breath()
            translationY = -3.dp.toPx() * b
            scaleX = 1f + 0.015f * b
            scaleY = 1f + 0.015f * b
        }
    ) {
        val s = size.width / 320f   // faktor skala dari viewBox 320x120

        // 1) Teks "KuliahKu"
        val tp = textProgress()
        val layout = textMeasurer.measure(
            text = "KuliahKu",
            style = TextStyle(
                fontSize = (60f * s).toSp(),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-2.5f * s).toSp()
            )
        )
        val baselineY = 62f * s
        withTransform({
            translate(top = (1f - tp) * 18f * s)
            val sc = 0.96f + 0.04f * tp
            scale(sc, sc, pivot = Offset(size.width / 2f, baselineY))
        }) {
            drawText(
                textLayoutResult = layout,
                color = brand,
                topLeft = Offset(
                    x = (size.width - layout.size.width) / 2f,
                    y = baselineY - layout.firstBaseline
                ),
                alpha = tp
            )
        }

        // 2) Garis buku terbuka (tergambar bertahap)
        val bp = bookProgress()
        if (bp > 0f) {
            val bookPath = Path().apply {
                moveTo(110f * s, 76f * s)
                quadraticBezierTo(138f * s, 76f * s, 160f * s, 92f * s)
                quadraticBezierTo(182f * s, 76f * s, 210f * s, 76f * s)
            }
            val measure = PathMeasure().apply { setPath(bookPath, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * bp, partial, true)
            drawPath(
                path = partial,
                brush = Brush.horizontalGradient(
                    colors = bookGradient,
                    startX = 110f * s,
                    endX = 210f * s
                ),
                style = Stroke(width = 10f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // 3) Kilau metalik (miring -20°), hanya tampil di atas piksel logo
        val sx = shimmerX() * s
        if (sx > -150f * s && sx < 370f * s) {
            val w = 110f * s
            val h = size.height
            val slant = h * tan(Math.toRadians(20.0)).toFloat()
            val parallelogram = Path().apply {
                moveTo(sx + slant, 0f)
                lineTo(sx + w + slant, 0f)
                lineTo(sx + w, h)
                lineTo(sx, h)
                close()
            }
            val white = Color.White
            drawPath(
                path = parallelogram,
                brush = Brush.horizontalGradient(
                    0.0f to white.copy(alpha = 0f),
                    0.3f to white.copy(alpha = 0.25f),
                    0.5f to white.copy(alpha = 0.95f),
                    0.7f to white.copy(alpha = 0.25f),
                    1.0f to white.copy(alpha = 0f),
                    startX = sx,
                    endX = sx + w + slant
                ),
                blendMode = BlendMode.SrcAtop
            )
        }
    }
}

@Composable
private fun BouncingDot(color: Color, delayMs: Int, size: Dp = 6.dp) {
    val transition = rememberInfiniteTransition(label = "dot")
    val offsetY by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMs)
        ),
        label = "dotY"
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { translationY = offsetY.dp.toPx() }
            .background(color, CircleShape)
    )
}