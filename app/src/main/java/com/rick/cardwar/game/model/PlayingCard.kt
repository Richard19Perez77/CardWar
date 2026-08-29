package com.rick.cardwar.game.model

data class PlayingCard(
    val id: Int,
    val rank: Rank,
    val suit: Suit,
)

data class PlacedCard(
    val card: PlayingCard,
    val owner: PlayerId,
)
