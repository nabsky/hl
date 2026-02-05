package com.zorindisplays.display.ui

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
import com.zorindisplays.display.R


@Composable
fun KonfettiOverlay(modifier: Modifier = Modifier) {

    val context = LocalContext.current

    val coinShape = remember {
        Shape.DrawableShape(
            drawable = ContextCompat.getDrawable(context, R.drawable.gold_coin)!!,
            tint = false // ОЧЕНЬ важно: не перекрашивать PNG
        )
    }

    val party = remember {
        Party(
            // центр снизу
            position = Position.Relative(0.5, 1.0),

            // вверх, но широко
            angle = Angle.TOP,
            spread = 120,                 // БОЛЬШЕ в стороны

            // быстрее старт
            speed = 48f,                 // было ~18
            maxSpeed = 60f,              // резкий выстрел

            // меньше торможения = быстрее падают
            damping = 0.95f,             // было 0.92

            // КРУПНЫЕ монеты
            size = listOf(
                Size(128)
            ),

            // форма монет
            shapes = listOf(coinShape),

            timeToLive = 400L,

            // золото
            colors = listOf(
                0xFFFFD700.toInt(),
                0xFFFFC107.toInt(),
                0xFFFFE082.toInt()
            ),

            // короткий, плотный burst
            emitter = Emitter(
                duration = 180,          // быстрее, не затягиваем
                TimeUnit.MILLISECONDS
            ).perSecond(320)              // МНОГО частиц сразу
        )
    }

    KonfettiView(
        modifier = modifier,
        parties = listOf(party)
    )
}