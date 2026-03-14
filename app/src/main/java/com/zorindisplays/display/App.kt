package com.zorindisplays.display

import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.zIndex
import com.zorindisplays.display.model.UiState
import com.zorindisplays.display.ui.GameViewModel
import com.zorindisplays.display.ui.buildSvgImageLoader
import com.zorindisplays.display.ui.screens.MainScreen

@Composable
fun App(
    vm: GameViewModel = viewModel()
) {
    val context = LocalContext.current
    val imageLoader = remember { buildSvgImageLoader(context) }
    val state by vm.state.collectAsState()

    val fgRes = when (state) {
        UiState.Idle -> null
        is UiState.Playing -> when ((state as UiState.Playing).revealedCount) {
            1 -> null
            2 -> R.drawable.fg_02
            3 -> R.drawable.fg_03
            4 -> R.drawable.fg_04
            else -> R.drawable.fg_05
        }
        is UiState.Won -> R.drawable.fg_05
        else -> null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MainScreen(
            imageLoader = imageLoader,
            vm = vm
        )

        if(fgRes != null) {
            Image(
                painter = painterResource(fgRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
                contentScale = ContentScale.Crop
            )
        }
    }

    var coinPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var winPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            coinPlayer?.release()
            coinPlayer = null
            winPlayer?.release()
            winPlayer = null
        }
    }

    if (state is UiState.Won && (state as UiState.Won).playWinnerSound) {
        LaunchedEffect((state as UiState.Won).amount) {

            winPlayer?.release()

            val player = MediaPlayer.create(context, R.raw.win)

            if (player != null) {
                winPlayer = player

                player.setOnCompletionListener { mp ->
                    mp.release()
                    if (winPlayer === mp) winPlayer = null
                    vm.onWinnerSoundPlayed()
                }

                player.start()
            } else {
                vm.onWinnerSoundPlayed()
            }
        }
    }

    if (state is UiState.Playing && (state as UiState.Playing).playCoinSound) {
        LaunchedEffect((state as UiState.Playing).confettiTick) {

            coinPlayer?.release()

            val player = MediaPlayer.create(context, R.raw.coins)

            if (player != null) {
                coinPlayer = player

                player.setOnCompletionListener { mp ->
                    mp.release()
                    if (coinPlayer === mp) coinPlayer = null
                    vm.onCoinSoundPlayed()
                }

                player.start()
            } else {
                vm.onCoinSoundPlayed()
            }
        }
    }
}