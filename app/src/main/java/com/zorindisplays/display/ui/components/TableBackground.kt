package com.zorindisplays.display.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*

@Composable
fun TableBackground(isFixedRtp: Boolean) {

    val infinite = rememberInfiniteTransition(label = "table")

    val baseColor = if (isFixedRtp) {
        Color(0xFF0B3A6E) // синий стол
    } else {
        Color(0xFF0F5A36) // зелёный стол
    }

    val centerColor = if (isFixedRtp) {
        Color(0xFF1C5FA8)
    } else {
        Color(0xFF1E7A4A)
    }

    // мягкое дыхание света
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {

        val w = size.width
        val h = size.height

        // базовый стол
        drawRect(
            color = baseColor
        )

        // центральный свет
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    centerColor,
                    baseColor
                ),
                center = Offset(w / 2, h / 2),
                radius = w * 0.9f * pulse
            )
        )

        // прожектор сверху
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

        // дополнительное мягкое свечение центра
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

        // виньетка
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