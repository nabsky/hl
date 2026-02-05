package com.zorindisplays.display

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
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

    val bgRes = when (state) {
        UiState.Idle -> R.drawable.bg_00
        is UiState.Playing -> when ((state as UiState.Playing).revealedCount) {
            1 -> R.drawable.bg_01
            2 -> R.drawable.bg_01
            3 -> R.drawable.bg_01
            4 -> R.drawable.bg_01
            else -> R.drawable.bg_01
        }
        is UiState.Won -> R.drawable.bg_01
        else -> R.drawable.bg_01
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

        Image(
            painter = painterResource(bgRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

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
}