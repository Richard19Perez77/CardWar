package com.rick.cardwar.game

import com.rick.cardwar.game.model.BoardSlot
import com.rick.cardwar.game.model.GameState
import com.rick.cardwar.game.model.GameStatus
import com.rick.cardwar.game.model.PlacedCard
import com.rick.cardwar.game.model.PlayerId
import com.rick.cardwar.game.model.isHumanSeat
import kotlin.random.Random

sealed interface GameAction {
    data class StartMatch(val random: Random = Random.Default) : GameAction
    data class SelectCard(val cardId: Int) : GameAction
    data class Place(val slot: BoardSlot, val cardId: Int? = null) : GameAction
    data class SetCpu(val enabled: Boolean) : GameAction
}

object GameEngine {

    const val PlacementsPerMatch = 8
    const val StartingScore = 5

    fun reduce(state: GameState, action: GameAction): GameState = when (action) {
        is GameAction.StartMatch -> startMatch(state, action.random)
        is GameAction.SelectCard -> selectCard(state, action.cardId)
        is GameAction.Place -> place(state, action.slot, action.cardId)
        is GameAction.SetCpu -> state.copy(cpuOpponent = action.enabled)
    }

    fun startMatch(state: GameState, random: Random = Random.Default): GameState {
        val cards = Deck.shuffled(random).toMutableList()
        val center = cards.removeAt(0)
        val player1 = cards.take(5)
        val player2 = cards.drop(5).take(5)
        return state.copy(
            status = GameStatus.Playing,
            board = mapOf(
                BoardSlot.Center to PlacedCard(card = center, owner = PlayerId.None),
            ),
            player1Hand = player1,
            player2Hand = player2,
            currentPlayer = PlayerId.One,
            selectedCardId = null,
            p1Score = StartingScore,
            p2Score = StartingScore,
            placementsThisMatch = 0,
            lastCapturedSlots = emptyList(),
            lastPlacedSlot = null,
        )
    }

    fun selectCard(state: GameState, cardId: Int): GameState {
        if (state.status != GameStatus.Playing) return state
        if (!state.currentPlayer.isHumanSeat) return state
        if (state.handOf(state.currentPlayer).none { it.id == cardId }) return state
        return state.copy(selectedCardId = cardId)
    }

    fun place(
        state: GameState,
        slot: BoardSlot,
        cardId: Int? = null,
    ): GameState {
        if (state.status != GameStatus.Playing) return state
        if (!state.currentPlayer.isHumanSeat) return state
        if (slot in state.board) return state

        val id = cardId ?: state.selectedCardId ?: return state
        val player = state.currentPlayer
        val card = state.handOf(player).find { it.id == id } ?: return state

        val occupied = state.board.toMutableMap()
        occupied[slot] = PlacedCard(card = card, owner = player)

        val captured = mutableListOf<BoardSlot>()
        var p1 = state.p1Score
        var p2 = state.p2Score

        for (neighbor in slot.orthogonalNeighbors) {
            val placed = occupied[neighbor] ?: continue
            if (placed.owner == player) continue
            if (card.rank.value > placed.card.rank.value) {
                occupied[neighbor] = placed.copy(owner = player)
                captured += neighbor
                if (player == PlayerId.One) {
                    p1++
                    p2--
                } else {
                    p1--
                    p2++
                }
            }
        }

        val newHand = state.handOf(player).filter { it.id != id }
        val placements = state.placementsThisMatch + 1
        val finished = placements >= PlacementsPerMatch

        var p1Wins = state.p1GamesWon
        var p2Wins = state.p2GamesWon
        var nextPlayer = if (player == PlayerId.One) PlayerId.Two else PlayerId.One
        var status = GameStatus.Playing

        if (finished) {
            status = GameStatus.Finished
            nextPlayer = PlayerId.None
            when {
                p1 > p2 -> p1Wins++
                p2 > p1 -> p2Wins++
            }
        }

        return state.copy(
            status = status,
            board = occupied,
            player1Hand = if (player == PlayerId.One) newHand else state.player1Hand,
            player2Hand = if (player == PlayerId.Two) newHand else state.player2Hand,
            currentPlayer = nextPlayer,
            selectedCardId = null,
            p1Score = p1,
            p2Score = p2,
            p1GamesWon = p1Wins,
            p2GamesWon = p2Wins,
            placementsThisMatch = placements,
            lastCapturedSlots = captured,
            lastPlacedSlot = slot,
        )
    }
}
