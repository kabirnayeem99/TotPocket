package io.github.kabirnayeem99.totpocket.games.shapematch

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kabirnayeem99.totpocket.audio.SoundPlayer
import io.github.kabirnayeem99.totpocket.audio.Sounds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

enum class ShapeMatchPhase { Playing, Celebrating, Done }

/** Deliberately has no score, timer or round counter for the child to see. */
@Immutable
data class ShapeMatchUiState(
    val round: ShapeRound,
    val placed: Set<ShapeKind>,
    val phase: ShapeMatchPhase,
)

sealed interface ShapeMatchAction {
    data class ShapeDropped(
        val kind: ShapeKind,
        val drop: Point,
        val holeCentres: Map<ShapeKind, Point>,
        val tolerancePx: Float,
    ) : ShapeMatchAction
}

class ShapeMatchViewModel(
    private val player: SoundPlayer,
    private val random: Random,
) : ViewModel() {

    private var completedRounds = 0
    private var celebrationJob: Job? = null

    private val _state = MutableStateFlow(
        ShapeMatchUiState(ShapeMatchEngine.newRound(random, previous = null), emptySet(), ShapeMatchPhase.Playing),
    )
    val state: StateFlow<ShapeMatchUiState> = _state.asStateFlow()

    fun onAction(action: ShapeMatchAction) {
        when (action) {
            is ShapeMatchAction.ShapeDropped -> onDropped(action)
        }
    }

    private fun onDropped(drop: ShapeMatchAction.ShapeDropped) {
        val current = _state.value
        if (current.phase != ShapeMatchPhase.Playing || drop.kind in current.placed) return
        // A miss is silent: the shape just springs back.
        ShapeMatchEngine.resolveDrop(drop.kind, drop.drop, drop.holeCentres, drop.tolerancePx) ?: return

        val placed = current.placed + drop.kind
        if (placed.size < current.round.holes.size) {
            player.playEffect(Sounds.Pop)
            _state.update { it.copy(placed = placed) }
            return
        }

        player.playEffect(Sounds.Chime)
        _state.update { it.copy(placed = placed, phase = ShapeMatchPhase.Celebrating) }
        celebrationJob = viewModelScope.launch {
            delay(CelebrationLength)
            completedRounds++
            if (ShapeMatchEngine.isSessionOver(completedRounds)) {
                _state.update { it.copy(phase = ShapeMatchPhase.Done) }
            } else {
                _state.update {
                    ShapeMatchUiState(ShapeMatchEngine.newRound(random, it.round), emptySet(), ShapeMatchPhase.Playing)
                }
            }
        }
    }

    override fun onCleared() {
        celebrationJob?.cancel()
    }

    companion object {
        val CelebrationLength: Duration = 1500.milliseconds
    }
}
