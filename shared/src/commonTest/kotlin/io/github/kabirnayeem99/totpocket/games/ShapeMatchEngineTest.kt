package io.github.kabirnayeem99.totpocket.games

import io.github.kabirnayeem99.totpocket.games.shapematch.Point
import io.github.kabirnayeem99.totpocket.games.shapematch.ShapeKind
import io.github.kabirnayeem99.totpocket.games.shapematch.ShapeMatchEngine
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShapeMatchEngineTest {

    private val holes = mapOf(
        ShapeKind.Circle to Point(100f, 100f),
        ShapeKind.Star to Point(400f, 100f),
    )

    @Test
    fun `a round has three different shapes and the tray is never in hole order`() {
        val random = Random(9)
        var previous = ShapeMatchEngine.newRound(random, null)
        repeat(300) {
            val round = ShapeMatchEngine.newRound(random, previous)
            assertEquals(ShapeMatchEngine.ShapesPerRound, round.holes.toSet().size)
            assertEquals(round.holes.toSet(), round.tray.toSet())
            assertNotEquals(round.holes, round.tray)
            assertNotEquals(previous.holes.toSet(), round.holes.toSet(), "consecutive rounds use a different set")
            previous = round
        }
    }

    @Test
    fun `a drop inside the tolerance of its own hole snaps`() {
        assertEquals(ShapeKind.Circle, ShapeMatchEngine.resolveDrop(ShapeKind.Circle, Point(140f, 130f), holes, 60f))
    }

    @Test
    fun `a drop exactly on the tolerance edge still snaps`() {
        assertEquals(ShapeKind.Circle, ShapeMatchEngine.resolveDrop(ShapeKind.Circle, Point(160f, 100f), holes, 60f))
    }

    @Test
    fun `a drop just outside the tolerance does not snap`() {
        assertNull(ShapeMatchEngine.resolveDrop(ShapeKind.Circle, Point(161f, 100f), holes, 60f))
    }

    @Test
    fun `a drop on another shape's hole does not snap`() {
        assertNull(ShapeMatchEngine.resolveDrop(ShapeKind.Circle, Point(400f, 100f), holes, 60f))
    }

    @Test
    fun `a shape whose hole hasn't been laid out yet does not snap`() {
        assertNull(ShapeMatchEngine.resolveDrop(ShapeKind.Heart, Point(100f, 100f), holes, 60f))
    }

    @Test
    fun `the session ends after five rounds`() {
        assertFalse(ShapeMatchEngine.isSessionOver(4))
        assertTrue(ShapeMatchEngine.isSessionOver(5))
    }
}
