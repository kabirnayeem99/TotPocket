package io.github.kabirnayeem99.totpocket.testing

import io.github.kabirnayeem99.totpocket.calls.CallSpeech
import io.github.kabirnayeem99.totpocket.calls.CallVoice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Records what the pretend caller said; a line is "being said" until [finishLine] or [stop]. */
class FakeCallSpeech : CallSpeech {

    val spoken = mutableListOf<String>()
    var stopCount = 0
        private set
    var prepared = false
        private set
    var released = false
        private set

    private val _speaking = MutableStateFlow<String?>(null)
    override val speaking: StateFlow<String?> = _speaking.asStateFlow()

    override fun prepare() {
        prepared = true
    }

    private var line: CompletableDeferred<Unit>? = null

    /** Suspends, like the real engine, until the line is finished ([finishLine]) or stopped. */
    override suspend fun say(text: String, voice: CallVoice) {
        spoken += text
        _speaking.value = text
        val done = CompletableDeferred<Unit>().also { line = it }
        try {
            done.await()
        } finally {
            if (line === done) _speaking.value = null
        }
    }

    override fun stop() {
        stopCount++
        line?.complete(Unit)
        _speaking.value = null
    }

    override fun release() {
        released = true
    }

    /** Simulates the current line being finished. */
    fun finishLine() {
        line?.complete(Unit)
        _speaking.value = null
    }
}
