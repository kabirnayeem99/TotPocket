package io.github.kabirnayeem99.totpocket.calls

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * [CallSpeech] with the phone's text-to-speech engine (Google TTS on the Tecno and Xiaomi).
 *
 * Only Bangla voices **installed on the phone** are used, never network ones, so nothing the
 * child hears goes online. Each caller gets a woman's or a man's voice where the phone has both
 * (Google's Bangla pack has one of each), falling back to any Bangla voice. With none installed
 * (a grown-up adds it once: Settings → Text-to-speech → Install voice data → বাংলা), calls stay
 * quiet. The engine starts while the phone rings and is freed when the call screen closes; its
 * callbacks arrive off the main thread, and volume follows the grown-up's limit ([volume]).
 */
class AndroidCallSpeech(context: Context, private val volume: () -> Float) : CallSpeech {

    private val appContext = context.applicationContext
    private val lock = Any()
    private var engine: TextToSpeech? = null

    /** Completes with the engine once it's ready, or `null` when there's no Bangla voice. */
    private var ready: CompletableDeferred<TextToSpeech?>? = null
    private var voices: List<Voice> = emptyList()
    private var utterance = 0
    private val waiting = mutableMapOf<String, CancellableContinuation<Unit>>()

    private val _speaking = MutableStateFlow<String?>(null)
    override val speaking: StateFlow<String?> = _speaking.asStateFlow()

    override fun prepare() {
        synchronized(lock) {
            if (engine != null) return
            val deferred = CompletableDeferred<TextToSpeech?>()
            ready = deferred
            lateinit var tts: TextToSpeech
            tts = TextToSpeech(appContext) { status -> deferred.complete(if (status == TextToSpeech.SUCCESS) setUp(tts) else null) }
            engine = tts
        }
    }

    /** Runs on the engine's thread once it's bound: finds the Bangla voices. */
    private fun setUp(tts: TextToSpeech): TextToSpeech? {
        val found = banglaVoices(tts)
        if (found.isEmpty()) {
            Log.w(TAG, "No Bangla voice installed on the phone; calls stay quiet")
            return null
        }
        Log.i(TAG, "Bangla voices: ${found.joinToString { it.name }}")
        synchronized(lock) { voices = found }
        tts.setOnUtteranceProgressListener(Progress())
        return tts
    }

    /** Installed, offline Bangla voices, Bangladesh ones first. */
    private fun banglaVoices(tts: TextToSpeech): List<Voice> =
        tts.voices.orEmpty()
            .filter { it.locale.language == "bn" && !it.isNetworkConnectionRequired }
            .filterNot { TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED in it.features.orEmpty() }
            .sortedBy { it.locale.country != "BD" }

    /**
     * A woman's or a man's voice. Google names its Bangla voices by letter, matching its cloud
     * voices: ban (Bangladesh), bnc — women; bnd — a man. Unknown names fall back to the first.
     */
    private fun voiceFor(woman: Boolean): Voice {
        val wanted = if (woman) listOf("bn-bd-x-ban", "bn-in-x-bnc") else listOf("bn-in-x-bnd")
        return wanted.firstNotNullOfOrNull { prefix -> voices.firstOrNull { it.name.startsWith(prefix) } } ?: voices.first()
    }

    override suspend fun say(text: String, voice: CallVoice) {
        prepare()
        val tts = synchronized(lock) { ready }?.await() ?: return
        suspendCancellableCoroutine { continuation ->
            val id = synchronized(lock) {
                val id = "line-${++utterance}"
                waiting[id] = continuation
                tts.voice = voiceFor(voice.woman)
                tts.setPitch(voice.pitch)
                tts.setSpeechRate(voice.rate)
                _speaking.value = text
                id
            }
            val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume().coerceIn(0f, 1f)) }
            if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, id) != TextToSpeech.SUCCESS) finished(id)
            continuation.invokeOnCancellation {
                tts.stop()
                finished(id)
            }
        }
    }

    /** A line ended (said, failed, flushed or stopped): let its caller go on. */
    private fun finished(id: String?) {
        val continuation = synchronized(lock) {
            if (id == "line-$utterance") _speaking.value = null
            waiting.remove(id)
        }
        if (continuation?.isActive == true) continuation.resume(Unit)
    }

    override fun stop() {
        synchronized(lock) { engine }?.stop()
        synchronized(lock) { waiting.keys.toList() }.forEach(::finished)
        _speaking.value = null
    }

    override fun release() {
        stop()
        synchronized(lock) {
            engine?.shutdown()
            engine = null
            ready?.complete(null)
            ready = null
        }
    }

    private companion object {
        const val TAG = "TotPocketSpeech"
    }

    private inner class Progress : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) = finished(utteranceId)

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) = finished(utteranceId)

        override fun onError(utteranceId: String?, errorCode: Int) = finished(utteranceId)

        override fun onStop(utteranceId: String?, interrupted: Boolean) = finished(utteranceId)
    }
}
