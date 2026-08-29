package com.rick.cardwar.audio

import android.media.AudioManager
import android.media.ToneGenerator

class GameSoundPlayer {
    private val toneGenerator = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneVolume)
    } catch (_: RuntimeException) {
        null
    }

    fun playSelected(enabled: Boolean) {
        play(ToneGenerator.TONE_PROP_BEEP, SelectDurationMs, enabled)
    }

    fun playPlaced(enabled: Boolean) {
        play(ToneGenerator.TONE_PROP_BEEP2, PlaceDurationMs, enabled)
    }

    fun playTurned(enabled: Boolean) {
        play(ToneGenerator.TONE_PROP_ACK, CaptureDurationMs, enabled)
    }

    private fun play(tone: Int, durationMs: Int, enabled: Boolean) {
        if (!enabled) return
        try {
            toneGenerator?.startTone(tone, durationMs)
        } catch (_: RuntimeException) {
            // Device audio is busy or unavailable.
        }
    }

    fun release() {
        toneGenerator?.release()
    }

    private companion object {
        const val ToneVolume = 70
        const val SelectDurationMs = 90
        const val PlaceDurationMs = 80
        const val CaptureDurationMs = 160
    }
}
