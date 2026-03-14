package com.zorindisplays.hilo.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.zorindisplays.hilo.R

class GameSoundManager(context: Context) {

    private val appContext = context.applicationContext

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val coinSoundId = soundPool.load(appContext, R.raw.coins, 1)
    private val winSoundId = soundPool.load(appContext, R.raw.win, 1)
    private val flipSoundId = soundPool.load(appContext, R.raw.flick, 1)
    private val registerSoundId = soundPool.load(appContext, R.raw.register, 1)

    fun playRegister() {
        soundPool.play(registerSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun playCoin() {
        soundPool.play(coinSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun playWin() {
        soundPool.play(winSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun playFlip() {
        soundPool.play(flipSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}