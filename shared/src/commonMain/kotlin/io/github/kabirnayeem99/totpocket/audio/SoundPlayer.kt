package io.github.kabirnayeem99.totpocket.audio

import kotlinx.coroutines.flow.StateFlow
import kotlin.jvm.JvmInline

/** Path of a bundled clip under `composeResources`, e.g. `files/gallery/animals/cow.ogg`. */
@JvmInline
value class SoundRef(val path: String)

/**
 * Plays TotPocket's bundled audio.
 *
 * Invariants every implementation keeps:
 * - At most one *content* clip plays at a time; [play] stops the previous one first.
 * - Nothing chains automatically: when a clip ends, [playing] returns to `null` and silence follows.
 * - *Effects* ([playEffect]) are short, preloaded UI sounds that may overlap a content clip.
 * - Volume never exceeds the parent ceiling ([setCeiling]); system volume is never touched.
 */
interface SoundPlayer {
    /** The content clip currently playing, or `null`. */
    val playing: StateFlow<SoundRef?>

    fun preload(effects: List<SoundRef>)

    fun playEffect(sound: SoundRef)

    fun play(sound: SoundRef, loop: Boolean = false)

    fun stop()

    /** 0f..1f multiplier applied to every sound this player makes. */
    fun setCeiling(fraction: Float)

    fun release()
}

object Sounds {
    val Boop = SoundRef("files/ui/boop.ogg")
    val Pop = SoundRef("files/ui/pop.ogg")
    val Chime = SoundRef("files/ui/chime.ogg")
    val Lullaby = SoundRef("files/ui/lullaby.ogg")
    val Ringtone = SoundRef("files/calls/ringtone.ogg")

    val Effects: List<SoundRef> = listOf(Boop, Pop, Chime)
}
