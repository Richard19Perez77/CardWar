package com.rick.cardwar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.rick.cardwar.game.model.PlayerId
import com.rick.cardwar.game.model.PlayingCard
import com.rick.cardwar.ui.theme.HudScrim
import com.rick.cardwar.ui.theme.TurnHighlight

private val SelectedNudge = 8.dp
private val PanelShape = RoundedCornerShape(12.dp)

/**
 * Where a hand sits relative to the board. This fixes both how many cards fit on a row and
 * which way a selected card leans, so the lean always points at the board.
 */
enum class HandSlot(val cardsPerRow: Int, val lean: DpOffset) {
    /** Portrait, above the board. */
    Above(PlayAreaLayout.PortraitRowSize, DpOffset(0.dp, SelectedNudge)),

    /** Portrait, below the board and within thumb reach. */
    Below(PlayAreaLayout.PortraitRowSize, DpOffset(0.dp, -SelectedNudge)),

    /** Landscape, in the left gutter. */
    Left(PlayAreaLayout.LandscapeRowSize, DpOffset(SelectedNudge, 0.dp)),

    /** Landscape, in the right gutter. */
    Right(PlayAreaLayout.LandscapeRowSize, DpOffset(-SelectedNudge, 0.dp)),
}

/**
 * A player's hand, framed by a panel that lights up on that player's turn. Cards are drawn
 * at [cardSize] so a hand never outgrows the board it sits next to.
 */
@Composable
fun PlayerHand(
    cards: List<PlayingCard>,
    player: PlayerId,
    selectedCardId: Int?,
    isCurrentPlayer: Boolean,
    slot: HandSlot,
    cardSize: DpSize,
    onCardClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    faceDown: Boolean = false,
) {
    TurnPanel(active = isCurrentPlayer, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PlayAreaLayout.SlotGap),
        ) {
            cards.chunked(slot.cardsPerRow).forEach { rowCards ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PlayAreaLayout.SlotGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    rowCards.forEach { card ->
                        val selected = selectedCardId == card.id && !faceDown
                        val lean = if (selected) slot.lean else DpOffset.Zero
                        CardFace(
                            card = if (faceDown) null else card,
                            owner = player,
                            selected = selected,
                            modifier = Modifier
                                .size(cardSize)
                                .offset(x = lean.x, y = lean.y)
                                .clickable(enabled = isCurrentPlayer) { onCardClick(card.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Frames a hand the way the HUD and score bar are framed: a dark scrim that lifts the cards
 * off the board art, outlined while it is that player's turn. Both the scrim and the outline
 * are flat colours rather than tints of the player's accent, so the two hands get the same
 * treatment against a background that is already teal.
 */
@Composable
private fun TurnPanel(
    active: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val fill by animateColorAsState(
        targetValue = if (active) HudScrim else Color.Transparent,
        label = "handPanelFill",
    )
    val outline by animateColorAsState(
        targetValue = if (active) TurnHighlight else Color.Transparent,
        label = "handPanelOutline",
    )
    Box(
        modifier = modifier
            .clip(PanelShape)
            .background(fill)
            .border(2.dp, outline, PanelShape)
            .padding(PlayAreaLayout.PanelPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
