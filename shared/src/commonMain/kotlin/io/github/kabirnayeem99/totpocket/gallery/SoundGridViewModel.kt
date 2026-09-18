package io.github.kabirnayeem99.totpocket.gallery

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kabirnayeem99.totpocket.audio.SoundPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@Immutable
data class SoundGridUiState(
    val categoryName: String,
    val page: Int,
    val pageCount: Int,
    val tiles: List<GalleryItem>,
    val playingId: GalleryItemId?,
) {
    val hasPrevious: Boolean get() = page > 0
    val hasNext: Boolean get() = page < pageCount - 1
}

sealed interface SoundGridAction {
    data class TileTapped(val id: GalleryItemId) : SoundGridAction
    data object NextPage : SoundGridAction
    data object PreviousPage : SoundGridAction
}

/**
 * One page of pictures at a time; tapping a picture plays its sound. Only one sound plays, a new
 * tap replaces it, turning the page silences it, and nothing ever plays on its own.
 */
class SoundGridViewModel(
    category: GalleryCategory,
    private val player: SoundPlayer,
) : ViewModel() {

    private val pages = category.items.chunked(PAGE_SIZE)
    private val page = MutableStateFlow(0)

    val state: StateFlow<SoundGridUiState> = combine(page, player.playing) { page, playing ->
        val tiles = pages[page]
        SoundGridUiState(
            categoryName = category.name,
            page = page,
            pageCount = pages.size,
            tiles = tiles,
            playingId = tiles.firstOrNull { it.sound == playing }?.id,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SoundGridUiState(category.name, 0, pages.size, pages.first(), playingId = null),
    )

    fun onAction(action: SoundGridAction) {
        when (action) {
            is SoundGridAction.TileTapped -> {
                val item = pages[page.value].firstOrNull { it.id == action.id } ?: return
                player.play(item.sound)
            }
            SoundGridAction.NextPage -> turnPage(+1)
            SoundGridAction.PreviousPage -> turnPage(-1)
        }
    }

    private fun turnPage(delta: Int) {
        val target = page.value + delta
        if (target !in pages.indices) return
        player.stop()
        page.update { target }
    }

    override fun onCleared() {
        player.stop()
    }

    companion object {
        const val PAGE_SIZE = 6
    }
}
