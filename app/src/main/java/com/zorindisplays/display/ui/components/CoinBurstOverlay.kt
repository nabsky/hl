package com.zorindisplays.display.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.zorindisplays.display.R
import kotlin.math.max
import kotlin.random.Random

private data class CoinBurstParticle(
    val xFraction: Float,
    val driftDp: Float,
    val sizeDp: Int,
    val startDelayMs: Int,
    val durationMs: Int,
    val rotationStart: Float,
    val rotationEnd: Float,
    val alpha: Float
)

@Composable
fun CoinBurstOverlay(
    modifier: Modifier = Modifier,
    coinCount: Int = 18,
    burstDurationMs: Int = 2500,
    onFinished: (() -> Unit)? = null
) {
    val progress = remember { Animatable(0f) }
    var finished by remember { mutableStateOf(false) }

    val particles = remember {
        List(coinCount) {
            CoinBurstParticle(
                xFraction = Random.nextFloat(),
                driftDp = Random.nextDouble(-70.0, 70.0).toFloat(),
                sizeDp = Random.nextInt(26, 54),
                startDelayMs = Random.nextInt(0, 500),
                durationMs = Random.nextInt(1200, 2200),
                rotationStart = Random.nextInt(0, 360).toFloat(),
                rotationEnd = Random.nextInt(360, 1080).toFloat(),
                alpha = Random.nextDouble(0.72, 1.0).toFloat()
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = burstDurationMs, easing = LinearEasing)
        )
        finished = true
        onFinished?.invoke()
    }

    if (finished) return

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        particles.forEach { particle ->
            val globalTimeMs = progress.value * burstDurationMs
            val localTimeMs = globalTimeMs - particle.startDelayMs

            if (localTimeMs <= 0f) return@forEach

            val localProgress = (localTimeMs / particle.durationMs).coerceIn(0f, 1f)
            if (localProgress >= 1f) return@forEach

            val xPx = widthPx * particle.xFraction + particle.driftDp * localProgress * 2.2f
            val yPx = -140f + (heightPx + 280f) * localProgress

            val fadeOutStart = 0.72f
            val alphaFactor = if (localProgress < fadeOutStart) {
                1f
            } else {
                1f - ((localProgress - fadeOutStart) / (1f - fadeOutStart))
            }.coerceIn(0f, 1f)

            val rotation =
                particle.rotationStart + (particle.rotationEnd - particle.rotationStart) * localProgress

            Image(
                painter = painterResource(R.drawable.gold_coin),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(particle.sizeDp.dp)
                    .graphicsLayer {
                        translationX = xPx
                        translationY = yPx
                        rotationZ = rotation
                        alpha = particle.alpha * alphaFactor
                    }
            )
        }
    }
}