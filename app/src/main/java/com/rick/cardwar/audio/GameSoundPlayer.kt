package com.rick.cardwar.audio

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Plays short system tones. The original card sound effects are gone, and the game never
 * had music, so built-in tones avoid shipping audio assets.
 */
class GameSoundPlayer : GameSounds {
    private val toneGenerator = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME)
    } catch (_: RuntimeException) {
        null
    }

    override fun playSelected(enabled: Boolean) {
        play(ToneGenerator.TONE_PROP_BEEP, SELECT_DURATION_MS, enabled)
    }

    override fun playPlaced(enabled: Boolean) {
        play(ToneGenerator.TONE_PROP_BEEP2, PLACE_DURATION_MS, enabled)
    }

    override fun playTurned(enabled: Boolean) {
        play(ToneGenerator.TONE_PROP_ACK, CAPTURE_DURATION_MS, enabled)
    }

    private fun play(tone: Int, durationMs: Int, enabled: Boolean) {
        if (!enabled) return
        try {
            toneGenerator?.startTone(tone, durationMs)
        } catch (_: RuntimeException) {
            // Device audio is busy or unavailable.
        }
    }

    override fun release() {
        toneGenerator?.release()
    }

    private companion object {
        const val TONE_VOLUME = 70
        const val SELECT_DURATION_MS = 90
        const val PLACE_DURATION_MS = 80
        const val CAPTURE_DURATION_MS = 160
    }
}
