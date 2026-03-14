package com.zorindisplays.display.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.zorindisplays.display.R
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import java.util.concurrent.TimeUnit

@Composable
fun WinKonfettiOverlay(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val coinShape = remember {
        Shape.DrawableShape(
            drawable = ContextCompat.getDrawable(context, R.drawable.gold_coin)!!,
            tint = false
        )
    }

    val leftParty = remember {
        Party(
            position = Position.Relative(-0.12, -0.24),
            angle = 65,
            spread = 36,
            speed = 14f,
            maxSpeed = 22f,
            damping = 0.96f,
            timeToLive = 2200L,
            size = listOf(Size(96), Size(128)),
            shapes = listOf(coinShape),
            colors = listOf(
                0xFFFFD700.toInt(),
                0xFFFFC107.toInt(),
                0xFFFFE082.toInt()
            ),
            emitter = Emitter(
                duration = 700,
                TimeUnit.MILLISECONDS
            ).perSecond(140)
        )
    }

    val rightParty = remember {
        Party(
            position = Position.Relative(1.12, -0.24),
            angle = 115,
            spread = 36,
            speed = 14f,
            maxSpeed = 22f,
            damping = 0.96f,
            timeToLive = 2200L,
            size = listOf(Size(96), Size(128)),
            shapes = listOf(coinShape),
            colors = listOf(
                0xFFFFD700.toInt(),
                0xFFFFC107.toInt(),
                0xFFFFE082.toInt()
            ),
            emitter = Emitter(
                duration = 700,
                TimeUnit.MILLISECONDS
            ).perSecond(140)
        )
    }

    val topCenterParty = remember {
        Party(
            position = Position.Relative(0.5, -0.24),
            angle = Angle.BOTTOM,
            spread = 70,
            speed = 10f,
            maxSpeed = 18f,
            damping = 0.97f,
            timeToLive = 2400L,
            size = listOf(Size(72), Size(96)),
            shapes = listOf(coinShape),
            colors = listOf(
                0xFFFFD700.toInt(),
                0xFFFFC107.toInt(),
                0xFFFFE082.toInt()
            ),
            emitter = Emitter(
                duration = 900,
                TimeUnit.MILLISECONDS
            ).perSecond(110)
        )
    }

    KonfettiView(
        modifier = modifier,
        parties = listOf(leftParty, rightParty, topCenterParty)
    )
}