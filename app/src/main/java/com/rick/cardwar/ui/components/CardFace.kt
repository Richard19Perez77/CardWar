package com.rick.cardwar.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rick.cardwar.R
import com.rick.cardwar.game.drawableRes
import com.rick.cardwar.game.model.PlayerId
import com.rick.cardwar.game.model.PlayingCard
import com.rick.cardwar.ui.theme.EmptySlotBorder
import com.rick.cardwar.ui.theme.Player1Border
import com.rick.cardwar.ui.theme.Player1Selected
import com.rick.cardwar.ui.theme.Player2Border
import com.rick.cardwar.ui.theme.Player2Selected
import com.rick.cardwar.ui.theme.UnownedBorder

const val CardAspectRatio = 93f / 120f

@Composable
fun CardFace(
    card: PlayingCard?,
    owner: PlayerId,
    selected: Boolean,
    modifier: Modifier = Modifier,
    borderWidth: Dp = if (selected) 8.dp else 6.dp,
    borderColor: Color = cardBorderColor(
        owner = owner,
        selected = selected,
        empty = card == null && owner == PlayerId.None,
    ),
) {
    // A null card with no owner is a vacant board slot; a null card with an owner is a
    // face-down hand card, which still shows that player's border.
    val isEmptySlot = card == null && owner == PlayerId.None
    val description = when {
        card != null -> "${card.rank.name} of ${card.suit.name}"
        isEmptySlot -> stringResource(R.string.empty_slot)
        else -> stringResource(R.string.face_down_card)
    }

    Box(
        modifier = modifier
            .border(borderWidth, borderColor)
            .padding(borderWidth)
            .background(Color.White),
    ) {
        if (card != null) {
            Image(
                painter = painterResource(card.drawableRes),
                contentDescription = description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        } else {
            Image(
                painter = painterResource(R.drawable.cardbw),
                contentDescription = description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                alpha = 0.9f,
            )
        }
    }
}

/** The colour that identifies a player across hands, board hints and the score bar. */
fun playerAccent(player: PlayerId): Color = when (player) {
    PlayerId.One -> Player1Selected
    PlayerId.Two -> Player2Selected
    PlayerId.None -> UnownedBorder
}

fun cardBorderColor(owner: PlayerId, selected: Boolean, empty: Boolean): Color = when {
    empty -> EmptySlotBorder
    selected && owner == PlayerId.One -> Player1Selected
    selected && owner == PlayerId.Two -> Player2Selected
    owner == PlayerId.One -> Player1Border
    owner == PlayerId.Two -> Player2Border
    else -> UnownedBorder
}
