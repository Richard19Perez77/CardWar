package com.rick.cardwar.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** Shared spacing and sizing for the board and both hands. */
object PlayAreaLayout {
    val SlotGap = 6.dp
    const val HandTopRowSize = 3
    const val HandFullRowSize = 5
    private val MinCardWidth = 44.dp

    /**
     * One card size for landscape: board height drives scale, then gutter width
     * is checked so a three-card hand row still fits beside the board.
     */
    fun landscapeCardSize(maxWidth: Dp, maxHeight: Dp): DpSize {
        val gap = SlotGap
        var cardHeight = (maxHeight - gap * 2) / 3
        var cardWidth = cardHeight * CardAspectRatio
        val gridWidth = cardWidth * 3 + gap * 2
        val gutterWidth = (maxWidth - gridWidth).coerceAtLeast(0.dp) / 2
        val handGaps = SlotGap * (HandTopRowSize - 1)
        val maxHandCardWidth = if (gutterWidth > 0.dp) {
            (gutterWidth - handGaps) / HandTopRowSize
        } else {
            cardWidth
        }
        if (maxHandCardWidth < cardWidth) {
            cardWidth = maxHandCardWidth.coerceAtLeast(MinCardWidth)
            cardHeight = cardWidth / CardAspectRatio
        }
        return DpSize(cardWidth, cardHeight)
    }

    /**
     * One card size for portrait: board area (~52% of height) drives scale, then
     * hand rows are checked so five cards still fit across the width.
     */
    fun portraitCardSize(maxWidth: Dp, maxHeight: Dp): DpSize {
        val gap = SlotGap
        val boardHeightShare = 2.2f / (1f + 2.2f + 1f)
        val boardHeight = maxHeight * boardHeightShare
        var cardHeight = (boardHeight - gap * 2) / 3
        var cardWidth = cardHeight * CardAspectRatio
        if (cardWidth * 3 + gap * 2 > maxWidth) {
            cardWidth = (maxWidth - gap * 2) / 3
            cardHeight = cardWidth / CardAspectRatio
        }
        val handGaps = SlotGap * (HandFullRowSize - 1)
        val maxHandCardWidth = (maxWidth - handGaps) / HandFullRowSize
        if (maxHandCardWidth < cardWidth) {
            cardWidth = maxHandCardWidth.coerceAtLeast(MinCardWidth)
            cardHeight = cardWidth / CardAspectRatio
        }
        return DpSize(cardWidth, cardHeight)
    }
}
