package io.github.kabirnayeem99.totpocket.gallery

import app.cash.turbine.test
import io.github.kabirnayeem99.totpocket.testing.FakeSoundPlayer
import io.github.kabirnayeem99.totpocket.testing.MainDispatcherTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SoundGridViewModelTest : MainDispatcherTest() {

    private val player = FakeSoundPlayer()
    private val animals = GalleryCatalog.Animals.items

    private fun viewModel(category: GalleryCategory = GalleryCatalog.Animals, pageSize: Int = SoundGridViewModel.DEFAULT_PAGE_SIZE) =
        SoundGridViewModel(category, player, pageSize)

    @Test
    fun `nothing plays until the child taps`() = runTest(dispatcher) {
        viewModel().state.test {
            awaitItem()
            advanceTimeBy(60_000)
            expectNoEvents()
        }
        assertTrue(player.played.isEmpty())
    }

    @Test
    fun `every gallery album fits on one screen`() = runTest(dispatcher) {
        GalleryCatalog.categories.forEach { category ->
            viewModel(category).state.test {
                val state = awaitItem()
                assertEquals(category.items, state.tiles)
                assertEquals(1, state.pageCount)
                assertNull(state.openItem)
            }
        }
    }

    @Test
    fun `tapping a picture opens it and plays its sound until the clip ends`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.state.test {
            awaitItem()
            viewModel.onAction(SoundGridAction.TileTapped(animals[0].id))
            runCurrent()
            val open = expectMostRecentItem()
            assertEquals(animals[0], open.openItem)
            assertEquals(animals[0].id, open.playingId)
            assertEquals(listOf(animals[0].sound), player.played)

            player.finishClip()
            val finished = awaitItem()
            assertNull(finished.playingId)
            assertEquals(animals[0], finished.openItem, "the picture stays open after its sound")
        }
    }

    @Test
    fun `tapping an open picture plays it again`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onAction(SoundGridAction.TileTapped(animals[2].id))
        viewModel.onAction(SoundGridAction.TileTapped(animals[2].id))
        assertEquals(listOf(animals[2].sound, animals[2].sound), player.played)
    }

    @Test
    fun `a second picture replaces the first sound rather than overlapping it`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onAction(SoundGridAction.TileTapped(animals[0].id))
        viewModel.onAction(SoundGridAction.TileTapped(animals[1].id))
        assertEquals(listOf(animals[0].sound, animals[1].sound), player.played)
        assertEquals(animals[1].sound, player.playing.value)
    }

    @Test
    fun `closing a picture silences it and returns to the grid`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.state.test {
            awaitItem()
            viewModel.onAction(SoundGridAction.TileTapped(animals[0].id))
            runCurrent()
            assertEquals(animals[0], expectMostRecentItem().openItem)
            viewModel.onAction(SoundGridAction.CloseItem)
            runCurrent()
            val state = expectMostRecentItem()
            assertNull(state.openItem)
            assertNull(state.playingId)
        }
        assertEquals(1, player.stopCount)
    }

    @Test
    fun `closing when nothing is open does nothing`() = runTest(dispatcher) {
        viewModel().onAction(SoundGridAction.CloseItem)
        assertTrue(player.events.isEmpty())
    }

    @Test
    fun `smaller pages turn and silence the current sound`() = runTest(dispatcher) {
        val viewModel = viewModel(pageSize = 8)
        viewModel.state.test {
            val first = awaitItem()
            assertEquals(animals.take(8), first.tiles)
            assertTrue(first.hasNext)

            viewModel.onAction(SoundGridAction.TileTapped(animals[0].id))
            viewModel.onAction(SoundGridAction.NextPage)
            runCurrent()
            val second = expectMostRecentItem()
            assertEquals(animals.drop(8), second.tiles)
            assertFalse(second.hasNext)
            assertNull(second.openItem)
        }
        assertEquals(1, player.stopCount)
    }

    @Test
    fun `a picture from another page cannot be opened`() = runTest(dispatcher) {
        viewModel(pageSize = 8).onAction(SoundGridAction.TileTapped(animals.last().id))
        assertTrue(player.played.isEmpty())
    }

    @Test
    fun `every item points at its own bundled sound path`() {
        GalleryCatalog.categories.forEach { category ->
            category.items.forEach { item ->
                assertEquals("files/gallery/${category.id}/${item.id.value}.ogg", item.sound.path)
            }
            assertTrue(category.items.size in 6..SoundGridViewModel.DEFAULT_PAGE_SIZE, "${category.id} should fit one screen")
        }
    }
}
