package com.zorindisplays.hilo.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.zorindisplays.hilo.ui.theme.Chango

@Composable
fun GoldShineText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 64.sp,
    strokeWidth: Float = 20f,
    textAlign: TextAlign = TextAlign.Center,
    shiny: Boolean = false,
    shineDurationMs: Int = 2200,
    shineAlpha: Float = 0.32f
) {
    val goldBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFF3B0),
            Color(0xFFFFD700),
            Color(0xFFFFC107),
            Color(0xFFFFE082),
        ),
        start = Offset(0f, 0f),
        end = Offset(0f, 220f),
        tileMode = TileMode.Clamp
    )

    val baseStyle = TextStyle(
        fontSize = fontSize,
        fontWeight = FontWeight.Black,
        textAlign = textAlign,
        fontFamily = Chango
    )

    Box(modifier = modifier) {
        BasicText(
            text = text,
            style = baseStyle.copy(
                color = Color(0xFF3A2A18),
                drawStyle = Stroke(width = strokeWidth)
            )
        )

        if (!shiny) {
            BasicText(
                text = text,
                style = baseStyle.copy(brush = goldBrush)
            )
        } else {
            val transition = rememberInfiniteTransition(label = "goldShine")
            val t by transition.animateFloat(
                initialValue = -0.6f,
                targetValue = 1.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(shineDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "shineT"
            )

            BasicText(
                text = text,
                style = baseStyle.copy(brush = goldBrush),
                modifier = Modifier
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithCache {
                        val band = size.width * 0.30f
                        val x = size.width * t

                        val shineBrush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = shineAlpha),
                                Color.Transparent
                            ),
                            start = Offset(x - band, 0f),
                            end = Offset(x + band, size.height)
                        )

                        onDrawWithContent {
                            drawContent()
                            drawRect(
                                brush = shineBrush,
                                blendMode = BlendMode.SrcAtop
                            )
                        }
                    }
            )
        }
    }
}