package com.zorindisplays.hilo.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

@Composable
fun TableBackground(isFixedRtp: Boolean) {
    val infinite = rememberInfiniteTransition(label = "table")

    val baseColor = if (isFixedRtp) {
        Color(0xFF0B3A6E)
    } else {
        Color(0xFF0F5A36)
    }

    val centerColor = if (isFixedRtp) {
        Color(0xFF1C5FA8)
    } else {
        Color(0xFF1E7A4A)
    }

    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val grainPattern = remember {
        val cols = 260
        val rows = 160
        Array(rows) { FloatArray(cols) { Random.nextFloat() } }
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val w = size.width
        val h = size.height

        drawRect(color = baseColor)

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(centerColor, baseColor),
                center = Offset(w / 2, h / 2),
                radius = w * 0.9f * pulse
            )
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.07f),
                    Color.Transparent
                ),
                center = Offset(w / 2, h * 0.15f),
                radius = w * 0.6f
            )
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                center = Offset(w / 2, h / 2),
                radius = w * 0.35f * pulse
            )
        )

        // Диагональная тканевая фактура
        val stripeStep = 18f
        val stripeWidth = 6f
        var startX = -h

        while (startX < w + h) {
            drawLine(
                color = Color.White.copy(alpha = 0.018f),
                start = Offset(startX, 0f),
                end = Offset(startX + h, h),
                strokeWidth = stripeWidth
            )

            drawLine(
                color = Color.Black.copy(alpha = 0.014f),
                start = Offset(startX + stripeStep * 0.5f, 0f),
                end = Offset(startX + stripeStep * 0.5f + h, h),
                strokeWidth = stripeWidth * 0.8f
            )

            startX += stripeStep
        }

        // Мелкий grain против banding
        val grainCell = 8f
        val grainCols = grainPattern[0].size
        val grainRows = grainPattern.size

        for (gy in 0 until grainRows) {
            val top = gy * grainCell
            if (top > h) break

            for (gx in 0 until grainCols) {
                val left = gx * grainCell
                if (left > w) break

                val n = grainPattern[gy][gx]
                val color = if (n > 0.5f) {
                    Color.White.copy(alpha = 0.016f)
                } else {
                    Color.Black.copy(alpha = 0.016f)
                }

                drawRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(2.2f, 2.2f)
                )
            }
        }

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.65f)
                ),
                center = Offset(w / 2, h / 2),
                radius = w * 0.85f
            )
        )
    }
}