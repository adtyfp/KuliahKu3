package com.kuliahku.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageCompressor {

    suspend fun compress(
        context: Context,
        uri: Uri,
        maxDimension: Int = Constants.MAX_AVATAR_SIZE,
        quality: Int = 85
    ): ByteArray = withContext(Dispatchers.IO) {
        val input: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Tidak bisa membuka gambar.")
        val original = BitmapFactory.decodeStream(input)
        input.close()

        val scaled = if (original.width > maxDimension || original.height > maxDimension) {
            val ratio = original.width.toFloat() / original.height
            val (w, h) = if (ratio > 1) {
                maxDimension to (maxDimension / ratio).toInt()
            } else {
                (maxDimension * ratio).toInt() to maxDimension
            }
            Bitmap.createScaledBitmap(original, w, h, true)
        } else original

        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
        if (scaled !== original) scaled.recycle()
        original.recycle()
        output.toByteArray()
    }
}