package com.zorindisplays.hilo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import java.util.concurrent.TimeUnit
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.zorindisplays.hilo.R


@Composable
fun KonfettiOverlay(modifier: Modifier = Modifier) {

    val context = LocalContext.current

    val coinShape = remember {
        Shape.DrawableShape(
            drawable = ContextCompat.getDrawable(context, R.drawable.gold_coin)!!,
            tint = false // ОЧЕНЬ важно: не перекрашивать PNG
        )
    }

    val party = Party(
        position = Position.Relative(0.5, 1.0),

        angle = Angle.TOP,
        spread = 95,

        speed = 42f,
        maxSpeed = 56f,

        damping = 0.95f,

        size = listOf(
            Size(96)
        ),

        shapes = listOf(coinShape),

        timeToLive = 320L,

        colors = listOf(
            0xFFFFD700.toInt(),
            0xFFFFC107.toInt(),
            0xFFFFE082.toInt()
        ),

        emitter = Emitter(
            duration = 160,
            TimeUnit.MILLISECONDS
        ).perSecond(110)
    )

    KonfettiView(
        modifier = modifier,
        parties = listOf(party)
    )
}