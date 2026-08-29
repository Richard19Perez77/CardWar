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

/** How a hand of cards is arranged in its slot. */
enum class HandLayout {
    /** Single horizontal row — the portrait strips above and below the board. */
    Row,

    /** Three cards on top, the rest centred below — the landscape panels beside the board. */
    GridThreeTwo,
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
    layout: HandLayout,
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
            handRows(cards, layout).forEach { rowCards ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PlayAreaLayout.SlotGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    rowCards.forEach { card ->
                        val selected = selectedCardId == card.id && !faceDown
                        val nudge = if (selected) nudgeTowardBoard(player, layout) else DpOffset.Zero
                        CardFace(
                            card = if (faceDown) null else card,
                            owner = player,
                            selected = selected,
                            modifier = Modifier
                                .size(cardSize)
                                .offset(x = nudge.x, y = nudge.y)
                                .clickable(enabled = isCurrentPlayer) { onCardClick(card.id) },
                        )
                    }
                }
            }
        }
    }
}

private fun handRows(cards: List<PlayingCard>, layout: HandLayout): List<List<PlayingCard>> {
    val rows = when (layout) {
        HandLayout.Row -> listOf(cards)
        HandLayout.GridThreeTwo -> listOf(
            cards.take(PlayAreaLayout.LandscapeRowSize),
            cards.drop(PlayAreaLayout.LandscapeRowSize),
        )
    }
    return rows.filter { it.isNotEmpty() }
}

/** A selected card leans towards the board, hinting at where it is about to go. */
private fun nudgeTowardBoard(player: PlayerId, layout: HandLayout): DpOffset = when (layout) {
    // Portrait stacks the opponent above the board and you below it.
    HandLayout.Row -> when (player) {
        PlayerId.One -> DpOffset(0.dp, -SelectedNudge)
        PlayerId.Two -> DpOffset(0.dp, SelectedNudge)
        PlayerId.None -> DpOffset.Zero
    }
    // Landscape puts the board between the two hands.
    HandLayout.GridThreeTwo -> when (player) {
        PlayerId.One -> DpOffset(SelectedNudge, 0.dp)
        PlayerId.Two -> DpOffset(-SelectedNudge, 0.dp)
        PlayerId.None -> DpOffset.Zero
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
