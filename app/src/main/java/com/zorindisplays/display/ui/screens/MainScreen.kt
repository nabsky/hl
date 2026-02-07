package com.zorindisplays.display.ui.screens

import android.media.MediaPlayer
import android.media.AudioAttributes
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zorindisplays.display.model.Guess
import com.zorindisplays.display.model.UiState
import com.zorindisplays.display.ui.GameViewModel
import com.zorindisplays.display.ui.theme.DefaultTextStyle
import coil3.ImageLoader
import coil3.compose.AsyncImage
import androidx.compose.ui.graphics.graphicsLayer
import com.zorindisplays.display.model.Card
import com.zorindisplays.display.model.cardBackAssetUrl
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import com.airbnb.lottie.compose.*
import com.zorindisplays.display.R
import com.zorindisplays.display.ui.KonfettiOverlay
import com.zorindisplays.display.ui.components.AnimatedAmountText
import com.zorindisplays.display.ui.components.GoldShineText
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.getValue


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
        val playing = state as? UiState.Playing
        val showConfetti = playing?.showConfetti == true
        val confettiTick = playing?.confettiTick ?: 0

        if (showConfetti) {
            key(confettiTick) {
                KonfettiOverlay(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(999f)
                )
            }
        }
        val isWon = state is UiState.Won

        if (isWon) {
            var showLottie by remember { mutableStateOf(false) }
            LaunchedEffect(isWon) {
                showLottie = false
                kotlinx.coroutines.delay(400)
                showLottie = true
            }
            if (showLottie) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.winner))
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = 1,
                    isPlaying = true
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1000f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier
                            .scale(4f)
                            .padding(bottom = 60.dp)  // отступ снизу
                    )
                }
            }
        }

        // Воспроизведение coins.wav через MediaPlayer
        val playCoinSound = (state as? UiState.Playing)?.playCoinSound == true
        val context = LocalContext.current
        var coinsPlayer: MediaPlayer? by remember { mutableStateOf(null) }
        LaunchedEffect(playCoinSound) {
            if (playCoinSound) {
                // Если уже проигрывается, не запускать повторно
                if (coinsPlayer?.isPlaying == true) return@LaunchedEffect
                coinsPlayer?.release()
                coinsPlayer = MediaPlayer.create(context, R.raw.coins)
                coinsPlayer?.setOnCompletionListener { mp -> mp.release(); coinsPlayer = null }
                coinsPlayer?.start()
                vm.onCoinSoundPlayed()
            }
        }

        // Воспроизведение winner.wav через MediaPlayer
        if (state is UiState.Won && (state as UiState.Won).playWinnerSound) {
            LaunchedEffect((state as UiState.Won).amount) {
                MediaPlayer.create(context, R.raw.win).apply {
                    setOnCompletionListener { release() }
                    start()
                }
                vm.onWinnerSoundPlayed()
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
            GoldShineText(
                text = formatAmount(raw.toLongOrNull() ?: 0L),
                fontSize = 120.sp,
                strokeWidth = 14f
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
                    visible = showAmount,
                    enter = fadeIn(),
                    exit = fadeOut(animationSpec = tween(400))
                ) {
                    AnimatedAmountText(
                        targetAmount = amount,
                        format = ::formatAmount,
                        fontSize = 120.sp,
                        strokeWidth = 12f,
                        animateOnFirst = false
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
    val context = LocalContext.current
    val rotation = remember { Animatable(0f) }
    var shownFaceUp by remember { mutableStateOf(faceUp) } // какая сторона сейчас реально показывается

    LaunchedEffect(faceUp) {
        if (shownFaceUp == faceUp) return@LaunchedEffect

        // 1) 0 -> 90 (половина времени)
        rotation.animateTo(
            targetValue = 90f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
        MediaPlayer.create(context, R.raw.flick).apply {
            setOnCompletionListener { release() }
            start()
        }

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


private val amountFormat by lazy {
    val symbols = DecimalFormatSymbols().apply {
        groupingSeparator = '\u00A0' // неразрывной пробел
    }
    DecimalFormat("#,###", symbols)
}

private fun formatAmount(v: Long): String {
    return amountFormat.format(v) + "\u00A0CFA"
}