package com.zorindisplays.hilo.ui.components

import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.max
import kotlin.random.Random

@Composable
fun TableBackground(isFixedRtp: Boolean) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.roundToPx().coerceAtLeast(1) }
        val heightPx = with(density) { maxHeight.roundToPx().coerceAtLeast(1) }

        val bitmap = remember(widthPx, heightPx, isFixedRtp) {
            buildTableBitmap(
                width = widthPx,
                height = heightPx,
                isFixedRtp = isFixedRtp
            )
        }

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

private fun buildTableBitmap(
    width: Int,
    height: Int,
    isFixedRtp: Boolean
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val baseColor = if (isFixedRtp) 0xFF0B3A6E.toInt() else 0xFF0F5A36.toInt()
    val centerColor = if (isFixedRtp) 0xFF1C5FA8.toInt() else 0xFF1E7A4A.toInt()

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // База
    fillPaint.shader = null
    fillPaint.color = baseColor
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint)

    // Основной мягкий центр
    fillPaint.shader = RadialGradient(
        width * 0.5f,
        height * 0.5f,
        width * 0.9f,
        centerColor,
        baseColor,
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint)

    // Верхний мягкий свет
    fillPaint.shader = RadialGradient(
        width * 0.5f,
        height * 0.15f,
        width * 0.6f,
        0x12FFFFFF,
        0x00FFFFFF,
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint)

    // Центральный мягкий highlight
    fillPaint.shader = RadialGradient(
        width * 0.5f,
        height * 0.5f,
        width * 0.35f,
        0x0AFFFFFF,
        0x00FFFFFF,
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint)

    // Диагональная тканевая фактура
    val stripePaintLight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x05FFFFFF
        strokeWidth = 6f
    }
    val stripePaintDark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x04000000
        strokeWidth = 4.8f
    }

    val stripeStep = 18f
    var startX = -height.toFloat()
    while (startX < width + height) {
        canvas.drawLine(
            startX, 0f,
            startX + height, height.toFloat(),
            stripePaintLight
        )
        canvas.drawLine(
            startX + stripeStep * 0.5f, 0f,
            startX + stripeStep * 0.5f + height, height.toFloat(),
            stripePaintDark
        )
        startX += stripeStep
    }

    // Grain — реже и дешевле, чем раньше
    val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    val rnd = Random(if (isFixedRtp) 2 else 1)
    val grainCell = 10
    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            val n = rnd.nextFloat()
            grainPaint.color = if (n > 0.5f) 0x04FFFFFF else 0x04000000
            canvas.drawRect(
                RectF(
                    x.toFloat(),
                    y.toFloat(),
                    x + 2.2f,
                    y + 2.2f
                ),
                grainPaint
            )
            x += grainCell
        }
        y += grainCell
    }

    // Лёгкая вертикальная глубина
    fillPaint.shader = LinearGradient(
        0f,
        0f,
        0f,
        height.toFloat(),
        intArrayOf(0x10000000, 0x00000000, 0x18000000),
        floatArrayOf(0f, 0.45f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint)

    // Виньетка
    fillPaint.shader = RadialGradient(
        width * 0.5f,
        height * 0.5f,
        width * 0.85f,
        0x00000000,
        0xA6000000.toInt(),
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint)

    return bitmap
}