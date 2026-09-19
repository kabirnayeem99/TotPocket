package io.github.kabirnayeem99.totpocket.calls

import app.cash.turbine.test
import io.github.kabirnayeem99.totpocket.testing.FakeSoundPlayer
import io.github.kabirnayeem99.totpocket.testing.MainDispatcherTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeypadViewModelTest : MainDispatcherTest() {

    private val player = FakeSoundPlayer()

    @Test
    fun `each key beeps its own tone and shows its digit`() = runTest(dispatcher) {
        val viewModel = KeypadViewModel(player)
        viewModel.onAction(KeypadAction.KeyPressed(KeypadKey.Five))
        viewModel.onAction(KeypadAction.KeyPressed(KeypadKey.Hash))

        assertEquals("5#", viewModel.state.value.dialled)
        assertEquals(
            listOf<FakeSoundPlayer.Event>(FakeSoundPlayer.Event.Effect(KeypadKey.Five.sound), FakeSoundPlayer.Event.Effect(KeypadKey.Hash.sound)),
            player.events,
        )
    }

    @Test
    fun `the number stops growing so it always fits on screen`() = runTest(dispatcher) {
        val viewModel = KeypadViewModel(player)
        repeat(30) { viewModel.onAction(KeypadAction.KeyPressed(KeypadKey.One)) }
        assertEquals(KeypadViewModel.MAX_DIGITS, viewModel.state.value.dialled.length)
        assertEquals(30, player.events.size, "every press still beeps")
    }

    @Test
    fun `calling with nothing dialled does nothing`() = runTest(dispatcher) {
        val viewModel = KeypadViewModel(player)
        viewModel.effects.test {
            assertFalse(viewModel.state.value.canCall)
            viewModel.onAction(KeypadAction.CallPressed)
            expectNoEvents()
        }
    }

    @Test
    fun `calling after dialling rings the unknown caller and clears the number`() = runTest(dispatcher) {
        val viewModel = KeypadViewModel(player)
        viewModel.effects.test {
            viewModel.onAction(KeypadAction.KeyPressed(KeypadKey.Seven))
            assertTrue(viewModel.state.value.canCall)
            viewModel.onAction(KeypadAction.CallPressed)
            assertEquals(KeypadEffect.StartCall(CallContacts.UnknownCaller.id), awaitItem())
            assertEquals("", viewModel.state.value.dialled)
        }
    }

    @Test
    fun `every key has a distinct tone file`() {
        assertEquals(KeypadKey.entries.size, KeypadKey.entries.map { it.sound }.toSet().size)
    }
}
