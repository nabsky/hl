package com.zorindisplays.hilo.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
    private var kalimbaPlayer: MediaPlayer? = null

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
        kalimbaPlayer?.release()
        kalimbaPlayer = null
        soundPool.release()
    }

    fun startKalimbaLoop() {
        if (kalimbaPlayer != null) return

        kalimbaPlayer = MediaPlayer.create(appContext, R.raw.kalimba_loop).apply {
            isLooping = true
            setVolume(0f, 0f)
            start()
        }

        // fade-in громкости
        Thread {
            val steps = 20
            val maxVolume = 0.6f

            for (i in 1..steps) {
                val v = maxVolume * (i.toFloat() / steps.toFloat())
                kalimbaPlayer?.setVolume(v, v)
                Thread.sleep(50)
            }
        }.start()
    }

    fun stopKalimbaLoop() {
        kalimbaPlayer?.release()
        kalimbaPlayer = null
    }
}