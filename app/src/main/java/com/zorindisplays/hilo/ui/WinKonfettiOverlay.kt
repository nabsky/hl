package com.zorindisplays.hilo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.zorindisplays.hilo.R
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
            position = Position.Relative(-0.06, -0.12),
            angle = 65,
            spread = 28,
            speed = 10f,
            maxSpeed = 16f,
            damping = 0.97f,
            timeToLive = 1600L,
            size = listOf(Size(72), Size(88)),
            shapes = listOf(coinShape),
            colors = listOf(
                0xFFFFD700.toInt(),
                0xFFFFC107.toInt(),
                0xFFFFE082.toInt()
            ),
            emitter = Emitter(
                duration = 420,
                TimeUnit.MILLISECONDS
            ).perSecond(70)
        )
    }

    val rightParty = remember {
        Party(
            position = Position.Relative(1.06, -0.12),
            angle = 115,
            spread = 28,
            speed = 10f,
            maxSpeed = 16f,
            damping = 0.97f,
            timeToLive = 1600L,
            size = listOf(Size(72), Size(88)),
            shapes = listOf(coinShape),
            colors = listOf(
                0xFFFFD700.toInt(),
                0xFFFFC107.toInt(),
                0xFFFFE082.toInt()
            ),
            emitter = Emitter(
                duration = 420,
                TimeUnit.MILLISECONDS
            ).perSecond(70)
        )
    }

    val topCenterParty = remember {
        Party(
            position = Position.Relative(0.5, -0.10),
            angle = Angle.BOTTOM,
            spread = 50,
            speed = 8f,
            maxSpeed = 14f,
            damping = 0.975f,
            timeToLive = 1700L,
            size = listOf(Size(64), Size(80)),
            shapes = listOf(coinShape),
            colors = listOf(
                0xFFFFD700.toInt(),
                0xFFFFC107.toInt(),
                0xFFFFE082.toInt()
            ),
            emitter = Emitter(
                duration = 520,
                TimeUnit.MILLISECONDS
            ).perSecond(55)
        )
    }

    KonfettiView(
        modifier = modifier,
        parties = listOf(leftParty, rightParty, topCenterParty)
    )
}
