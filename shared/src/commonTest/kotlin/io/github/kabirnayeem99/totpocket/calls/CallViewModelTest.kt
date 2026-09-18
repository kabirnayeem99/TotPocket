package io.github.kabirnayeem99.totpocket.calls

import app.cash.turbine.test
import io.github.kabirnayeem99.totpocket.audio.Sounds
import io.github.kabirnayeem99.totpocket.testing.FakeDeviceController
import io.github.kabirnayeem99.totpocket.testing.FakeSoundPlayer
import io.github.kabirnayeem99.totpocket.testing.MainDispatcherTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CallViewModelTest : MainDispatcherTest() {

    private val player = FakeSoundPlayer()
    private val device = FakeDeviceController()
    private val mum = CallContacts.Mum

    private fun TestScope.newCall(): CallViewModel {
        val viewModel = CallViewModel(mum, player, device, Random(42))
        // state is shared WhileSubscribed, as the screen would subscribe; keep one subscriber alive.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        runCurrent()
        return viewModel
    }

    @Test
    fun `a call starts by ringing back and keeps the screen awake`() = runTest(dispatcher) {
        val viewModel = newCall()
        assertEquals(CallPhase.Calling, viewModel.state.value.phase)
        assertEquals(FakeSoundPlayer.Event.Played(Sounds.Ringback, loop = true), player.events.first())
        assertTrue(device.screenOn)
    }

    @Test
    fun `the other person picks up after a few seconds and greets the child`() = runTest(dispatcher) {
        val viewModel = newCall()
        advanceTimeBy(CallScript.ConnectDelay - 1.milliseconds)
        runCurrent()
        assertEquals(CallPhase.Calling, viewModel.state.value.phase)

        advanceTimeBy(2.milliseconds)
        runCurrent()
        assertEquals(CallPhase.InCall, viewModel.state.value.phase)
        assertEquals(mum.greeting, player.played.last())
    }

    @Test
    fun `short replies follow every few seconds`() = runTest(dispatcher) {
        newCall().connectNow()
        player.finishClip()
        advanceTimeBy(CallScript.FillerGapMax + 1.milliseconds)
        runCurrent()
        assertTrue(player.played.last() in mum.fillers)
    }

    @Test
    fun `a reply never interrupts a line that is still playing`() = runTest(dispatcher) {
        newCall().connectNow()
        // The greeting never "finishes" here, so no reply may start.
        advanceTimeBy(30.seconds)
        runCurrent()
        assertEquals(listOf(Sounds.Ringback, mum.greeting), player.played)
    }

    @Test
    fun `the call says goodbye by itself after a minute`() = runTest(dispatcher) {
        val viewModel = newCall().connectNow()
        viewModel.effects.test {
            advanceTimeBy(CallScript.CallLength + 1.milliseconds)
            runCurrent()
            assertEquals(CallPhase.Ended, viewModel.state.value.phase)
            assertEquals(mum.bye, player.played.last())
            assertFalse(device.screenOn)

            advanceTimeBy(CallScript.EndedHold)
            assertEquals(CallEffect.Finished, awaitItem())
        }
    }

    @Test
    fun `hanging up mid-call says bye and cancels every pending timer`() = runTest(dispatcher) {
        val viewModel = newCall().connectNow()
        viewModel.onAction(CallAction.HangUp)
        runCurrent()
        val heardAtHangUp = player.played.toList()
        assertEquals(mum.bye, heardAtHangUp.last())

        advanceTimeBy(5.seconds)
        runCurrent()
        assertEquals(heardAtHangUp, player.played, "no replies or second goodbye after hanging up")
    }

    @Test
    fun `hanging up before anyone answers is silent and stops the ringing`() = runTest(dispatcher) {
        val viewModel = newCall()
        viewModel.effects.test {
            viewModel.onAction(CallAction.HangUp)
            runCurrent()
            assertEquals(CallPhase.Ended, viewModel.state.value.phase)
            assertEquals(1, player.stopCount)
            assertFalse(device.screenOn)

            advanceTimeBy(CallScript.ConnectDelay + CallScript.EndedHold)
            assertEquals(CallEffect.Finished, awaitItem())
        }
        assertEquals(listOf(Sounds.Ringback), player.played, "nobody picks up after hanging up")
    }

    @Test
    fun `the caller shows as talking only while one of their lines plays`() = runTest(dispatcher) {
        val viewModel = newCall()
        viewModel.state.test {
            assertFalse(awaitItem().isTalking, "the ringback is not the caller talking")
            advanceTimeBy(CallScript.ConnectDelay + 1.milliseconds)
            assertTrue(expectMostRecentAfter { runCurrent() }.isTalking)
            player.finishClip()
            assertFalse(awaitItem().isTalking)
        }
    }

    private fun CallViewModel.connectNow(): CallViewModel = also {
        dispatcher.scheduler.advanceTimeBy(CallScript.ConnectDelay + 1.milliseconds)
        dispatcher.scheduler.runCurrent()
    }

    private fun <T> app.cash.turbine.TurbineTestContext<T>.expectMostRecentAfter(block: () -> Unit): T {
        block()
        return expectMostRecentItem()
    }
}
