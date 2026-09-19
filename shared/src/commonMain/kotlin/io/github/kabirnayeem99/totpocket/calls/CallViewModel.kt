package io.github.kabirnayeem99.totpocket.calls

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kabirnayeem99.totpocket.audio.SoundPlayer
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

    /** The child left the call screen (Home, Back, or the app went away): end it quietly, now. */
    data object Leave : CallAction
}

sealed interface CallEffect {
    /** The call is over and the goodbye has been shown — leave the call screen. */
    data object Finished : CallEffect
}

/**
 * One pretend call: it rings (the ringing tone on [player]), the caller picks up and talks in
 * Bangla through [speech] — a salam, a little story or rhyme, Allah Hafez — then the call ends by
 * itself. Hanging up stops them mid-line.
 */
class CallViewModel(
    private val contact: Contact,
    private val player: SoundPlayer,
    private val speech: CallSpeech,
    private val device: DeviceController,
    private val random: Random,
) : ViewModel() {

    private val script = CallScript(contact)
    private val phase = MutableStateFlow(CallPhase.Calling)
    private var phaseJob: Job? = null

    private val _effects = Channel<CallEffect>(Channel.BUFFERED)
    val effects: Flow<CallEffect> = _effects.receiveAsFlow()

    val state: StateFlow<CallUiState> = combine(phase, speech.speaking) { phase, speaking ->
        CallUiState(contact, phase, isTalking = phase == CallPhase.InCall && speaking != null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CallUiState(contact, CallPhase.Calling, isTalking = false),
    )

    init {
        device.keepScreenOn(true)
        player.play(Sounds.Ringback, loop = true)
        speech.prepare()
        phaseJob = viewModelScope.launch {
            delay(CallScript.ConnectDelay)
            connect()
        }
    }

    fun onAction(action: CallAction) {
        when (action) {
            CallAction.HangUp -> if (phase.value != CallPhase.Ended) endCall(sayBye = phase.value == CallPhase.InCall)
            CallAction.Leave -> {
                phaseJob?.cancel()
                phase.update { CallPhase.Ended }
                player.stop()
                speech.stop()
                device.keepScreenOn(false)
                // Back on the contacts list when the child returns, not a dead call.
                viewModelScope.launch { _effects.send(CallEffect.Finished) }
            }
        }
    }

    private fun connect() {
        phase.update { CallPhase.InCall }
        player.play(Sounds.CallAmbience, loop = true)
        phaseJob = viewModelScope.launch {
            // A backstop only: the story or rhyme ends the call itself, long before this.
            launch {
                delay(CallScript.CallLength)
                endCall(sayBye = true)
            }
            speech.say(script.greeting, contact.voice)
            val talk = script.talk(random, lastTalk).also { lastTalk = it }
            for (line in talk.lines) {
                delay(script.pauseAfterLine(talk))
                speech.say(line, contact.voice)
            }
            delay(CallScript.ReplyPause)
            endCall(sayBye = true)
        }
    }

    private fun endCall(sayBye: Boolean) {
        val talking = phaseJob
        phase.update { CallPhase.Ended }
        player.stop()
        device.keepScreenOn(false)
        phaseJob = viewModelScope.launch {
            // Stop whatever was being said (this may be that very job) before saying goodbye.
            talking?.cancel()
            if (sayBye) speech.say(script.bye, contact.voice) else speech.stop()
            delay(CallScript.EndedHold)
            _effects.send(CallEffect.Finished)
        }
    }

    private companion object {
        /** The story or rhyme told on the last call, so the next call tells a different one. */
        var lastTalk: CallTalk? = null
    }

    override fun onCleared() {
        player.stop()
        speech.stop()
        speech.release()
        device.keepScreenOn(false)
    }
}
