package com.rick.cardwar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.rick.cardwar.game.model.BoardSlot
import com.rick.cardwar.game.model.PlacedCard
import com.rick.cardwar.game.model.PlayerId

@Composable
fun BoardGrid(
    board: Map<BoardSlot, PlacedCard>,
    hasSelection: Boolean,
    interactive: Boolean,
    onSlotClick: (BoardSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val gap = 6.dp
        val maxCardWidth = (maxWidth - gap * 2) / 3
        val maxCardHeight = (maxHeight - gap * 2) / 3
        val cardHeight = min(maxCardHeight, maxCardWidth / CardAspectRatio)
        val cardWidth = cardHeight * CardAspectRatio

        Column(
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoardSlot.rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { slot ->
                        val placed = board[slot]
                        BoardCell(
                            placed = placed,
                            clickable = interactive && hasSelection && placed == null,
                            onClick = { onSlotClick(slot) },
                            modifier = Modifier.size(cardWidth, cardHeight),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardCell(
    placed: PlacedCard?,
    clickable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.clickable(enabled = clickable, onClick = onClick),
    ) {
        CardFace(
            card = placed?.card,
            owner = placed?.owner ?: PlayerId.None,
            selected = false,
            modifier = Modifier.matchParentSize(),
        )
    }
}
