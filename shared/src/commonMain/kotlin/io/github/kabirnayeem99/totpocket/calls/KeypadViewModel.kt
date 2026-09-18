package io.github.kabirnayeem99.totpocket.calls

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import io.github.kabirnayeem99.totpocket.audio.SoundPlayer
import io.github.kabirnayeem99.totpocket.audio.SoundRef
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/** A keypad key. [sound] is its dial tone, expected at `files/calls/dtmf_<name>.ogg`. */
enum class KeypadKey(val label: String, soundName: String) {
    One("1", "1"), Two("2", "2"), Three("3", "3"),
    Four("4", "4"), Five("5", "5"), Six("6", "6"),
    Seven("7", "7"), Eight("8", "8"), Nine("9", "9"),
    Star("✱", "star"), Zero("0", "0"), Hash("#", "hash");

    val sound: SoundRef = SoundRef("files/calls/dtmf_$soundName.ogg")
}

@Immutable
data class KeypadUiState(val dialled: Int) {
    val canCall: Boolean get() = dialled > 0
}

sealed interface KeypadAction {
    data class KeyPressed(val key: KeypadKey) : KeypadAction
    data object CallPressed : KeypadAction
}

sealed interface KeypadEffect {
    data class StartCall(val contactId: ContactId) : KeypadEffect
}

/**
 * Pretend dialling: every key beeps and adds a dot (numbers are never shown — the child doesn't
 * need them, and it keeps real numbers out of the picture). Calling rings the silly monkey.
 */
class KeypadViewModel(private val player: SoundPlayer) : ViewModel() {

    private val _state = MutableStateFlow(KeypadUiState(dialled = 0))
    val state: StateFlow<KeypadUiState> = _state.asStateFlow()

    private val _effects = Channel<KeypadEffect>(Channel.BUFFERED)
    val effects: Flow<KeypadEffect> = _effects.receiveAsFlow()

    init {
        player.preload(KeypadKey.entries.map { it.sound })
    }

    fun onAction(action: KeypadAction) {
        when (action) {
            is KeypadAction.KeyPressed -> {
                player.playEffect(action.key.sound)
                _state.update { it.copy(dialled = (it.dialled + 1).coerceAtMost(MAX_DOTS)) }
            }
            KeypadAction.CallPressed -> {
                if (!_state.value.canCall) return
                _state.update { it.copy(dialled = 0) }
                _effects.trySend(KeypadEffect.StartCall(CallContacts.SillyMonkey.id))
            }
        }
    }

    companion object {
        const val MAX_DOTS = 8
    }
}
