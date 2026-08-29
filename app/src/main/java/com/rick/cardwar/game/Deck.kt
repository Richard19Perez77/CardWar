package com.rick.cardwar.game

import com.rick.cardwar.game.model.PlayingCard
import com.rick.cardwar.game.model.Rank
import com.rick.cardwar.game.model.Suit
import kotlin.random.Random

object Deck {
    fun full(): List<PlayingCard> {
        var id = 0
        return buildList {
            for (suit in Suit.entries) {
                for (rank in Rank.entries) {
                    add(PlayingCard(id = id++, rank = rank, suit = suit))
                }
            }
        }
    }

    fun shuffled(random: Random = Random.Default): List<PlayingCard> =
        full().shuffled(random)
}
