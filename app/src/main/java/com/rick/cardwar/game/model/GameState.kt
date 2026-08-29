package com.rick.cardwar.game.model

import androidx.compose.runtime.Immutable

enum class GameStatus {
    Idle,
    Playing,
    Finished,
}

/**
 * Whole-game snapshot. Marked [Immutable] because every property is a `val` and the
 * collections are rebuilt on each transition instead of being mutated in place, which
 * lets Compose skip recomposition when the instance is unchanged.
 */
@Immutable
data class GameState(
    val status: GameStatus = GameStatus.Idle,
    val board: Map<BoardSlot, PlacedCard> = emptyMap(),
    val player1Hand: List<PlayingCard> = emptyList(),
    val player2Hand: List<PlayingCard> = emptyList(),
    val currentPlayer: PlayerId = PlayerId.None,
    val selectedCardId: Int? = null,
    val p1Score: Int = GameRules.StartingScore,
    val p2Score: Int = GameRules.StartingScore,
    val p1GamesWon: Int = 0,
    val p2GamesWon: Int = 0,
    val placementsThisMatch: Int = 0,
    val cpuOpponent: Boolean = false,
    val lastCapturedSlots: List<BoardSlot> = emptyList(),
    val lastPlacedSlot: BoardSlot? = null,
) {
    val emptySlots: List<BoardSlot>
        get() = BoardSlot.entries.filter { it !in board }

    fun handOf(player: PlayerId): List<PlayingCard> = when (player) {
        PlayerId.One -> player1Hand
        PlayerId.Two -> player2Hand
        PlayerId.None -> emptyList()
    }
}
