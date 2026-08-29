package com.rick.cardwar.game.model

/**
 * Tunable rule constants shared by the engine, the default [GameState], and tests.
 * The board has nine slots; the center is dealt face up, leaving eight placements.
 */
object GameRules {
    const val HandSize = 5
    const val StartingScore = 5
    const val PlacementsPerMatch = 8
}
