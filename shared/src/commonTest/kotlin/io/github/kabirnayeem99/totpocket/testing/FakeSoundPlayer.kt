package io.github.kabirnayeem99.totpocket.testing

import io.github.kabirnayeem99.totpocket.audio.SoundPlayer
import io.github.kabirnayeem99.totpocket.audio.SoundRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Records what would have been heard, and follows the real player's one-clip-at-a-time rule. */
class FakeSoundPlayer : SoundPlayer {

    sealed interface Event {
        data class Played(val sound: SoundRef, val loop: Boolean) : Event
        data class Effect(val sound: SoundRef) : Event
        data object Stopped : Event
    }

    val events = mutableListOf<Event>()
    var ceiling: Float = 1f
        private set

    private val _playing = MutableStateFlow<SoundRef?>(null)
    override val playing: StateFlow<SoundRef?> = _playing.asStateFlow()

    val played: List<SoundRef> get() = events.filterIsInstance<Event.Played>().map { it.sound }
    val stopCount: Int get() = events.count { it == Event.Stopped }

    override fun preload(effects: List<SoundRef>) = Unit

    override fun playEffect(sound: SoundRef) {
        events += Event.Effect(sound)
    }

    override fun play(sound: SoundRef, loop: Boolean) {
        events += Event.Played(sound, loop)
        _playing.value = sound
    }

    override fun stop() {
        events += Event.Stopped
        _playing.value = null
    }

    /** Simulates the current clip reaching its end. */
    fun finishClip() {
        _playing.value = null
    }

    override fun setCeiling(fraction: Float) {
        ceiling = fraction
    }

    override fun release() = Unit
}
