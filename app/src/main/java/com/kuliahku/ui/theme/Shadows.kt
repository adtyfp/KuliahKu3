package com.kuliahku.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Mengembalikan shadow ke versi paling awal (Standar Material Shadow).
 * Menggunakan Modifier.shadow bawaan untuk performa tinggi dan tampilan native.
 */
@Composable
fun Modifier.cardShadow(
    shapeRadius: Dp = 16.dp,
    elevation: Dp = 4.dp
): Modifier = this.shadow(
    elevation = elevation,
    shape = RoundedCornerShape(shapeRadius),
    clip = false
)

/**
 * Overload untuk menjaga kompatibilitas dengan parameter lama di seluruh project.
 * Parameter kustom seperti blurRadius dan offsetY sekarang akan diabaikan 
 * untuk kembali ke standar elevasi 4.dp.
 */
@Composable
fun Modifier.cardShadow(
    shapeRadius: Dp = 20.dp,
    color: Color = Color.Black.copy(alpha = 0.1f),
    blurRadius: Dp = 35.dp,
    offsetY: Dp = 10.dp,
    offsetX: Dp = 0.dp,
    spread: Dp = 0.dp
): Modifier = this.shadow(
    elevation = 4.dp,
    shape = RoundedCornerShape(shapeRadius),
    clip = false
)
