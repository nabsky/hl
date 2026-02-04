package com.zorindisplays.display.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zorindisplays.display.model.Guess
import com.zorindisplays.display.model.UiState
import com.zorindisplays.display.ui.GameViewModel
import com.zorindisplays.display.ui.theme.DefaultTextStyle
import java.text.NumberFormat
import coil3.ImageLoader
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import com.zorindisplays.display.model.Card
import com.zorindisplays.display.model.cardBackAssetUrl

@Composable
fun MainScreen(
    imageLoader: ImageLoader,
    vm: GameViewModel = viewModel()
) {
    val state by vm.state.collectAsState()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false

                when (e.key) {
                    Key.Enter -> { vm.onEnter(); true }
                    Key.Backspace -> { vm.onBackspace(); true }
                    Key.S -> { vm.onStart(); true }
                    Key.H -> { vm.onGuess(Guess.HIGHER); true }
                    Key.L -> { vm.onGuess(Guess.LOWER); true }
                    Key.Zero, Key.NumPad0 -> { vm.onDigit(0); true }
                    Key.One, Key.NumPad1 -> { vm.onDigit(1); true }
                    Key.Two, Key.NumPad2 -> { vm.onDigit(2); true }
                    Key.Three, Key.NumPad3 -> { vm.onDigit(3); true }
                    Key.Four, Key.NumPad4 -> { vm.onDigit(4); true }
                    Key.Five, Key.NumPad5 -> { vm.onDigit(5); true }
                    Key.Six, Key.NumPad6 -> { vm.onDigit(6); true }
                    Key.Seven, Key.NumPad7 -> { vm.onDigit(7); true }
                    Key.Eight, Key.NumPad8 -> { vm.onDigit(8); true }
                    Key.Nine, Key.NumPad9 -> { vm.onDigit(9); true }
                    else -> false
                }
            }
    ) {
        val st = state
        when (st) {
            UiState.Idle -> IdleView()

            is UiState.AmountEntry -> AmountEntryView(raw = st.raw)

            else -> {
                // сюда попадут Ready / Playing / Lost / Won
                val cards = when (st) {
                    is UiState.Ready -> st.cards
                    is UiState.Playing -> st.cards
                    is UiState.Lost -> st.cards
                    is UiState.Won -> st.cards
                    else -> emptyList()
                }

                val revealedCount = when (st) {
                    is UiState.Ready -> 0
                    is UiState.Playing -> st.revealedCount
                    is UiState.Lost -> st.revealedCount
                    is UiState.Won -> 5
                    else -> 0
                }

                val amount: Long? = when (st) {
                    is UiState.Ready -> st.amount
                    is UiState.Playing -> st.amount
                    is UiState.Won -> st.amount
                    is UiState.Lost -> st.lastAmount
                    else -> null
                }

                val bottomText = when (st) {
                    is UiState.Ready -> "PRESS START TO BEGIN"
                    is UiState.Playing -> if (st.awaitingGuess) "HIGHER OR LOWER" else ""
                    is UiState.Lost -> "BETTER LUCK NEXT TIME!"
                    is UiState.Won -> "YOU WON!"
                    else -> ""
                }

                RoundView(
                    amount = amount,
                    cards = cards,
                    revealedCount = revealedCount,
                    bottomText = bottomText,
                    imageLoader = imageLoader
                )
            }
        }
    }
}

@Composable
private fun IdleView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(
            text = "GUESS HIGHER OR LOWER\nAND DOUBLE YOUR PRIZE",
            style = DefaultTextStyle.copy(
                fontSize = 48.sp,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun AmountEntryView(raw: String) {
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        ) {
            BasicText(
                text = formatAmount(raw.toLongOrNull() ?: 0L),
                style = DefaultTextStyle.copy(fontSize = 96.sp)
            )

        }

        Box(Modifier.align(Alignment.Center)) {
            BasicText(
                text = "LOADING THE PRIZE\nPLEASE WAIT",
                style = DefaultTextStyle.copy(
                    fontSize = 36.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun RoundView(
    amount: Long?,
    cards: List<Card>,
    revealedCount: Int,
    bottomText: String,
    imageLoader: ImageLoader
) {
    val isLoseScreen = bottomText == "BETTER LUCK NEXT TIME!"

    var showAmount by remember { mutableStateOf(true) }

    LaunchedEffect(isLoseScreen) {
        if (isLoseScreen) {
            showAmount = true
            kotlinx.coroutines.delay(1000)
            showAmount = false
        } else {
            showAmount = true
        }
    }

    Box(Modifier.fillMaxSize()) {

        if (showAmount && amount != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
            ) {
                AnimatedVisibility(
                    visible = showAmount && amount != null,
                    enter = fadeIn(),
                    exit = fadeOut(animationSpec = tween(400))
                ) {
                    BasicText(
                        text = formatAmount(amount ?: 0L),
                        style = DefaultTextStyle.copy(fontSize = 96.sp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { i ->
                val faceUp = i < revealedCount
                FlipCard(
                    imageLoader = imageLoader,
                    faceUp = faceUp,
                    card = cards.getOrNull(i),
                    modifier = Modifier
                        .width(180.dp)
                        .height(260.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            BasicText(
                text = bottomText,
                style = DefaultTextStyle.copy(fontSize = 36.sp, textAlign = TextAlign.Center)
            )
        }
    }
}

@Composable
private fun FlipCard(
    imageLoader: ImageLoader,
    faceUp: Boolean,
    card: Card?,
    modifier: Modifier = Modifier
) {
    val rotation = remember { Animatable(0f) }
    var shownFaceUp by remember { mutableStateOf(faceUp) } // какая сторона сейчас реально показывается

    LaunchedEffect(faceUp) {
        if (shownFaceUp == faceUp) return@LaunchedEffect

        // 1) 0 -> 90 (половина времени)
        rotation.animateTo(
            targetValue = 90f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )

        // 2) на ребре переключаем сторону
        shownFaceUp = faceUp

        // 3) прыжок на -90, чтобы продолжить "раскрытие"
        rotation.snapTo(-90f)

        // 4) -90 -> 0 (вторая половина времени)
        rotation.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
    }

    val url = if (shownFaceUp && card != null) card.assetUrl() else cardBackAssetUrl()

    Box(
        modifier = modifier.graphicsLayer {
            rotationY = rotation.value
            cameraDistance = 24f * density
        }
    ) {
        AsyncImage(
            model = url,
            imageLoader = imageLoader,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}


private fun formatAmount(v: Long): String {
    val nf = NumberFormat.getIntegerInstance()
    return nf.format(v)
}