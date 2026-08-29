package com.rick.cardwar.ui

import com.rick.cardwar.audio.GameSounds
import com.rick.cardwar.game.model.BoardSlot
import com.rick.cardwar.game.model.GameStatus
import com.rick.cardwar.game.model.PlayerId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private class FakeSounds : GameSounds {
        var selected = 0
        var placed = 0
        var turned = 0
        var released = false

        override fun playSelected(enabled: Boolean) {
            if (enabled) selected++
        }

        override fun playPlaced(enabled: Boolean) {
            if (enabled) placed++
        }

        override fun playTurned(enabled: Boolean) {
            if (enabled) turned++
        }

        override fun release() {
            released = true
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private lateinit var sounds: FakeSounds
    private lateinit var viewModel: GameViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        sounds = FakeSounds()
        viewModel = GameViewModel(sounds)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun firstEmptySlot() = viewModel.state.value.emptySlots.first()

    private fun selectFirstCardOf(player: PlayerId) {
        val card = viewModel.state.value.handOf(player).first()
        viewModel.selectCard(card.id)
    }

    @Test
    fun startGameDealsBothHands() {
        viewModel.startGame()

        val state = viewModel.state.value
        assertEquals(GameStatus.Playing, state.status)
        assertEquals(5, state.player1Hand.size)
        assertEquals(5, state.player2Hand.size)
        assertEquals(PlayerId.One, state.currentPlayer)
        assertEquals(0, sounds.placed)
        assertEquals(0, sounds.selected)
    }

    @Test
    fun selectingThenPlacingAdvancesTheTurn() {
        viewModel.startGame()
        selectFirstCardOf(PlayerId.One)
        assertEquals(1, sounds.selected)

        viewModel.tapSlot(firstEmptySlot())

        val state = viewModel.state.value
        assertEquals(1, state.placementsThisMatch)
        assertEquals(PlayerId.Two, state.currentPlayer)
        assertEquals(4, state.player1Hand.size)
        assertNull(state.selectedCardId)
    }

    @Test
    fun placingWithoutSelectionIsIgnored() {
        viewModel.startGame()

        viewModel.tapSlot(firstEmptySlot())

        val state = viewModel.state.value
        assertEquals(0, state.placementsThisMatch)
        assertEquals(PlayerId.One, state.currentPlayer)
        assertEquals(0, sounds.placed)
        assertEquals(0, sounds.turned)
    }

    @Test
    fun placingOnTheOccupiedCenterIsIgnored() {
        viewModel.startGame()
        selectFirstCardOf(PlayerId.One)

        viewModel.tapSlot(BoardSlot.Center)

        assertEquals(0, viewModel.state.value.placementsThisMatch)
    }

    @Test
    fun cpuTakesItsTurnAfterTheHumanPlaces() = runTest(dispatcher) {
        viewModel.startGame()
        viewModel.setCpuEnabled(true)
        selectFirstCardOf(PlayerId.One)
        viewModel.tapSlot(firstEmptySlot())

        assertEquals(PlayerId.Two, viewModel.state.value.currentPlayer)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.placementsThisMatch)
        assertEquals(PlayerId.One, state.currentPlayer)
        assertEquals(4, state.player2Hand.size)
    }

    @Test
    fun cpuDoesNotMoveWhileDisabled() = runTest(dispatcher) {
        viewModel.startGame()
        selectFirstCardOf(PlayerId.One)
        viewModel.tapSlot(firstEmptySlot())

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.placementsThisMatch)
        assertEquals(PlayerId.Two, state.currentPlayer)
        assertEquals(5, state.player2Hand.size)
    }

    @Test
    fun startingANewMatchCancelsAPendingCpuMove() = runTest(dispatcher) {
        viewModel.startGame()
        viewModel.setCpuEnabled(true)
        selectFirstCardOf(PlayerId.One)
        viewModel.tapSlot(firstEmptySlot())

        viewModel.startGame()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(0, state.placementsThisMatch)
        assertEquals(PlayerId.One, state.currentPlayer)
        assertEquals(5, state.player2Hand.size)
    }

    @Test
    fun mutingSuppressesSoundEffects() {
        viewModel.startGame()
        viewModel.toggleSound()
        assertFalse(viewModel.soundEnabled.value)

        selectFirstCardOf(PlayerId.One)
        viewModel.tapSlot(firstEmptySlot())

        assertEquals(0, sounds.selected)
        assertEquals(0, sounds.placed)
        assertEquals(0, sounds.turned)
        assertTrue(viewModel.state.value.placementsThisMatch == 1)
    }
}
