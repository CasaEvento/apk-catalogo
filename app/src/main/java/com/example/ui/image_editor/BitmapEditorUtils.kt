package com.example.ui.image_editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

object BitmapEditorUtils {

    /**
     * Replaces colors in the bitmap similar to the target color at (x, y) with a transparent value (Alpha = 0).
     * Calculates the 3D RGB distance and tests against the user-supplied tolerance.
     */
    suspend fun removeColorSweep(
        source: Bitmap,
        targetColor: Int,
        tolerance: Float // 0.0 to 1.0 representing color similarity
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = source.width
        val height = source.height
        val mutableBitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        mutableBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val targetR = Color.red(targetColor)
        val targetG = Color.green(targetColor)
        val targetB = Color.blue(targetColor)

        // Tolerance converted to max distance in RGB space (max distance is sqrt(3) * 255 = ~441.67)
        val maxDist = 441.67f * tolerance

        for (i in pixels.indices) {
            val color = pixels[i]
            if (Color.alpha(color) == 0) continue

            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)

            // Euclidean distance in RGB color space
            val dist = sqrt(
                (r - targetR).toDouble().pow(2.0) +
                (g - targetG).toDouble().pow(2.0) +
                (b - targetB).toDouble().pow(2.0)
            )

            if (dist <= maxDist) {
                pixels[i] = Color.TRANSPARENT
            }
        }

        mutableBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return@withContext mutableBitmap
    }

    /**
     * Erases pixels from a bitmap where a brush path is drawn by the user.
     */
    fun eraseWithBrush(
        source: Bitmap,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        brushSize: Float
    ): Bitmap {
        val mutableBitmap = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = brushSize
            // Clear pixels (render transparent background)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            isAntiAlias = true
        }
        canvas.drawLine(startX, startY, endX, endY, paint)
        return mutableBitmap
    }
}
