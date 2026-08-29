package com.rick.cardwar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.rick.cardwar.game.model.PlayerId
import com.rick.cardwar.ui.theme.Player1Selected
import com.rick.cardwar.ui.theme.Player2Selected

private val PanelShape = RoundedCornerShape(12.dp)

@Composable
fun HandPanel(
    player: PlayerId,
    active: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val accent = when (player) {
        PlayerId.One -> Player1Selected
        PlayerId.Two -> Player2Selected
        PlayerId.None -> return Box(modifier, contentAlignment = Alignment.Center) { content() }
    }
    Box(
        modifier = modifier
            .then(
                if (active) {
                    Modifier
                        .clip(PanelShape)
                        .background(accent.copy(alpha = 0.10f))
                        .border(2.dp, accent.copy(alpha = 0.55f), PanelShape)
                } else {
                    Modifier
                },
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
