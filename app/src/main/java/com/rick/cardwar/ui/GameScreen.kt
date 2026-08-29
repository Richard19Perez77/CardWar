package com.rick.cardwar.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rick.cardwar.R
import com.rick.cardwar.game.model.BoardSlot
import com.rick.cardwar.game.model.GameState
import com.rick.cardwar.game.model.GameStatus
import com.rick.cardwar.game.model.PlayerId
import com.rick.cardwar.ui.components.BoardGrid
import com.rick.cardwar.ui.components.GameHud
import com.rick.cardwar.ui.components.PlayerHand
import com.rick.cardwar.ui.theme.CardWarTheme
import com.rick.cardwar.ui.theme.HudScrim
import com.rick.cardwar.ui.theme.Player1Selected
import com.rick.cardwar.ui.theme.Player2Border
import com.rick.cardwar.ui.theme.ScoreText

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    GameScreenContent(
        state = state,
        soundEnabled = soundEnabled,
        onStart = viewModel::startGame,
        onCardClick = viewModel::selectCard,
        onSlotClick = viewModel::tapSlot,
        onCpuChange = viewModel::setCpuEnabled,
        onSoundToggle = viewModel::toggleSound,
    )
}

@Composable
fun GameScreenContent(
    state: GameState,
    soundEnabled: Boolean,
    onStart: () -> Unit,
    onCardClick: (Int) -> Unit,
    onSlotClick: (BoardSlot) -> Unit,
    onCpuChange: (Boolean) -> Unit,
    onSoundToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRules by remember { mutableStateOf(false) }
    val playing = state.status == GameStatus.Playing

    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.board),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            GameHud(
                cpuEnabled = state.cpuOpponent,
                soundEnabled = soundEnabled,
                onStart = onStart,
                onCpuChange = onCpuChange,
                onRules = { showRules = true },
                onSoundToggle = onSoundToggle,
            )
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                val landscape = maxWidth > maxHeight
                if (landscape) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlayerHand(
                            cards = state.player1Hand,
                            player = PlayerId.One,
                            selectedCardId = state.selectedCardId,
                            isCurrentPlayer = playing && state.currentPlayer == PlayerId.One,
                            vertical = true,
                            onCardClick = onCardClick,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(8.dp),
                        )
                        BoardGrid(
                            board = state.board,
                            selectedCardId = state.selectedCardId,
                            interactive = playing && !(state.cpuOpponent && state.currentPlayer == PlayerId.Two),
                            onSlotClick = onSlotClick,
                            modifier = Modifier
                                .weight(1.8f)
                                .fillMaxHeight()
                                .padding(4.dp),
                        )
                        PlayerHand(
                            cards = state.player2Hand,
                            player = PlayerId.Two,
                            selectedCardId = state.selectedCardId,
                            isCurrentPlayer = playing && state.currentPlayer == PlayerId.Two && !state.cpuOpponent,
                            vertical = true,
                            onCardClick = onCardClick,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(8.dp),
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PlayerHand(
                            cards = state.player2Hand,
                            player = PlayerId.Two,
                            selectedCardId = state.selectedCardId,
                            isCurrentPlayer = playing && state.currentPlayer == PlayerId.Two && !state.cpuOpponent,
                            vertical = false,
                            onCardClick = onCardClick,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(8.dp),
                        )
                        BoardGrid(
                            board = state.board,
                            selectedCardId = state.selectedCardId,
                            interactive = playing && !(state.cpuOpponent && state.currentPlayer == PlayerId.Two),
                            onSlotClick = onSlotClick,
                            modifier = Modifier
                                .weight(2.2f)
                                .fillMaxWidth()
                                .padding(4.dp),
                        )
                        PlayerHand(
                            cards = state.player1Hand,
                            player = PlayerId.One,
                            selectedCardId = state.selectedCardId,
                            isCurrentPlayer = playing && state.currentPlayer == PlayerId.One,
                            vertical = false,
                            onCardClick = onCardClick,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(8.dp),
                        )
                    }
                }
            }
            ScoreBar(state)
        }
    }

    if (showRules) {
        GameInfoDialog(
            title = stringResource(R.string.rules_title),
            body = stringResource(R.string.rules_body),
            onDismiss = { showRules = false },
        )
    }
}

@Composable
private fun ScoreBar(state: GameState) {
    val statusText = when (state.status) {
        GameStatus.Idle -> stringResource(R.string.tap_start)
        GameStatus.Playing -> when (state.currentPlayer) {
            PlayerId.One -> stringResource(R.string.turn_player_1)
            PlayerId.Two -> stringResource(R.string.turn_player_2)
            PlayerId.None -> ""
        }
        GameStatus.Finished -> stringResource(R.string.match_over)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HudScrim)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.p1_wins, state.p1GamesWon),
                modifier = Modifier.weight(1f),
                color = Player1Selected,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Start,
            )
            Text(
                text = stringResource(R.string.match_score, state.p1Score, state.p2Score),
                modifier = Modifier.weight(1f),
                color = ScoreText,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.p2_wins, state.p2GamesWon),
                modifier = Modifier.weight(1f),
                color = Player2Border,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.End,
            )
        }
        Text(
            text = statusText,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.tertiary,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GameInfoDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 800, heightDp = 480)
@Composable
private fun GameScreenLandscapePreview() {
    CardWarTheme(darkTheme = true) {
        GameScreenContent(
            state = GameState(),
            soundEnabled = true,
            onStart = {},
            onCardClick = {},
            onSlotClick = {},
            onCpuChange = {},
            onSoundToggle = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun GameScreenPortraitPreview() {
    CardWarTheme(darkTheme = true) {
        GameScreenContent(
            state = GameState(),
            soundEnabled = true,
            onStart = {},
            onCardClick = {},
            onSlotClick = {},
            onCpuChange = {},
            onSoundToggle = {},
        )
    }
}
