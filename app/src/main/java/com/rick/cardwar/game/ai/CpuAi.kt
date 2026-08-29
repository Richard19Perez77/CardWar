package com.rick.cardwar.game.ai

import com.rick.cardwar.game.model.BoardSlot
import com.rick.cardwar.game.model.GameState
import com.rick.cardwar.game.model.PlayerId
import com.rick.cardwar.game.model.PlayingCard
import kotlin.random.Random

data class CpuMove(
    val cardId: Int,
    val slot: BoardSlot,
)

object CpuAi {

    /**
     * Looks for any placement that flips a Player 1 card, checking cards and slots in a
     * random order so repeated matches do not play out identically. Falls back to a
     * random legal move when nothing can be captured.
     */
    fun chooseMove(state: GameState, random: Random = Random.Default): CpuMove? {
        val cards = state.player2Hand
        val emptySlots = state.emptySlots
        if (cards.isEmpty() || emptySlots.isEmpty()) return null

        for (card in cards.shuffled(random)) {
            for (slot in emptySlots.shuffled(random)) {
                if (wouldCapturePlayer1(card, slot, state)) {
                    return CpuMove(cardId = card.id, slot = slot)
                }
            }
        }

        return CpuMove(
            cardId = cards.random(random).id,
            slot = emptySlots.random(random),
        )
    }

    internal fun wouldCapturePlayer1(
        card: PlayingCard,
        slot: BoardSlot,
        state: GameState,
    ): Boolean {
        for (neighbor in slot.orthogonalNeighbors) {
            val placed = state.board[neighbor] ?: continue
            if (placed.owner == PlayerId.One && card.rank.value > placed.card.rank.value) {
                return true
            }
        }
        return false
    }
}
