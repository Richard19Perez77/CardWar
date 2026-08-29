package com.rick.cardwar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import com.rick.cardwar.game.model.BoardSlot
import com.rick.cardwar.game.model.PlacedCard
import com.rick.cardwar.game.model.PlayerId

/**
 * The 3x3 play area.
 *
 * @param playableBy the player who can fill an empty slot right now, or null when tapping
 * the board does nothing. Open slots take on that player's colour as a drop hint.
 */
@Composable
fun BoardGrid(
    board: Map<BoardSlot, PlacedCard>,
    playableBy: PlayerId?,
    cardSize: DpSize,
    onSlotClick: (BoardSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(PlayAreaLayout.SlotGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoardSlot.rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(PlayAreaLayout.SlotGap)) {
                row.forEach { slot ->
                    BoardSlotCell(
                        placed = board[slot],
                        playableBy = playableBy,
                        cardSize = cardSize,
                        onClick = { onSlotClick(slot) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardSlotCell(
    placed: PlacedCard?,
    playableBy: PlayerId?,
    cardSize: DpSize,
    onClick: () -> Unit,
) {
    val owner = placed?.owner ?: PlayerId.None
    val openTo = playableBy?.takeIf { placed == null }
    val border by animateColorAsState(
        targetValue = openTo?.let(::playerAccent)
            ?: cardBorderColor(placed?.card, owner, selected = false),
        label = "slotBorder",
    )
    CardFace(
        card = placed?.card,
        owner = owner,
        selected = false,
        borderColor = border,
        modifier = Modifier
            .size(cardSize)
            .clickable(enabled = openTo != null, onClick = onClick),
    )
}
