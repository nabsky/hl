package com.zorindisplays.display.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
fun AnimatedAmountText(
    targetAmount: Long,
    format: (Long) -> String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    strokeWidth: Float,
    textAlign: TextAlign = TextAlign.Center,
    animateOnFirst: Boolean = false,
    countDurationMs: Int = 650,
    flashDurationMs: Int = 180
) {
    val amountAnim = remember { Animatable(targetAmount.toFloat()) }

    // Вспышка
    val scaleAnim = remember { Animatable(1f) }
    val alphaAnim = remember { Animatable(1f) }

    var isFirst by remember { mutableStateOf(true) }
    var lastTarget by remember { mutableStateOf(targetAmount) }

    val shown = amountAnim.value.roundToLong().coerceAtLeast(0L)

    LaunchedEffect(targetAmount) {
        val shouldAnimate = !isFirst || animateOnFirst

        if (!shouldAnimate) {
            amountAnim.snapTo(targetAmount.toFloat())
            lastTarget = targetAmount
            isFirst = false
            return@LaunchedEffect
        }

        val changed = targetAmount != lastTarget
        lastTarget = targetAmount
        isFirst = false

        if (changed) {
            // pop + flash
            scaleAnim.snapTo(1f)
            alphaAnim.snapTo(1f)

            launch {
                scaleAnim.animateTo(
                    1.10f,
                    animationSpec = tween(flashDurationMs, easing = FastOutSlowInEasing)
                )
                scaleAnim.animateTo(
                    1.00f,
                    animationSpec = tween(flashDurationMs, easing = FastOutSlowInEasing)
                )
            }
            launch {
                alphaAnim.animateTo(
                    0.85f,
                    animationSpec = tween(flashDurationMs, easing = FastOutSlowInEasing)
                )
                alphaAnim.animateTo(
                    1.00f,
                    animationSpec = tween(flashDurationMs, easing = FastOutSlowInEasing)
                )
            }
        }

        val from = amountAnim.value
        val to = targetAmount.toFloat()
        val d = if (abs(to - from) > 5_000_000f) 450 else countDurationMs

        amountAnim.animateTo(
            targetValue = to,
            animationSpec = tween(durationMillis = d, easing = FastOutSlowInEasing)
        )
    }

    GoldShineText(
        text = format(shown),
        fontSize = fontSize,
        strokeWidth = strokeWidth,
        textAlign = textAlign,
        modifier = modifier.graphicsLayer {
            scaleX = scaleAnim.value
            scaleY = scaleAnim.value
            alpha = alphaAnim.value
        }
    )
}
