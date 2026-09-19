package io.github.kabirnayeem99.totpocket.calls

import app.cash.turbine.test
import io.github.kabirnayeem99.totpocket.audio.Sounds
import io.github.kabirnayeem99.totpocket.testing.FakeCallSpeech
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
    private val speech = FakeCallSpeech()
    private val device = FakeDeviceController()
    private val mum = CallContacts.Tuntuni

    private fun TestScope.newCall(): CallViewModel {
        val viewModel = CallViewModel(mum, player, speech, device, Random(42))
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
        assertTrue(speech.prepared, "the voice gets ready while it rings")
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
        assertEquals(FakeSoundPlayer.Event.Played(Sounds.CallAmbience, loop = true), player.events.last(), "soft rain replaces the ringing")
        assertEquals(mum.lines.greeting, speech.spoken.last())
    }

    @Test
    fun `after the salam the caller tells a story or rhyme, a line at a time, then says goodbye and hangs up`() = runTest(dispatcher) {
        val viewModel = newCall().connectNow()
        viewModel.effects.test {
            // Every line is said in full before the next one, after a pause.
            repeat(30) {
                speech.finishLine()
                advanceTimeBy(CallScript.ReplyPause + 1.milliseconds)
                runCurrent()
            }
            assertEquals(mum.lines.greeting, speech.spoken.first())
            assertEquals(mum.lines.bye, speech.spoken.last())
            val told = speech.spoken.drop(1).dropLast(1)
            assertTrue(mum.lines.talks.any { it.lines == told }, "one whole story or rhyme, in order: $told")
            assertEquals(CallPhase.Ended, viewModel.state.value.phase)
            assertFalse(device.screenOn)
            advanceTimeBy(CallScript.EndedHold)
            assertEquals(CallEffect.Finished, awaitItem())
        }
    }

    @Test
    fun `the next line waits until the previous one is finished`() = runTest(dispatcher) {
        newCall().connectNow()
        // The salam never "finishes" here, so nothing else may be said.
        advanceTimeBy(30.seconds)
        runCurrent()
        assertEquals(listOf(mum.lines.greeting), speech.spoken)
    }

    @Test
    fun `a call ends by itself even if a line never finishes`() = runTest(dispatcher) {
        val viewModel = newCall().connectNow()
        advanceTimeBy(CallScript.CallLength + 1.milliseconds)
        runCurrent()
        assertEquals(CallPhase.Ended, viewModel.state.value.phase)
        assertEquals(mum.lines.bye, speech.spoken.last())
    }

    @Test
    fun `hanging up mid-call says bye and cancels every pending timer`() = runTest(dispatcher) {
        val viewModel = newCall().connectNow()
        viewModel.onAction(CallAction.HangUp)
        runCurrent()
        val saidAtHangUp = speech.spoken.toList()
        assertEquals(mum.lines.bye, saidAtHangUp.last())

        advanceTimeBy(5.seconds)
        runCurrent()
        assertEquals(saidAtHangUp, speech.spoken, "no replies or second goodbye after hanging up")
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
        assertEquals(listOf(Sounds.Ringback), player.played)
        assertEquals(emptyList(), speech.spoken, "nobody picks up after hanging up")
    }

    @Test
    fun `the caller shows as talking only while one of their lines is said`() = runTest(dispatcher) {
        val viewModel = newCall()
        viewModel.state.test {
            assertFalse(awaitItem().isTalking, "the ringback is not the caller talking")
            advanceTimeBy(CallScript.ConnectDelay + 1.milliseconds)
            assertTrue(expectMostRecentAfter { runCurrent() }.isTalking)
            speech.finishLine()
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
