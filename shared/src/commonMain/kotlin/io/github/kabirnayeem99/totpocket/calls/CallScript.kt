package io.github.kabirnayeem99.totpocket.calls

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * How a pretend call unfolds. Pure rules, no timers: the ViewModel asks what to say and how long
 * to wait.
 *
 * The child makes the call, so it rings back for [ConnectDelay] and then the other person picks
 * up: a salam, then one of their stories or rhymes (never the one told last time) — each story
 * line followed by [ReplyPause] for the child to answer — then goodbye, and the call ends by
 * itself. [CallLength] is only a backstop. The "call
 * ended" screen stays for [EndedHold].
 */
class CallScript(private val contact: Contact) {

    val greeting: String get() = contact.lines.greeting
    val bye: String get() = contact.lines.bye

    /** One of the caller's stories or rhymes, at random but never [previous] (the last one told). */
    fun talk(random: Random, previous: CallTalk?): CallTalk {
        val talks = contact.lines.talks
        val choices = talks.filter { it != previous }.ifEmpty { talks }
        return if (choices.isEmpty()) CallTalk(emptyList(), waitsForChild = false) else choices[random.nextInt(choices.size)]
    }

    /** The pause after each line of [talk]. */
    fun pauseAfterLine(talk: CallTalk): Duration = if (talk.waitsForChild) ReplyPause else RhymePause

    companion object {
        val ConnectDelay: Duration = 3.seconds

        /** After each line: time for the child to say something back. */
        val ReplyPause: Duration = 2.seconds

        /** Before a rhyme, which is said in one go: just a breath. */
        val RhymePause: Duration = 400.milliseconds

        /** A call always ends by now, even if a line never finishes. */
        val CallLength: Duration = 2.minutes
        val EndedHold: Duration = 2.seconds
    }
}
