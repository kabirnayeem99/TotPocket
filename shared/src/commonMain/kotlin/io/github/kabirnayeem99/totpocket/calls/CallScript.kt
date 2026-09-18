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
 * A call rings for at most [RingLimit], then — once answered — greets the child, answers
 * with a short "mm-hmm" every [FillerGapMin]–[FillerGapMax] (never the same line twice in a
 * row), and says goodbye after [CallLength]. The goodbye screen stays for [EndedHold].
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
        val RingLimit: Duration = 10.seconds
        val CallLength: Duration = 60.seconds
        val EndedHold: Duration = 2.seconds
        val FillerGapMin: Duration = 4.seconds
        val FillerGapMax: Duration = 8.seconds
    }
}
