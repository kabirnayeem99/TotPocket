package io.github.kabirnayeem99.totpocket.games.shapematch

import androidx.compose.runtime.Immutable
import kotlin.math.hypot
import kotlin.random.Random

enum class ShapeKind { Circle, Square, Triangle, Star, Heart, Diamond }

/** A position in root coordinates (px). Kept free of Compose types so the rules stay pure. */
@Immutable
data class Point(val x: Float, val y: Float)

@Immutable
data class ShapeRound(
    /** Order of the outlined holes along the top. */
    val holes: List<ShapeKind>,
    /** Order of the loose shapes along the bottom — never the same order as [holes]. */
    val tray: List<ShapeKind>,
)

/**
 * Rules of the shape-matching game. Pure functions only.
 *
 * Each round shows [ShapesPerRound] shapes from a pool of six. A dropped shape counts only if its
 * centre lands within the snap tolerance of *its own* hole; anything else is simply not a match
 * (no penalty). A session is [RoundsPerSession] rounds, then the game says "all done".
 */
object ShapeMatchEngine {

    const val ShapesPerRound = 3
    const val RoundsPerSession = 5

    /** How far (in dp) from a hole's centre a drop still snaps in. Generous for small hands. */
    const val SnapToleranceDp = 60f

    fun newRound(random: Random, previous: ShapeRound?): ShapeRound {
        var kinds: List<ShapeKind>
        do {
            kinds = ShapeKind.entries.shuffled(random).take(ShapesPerRound)
        } while (previous != null && kinds.toSet() == previous.holes.toSet())

        var tray: List<ShapeKind>
        do {
            tray = kinds.shuffled(random)
        } while (tray == kinds)

        return ShapeRound(holes = kinds, tray = tray)
    }

    /** The hole [kind] snaps into, or `null` when the drop isn't close enough to its own hole. */
    fun resolveDrop(kind: ShapeKind, drop: Point, holeCentres: Map<ShapeKind, Point>, tolerancePx: Float): ShapeKind? {
        val hole = holeCentres[kind] ?: return null
        return kind.takeIf { distance(drop, hole) <= tolerancePx }
    }

    fun isSessionOver(completedRounds: Int): Boolean = completedRounds >= RoundsPerSession

    private fun distance(a: Point, b: Point): Float = hypot(a.x - b.x, a.y - b.y)
}
