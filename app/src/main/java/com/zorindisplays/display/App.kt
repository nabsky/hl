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
            2 -> R.drawable.bg_02
            3 -> R.drawable.bg_03
            4 -> R.drawable.bg_04
            else -> R.drawable.bg_05
        }
        is UiState.Won -> R.drawable.bg_05
        else -> R.drawable.bg_01
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
    }
}