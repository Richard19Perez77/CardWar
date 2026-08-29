package com.rick.cardwar.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.rick.cardwar.game.model.GameRules

/** Card sizes for one orientation, measured once so the board and hands stay in proportion. */
@Immutable
data class PlayAreaSizes(val boardCard: DpSize, val handCard: DpSize)

/**
 * Card geometry for the play area.
 *
 * Both orientations size cards from the space the board and hands actually get, so a
 * card is never scaled past the space it has and the board always reads as the focus.
 */
object PlayAreaLayout {
    /** Space between neighbouring cards, in a hand row or on the board. */
    val SlotGap = 6.dp

    /** Breathing room between the board and whatever sits next to it. */
    val BoardPadding = 12.dp

    /** Inset inside a hand panel. Wide enough that a nudged selected card is not clipped. */
    val PanelPadding = 10.dp

    /** Cards across a landscape hand's top row, matching a board row so both share a size. */
    const val LandscapeRowSize = 3

    /** Cards across a portrait hand, which holds the whole hand on one row. */
    const val PortraitRowSize = GameRules.HAND_SIZE

    /** Portrait height split: the board has three rows of cards, each hand has one. */
    const val PortraitBoardWeight = 3f
    const val PortraitHandWeight = 1f

    /** Floor that keeps a card tappable when space runs out on very small screens. */
    private val MinCardWidth = 48.dp

    /**
     * Landscape lays out three columns — hand, board, hand — and each holds three cards
     * across, so a single card size serves all of them.
     */
    fun landscape(maxWidth: Dp, maxHeight: Dp): PlayAreaSizes {
        val widthForCards = maxWidth - BoardPadding * 2 - PanelPadding * 4 - SlotGap * 6
        val fromWidth = widthForCards / (LandscapeRowSize * 3)
        val fromBoardHeight = rowHeight(maxHeight, rows = 3) * CardAspectRatio
        val fromHandHeight = rowHeight(maxHeight - PanelPadding * 2, rows = 2) * CardAspectRatio
        val card = cardOf(minOf(fromWidth, fromBoardHeight, fromHandHeight))
        return PlayAreaSizes(boardCard = card, handCard = card)
    }

    /**
     * Portrait stacks hand, board, hand. A hand holds five cards across but a board row
     * only three, so the board keeps the larger card instead of shrinking to match.
     */
    fun portrait(maxWidth: Dp, maxHeight: Dp): PlayAreaSizes {
        val totalWeight = PortraitBoardWeight + PortraitHandWeight * 2
        val boardHeight = maxHeight * (PortraitBoardWeight / totalWeight) - BoardPadding * 2
        val handHeight = maxHeight * (PortraitHandWeight / totalWeight) - PanelPadding * 2

        val boardFromWidth = (maxWidth - BoardPadding * 2 - SlotGap * 2) / 3
        val boardFromHeight = rowHeight(boardHeight, rows = 3) * CardAspectRatio

        val handFromWidth =
            (maxWidth - PanelPadding * 2 - SlotGap * (PortraitRowSize - 1)) / PortraitRowSize
        val handFromHeight = handHeight * CardAspectRatio

        return PlayAreaSizes(
            boardCard = cardOf(minOf(boardFromWidth, boardFromHeight)),
            handCard = cardOf(minOf(handFromWidth, handFromHeight)),
        )
    }

    private fun rowHeight(available: Dp, rows: Int): Dp =
        (available - SlotGap * (rows - 1)) / rows

    private fun cardOf(width: Dp): DpSize {
        val clamped = width.coerceAtLeast(MinCardWidth)
        return DpSize(clamped, clamped / CardAspectRatio)
    }
}
