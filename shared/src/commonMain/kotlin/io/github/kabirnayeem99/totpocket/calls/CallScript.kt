package io.github.kabirnayeem99.totpocket.calls

import io.github.kabirnayeem99.totpocket.audio.SoundRef
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * How a pretend call unfolds. Pure rules, no timers: the ViewModel asks what to play and how
 * long to wait.
 *
 * The child makes the call, so it rings back for [ConnectDelay] and then the other person picks
 * up: they greet the child, answer with a short "mm-hmm" every [FillerGapMin]–[FillerGapMax]
 * (never the same line twice in a row), and say goodbye after [CallLength]. The "call ended"
 * screen stays for [EndedHold].
 */
class CallScript(private val contact: Contact) {

    val greeting: SoundRef get() = contact.greeting
    val bye: SoundRef get() = contact.bye

    fun nextFiller(random: Random, previous: SoundRef?): SoundRef {
        val choices = contact.fillers.filter { it != previous }.ifEmpty { contact.fillers }
        return choices[random.nextInt(choices.size)]
    }

    fun nextFillerGap(random: Random): Duration =
        random.nextLong(FillerGapMin.inWholeMilliseconds, FillerGapMax.inWholeMilliseconds + 1).milliseconds

    companion object {
        val ConnectDelay: Duration = 3.seconds
        val CallLength: Duration = 60.seconds
        val EndedHold: Duration = 2.seconds
        val FillerGapMin: Duration = 4.seconds
        val FillerGapMax: Duration = 8.seconds
    }
}
