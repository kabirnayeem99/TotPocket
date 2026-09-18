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

enum class YouTubeTab { Home, Shorts }

/** How the current video is shown. */
enum class PlayerMode {
    /** The watch page: player on top, details and more videos below. */
    Watch,

    /** Just the video, filling the screen. */
    FullScreen,

    /** Shorts: full-screen videos you swipe up and down through, playing one after another. */
    Reels,
}

@Immutable
data class PlayerState(
    val video: MediaVideo,
    val mode: PlayerMode = PlayerMode.Watch,
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
    val tab: YouTubeTab,
    val player: PlayerState?,
) {
    /** Three other videos to pick next on the watch page. */
    val upNext: List<MediaVideo>
        get() {
            val current = player?.video ?: return emptyList()
            val start = videos.indexOf(current).coerceAtLeast(0)
            return (1..minOf(3, videos.size - 1)).map { videos[(start + it) % videos.size] }
        }

    /** Position of the current video in [videos], for the Shorts pager. */
    val reelIndex: Int get() = player?.let { videos.indexOf(it.video) }?.coerceAtLeast(0) ?: 0
}

sealed interface YouTubeAction {
    data class SelectTab(val tab: YouTubeTab) : YouTubeAction
    data class Open(val video: MediaVideo) : YouTubeAction
    data class OpenReels(val video: MediaVideo) : YouTubeAction
    /** The Shorts pager settled on a video (the child swiped to it). */
    data class ReelShown(val video: MediaVideo) : YouTubeAction
    data object EnterFullScreen : YouTubeAction
    data object ExitFullScreen : YouTubeAction
    data object Replay : YouTubeAction
    data object Close : YouTubeAction
    /** A phone video reached its end. */
    data object VideoEnded : YouTubeAction
}

/**
 * The pretend YouTube: photo-pack slideshows and the phone's own videos. A slideshow shows each
 * photo for a few seconds (playing its sound, if any). On the watch page and full screen a video
 * plays once; in Shorts, as on the real app, the next one starts when it ends.
 */
class YouTubeViewModel(
    library: MediaLibrary,
    private val player: SoundPlayer,
) : ViewModel() {

    private val tab = MutableStateFlow(YouTubeTab.Home)
    private val playerState = MutableStateFlow<PlayerState?>(null)
    private var slideshow: Job? = null
    private var videos: List<MediaVideo> = emptyList()

    val state: StateFlow<YouTubeUiState> = combine(library.state, tab, playerState) { library, tab, playing ->
        videos = library.videos
        YouTubeUiState(library.loading, library.videos, tab, playing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YouTubeUiState(true, emptyList(), YouTubeTab.Home, null))

    fun onAction(action: YouTubeAction) {
        when (action) {
            is YouTubeAction.SelectTab -> tab.update { action.tab }
            is YouTubeAction.Open -> start(action.video, PlayerMode.Watch, playKey = 0)
            is YouTubeAction.OpenReels -> start(action.video, PlayerMode.Reels, playKey = 0)
            is YouTubeAction.ReelShown -> if (playerState.value?.video != action.video) start(action.video, PlayerMode.Reels, 0)
            YouTubeAction.EnterFullScreen -> playerState.update { it?.copy(mode = PlayerMode.FullScreen) }
            YouTubeAction.ExitFullScreen -> playerState.update { it?.copy(mode = PlayerMode.Watch) }
            YouTubeAction.Replay -> playerState.value?.let { start(it.video, it.mode, it.playKey + 1) }
            YouTubeAction.Close -> when (playerState.value?.mode) {
                PlayerMode.FullScreen -> playerState.update { it?.copy(mode = PlayerMode.Watch) }
                else -> {
                    stopAll()
                    playerState.update { null }
                }
            }
            YouTubeAction.VideoEnded -> finished()
        }
    }

    private fun start(video: MediaVideo, mode: PlayerMode, playKey: Int) {
        stopAll()
        playerState.update { PlayerState(video, mode, slide = 0, playing = true, playKey = playKey) }
        if (video is MediaVideo.Slideshow) {
            slideshow = viewModelScope.launch {
                video.photos.forEachIndexed { index, photo ->
                    playerState.update { it?.copy(slide = index) }
                    photo.sound?.let(player::play)
                    delay(MediaVideo.SLIDE_MS)
                }
                player.stop()
                finished()
            }
        }
    }

    /** End of a video: Shorts moves on to the next one; everywhere else it waits for "watch again". */
    private fun finished() {
        val current = playerState.value ?: return
        if (current.mode == PlayerMode.Reels && videos.isNotEmpty()) {
            val next = videos[(videos.indexOf(current.video) + 1).mod(videos.size)]
            start(next, PlayerMode.Reels, 0)
        } else {
            playerState.update { it?.copy(playing = false) }
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
