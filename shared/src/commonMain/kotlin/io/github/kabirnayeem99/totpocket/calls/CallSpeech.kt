package io.github.kabirnayeem99.totpocket.calls

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How a pretend caller sounds: a woman's or a man's voice, with [pitch] and [rate] around 1.0 (normal). */
@Immutable
data class CallVoice(val woman: Boolean, val pitch: Float, val rate: Float)

/**
 * Something a caller tells in one go: a little story whose lines each wait for the child to answer
 * ([waitsForChild]), or a rhyme, said as a single line.
 */
@Immutable
data class CallTalk(val lines: List<String>, val waitsForChild: Boolean)

/** What a pretend caller says, in Bangla: a salam, one of their [talks], then goodbye. */
@Immutable
data class CallLines(val greeting: String, val talks: List<CallTalk>, val bye: String)

/**
 * Speaks the pretend caller's lines with the phone's text-to-speech, on the phone only (no network
 * voice). Where no Bangla voice is installed, calls simply stay quiet.
 *
 * One line at a time; nothing is said unless asked.
 */
interface CallSpeech {
    /** The line being said right now, or `null`. */
    val speaking: StateFlow<String?>

    /** Starts the speech engine early — while the phone rings — so the greeting isn't late. */
    fun prepare()

    /** Says [text] and returns once it's finished (at once where there's no voice). Cancelling stops it. */
    suspend fun say(text: String, voice: CallVoice)

    fun stop()

    /** Frees the speech engine; each call has its own and frees it when the call screen closes. */
    fun release()
}

/** For previews, tests and platforms without speech yet. */
object SilentCallSpeech : CallSpeech {
    override val speaking: StateFlow<String?> = MutableStateFlow<String?>(null).asStateFlow()
    override fun prepare() = Unit
    override suspend fun say(text: String, voice: CallVoice) = Unit
    override fun stop() = Unit
    override fun release() = Unit
}
