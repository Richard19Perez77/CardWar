package com.rick.cardwar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.rick.cardwar.game.model.PlayerId
import com.rick.cardwar.game.model.PlayingCard

private val HandGap = PlayAreaLayout.SlotGap
private val SelectedNudge = 8.dp
private const val TopRowSize = PlayAreaLayout.HandTopRowSize

/** How a hand of cards is arranged in its slot. */
enum class HandLayout {
    /** Single horizontal row — portrait side strips. */
    Row,

    /** Three cards on top, remainder centered below — landscape panels beside the board. */
    GridThreeTwo,
}

@Composable
fun PlayerHand(
    cards: List<PlayingCard>,
    player: PlayerId,
    selectedCardId: Int?,
    isCurrentPlayer: Boolean,
    layout: HandLayout,
    onCardClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    faceDown: Boolean = false,
    cardSize: DpSize? = null,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        if (cards.isEmpty()) return@BoxWithConstraints

        when (layout) {
            HandLayout.Row -> RowHandContent(
                cards = cards,
                player = player,
                selectedCardId = selectedCardId,
                isCurrentPlayer = isCurrentPlayer,
                faceDown = faceDown,
                onCardClick = onCardClick,
                cardSize = cardSize,
            )

            HandLayout.GridThreeTwo -> GridHandContent(
                cards = cards,
                player = player,
                selectedCardId = selectedCardId,
                isCurrentPlayer = isCurrentPlayer,
                faceDown = faceDown,
                onCardClick = onCardClick,
                cardSize = cardSize,
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.RowHandContent(
    cards: List<PlayingCard>,
    player: PlayerId,
    selectedCardId: Int?,
    isCurrentPlayer: Boolean,
    faceDown: Boolean,
    onCardClick: (Int) -> Unit,
    cardSize: DpSize?,
) {
    val computed = rowCardSize(cardCount = cards.size)
        .coerceAtMost(rowCardSize(cardCount = PlayAreaLayout.HandFullRowSize))
    val resolved = cardSize?.let { computed.coerceAtMost(it) } ?: computed
    Row(
        horizontalArrangement = Arrangement.spacedBy(HandGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cards.forEach { card ->
            HandCard(
                card = card,
                player = player,
                selected = selectedCardId == card.id,
                enabled = isCurrentPlayer,
                size = resolved,
                faceDown = faceDown,
                nudgeX = nudgeTowardBoard(player, HandLayout.Row),
                nudgeY = 0.dp,
                onClick = { onCardClick(card.id) },
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.GridHandContent(
    cards: List<PlayingCard>,
    player: PlayerId,
    selectedCardId: Int?,
    isCurrentPlayer: Boolean,
    faceDown: Boolean,
    onCardClick: (Int) -> Unit,
    cardSize: DpSize?,
) {
    val topRow = cards.take(TopRowSize)
    val bottomRow = cards.drop(TopRowSize)
    val fullHandSize = gridCardSize(topCount = TopRowSize, bottomCount = 2)
    val computed = gridCardSize(
        topCount = topRow.size,
        bottomCount = bottomRow.size,
    ).coerceAtMost(fullHandSize)
    val resolved = cardSize?.let { computed.coerceAtMost(it) } ?: computed
    val nudgeX = nudgeTowardBoard(player, HandLayout.GridThreeTwo)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HandGap),
    ) {
        HandRow(
            cards = topRow,
            player = player,
            selectedCardId = selectedCardId,
            isCurrentPlayer = isCurrentPlayer,
            cardSize = resolved,
            faceDown = faceDown,
            nudgeX = nudgeX,
            onCardClick = onCardClick,
        )
        if (bottomRow.isNotEmpty()) {
            HandRow(
                cards = bottomRow,
                player = player,
                selectedCardId = selectedCardId,
                isCurrentPlayer = isCurrentPlayer,
                cardSize = resolved,
                faceDown = faceDown,
                nudgeX = nudgeX,
                onCardClick = onCardClick,
            )
        }
    }
}

@Composable
private fun HandRow(
    cards: List<PlayingCard>,
    player: PlayerId,
    selectedCardId: Int?,
    isCurrentPlayer: Boolean,
    cardSize: DpSize,
    faceDown: Boolean,
    nudgeX: Dp,
    onCardClick: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(HandGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cards.forEach { card ->
            HandCard(
                card = card,
                player = player,
                selected = selectedCardId == card.id,
                enabled = isCurrentPlayer,
                size = cardSize,
                faceDown = faceDown,
                nudgeX = nudgeX,
                nudgeY = 0.dp,
                onClick = { onCardClick(card.id) },
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.rowCardSize(cardCount: Int): DpSize {
    val gaps = HandGap * (cardCount - 1).coerceAtLeast(0)
    val width = (maxWidth - gaps) / cardCount
    val height = width / CardAspectRatio
    val fittedHeight = height.coerceAtMost(maxHeight)
    val fittedWidth = fittedHeight * CardAspectRatio
    return DpSize(fittedWidth, fittedHeight)
}

@Composable
private fun BoxWithConstraintsScope.gridCardSize(
    topCount: Int,
    bottomCount: Int,
): DpSize {
    val rowCount = if (bottomCount > 0) 2 else 1
    val rowGaps = HandGap * (rowCount - 1)
    val maxHeightPerRow = (maxHeight - rowGaps) / rowCount

    val maxWFromWidth = rowMaxCardWidth(topCount, maxWidth)
    val bottomMaxW = if (bottomCount > 0) rowMaxCardWidth(bottomCount, maxWidth) else maxWFromWidth
    val maxWFromWidthConstraint = minOf(maxWFromWidth, bottomMaxW)
    val maxWFromHeight = maxHeightPerRow * CardAspectRatio

    var width = minOf(maxWFromWidthConstraint, maxWFromHeight)
    var height = width / CardAspectRatio

    if (height > maxHeightPerRow) {
        height = maxHeightPerRow
        width = height * CardAspectRatio
    }

    return DpSize(width, height)
}

private fun rowMaxCardWidth(cardCount: Int, rowWidth: Dp): Dp {
    if (cardCount <= 0) return rowWidth
    val gaps = HandGap * (cardCount - 1)
    return (rowWidth - gaps) / cardCount
}

private fun DpSize.coerceAtMost(max: DpSize): DpSize {
    if (width <= max.width && height <= max.height) return this
    val scale = minOf(max.width / width, max.height / height)
    return DpSize(width * scale, height * scale)
}

private fun nudgeTowardBoard(player: PlayerId, layout: HandLayout): Dp = when (layout) {
    HandLayout.Row -> when (player) {
        PlayerId.One -> -SelectedNudge
        PlayerId.Two -> SelectedNudge
        PlayerId.None -> 0.dp
    }

    HandLayout.GridThreeTwo -> when (player) {
        PlayerId.One -> SelectedNudge
        PlayerId.Two -> -SelectedNudge
        PlayerId.None -> 0.dp
    }
}

@Composable
private fun HandCard(
    card: PlayingCard,
    player: PlayerId,
    selected: Boolean,
    enabled: Boolean,
    size: DpSize,
    faceDown: Boolean,
    nudgeX: Dp,
    nudgeY: Dp,
    onClick: () -> Unit,
) {
    val appliedNudgeX = if (!selected || faceDown) 0.dp else nudgeX
    val appliedNudgeY = if (!selected || faceDown) 0.dp else nudgeY
    CardFace(
        card = if (faceDown) null else card,
        owner = player,
        selected = selected && !faceDown,
        modifier = Modifier
            .size(size)
            .offset(x = appliedNudgeX, y = appliedNudgeY)
            .clickable(enabled = enabled, onClick = onClick),
    )
}
