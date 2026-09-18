package io.github.kabirnayeem99.totpocket.calls

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kabirnayeem99.totpocket.audio.SoundPlayer
import io.github.kabirnayeem99.totpocket.audio.SoundRef
import io.github.kabirnayeem99.totpocket.audio.Sounds
import io.github.kabirnayeem99.totpocket.device.DeviceController
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class CallPhase { Calling, InCall, Ended }

@Immutable
data class CallUiState(
    val contact: Contact,
    val phase: CallPhase,
    /** The pretend caller is speaking right now. */
    val isTalking: Boolean,
)

sealed interface CallAction {
    data object HangUp : CallAction
}

sealed interface CallEffect {
    /** The call is over and the goodbye has been shown — leave the call screen. */
    data object Finished : CallEffect
}

class CallViewModel(
    contact: Contact,
    private val player: SoundPlayer,
    private val device: DeviceController,
    private val random: Random,
) : ViewModel() {

    private val script = CallScript(contact)
    private val phase = MutableStateFlow(CallPhase.Calling)
    private var phaseJob: Job? = null
    private var lastLine: SoundRef? = null

    private val _effects = Channel<CallEffect>(Channel.BUFFERED)
    val effects: Flow<CallEffect> = _effects.receiveAsFlow()

    val state: StateFlow<CallUiState> = combine(phase, player.playing) { phase, playing ->
        CallUiState(contact, phase, isTalking = phase == CallPhase.InCall && playing != null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CallUiState(contact, CallPhase.Calling, isTalking = false),
    )

    init {
        device.keepScreenOn(true)
        player.play(Sounds.Ringback, loop = true)
        phaseJob = viewModelScope.launch {
            delay(CallScript.ConnectDelay)
            connect()
        }
    }

    fun onAction(action: CallAction) {
        when (action) {
            CallAction.HangUp -> if (phase.value != CallPhase.Ended) endCall(sayBye = phase.value == CallPhase.InCall)
        }
    }

    private fun connect() {
        phase.update { CallPhase.InCall }
        player.play(script.greeting)
        phaseJob = viewModelScope.launch {
            launch {
                delay(CallScript.CallLength)
                endCall(sayBye = true)
            }
            while (true) {
                delay(script.nextFillerGap(random))
                // Never talk over the caller's own line that is still playing.
                if (player.playing.value == null) {
                    val line = script.nextFiller(random, lastLine)
                    lastLine = line
                    player.play(line)
                }
            }
        }
    }

    private fun endCall(sayBye: Boolean) {
        phaseJob?.cancel()
        phase.update { CallPhase.Ended }
        if (sayBye) player.play(script.bye) else player.stop()
        device.keepScreenOn(false)
        phaseJob = viewModelScope.launch {
            delay(CallScript.EndedHold)
            _effects.send(CallEffect.Finished)
        }
    }

    override fun onCleared() {
        player.stop()
        device.keepScreenOn(false)
    }
}
