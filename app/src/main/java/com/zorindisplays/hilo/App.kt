package com.zorindisplays.hilo

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
import com.zorindisplays.hilo.audio.GameSoundManager
import com.zorindisplays.hilo.model.UiState
import com.zorindisplays.hilo.ui.GameViewModel
import com.zorindisplays.hilo.ui.buildSvgImageLoader
import com.zorindisplays.hilo.ui.screens.MainScreen

@Composable
fun App(
    vm: GameViewModel = viewModel()
) {
    val context = LocalContext.current
    val imageLoader = remember { buildSvgImageLoader(context) }
    val state by vm.state.collectAsState()

    val soundManager = remember { GameSoundManager(context) }

    DisposableEffect(Unit) {
        onDispose {
            soundManager.release()
        }
    }

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
            vm = vm,
            soundManager = soundManager
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

    if (state is UiState.Playing && (state as UiState.Playing).playCoinSound) {
        LaunchedEffect((state as UiState.Playing).confettiTick) {
            soundManager.playCoin()
            vm.onCoinSoundPlayed()
        }
    }

    if (state is UiState.Won && (state as UiState.Won).playWinnerSound) {
        LaunchedEffect((state as UiState.Won).amount) {
            soundManager.playWin()
            vm.onWinnerSoundPlayed()
        }
    }

    if (state is UiState.Ready && (state as UiState.Ready).playRegisterSound) {
        LaunchedEffect((state as UiState.Ready).amount) {
            soundManager.playRegister()
            vm.onRegisterSoundPlayed()
        }
    }

    if (state is UiState.Won) {
        LaunchedEffect("kalimba_start") {
            kotlinx.coroutines.delay(3000)
            soundManager.startKalimbaLoop()
        }
    }

    if (state !is UiState.Won) {
        LaunchedEffect("kalimba_stop") {
            soundManager.stopKalimbaLoop()
        }
    }
}