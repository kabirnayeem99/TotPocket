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
    /** The picture open full-screen (Gallery viewer / YouTube player), or `null` for the grid. */
    val openItem: GalleryItem?,
    val playingId: GalleryItemId?,
) {
    val hasPrevious: Boolean get() = page > 0
    val hasNext: Boolean get() = page < pageCount - 1
}

sealed interface SoundGridAction {
    /** Opens the picture and plays its sound; on an open picture, plays it again. */
    data class TileTapped(val id: GalleryItemId) : SoundGridAction
    data object CloseItem : SoundGridAction
    data object NextPage : SoundGridAction
    data object PreviousPage : SoundGridAction
}

/**
 * Pictures that make a sound — behind both the Gallery and the pretend YouTube. One page at a
 * time; tapping a picture opens it and plays its sound. Only one sound plays, a new tap replaces
 * it, closing or turning the page silences it, and nothing ever plays or advances on its own.
 */
class SoundGridViewModel(
    category: GalleryCategory,
    private val player: SoundPlayer,
    pageSize: Int = DEFAULT_PAGE_SIZE,
) : ViewModel() {

    private val pages = category.items.chunked(pageSize)
    private val page = MutableStateFlow(0)
    private val openId = MutableStateFlow<GalleryItemId?>(null)

    val state: StateFlow<SoundGridUiState> = combine(page, openId, player.playing) { page, openId, playing ->
        val tiles = pages[page]
        SoundGridUiState(
            categoryName = category.name,
            page = page,
            pageCount = pages.size,
            tiles = tiles,
            openItem = tiles.firstOrNull { it.id == openId },
            playingId = tiles.firstOrNull { it.sound == playing }?.id,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SoundGridUiState(category.name, 0, pages.size, pages.first(), openItem = null, playingId = null),
    )

    fun onAction(action: SoundGridAction) {
        when (action) {
            is SoundGridAction.TileTapped -> {
                val item = pages[page.value].firstOrNull { it.id == action.id } ?: return
                openId.update { item.id }
                player.play(item.sound)
            }
            SoundGridAction.CloseItem -> {
                if (openId.value == null) return
                player.stop()
                openId.update { null }
            }
            SoundGridAction.NextPage -> turnPage(+1)
            SoundGridAction.PreviousPage -> turnPage(-1)
        }
    }

    private fun turnPage(delta: Int) {
        val target = page.value + delta
        if (target !in pages.indices) return
        player.stop()
        openId.update { null }
        page.update { target }
    }

    override fun onCleared() {
        player.stop()
    }

    companion object {
        /** A 3×4 photo grid — every category fits on one screen. */
        const val DEFAULT_PAGE_SIZE = 12
    }
}
