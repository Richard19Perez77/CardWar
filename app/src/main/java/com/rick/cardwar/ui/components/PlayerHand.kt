package com.rick.cardwar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.rick.cardwar.game.model.PlayerId
import com.rick.cardwar.game.model.PlayingCard

private val HandGap = 6.dp
private val SelectedNudge = 8.dp

@Composable
fun PlayerHand(
    cards: List<PlayingCard>,
    player: PlayerId,
    selectedCardId: Int?,
    isCurrentPlayer: Boolean,
    vertical: Boolean,
    onCardClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    faceDown: Boolean = false,
) {
    BoxWithConstraints(modifier) {
        if (cards.isEmpty()) return@BoxWithConstraints

        val n = cards.size
        val gaps = HandGap * (n - 1)
        val cardSize = if (vertical) {
            val height = min(maxHeight, (maxHeight - gaps) / n)
            val fromHeight = height * CardAspectRatio
            val width = min(maxWidth, fromHeight)
            DpSize(width, width / CardAspectRatio)
        } else {
            val width = min(maxWidth, (maxWidth - gaps) / n)
            val fromWidth = width / CardAspectRatio
            val height = min(maxHeight, fromWidth)
            DpSize(height * CardAspectRatio, height)
        }

        if (vertical) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(HandGap, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                cards.forEach { card ->
                    HandCard(
                        card = card,
                        player = player,
                        selected = selectedCardId == card.id,
                        enabled = isCurrentPlayer,
                        vertical = true,
                        size = cardSize,
                        faceDown = faceDown,
                        onClick = { onCardClick(card.id) },
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(HandGap, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                cards.forEach { card ->
                    HandCard(
                        card = card,
                        player = player,
                        selected = selectedCardId == card.id,
                        enabled = isCurrentPlayer,
                        vertical = false,
                        size = cardSize,
                        faceDown = faceDown,
                        onClick = { onCardClick(card.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HandCard(
    card: PlayingCard,
    player: PlayerId,
    selected: Boolean,
    enabled: Boolean,
    vertical: Boolean,
    size: DpSize,
    faceDown: Boolean,
    onClick: () -> Unit,
) {
    val nudge = if (!selected || faceDown) {
        0.dp
    } else if (vertical) {
        if (player == PlayerId.One) SelectedNudge else -SelectedNudge
    } else {
        if (player == PlayerId.One) -SelectedNudge else SelectedNudge
    }
    CardFace(
        card = if (faceDown) null else card,
        owner = player,
        selected = selected && !faceDown,
        modifier = Modifier
            .size(size)
            .offset(
                x = if (vertical) nudge else 0.dp,
                y = if (vertical) 0.dp else nudge,
            )
            .clickable(enabled = enabled, onClick = onClick),
    )
}
