package com.rick.cardwar.game.model

/**
 * Tunable rule constants shared by the engine, the default [GameState], and tests.
 * The board has nine slots; the center is dealt face up, leaving eight placements.
 */
object GameRules {
    const val HAND_SIZE = 5
    const val STARTING_SCORE = 5
    const val PLACEMENTS_PER_MATCH = 8
}
