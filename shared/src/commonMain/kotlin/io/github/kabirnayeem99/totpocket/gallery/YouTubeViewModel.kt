package io.github.kabirnayeem99.totpocket.gallery

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kabirnayeem99.totpocket.audio.SoundPlayer
import io.github.kabirnayeem99.totpocket.media.MediaLibrary
import io.github.kabirnayeem99.totpocket.media.MediaVideo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class PlayerState(
    val video: MediaVideo,
    /** Photo showing, for a slideshow. */
    val slide: Int = 0,
    val playing: Boolean = true,
    /** Bumped on replay so a phone video restarts. */
    val playKey: Int = 0,
)

@Immutable
data class YouTubeUiState(
    val loading: Boolean,
    val videos: List<MediaVideo>,
    val player: PlayerState?,
) {
    /** Three other videos to pick next — never started automatically. */
    val upNext: List<MediaVideo>
        get() {
            val current = player?.video ?: return emptyList()
            val start = videos.indexOf(current).coerceAtLeast(0)
            return (1..minOf(3, videos.size - 1)).map { videos[(start + it) % videos.size] }
        }
}

sealed interface YouTubeAction {
    data class Open(val video: MediaVideo) : YouTubeAction
    data object Replay : YouTubeAction
    data object Close : YouTubeAction
    /** A phone video reached its end. */
    data object VideoEnded : YouTubeAction
}

/**
 * The pretend YouTube: photo-pack slideshows and the phone's own videos. A slideshow shows each
 * photo for a few seconds (playing its sound, if any) and stops at the last one; a video plays
 * once. Nothing ever starts the next video — the child picks every one.
 */
class YouTubeViewModel(
    library: MediaLibrary,
    private val player: SoundPlayer,
) : ViewModel() {

    private val playerState = MutableStateFlow<PlayerState?>(null)
    private var slideshow: Job? = null

    val state: StateFlow<YouTubeUiState> = combine(library.state, playerState) { library, playing ->
        YouTubeUiState(library.loading, library.videos, playing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YouTubeUiState(true, emptyList(), null))

    fun onAction(action: YouTubeAction) {
        when (action) {
            is YouTubeAction.Open -> start(action.video, playKey = 0)
            YouTubeAction.Replay -> playerState.value?.let { start(it.video, it.playKey + 1) }
            YouTubeAction.Close -> {
                stopAll()
                playerState.update { null }
            }
            YouTubeAction.VideoEnded -> playerState.update { it?.copy(playing = false) }
        }
    }

    private fun start(video: MediaVideo, playKey: Int) {
        stopAll()
        playerState.update { PlayerState(video, slide = 0, playing = true, playKey = playKey) }
        if (video is MediaVideo.Slideshow) {
            slideshow = viewModelScope.launch {
                video.photos.forEachIndexed { index, photo ->
                    playerState.update { it?.copy(slide = index) }
                    photo.sound?.let(player::play)
                    delay(MediaVideo.SLIDE_MS)
                }
                player.stop()
                playerState.update { it?.copy(playing = false) }
            }
        }
    }

    private fun stopAll() {
        slideshow?.cancel()
        slideshow = null
        player.stop()
    }

    override fun onCleared() {
        stopAll()
    }
}
