package com.zorindisplays.display

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.zorindisplays.display.ui.buildSvgImageLoader
import com.zorindisplays.display.ui.screens.MainScreen
import com.zorindisplays.display.ui.theme.DefaultBackground

@Composable
fun App() {
    val context = LocalContext.current
    val imageLoader = remember { buildSvgImageLoader(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DefaultBackground)
    ) {
        MainScreen(imageLoader = imageLoader)
    }
}
