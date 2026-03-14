package com.zorindisplays.display.ui.screens

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.zorindisplays.display.R
import com.zorindisplays.display.model.Card
import com.zorindisplays.display.model.Guess
import com.zorindisplays.display.model.UiState
import com.zorindisplays.display.model.cardBackAssetUrl
import com.zorindisplays.display.ui.DeckMode
import com.zorindisplays.display.ui.GameViewModel
import com.zorindisplays.display.ui.KonfettiOverlay
import com.zorindisplays.display.ui.components.AnimatedAmountText
import com.zorindisplays.display.ui.components.GoldShineText
import com.zorindisplays.display.ui.theme.DefaultTextStyle
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.zorindisplays.display.ui.WinKonfettiOverlay
import com.zorindisplays.display.ui.components.TableBackground
import com.zorindisplays.display.ui.theme.JackpotTopAmountPadding
import kotlinx.coroutines.launch
import com.zorindisplays.display.util.ApkUpdater
import kotlinx.coroutines.coroutineScope

@Composable
fun MainScreen(
    imageLoader: ImageLoader,
    vm: GameViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val deckMode by vm.deckMode.collectAsState()
    val fixedRtpInput by vm.fixedRtpInput.collectAsState()
    val stats by vm.stats.collectAsState()

    var showDeckModeDialog by remember { mutableStateOf(false) }
    var tempDeckMode by remember(showDeckModeDialog, deckMode) { mutableStateOf(deckMode) }
    var tempRtpInput by remember(showDeckModeDialog, fixedRtpInput) { mutableStateOf(fixedRtpInput) }

    val showTopTokens = state is UiState.AmountEntry || state is UiState.Ready

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val updater = remember { ApkUpdater(context) }

    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateCode by remember { mutableStateOf("") }
    var updateStatus by remember { mutableStateOf("") }
    var updateProgress by remember { mutableStateOf(0) }
    var updateInProgress by remember { mutableStateOf(false) }
    val tokensPainter = painterResource(R.drawable.tokens)

    val isFixedRtpMode = deckMode == DeckMode.FIXED_RTP

    val trophyGlowMain = if (isFixedRtpMode) {
        Color(0x8859A8FF)
    } else {
        Color(0x88FFD54A)
    }

    val trophyGlowSecondary = if (isFixedRtpMode) {
        Color(0x224C8DFF)
    } else {
        Color(0x33FFD54A)
    }

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }

    LaunchedEffect(Unit) {
        vm.loadSettings(context)
    }

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
                    Key.R -> { showDeckModeDialog = true; true }
                    Key.U -> { showUpdateDialog = true; true }

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
        TableBackground(isFixedRtp = deckMode == DeckMode.FIXED_RTP)


        AnimatedVisibility(
            visible = showTopTokens,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(550)
            ) + fadeIn(
                animationSpec = tween(550)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(380)
            ) + fadeOut(
                animationSpec = tween(380)
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(40.dp)
        ) {
            Image(
                painter = tokensPainter,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(180.dp, 115.dp)
                    .alpha(0.9f)
            )
        }

        AnimatedVisibility(
            visible = showTopTokens,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(550)
            ) + fadeIn(
                animationSpec = tween(550)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(380)
            ) + fadeOut(
                animationSpec = tween(380)
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(40.dp)
        ) {
            Image(
                painter = tokensPainter,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .scale(scaleX = -1f, scaleY = 1f)
                    .size(180.dp, 115.dp)
                    .alpha(0.9f)
            )
        }

        when (val st = state) {
            UiState.Idle -> IdleView()

            is UiState.AmountEntry -> AmountEntryView(raw = st.raw)

            else -> {
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

                RoundView(
                    amount = amount,
                    cards = cards,
                    revealedCount = revealedCount,
                    imageLoader = imageLoader,
                    isLoseScreen = st is UiState.Lost,
                    isWonScreen = st is UiState.Won
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
            var showTrophy by remember { mutableStateOf(false) }

            LaunchedEffect(isWon) {
                showTrophy = false
                kotlinx.coroutines.delay(250)
                showTrophy = true
            }

            if (showTrophy) {
                val trophyOffsetY = remember { Animatable(-900f) }
                val trophyScale = remember { Animatable(0.55f) }
                val trophyAlpha = remember { Animatable(0f) }
                val trophyRotation = remember { Animatable(-10f) }

                val glowAlpha = remember { Animatable(0f) }
                val flashAlpha = remember { Animatable(0f) }

                val sparkLeftAlpha = remember { Animatable(0f) }
                val sparkRightAlpha = remember { Animatable(0f) }
                val sparkTopAlpha = remember { Animatable(0f) }

                val sparkLeftOffset = remember { Animatable(0f) }
                val sparkRightOffset = remember { Animatable(0f) }
                val sparkTopOffset = remember { Animatable(0f) }

                LaunchedEffect(Unit) {
                    coroutineScope {
                        launch {
                            trophyAlpha.animateTo(1f, tween(220))
                        }

                        launch {
                            glowAlpha.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(700, easing = FastOutSlowInEasing)
                            )
                        }

                        launch {
                            flashAlpha.animateTo(1f, tween(120))
                            flashAlpha.animateTo(0f, tween(260))
                        }

                        launch {
                            trophyRotation.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(700, easing = FastOutSlowInEasing)
                            )
                        }

                        trophyOffsetY.animateTo(
                            targetValue = 40f,
                            animationSpec = tween(
                                durationMillis = 650,
                                easing = FastOutSlowInEasing
                            )
                        )

                        trophyOffsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )

                        launch {
                            trophyScale.animateTo(
                                targetValue = 1.08f,
                                animationSpec = tween(500, easing = FastOutSlowInEasing)
                            )
                            trophyScale.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            )
                        }

                        launch {
                            kotlinx.coroutines.delay(120)
                            sparkLeftAlpha.animateTo(1f, tween(100))
                            sparkLeftOffset.animateTo(-30f, tween(450, easing = FastOutSlowInEasing))
                            sparkLeftAlpha.animateTo(0f, tween(220))
                        }

                        launch {
                            kotlinx.coroutines.delay(170)
                            sparkRightAlpha.animateTo(1f, tween(100))
                            sparkRightOffset.animateTo(30f, tween(450, easing = FastOutSlowInEasing))
                            sparkRightAlpha.animateTo(0f, tween(220))
                        }

                        launch {
                            kotlinx.coroutines.delay(90)
                            sparkTopAlpha.animateTo(1f, tween(100))
                            sparkTopOffset.animateTo(-26f, tween(420, easing = FastOutSlowInEasing))
                            sparkTopAlpha.animateTo(0f, tween(220))
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1000f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.18f))
                    )

                    WinKonfettiOverlay(
                        modifier = Modifier
                            .fillMaxSize()
                    )

                    Canvas(
                        modifier = Modifier
                            .size(560.dp)
                            .graphicsLayer { alpha = glowAlpha.value }
                    ) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    trophyGlowMain,
                                    trophyGlowSecondary,
                                    Color.Transparent
                                )
                            ),
                            radius = size.minDimension * 0.48f
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = flashAlpha.value }
                            .background(Color.White.copy(alpha = 0.35f))
                    )

                    Image(
                        painter = painterResource(R.drawable.spark_star),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = (-120).dp + sparkLeftOffset.value.dp, y = (-90).dp)
                            .graphicsLayer { alpha = sparkLeftAlpha.value }
                            .size(42.dp)
                    )

                    Image(
                        painter = painterResource(R.drawable.spark_star),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = 120.dp + sparkRightOffset.value.dp, y = (-70).dp)
                            .graphicsLayer { alpha = sparkRightAlpha.value }
                            .size(36.dp)
                    )

                    Image(
                        painter = painterResource(R.drawable.spark_star),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(y = (-180).dp + sparkTopOffset.value.dp)
                            .graphicsLayer { alpha = sparkTopAlpha.value }
                            .size(48.dp)
                    )

                    Image(
                        painter = painterResource(R.drawable.trophy),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(y = trophyOffsetY.value.dp + 20.dp)
                            .graphicsLayer {
                                alpha = trophyAlpha.value
                                scaleX = trophyScale.value
                                scaleY = trophyScale.value
                                rotationZ = trophyRotation.value
                            }
                            .size(420.dp)
                    )
                }
            }
        }


        val bottomOverlayText = when (val st = state) {
            is UiState.Ready -> "PRESS START TO BEGIN"
            is UiState.Playing -> if (st.awaitingGuess) "HIGHER OR LOWER" else ""
            is UiState.Lost -> "BETTER LUCK NEXT TIME!"
            is UiState.Won -> "YOU WON!"
            else -> ""
        }

        if (bottomOverlayText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2000f),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    if (state is UiState.Won) {
                        GoldShineText(
                            text = bottomOverlayText,
                            fontSize = 36.sp,
                            strokeWidth = 6f
                        )
                    } else {
                        BasicText(
                            text = bottomOverlayText,
                            style = DefaultTextStyle.copy(
                                fontSize = 36.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        }

        val playCoinSound = (state as? UiState.Playing)?.playCoinSound == true
        val context = LocalContext.current
        var coinsPlayer: MediaPlayer? by remember { mutableStateOf(null) }

        LaunchedEffect(playCoinSound) {
            if (playCoinSound) {
                if (coinsPlayer?.isPlaying == true) return@LaunchedEffect
                coinsPlayer?.release()
                coinsPlayer = MediaPlayer.create(context, R.raw.coins)
                coinsPlayer?.setOnCompletionListener { mp ->
                    mp.release()
                    coinsPlayer = null
                }
                coinsPlayer?.start()
                vm.onCoinSoundPlayed()
            }
        }

        if (state is UiState.Won && (state as UiState.Won).playWinnerSound) {
            LaunchedEffect((state as UiState.Won).amount) {
                MediaPlayer.create(context, R.raw.win).apply {
                    setOnCompletionListener { release() }
                    start()
                }
                vm.onWinnerSoundPlayed()
            }
        }

        if (showDeckModeDialog) {
            val rtpValue = tempRtpInput.replace(',', '.').toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
            val winChancePercent = rtpValue / 16.0
            val oneIn = if (winChancePercent > 0.0) 100.0 / winChancePercent else null

            val currentFactRtp = if (stats.totalIn > 0L) {
                stats.totalOut.toDouble() * 100.0 / stats.totalIn.toDouble()
            } else {
                0.0
            }

            AlertDialog(
                onDismissRequest = { showDeckModeDialog = false },
                title = {
                    Text("Deck Mode")
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Games: ${stats.gamesCount}    Wins: ${stats.winsCount}"
                        )

                        Text(
                            text = "IN: ${formatAmount(stats.totalIn)}    OUT: ${formatAmount(stats.totalOut)}    RTP: ${"%.2f".format(currentFactRtp)}%"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempDeckMode = DeckMode.RANDOM_DECK },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tempDeckMode == DeckMode.RANDOM_DECK,
                                onClick = { tempDeckMode = DeckMode.RANDOM_DECK }
                            )
                            Text("Random Deck")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = tempDeckMode == DeckMode.FIXED_RTP,
                                onClick = { tempDeckMode = DeckMode.FIXED_RTP }
                            )

                            OutlinedTextField(
                                value = tempRtpInput,
                                onValueChange = { newValue ->
                                    val normalized = newValue.replace(',', '.')
                                    if (normalized.count { it == '.' } <= 1) {
                                        val filtered = buildString {
                                            normalized.forEach { ch ->
                                                if (ch.isDigit() || ch == '.') append(ch)
                                            }
                                        }

                                        val parts = filtered.split('.')
                                        tempRtpInput = when {
                                            parts.size == 1 -> parts[0].take(6)
                                            else -> {
                                                val intPart = parts[0].take(6)
                                                val fracPart = parts[1].take(2)
                                                "$intPart.$fracPart"
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.width(120.dp),
                                singleLine = true,
                                label = { Text("Fixed RTP") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )

                            Text(
                                text = if (oneIn != null) {
                                    "Win: ${"%.2f".format(winChancePercent)}% (~1 in ${"%.2f".format(oneIn)})"
                                } else {
                                    "Win: 0.00%"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            vm.setDeckMode(tempDeckMode, context)
                            vm.commitFixedRtp(tempRtpInput, context)
                            showDeckModeDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeckModeDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showUpdateDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!updateInProgress) {
                        showUpdateDialog = false
                        updateStatus = ""
                        updateProgress = 0
                    }
                },
                title = {
                    Text("Software Update")
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Current version: $versionName")

                        OutlinedTextField(
                            value = updateCode,
                            onValueChange = {
                                updateCode = it.filter { ch -> ch.isDigit() }
                                updateStatus = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Update code") },
                            singleLine = true,
                            enabled = !updateInProgress,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            )
                        )

                        if (updateInProgress) {
                            Text("Downloading: $updateProgress%")
                        }

                        if (updateStatus.isNotEmpty()) {
                            Text(updateStatus)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !updateInProgress,
                        onClick = {
                            val code = updateCode.trim()
                            if (code.isEmpty()) {
                                updateStatus = "Enter code"
                                return@Button
                            }

                            scope.launch {
                                try {
                                    updateInProgress = true
                                    updateProgress = 0
                                    updateStatus = "Preparing update..."

                                    val url = "https://nabsky.bitbucket.io/higherlower/$code.apk"

                                    updater.downloadAndInstall(
                                        url = url,
                                        fileName = "higherlower_update.apk",
                                        onProgress = { percent ->
                                            updateProgress = percent
                                        }
                                    )

                                    updateStatus = "Starting install..."
                                    showUpdateDialog = false
                                    updateStatus = ""
                                    updateProgress = 0
                                    updateCode = ""
                                } catch (t: Throwable) {
                                    updateStatus = t.message ?: "Update failed"
                                } finally {
                                    updateInProgress = false
                                }
                            }
                        }
                    ) {
                        Text("Update")
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !updateInProgress,
                        onClick = {
                            showUpdateDialog = false
                            updateStatus = ""
                            updateProgress = 0
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
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
                .padding(top = JackpotTopAmountPadding)
        ) {
            GoldShineText(
                text = formatAmount(raw.toLongOrNull() ?: 0L),
                fontSize = 72.sp,
                strokeWidth = 20f
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
    imageLoader: ImageLoader,
    isLoseScreen: Boolean,
    isWonScreen: Boolean
) {
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
                    .padding(top = JackpotTopAmountPadding)
            ) {
                AnimatedVisibility(
                    visible = showAmount,
                    enter = fadeIn(),
                    exit = fadeOut(animationSpec = tween(400))
                ) {
                    AnimatedAmountText(
                        targetAmount = amount,
                        format = ::formatAmount,
                        fontSize = 72.sp,
                        strokeWidth = 20f,
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
    var shownFaceUp by remember { mutableStateOf(faceUp) }

    LaunchedEffect(faceUp) {
        if (shownFaceUp == faceUp) return@LaunchedEffect

        rotation.animateTo(
            targetValue = 90f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )

        MediaPlayer.create(context, R.raw.flick).apply {
            setOnCompletionListener { release() }
            start()
        }

        shownFaceUp = faceUp
        rotation.snapTo(-90f)

        rotation.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
    }

    val url = if (shownFaceUp && card != null) card.assetUrl() else cardBackAssetUrl()

    val isAnimating = kotlin.math.abs(rotation.value) > 0.5f

    Box(
        modifier = modifier
            .then(
                if (!isAnimating) {
                    Modifier.shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(10.dp),
                        clip = false
                    )
                } else {
                    Modifier
                }
            )
            .graphicsLayer {
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

private fun formatAmount(value: Long): String {
    val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ' '
    }
    return DecimalFormat("#,###", symbols).format(value)
}