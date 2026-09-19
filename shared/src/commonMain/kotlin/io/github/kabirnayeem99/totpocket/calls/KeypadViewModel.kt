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
enum class KeypadKey(val label: String, val letters: String, soundName: String) {
    One("1", "", "1"), Two("2", "ABC", "2"), Three("3", "DEF", "3"),
    Four("4", "GHI", "4"), Five("5", "JKL", "5"), Six("6", "MNO", "6"),
    Seven("7", "PQRS", "7"), Eight("8", "TUV", "8"), Nine("9", "WXYZ", "9"),
    Star("*", "", "star"), Zero("0", "+", "0"), Hash("#", "", "hash");

    val sound: SoundRef = SoundRef("files/calls/dtmf_$soundName.ogg")
}

@Immutable
data class KeypadUiState(val dialled: String) {
    val canCall: Boolean get() = dialled.isNotEmpty()
}

sealed interface KeypadAction {
    data class KeyPressed(val key: KeypadKey) : KeypadAction
    data object CallPressed : KeypadAction
}

sealed interface KeypadEffect {
    data class StartCall(val contactId: ContactId) : KeypadEffect
}

/**
 * Pretend dialling, like the real dialer: every key beeps and its digit appears. Whatever is
 * dialled, calling rings an unknown caller — no real number is ever called.
 */
class KeypadViewModel(private val player: SoundPlayer) : ViewModel() {

    private val _state = MutableStateFlow(KeypadUiState(dialled = ""))
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
                _state.update { if (it.dialled.length < MAX_DIGITS) it.copy(dialled = it.dialled + action.key.label) else it }
            }
            KeypadAction.CallPressed -> {
                if (!_state.value.canCall) return
                _state.update { it.copy(dialled = "") }
                _effects.trySend(KeypadEffect.StartCall(CallContacts.UnknownCaller.id))
            }
        }
    }

    companion object {
        const val MAX_DIGITS = 13
    }
}
