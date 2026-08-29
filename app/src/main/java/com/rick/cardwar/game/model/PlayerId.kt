package com.rick.cardwar.game.model

enum class PlayerId {
    None,
    One,
    Two,
}

val PlayerId.isHumanSeat: Boolean
    get() = this == PlayerId.One || this == PlayerId.Two
