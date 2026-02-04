package com.zorindisplays.display.ui.screens

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
        when (val st = state) {
            UiState.Idle -> IdleView()

            is UiState.AmountEntry -> AmountEntryView(raw = st.raw)

            is UiState.Ready -> ReadyView(
                amount = st.amount,
                cards = st.cards,
                revealedCount = 0,
                bottomText = "PRESS START TO BEGIN",
                imageLoader = imageLoader
            )

            is UiState.Playing -> {
                val bottom = if (st.awaitingGuess) "HIGHER OR LOWER" else ""
                ReadyView(
                    amount = st.amount,
                    cards = st.cards,
                    revealedCount = st.revealedCount,
                    bottomText = bottom,
                    imageLoader = imageLoader
                )
            }

            is UiState.Lost -> ReadyView(
                amount = null,
                cards = st.cards,
                revealedCount = st.revealedCount,
                bottomText = "BETTER LUCK NEXT TIME!",
                imageLoader = imageLoader
            )

            is UiState.Won -> ReadyView(
                amount = st.amount,
                cards = st.cards,
                revealedCount = 5,
                bottomText = "YOU WON!",
                imageLoader = imageLoader
            )
        }
    }
}

@Composable
private fun IdleView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(
            text = "Guess whether the next card will be higher or lower to increase your prize",
            style = DefaultTextStyle
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
                style = DefaultTextStyle.copy(fontSize = 36.sp)
            )

        }

        Box(Modifier.align(Alignment.Center)) {
            BasicText(
                text = "Type prize amount, press Enter",
                style = DefaultTextStyle
            )
        }
    }
}

@Composable
private fun ReadyView(
    amount: Long?,
    cards: List<Card>,
    revealedCount: Int,
    bottomText: String,
    imageLoader: ImageLoader
) {
    Box(Modifier.fillMaxSize()) {

        if (amount != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
            ) {
                BasicText(
                    text = formatAmount(amount),
                    style = DefaultTextStyle.copy(fontSize = 42.sp)
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { i ->
                val faceUp = i < revealedCount
                CardImage(
                    imageLoader = imageLoader,
                    faceUp = faceUp,
                    card = cards.getOrNull(i)
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
                style = DefaultTextStyle.copy(
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun CardImage(
    imageLoader: ImageLoader,
    faceUp: Boolean,
    card: Card?,
) {
    val url = if (faceUp && card != null) card.assetUrl() else cardBackAssetUrl()

    AsyncImage(
        model = url,
        imageLoader = imageLoader,
        contentDescription = null,
        modifier = Modifier
            .width(90.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(10.dp)),
        contentScale = ContentScale.Fit
    )
}

private fun formatAmount(v: Long): String {
    val nf = NumberFormat.getIntegerInstance()
    return nf.format(v)
}
