package com.rick.cardwar.game.model

enum class GameStatus {
    Idle,
    Playing,
    Finished,
}

data class GameState(
    val status: GameStatus = GameStatus.Idle,
    val board: Map<BoardSlot, PlacedCard> = emptyMap(),
    val player1Hand: List<PlayingCard> = emptyList(),
    val player2Hand: List<PlayingCard> = emptyList(),
    val currentPlayer: PlayerId = PlayerId.None,
    val selectedCardId: Int? = null,
    val p1Score: Int = 5,
    val p2Score: Int = 5,
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

    fun isSelected(cardId: Int): Boolean = selectedCardId == cardId
}
