package io.github.kabirnayeem99.totpocket.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A [SoundPlayer] that makes no sound — for previews and platforms without audio yet. */
class SilentSoundPlayer : SoundPlayer {
    private val _playing = MutableStateFlow<SoundRef?>(null)
    override val playing: StateFlow<SoundRef?> = _playing.asStateFlow()

    override fun preload(effects: List<SoundRef>) = Unit
    override fun playEffect(sound: SoundRef) = Unit
    override fun play(sound: SoundRef, loop: Boolean) = Unit
    override fun stop() = Unit
    override fun setCeiling(fraction: Float) = Unit
    override fun release() = Unit
}
