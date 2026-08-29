package com.rick.cardwar.audio

/**
 * Sound effects the game can trigger. Kept as an interface so the ViewModel stays
 * testable on the JVM, where the Android [GameSoundPlayer] cannot be constructed.
 */
interface GameSounds {
    fun playSelected(enabled: Boolean)

    fun playPlaced(enabled: Boolean)

    fun playTurned(enabled: Boolean)

    fun release()
}
