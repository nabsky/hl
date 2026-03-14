package com.zorindisplays.hilo.ui

import android.content.Context
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.svg.SvgDecoder

fun buildSvgImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(SvgDecoder.Factory())
        }
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        // .logger(DebugLogger()) // включи если надо дебажить загрузку
        .build()
}
