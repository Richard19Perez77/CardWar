package com.rick.cardwar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rick.cardwar.audio.GameSoundPlayer
import com.rick.cardwar.audio.GameSounds
import com.rick.cardwar.game.GameAction
import com.rick.cardwar.game.GameEngine
import com.rick.cardwar.game.ai.CpuAi
import com.rick.cardwar.game.model.BoardSlot
import com.rick.cardwar.game.model.GameState
import com.rick.cardwar.game.model.GameStatus
import com.rick.cardwar.game.model.PlayerId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(
    private val sounds: GameSounds = GameSoundPlayer(),
) : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private var cpuJob: Job? = null

    fun startGame() {
        cpuJob?.cancel()
        dispatch(GameAction.StartMatch())
    }

    fun selectCard(cardId: Int) {
        dispatch(GameAction.SelectCard(cardId))
    }

    fun tapSlot(slot: BoardSlot) {
        if (!dispatch(GameAction.Place(slot))) return
        scheduleCpuTurn()
    }

    fun setCpuEnabled(enabled: Boolean) {
        dispatch(GameAction.SetCpu(enabled))
        scheduleCpuTurn()
    }

    fun toggleSound() {
        _soundEnabled.value = !_soundEnabled.value
    }

    /**
     * Applies [action] through the engine. Returns false when the engine rejected it,
     * which it signals by handing back the same state instance.
     */
    private fun dispatch(action: GameAction): Boolean {
        val before = _state.value
        val after = GameEngine.reduce(before, action)
        if (after === before) return false
        _state.value = after
        playFeedback(before, after)
        return true
    }

    private fun playFeedback(before: GameState, after: GameState) {
        val enabled = _soundEnabled.value
        when {
            after.placementsThisMatch > before.placementsThisMatch ->
                if (after.lastCapturedSlots.isEmpty()) {
                    sounds.playPlaced(enabled)
                } else {
                    sounds.playTurned(enabled)
                }

            after.selectedCardId != null && after.selectedCardId != before.selectedCardId ->
                sounds.playSelected(enabled)
        }
    }

    private fun scheduleCpuTurn() {
        cpuJob?.cancel()
        if (!isCpuTurn(_state.value)) return

        cpuJob = viewModelScope.launch {
            delay(CpuTurnDelayMs)
            val current = _state.value
            if (!isCpuTurn(current)) return@launch
            val move = CpuAi.chooseMove(current) ?: return@launch
            dispatch(GameAction.Place(move.slot, move.cardId))
        }
    }

    private fun isCpuTurn(state: GameState): Boolean =
        state.cpuOpponent &&
            state.status == GameStatus.Playing &&
            state.currentPlayer == PlayerId.Two

    override fun onCleared() {
        cpuJob?.cancel()
        sounds.release()
        super.onCleared()
    }

    private companion object {
        const val CpuTurnDelayMs = 400L
    }
}
