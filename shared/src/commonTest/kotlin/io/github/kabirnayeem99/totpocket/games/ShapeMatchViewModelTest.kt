package io.github.kabirnayeem99.totpocket.games

import io.github.kabirnayeem99.totpocket.audio.Sounds
import io.github.kabirnayeem99.totpocket.games.shapematch.Point
import io.github.kabirnayeem99.totpocket.games.shapematch.ShapeKind
import io.github.kabirnayeem99.totpocket.games.shapematch.ShapeMatchAction
import io.github.kabirnayeem99.totpocket.games.shapematch.ShapeMatchEngine
import io.github.kabirnayeem99.totpocket.games.shapematch.ShapeMatchPhase
import io.github.kabirnayeem99.totpocket.games.shapematch.ShapeMatchViewModel
import io.github.kabirnayeem99.totpocket.testing.FakeSoundPlayer
import io.github.kabirnayeem99.totpocket.testing.MainDispatcherTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class ShapeMatchViewModelTest : MainDispatcherTest() {

    private val player = FakeSoundPlayer()

    /** Lays every hole out 300px apart, as the screen would report them. */
    private fun ShapeMatchViewModel.holes(): Map<ShapeKind, Point> =
        state.value.round.holes.withIndex().associate { (i, kind) -> kind to Point(300f * i, 0f) }

    private fun ShapeMatchViewModel.dropOnOwnHole(kind: ShapeKind) {
        val holes = holes()
        onAction(ShapeMatchAction.ShapeDropped(kind, holes.getValue(kind), holes, tolerancePx = 60f))
    }

    private fun TestScope.completeRound(viewModel: ShapeMatchViewModel) {
        viewModel.state.value.round.tray.forEach { viewModel.dropOnOwnHole(it) }
        advanceTimeBy(ShapeMatchViewModel.CelebrationLength + 1.milliseconds)
        runCurrent()
    }

    @Test
    fun `a correct drop places the shape with a pop`() = runTest(dispatcher) {
        val viewModel = ShapeMatchViewModel(player, Random(1))
        val kind = viewModel.state.value.round.tray.first()
        viewModel.dropOnOwnHole(kind)
        assertEquals(setOf(kind), viewModel.state.value.placed)
        assertEquals(listOf<FakeSoundPlayer.Event>(FakeSoundPlayer.Event.Effect(Sounds.Pop)), player.events)
    }

    @Test
    fun `a wrong drop changes nothing and makes no sound`() = runTest(dispatcher) {
        val viewModel = ShapeMatchViewModel(player, Random(1))
        val (first, second) = viewModel.state.value.round.holes
        val holes = viewModel.holes()
        viewModel.onAction(ShapeMatchAction.ShapeDropped(first, holes.getValue(second), holes, 60f))
        assertTrue(viewModel.state.value.placed.isEmpty())
        assertTrue(player.events.isEmpty())
    }

    @Test
    fun `filling every hole celebrates with a chime then starts a fresh round`() = runTest(dispatcher) {
        val viewModel = ShapeMatchViewModel(player, Random(1))
        val firstRound = viewModel.state.value.round
        firstRound.tray.forEach { viewModel.dropOnOwnHole(it) }
        assertEquals(ShapeMatchPhase.Celebrating, viewModel.state.value.phase)
        assertEquals(FakeSoundPlayer.Event.Effect(Sounds.Chime), player.events.last())

        advanceTimeBy(ShapeMatchViewModel.CelebrationLength + 1.milliseconds)
        runCurrent()
        val state = viewModel.state.value
        assertEquals(ShapeMatchPhase.Playing, state.phase)
        assertTrue(state.placed.isEmpty())
        assertNotEquals(firstRound, state.round)
    }

    @Test
    fun `drops during the celebration are ignored`() = runTest(dispatcher) {
        val viewModel = ShapeMatchViewModel(player, Random(1))
        viewModel.state.value.round.tray.forEach { viewModel.dropOnOwnHole(it) }
        val before = player.events.size
        viewModel.dropOnOwnHole(viewModel.state.value.round.tray.first())
        assertEquals(before, player.events.size)
    }

    @Test
    fun `the game stops after five rounds and stays done`() = runTest(dispatcher) {
        val viewModel = ShapeMatchViewModel(player, Random(1))
        repeat(ShapeMatchEngine.RoundsPerSession) { completeRound(viewModel) }
        assertEquals(ShapeMatchPhase.Done, viewModel.state.value.phase)

        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(ShapeMatchPhase.Done, viewModel.state.value.phase)
    }
}
