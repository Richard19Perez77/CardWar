package com.rick.cardwar.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.fillMaxSize
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
import com.rick.cardwar.game.GameEngine
import com.rick.cardwar.game.model.BoardSlot
import com.rick.cardwar.game.model.GameState
import com.rick.cardwar.game.model.GameStatus
import com.rick.cardwar.game.model.PlayerId
import com.rick.cardwar.ui.components.BoardGrid
import com.rick.cardwar.ui.components.GameHud
import com.rick.cardwar.ui.components.HandLayout
import com.rick.cardwar.ui.components.HandPanel
import com.rick.cardwar.ui.components.PlayAreaLayout
import com.rick.cardwar.ui.components.PlayerHand
import com.rick.cardwar.ui.theme.CardWarTheme
import com.rick.cardwar.ui.theme.HudScrim
import com.rick.cardwar.ui.theme.Player1Selected
import com.rick.cardwar.ui.theme.Player2Border
import com.rick.cardwar.ui.theme.ScoreText
import kotlin.random.Random

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
    val cpuThinking = state.cpuOpponent && state.currentPlayer == PlayerId.Two
    val boardInteractive = playing && !cpuThinking
    val player1Active = playing && state.currentPlayer == PlayerId.One
    val player2Active = playing && state.currentPlayer == PlayerId.Two && !state.cpuOpponent

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
                    val cardSize = PlayAreaLayout.landscapeCardSize(maxWidth, maxHeight)
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HandPanel(
                            player = PlayerId.One,
                            active = player1Active,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        ) {
                            PlayerHand(
                                cards = state.player1Hand,
                                player = PlayerId.One,
                                selectedCardId = state.selectedCardId,
                                isCurrentPlayer = player1Active,
                                layout = HandLayout.GridThreeTwo,
                                onCardClick = onCardClick,
                                cardSize = cardSize,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        BoardGrid(
                            board = state.board,
                            hasSelection = state.selectedCardId != null,
                            interactive = boardInteractive,
                            onSlotClick = onSlotClick,
                            cardSize = cardSize,
                            modifier = Modifier
                                .fillMaxHeight()
                                .wrapContentWidth()
                                .padding(horizontal = 12.dp),
                        )
                        HandPanel(
                            player = PlayerId.Two,
                            active = player2Active,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        ) {
                            PlayerHand(
                                cards = state.player2Hand,
                                player = PlayerId.Two,
                                selectedCardId = state.selectedCardId,
                                isCurrentPlayer = player2Active,
                                layout = HandLayout.GridThreeTwo,
                                faceDown = state.cpuOpponent,
                                onCardClick = onCardClick,
                                cardSize = cardSize,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                } else {
                    val cardSize = PlayAreaLayout.portraitCardSize(maxWidth, maxHeight)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        HandPanel(
                            player = PlayerId.Two,
                            active = player2Active,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            PlayerHand(
                                cards = state.player2Hand,
                                player = PlayerId.Two,
                                selectedCardId = state.selectedCardId,
                                isCurrentPlayer = player2Active,
                                layout = HandLayout.Row,
                                faceDown = state.cpuOpponent,
                                onCardClick = onCardClick,
                                cardSize = cardSize,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        BoardGrid(
                            board = state.board,
                            hasSelection = state.selectedCardId != null,
                            interactive = boardInteractive,
                            onSlotClick = onSlotClick,
                            cardSize = cardSize,
                            modifier = Modifier
                                .weight(2.2f)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                        HandPanel(
                            player = PlayerId.One,
                            active = player1Active,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            PlayerHand(
                                cards = state.player1Hand,
                                player = PlayerId.One,
                                selectedCardId = state.selectedCardId,
                                isCurrentPlayer = player1Active,
                                layout = HandLayout.Row,
                                onCardClick = onCardClick,
                                cardSize = cardSize,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
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

/** Deterministic mid-match state so previews show dealt hands instead of a bare board. */
private fun previewState(): GameState {
    val dealt = GameEngine.startMatch(GameState(), Random(seed = 7))
    val withPlacement = GameEngine.place(
        state = dealt.copy(selectedCardId = dealt.player1Hand.first().id),
        slot = BoardSlot.TopLeft,
    )
    return withPlacement.copy(currentPlayer = PlayerId.One, selectedCardId = null)
}

@Preview(name = "Landscape", showBackground = true, widthDp = 800, heightDp = 480)
@Preview(name = "Portrait", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun GameScreenPreview() {
    CardWarTheme(darkTheme = true, dynamicColor = false) {
        GameScreenContent(
            state = previewState(),
            soundEnabled = true,
            onStart = {},
            onCardClick = {},
            onSlotClick = {},
            onCpuChange = {},
            onSoundToggle = {},
        )
    }
}
