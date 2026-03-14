package com.zorindisplays.display.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.zorindisplays.display.R
import kotlin.random.Random

private data class CoinParticle(
    val xFraction: Float,
    val sizeDp: Int,
    val durationMs: Int,
    val delayMs: Int,
    val rotationStart: Float,
    val rotationEnd: Float,
    val alpha: Float
)

@Composable
fun CoinRainOverlay(
    modifier: Modifier = Modifier,
    coinCount: Int = 18
) {
    val particles = remember {
        List(coinCount) {
            CoinParticle(
                xFraction = Random.nextFloat(),
                sizeDp = Random.nextInt(28, 60),
                durationMs = Random.nextInt(1800, 3200),
                delayMs = Random.nextInt(0, 1400),
                rotationStart = Random.nextInt(0, 360).toFloat(),
                rotationEnd = Random.nextInt(360, 1080).toFloat(),
                alpha = Random.nextDouble(0.70, 1.0).toFloat()
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        particles.forEach { particle ->
            val transition = rememberInfiniteTransition(label = "coinRain")

            val progress = transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = particle.durationMs,
                        delayMillis = particle.delayMs,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "fallProgress"
            )

            val rotation = transition.animateFloat(
                initialValue = particle.rotationStart,
                targetValue = particle.rotationEnd,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = particle.durationMs,
                        delayMillis = particle.delayMs,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            val x = widthPx * particle.xFraction
            val y = -120f + (heightPx + 240f) * progress.value

            Image(
                painter = painterResource(R.drawable.gold_coin),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(particle.sizeDp.dp)
                    .graphicsLayer {
                        translationX = x
                        translationY = y
                        alpha = particle.alpha
                        rotationZ = rotation.value
                    }
            )
        }
    }
}