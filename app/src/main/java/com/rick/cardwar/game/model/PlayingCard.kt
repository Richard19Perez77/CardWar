package com.rick.cardwar.game.model

import androidx.compose.runtime.Immutable

@Immutable
data class PlayingCard(
    val id: Int,
    val rank: Rank,
    val suit: Suit,
)

@Immutable
data class PlacedCard(
    val card: PlayingCard,
    val owner: PlayerId,
)
