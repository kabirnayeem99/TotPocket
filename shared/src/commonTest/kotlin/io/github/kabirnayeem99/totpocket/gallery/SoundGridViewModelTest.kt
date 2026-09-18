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

    private fun viewModel(category: GalleryCategory = GalleryCatalog.Animals) = SoundGridViewModel(category, player)

    @Test
    fun `nothing plays until the child taps`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.state.test {
            awaitItem()
            advanceTimeBy(60_000)
            expectNoEvents()
        }
        assertTrue(player.played.isEmpty())
    }

    @Test
    fun `first page shows six tiles and a second page exists`() = runTest(dispatcher) {
        viewModel().state.test {
            val state = awaitItem()
            assertEquals(animals.take(6), state.tiles)
            assertEquals(2, state.pageCount)
            assertFalse(state.hasPrevious)
            assertTrue(state.hasNext)
        }
    }

    @Test
    fun `tapping a tile plays its sound and highlights it until the clip ends`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.state.test {
            awaitItem()
            viewModel.onAction(SoundGridAction.TileTapped(animals[0].id))
            assertEquals(animals[0].id, awaitItem().playingId)
            assertEquals(listOf(animals[0].sound), player.played)

            player.finishClip()
            assertNull(awaitItem().playingId)
        }
    }

    @Test
    fun `a second tap replaces the first sound rather than overlapping it`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.state.test {
            awaitItem()
            viewModel.onAction(SoundGridAction.TileTapped(animals[0].id))
            awaitItem()
            viewModel.onAction(SoundGridAction.TileTapped(animals[1].id))
            assertEquals(animals[1].id, awaitItem().playingId)
        }
        assertEquals(listOf(animals[0].sound, animals[1].sound), player.played)
        assertEquals(animals[1].sound, player.playing.value)
    }

    @Test
    fun `tapping the same tile again restarts it`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onAction(SoundGridAction.TileTapped(animals[2].id))
        viewModel.onAction(SoundGridAction.TileTapped(animals[2].id))
        assertEquals(listOf(animals[2].sound, animals[2].sound), player.played)
    }

    @Test
    fun `turning the page silences the current sound and shows the next tiles`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.state.test {
            awaitItem()
            viewModel.onAction(SoundGridAction.TileTapped(animals[0].id))
            awaitItem()
            viewModel.onAction(SoundGridAction.NextPage)
            runCurrent()
            val state = expectMostRecentItem()
            assertEquals(1, state.page)
            assertEquals(animals.drop(6), state.tiles)
            assertTrue(state.hasPrevious)
            assertFalse(state.hasNext)
            assertNull(state.playingId)
        }
        assertEquals(1, player.stopCount)
    }

    @Test
    fun `paging past either end does nothing`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.state.test {
            awaitItem()
            viewModel.onAction(SoundGridAction.PreviousPage)
            viewModel.onAction(SoundGridAction.NextPage)
            viewModel.onAction(SoundGridAction.NextPage)
            runCurrent()
            assertEquals(1, expectMostRecentItem().page)
        }
        assertEquals(1, player.stopCount)
    }

    @Test
    fun `a tile from another page cannot be played`() = runTest(dispatcher) {
        viewModel().onAction(SoundGridAction.TileTapped(animals.last().id))
        assertTrue(player.played.isEmpty())
    }

    @Test
    fun `single-page categories have no arrows`() = runTest(dispatcher) {
        viewModel(GalleryCatalog.Flowers).state.test {
            val state = awaitItem()
            assertEquals(1, state.pageCount)
            assertFalse(state.hasNext)
            assertFalse(state.hasPrevious)
        }
    }

    @Test
    fun `every item points at its own bundled sound path`() {
        GalleryCatalog.categories.forEach { category ->
            category.items.forEach { item ->
                assertEquals("files/gallery/${category.id}/${item.id.value}.ogg", item.sound.path)
            }
            assertTrue(category.items.size in 6..24, "${category.id} should fill 1–4 pages")
        }
    }
}
