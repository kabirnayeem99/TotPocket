package io.github.kabirnayeem99.totpocket.parent

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import io.github.kabirnayeem99.totpocket.settings.ParentSettings
import io.github.kabirnayeem99.totpocket.settings.SettingsStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

enum class GateStep {
    /** No PIN yet: the grown-up chooses one. */
    Create,
    /** Type the new PIN again to make sure. */
    Confirm,
    /** A PIN exists: type it to get in. */
    Enter,
}

@Immutable
data class ParentGateUiState(
    val step: GateStep,
    val entered: Int,
    /** The last attempt was wrong (or the confirmation didn't match). */
    val error: Boolean = false,
)

sealed interface ParentGateAction {
    data class Digit(val digit: Int) : ParentGateAction
    data object Delete : ParentGateAction
}

sealed interface ParentGateEffect {
    data object Unlocked : ParentGateEffect
}

/**
 * The grown-up PIN in front of settings and exit. The first time, it asks for a new PIN twice;
 * after that it asks for that PIN. A wrong PIN just clears the dots — there's no lock-out.
 */
class ParentGateViewModel(private val store: SettingsStore) : ViewModel() {

    private var digits = ""
    private var firstEntry = ""

    private val _state = MutableStateFlow(
        ParentGateUiState(step = if (store.settings.value.pin == null) GateStep.Create else GateStep.Enter, entered = 0),
    )
    val state: StateFlow<ParentGateUiState> = _state.asStateFlow()

    private val _effects = Channel<ParentGateEffect>(Channel.BUFFERED)
    val effects: Flow<ParentGateEffect> = _effects.receiveAsFlow()

    fun onAction(action: ParentGateAction) {
        when (action) {
            is ParentGateAction.Digit -> {
                if (digits.length >= ParentSettings.PIN_LENGTH) return
                digits += action.digit.coerceIn(0, 9)
                _state.update { it.copy(entered = digits.length, error = false) }
                if (digits.length == ParentSettings.PIN_LENGTH) submit()
            }
            ParentGateAction.Delete -> {
                digits = digits.dropLast(1)
                _state.update { it.copy(entered = digits.length) }
            }
        }
    }

    private fun submit() {
        val attempt = digits
        digits = ""
        when (_state.value.step) {
            GateStep.Create -> {
                firstEntry = attempt
                _state.update { ParentGateUiState(GateStep.Confirm, entered = 0) }
            }
            GateStep.Confirm -> if (attempt == firstEntry) {
                store.update { it.copy(pin = attempt) }
                _effects.trySend(ParentGateEffect.Unlocked)
            } else {
                firstEntry = ""
                _state.update { ParentGateUiState(GateStep.Create, entered = 0, error = true) }
            }
            GateStep.Enter -> if (attempt == store.settings.value.pin) {
                _effects.trySend(ParentGateEffect.Unlocked)
            } else {
                _state.update { it.copy(entered = 0, error = true) }
            }
        }
    }
}
