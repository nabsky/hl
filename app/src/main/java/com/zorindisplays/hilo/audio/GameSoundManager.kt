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
    @Volatile
    private var kalimbaRunning = false

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
        kalimbaRunning = false
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

        kalimbaRunning = true

        Thread {
            val steps = 20
            val maxVolume = 0.6f

            // fade-in
            for (i in 1..steps) {
                if (!kalimbaRunning) return@Thread

                val v = maxVolume * (i.toFloat() / steps.toFloat())
                kalimbaPlayer?.setVolume(v, v)
                Thread.sleep(50)
            }

            startEvolvingStereoDrift(baseVolume = maxVolume)
        }.start()
    }

    fun stopKalimbaLoop() {
        kalimbaRunning = false
        kalimbaPlayer?.release()
        kalimbaPlayer = null
    }

    private fun startEvolvingStereoDrift(baseVolume: Float) {
        Thread {
            var phase = 0.0
            var t = 0

            while (kalimbaRunning && kalimbaPlayer != null) {
                // drift со временем становится чуть шире
                val driftAmount = (0.10f + (t / 2000f)).coerceAtMost(0.22f)

                // и очень медленно "дышит" общая громкость
                val volumePulse = (Math.sin(t * 0.015) * 0.05f).toFloat()

                val drift = (Math.sin(phase) * driftAmount).toFloat()

                val left = (baseVolume + volumePulse + drift).coerceIn(0f, 1f)
                val right = (baseVolume + volumePulse - drift).coerceIn(0f, 1f)

                kalimbaPlayer?.setVolume(left, right)

                // drift постепенно чуть ускоряется
                val phaseStep = (0.045 + (t / 20000.0)).coerceAtMost(0.085)
                phase += phaseStep

                t += 1
                Thread.sleep(60)
            }
        }.start()
    }
}