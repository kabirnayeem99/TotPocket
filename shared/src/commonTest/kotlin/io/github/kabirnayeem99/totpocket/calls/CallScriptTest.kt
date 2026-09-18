package io.github.kabirnayeem99.totpocket.calls

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CallScriptTest {

    private val script = CallScript(CallContacts.Mum)

    @Test
    fun `filler gaps stay between four and eight seconds`() {
        val random = Random(1)
        repeat(500) {
            val gap = script.nextFillerGap(random)
            assertTrue(gap in CallScript.FillerGapMin..CallScript.FillerGapMax, "gap $gap out of range")
        }
    }

    @Test
    fun `the same filler line never plays twice in a row`() {
        val random = Random(2)
        var previous = script.nextFiller(random, null)
        repeat(500) {
            val next = script.nextFiller(random, previous)
            assertNotEquals(previous, next)
            previous = next
        }
    }

    @Test
    fun `fillers come from the caller's own lines`() {
        val random = Random(3)
        repeat(50) { assertTrue(script.nextFiller(random, null) in CallContacts.Mum.fillers) }
    }
}
