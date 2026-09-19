package io.github.kabirnayeem99.totpocket.calls

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CallScriptTest {

    private val script = CallScript(CallContacts.Tuntuni)

    @Test
    fun `each call tells one of the caller's own stories or rhymes, never the last one again`() {
        val random = Random(3)
        var previous: CallTalk? = null
        repeat(50) {
            val talk = script.talk(random, previous)
            assertTrue(talk in CallContacts.Tuntuni.lines.talks)
            assertTrue(talk != previous, "never the same one twice in a row")
            previous = talk
        }
    }

    @Test
    fun `a story waits for the child after each line, a rhyme only takes a breath`() {
        assertEquals(CallScript.ReplyPause, script.pauseAfterLine(CallTalk(listOf("a"), waitsForChild = true)))
        assertEquals(CallScript.RhymePause, script.pauseAfterLine(CallTalk(listOf("a"), waitsForChild = false)))
    }

    @Test
    fun `no caller is one of the child's relatives`() {
        val relatives = listOf("Mum", "Dad", "Grandma", "Grandpa", "Mama", "Papa", "Nana", "Nani", "Dada", "Dadi")
        (CallContacts.favourites + CallContacts.UnknownCaller).forEach { assertTrue(it.name !in relatives, it.name) }
    }
}
