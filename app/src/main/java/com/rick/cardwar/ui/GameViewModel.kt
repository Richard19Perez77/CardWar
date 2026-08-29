package com.rick.cardwar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rick.cardwar.audio.GameSoundPlayer
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

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val sounds = GameSoundPlayer()
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private var cpuJob: Job? = null

    fun startGame() {
        cpuJob?.cancel()
        _state.value = GameEngine.startMatch(_state.value)
    }

    fun selectCard(cardId: Int) {
        val before = _state.value
        val after = GameEngine.selectCard(before, cardId)
        if (after.selectedCardId != null && after.selectedCardId != before.selectedCardId) {
            sounds.playSelected(_soundEnabled.value)
        }
        _state.value = after
    }

    fun tapSlot(slot: BoardSlot) {
        val before = _state.value
        val after = GameEngine.place(before, slot)
        if (after.placementsThisMatch == before.placementsThisMatch) return
        _state.value = after
        if (after.lastCapturedSlots.isNotEmpty()) {
            sounds.playTurned(_soundEnabled.value)
        } else {
            sounds.playPlaced(_soundEnabled.value)
        }
        maybePlayCpuTurn()
    }

    fun setCpuEnabled(enabled: Boolean) {
        _state.value = GameEngine.reduce(_state.value, GameAction.SetCpu(enabled))
        maybePlayCpuTurn()
    }

    fun toggleSound() {
        _soundEnabled.value = !_soundEnabled.value
    }

    private fun maybePlayCpuTurn() {
        cpuJob?.cancel()
        val snapshot = _state.value
        if (!snapshot.cpuOpponent) return
        if (snapshot.status != GameStatus.Playing) return
        if (snapshot.currentPlayer != PlayerId.Two) return

        cpuJob = viewModelScope.launch {
            delay(CpuTurnDelayMs)
            val current = _state.value
            if (!current.cpuOpponent ||
                current.status != GameStatus.Playing ||
                current.currentPlayer != PlayerId.Two
            ) {
                return@launch
            }
            val move = CpuAi.chooseMove(current) ?: return@launch
            val after = GameEngine.place(current, move.slot, move.cardId)
            _state.value = after
            if (after.lastCapturedSlots.isNotEmpty()) {
                sounds.playTurned(_soundEnabled.value)
            } else {
                sounds.playPlaced(_soundEnabled.value)
            }
        }
    }

    override fun onCleared() {
        cpuJob?.cancel()
        sounds.release()
        super.onCleared()
    }

    private companion object {
        const val CpuTurnDelayMs = 400L
    }
}
