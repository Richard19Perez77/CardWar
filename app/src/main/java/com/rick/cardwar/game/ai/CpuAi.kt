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

    fun chooseMove(state: GameState, random: Random = Random.Default): CpuMove? {
        val cards = state.player2Hand.toMutableList()
        if (cards.isEmpty()) return null

        val emptySlots = state.emptySlots
        if (emptySlots.isEmpty()) return null

        val remainingCards = cards.toMutableList()
        while (remainingCards.isNotEmpty()) {
            val card = remainingCards.removeAt(random.nextInt(remainingCards.size))
            val slots = emptySlots.toMutableList()
            while (slots.isNotEmpty()) {
                val slot = slots.removeAt(random.nextInt(slots.size))
                if (wouldCapturePlayer1(card, slot, state)) {
                    return CpuMove(cardId = card.id, slot = slot)
                }
            }
        }

        val randomCard = cards[random.nextInt(cards.size)]
        val randomSlot = emptySlots[random.nextInt(emptySlots.size)]
        return CpuMove(cardId = randomCard.id, slot = randomSlot)
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
