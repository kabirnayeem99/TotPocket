package io.github.kabirnayeem99.totpocket.gallery

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kabirnayeem99.totpocket.audio.SoundPlayer
import io.github.kabirnayeem99.totpocket.media.MediaAlbum
import io.github.kabirnayeem99.totpocket.media.MediaLibrary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@Immutable
data class AlbumUiState(
    val album: MediaAlbum?,
    /** Index of the photo open full-screen, or `null` for the grid. */
    val openIndex: Int?,
)

sealed interface AlbumAction {
    data class PhotoTapped(val index: Int) : AlbumAction
    /** The viewer settled on a photo (after a swipe or on opening). */
    data class PhotoShown(val index: Int) : AlbumAction
    data object CloseViewer : AlbumAction
}

/**
 * One album: a grid of photos, and a full-screen viewer to swipe through them. A photo with a
 * sound plays it when shown; swiping on or closing silences it. Nothing advances by itself.
 */
class AlbumViewModel(
    albumId: String,
    library: MediaLibrary,
    private val player: SoundPlayer,
) : ViewModel() {

    private val openIndex = MutableStateFlow<Int?>(null)

    val state: StateFlow<AlbumUiState> = combine(library.state, openIndex) { library, open ->
        AlbumUiState(library.album(albumId), open)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlbumUiState(library.state.value.album(albumId), null))

    fun onAction(action: AlbumAction) {
        when (action) {
            is AlbumAction.PhotoTapped -> openIndex.update { action.index }
            is AlbumAction.PhotoShown -> {
                val sound = state.value.album?.photos?.getOrNull(action.index)?.sound
                if (sound != null) player.play(sound) else player.stop()
            }
            AlbumAction.CloseViewer -> {
                player.stop()
                openIndex.update { null }
            }
        }
    }

    override fun onCleared() {
        player.stop()
    }
}
