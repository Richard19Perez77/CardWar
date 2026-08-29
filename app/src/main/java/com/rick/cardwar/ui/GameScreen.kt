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
import androidx.compose.ui.unit.DpSize
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
import com.rick.cardwar.ui.components.HandSlot
import com.rick.cardwar.ui.components.PlayAreaLayout
import com.rick.cardwar.ui.components.PlayAreaSizes
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
                if (maxWidth > maxHeight) {
                    LandscapePlayArea(
                        state = state,
                        sizes = PlayAreaLayout.landscape(maxWidth, maxHeight),
                        onCardClick = onCardClick,
                        onSlotClick = onSlotClick,
                    )
                } else {
                    PortraitPlayArea(
                        state = state,
                        sizes = PlayAreaLayout.portrait(maxWidth, maxHeight),
                        onCardClick = onCardClick,
                        onSlotClick = onSlotClick,
                    )
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

/** Board in the middle, a hand in each gutter, every card at the same size. */
@Composable
private fun LandscapePlayArea(
    state: GameState,
    sizes: PlayAreaSizes,
    onCardClick: (Int) -> Unit,
    onSlotClick: (BoardSlot) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val gutter = Modifier
            .weight(1f)
            .fillMaxHeight()
        Hand(state, PlayerId.One, HandSlot.Left, sizes.handCard, onCardClick, gutter)
        BoardGrid(
            board = state.board,
            playableBy = state.playableBy,
            cardSize = sizes.boardCard,
            onSlotClick = onSlotClick,
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = PlayAreaLayout.BoardPadding),
        )
        Hand(state, PlayerId.Two, HandSlot.Right, sizes.handCard, onCardClick, gutter)
    }
}

/** Opponent on top, board in the middle, your hand within thumb reach at the bottom. */
@Composable
private fun PortraitPlayArea(
    state: GameState,
    sizes: PlayAreaSizes,
    onCardClick: (Int) -> Unit,
    onSlotClick: (BoardSlot) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val strip = Modifier
            .weight(PlayAreaLayout.PortraitHandWeight)
            .fillMaxWidth()
        Hand(state, PlayerId.Two, HandSlot.Above, sizes.handCard, onCardClick, strip)
        BoardGrid(
            board = state.board,
            playableBy = state.playableBy,
            cardSize = sizes.boardCard,
            onSlotClick = onSlotClick,
            modifier = Modifier
                .weight(PlayAreaLayout.PortraitBoardWeight)
                .fillMaxWidth()
                .padding(PlayAreaLayout.BoardPadding),
        )
        Hand(state, PlayerId.One, HandSlot.Below, sizes.handCard, onCardClick, strip)
    }
}

@Composable
private fun Hand(
    state: GameState,
    player: PlayerId,
    slot: HandSlot,
    cardSize: DpSize,
    onCardClick: (Int) -> Unit,
    modifier: Modifier,
) {
    PlayerHand(
        cards = state.handOf(player),
        player = player,
        selectedCardId = state.selectedCardId,
        isCurrentPlayer = state.acceptsTouchFrom(player),
        slot = slot,
        cardSize = cardSize,
        faceDown = state.isCpu(player),
        onCardClick = onCardClick,
        modifier = modifier,
    )
}

/** A player drives the UI only on their own turn, and never while the CPU plays for them. */
private fun GameState.acceptsTouchFrom(player: PlayerId): Boolean =
    status == GameStatus.Playing &&
        currentPlayer == player &&
        !isCpu(player)

/** The player who can drop the selected card onto the board right now, if any. */
private val GameState.playableBy: PlayerId?
    get() = currentPlayer.takeIf { selectedCardId != null && acceptsTouchFrom(it) }

/** True when the machine plays this hand, so it neither takes touches nor shows its cards. */
private fun GameState.isCpu(player: PlayerId): Boolean = cpuOpponent && player == PlayerId.Two

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
