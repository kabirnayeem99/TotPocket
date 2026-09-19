package io.github.kabirnayeem99.totpocket.media

import androidx.compose.runtime.Immutable
import io.github.kabirnayeem99.totpocket.audio.SoundRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where a picture's pixels come from. */
@Immutable
sealed interface ImageSource {
    /** A file TotPocket unpacked from its bundled photo pack. */
    data class FilePath(val path: String) : ImageSource

    /** A picture or video on the phone, as a MediaStore content URI. */
    data class ContentUri(val uri: String) : ImageSource

    /** A picture on the internet — only used by the grown-ups' online preview. */
    data class Url(val url: String) : ImageSource
}

@Immutable
data class MediaPhoto(
    val id: String,
    /** Shown under the photo when it's open, e.g. "Ginger cat" or "18 Sep 2026". */
    val title: String,
    val image: ImageSource,
    val thumb: ImageSource,
    /** Plays when the photo is opened, if the clip is bundled. */
    val sound: SoundRef? = null,
    /** Photographer and licence, shown small under the photo, e.g. "Photo: Jane Doe · CC BY-SA 4.0". */
    val credit: String? = null,
)

/** A folder of photos: a photo-pack category ("Animals") or a phone folder ("Camera"). */
@Immutable
data class MediaAlbum(
    val id: String,
    val title: String,
    val photos: List<MediaPhoto>,
    val onDevice: Boolean,
) {
    val cover: MediaPhoto? get() = photos.firstOrNull()
}

/** Something the pretend YouTube can play. */
@Immutable
sealed interface MediaVideo {
    val id: String
    val title: String
    val channel: String
    val thumb: ImageSource
    val durationMs: Long

    /** Photos shown one after another, like a video. */
    data class Slideshow(
        override val id: String,
        override val title: String,
        override val channel: String,
        val photos: List<MediaPhoto>,
    ) : MediaVideo {
        override val thumb: ImageSource get() = photos.first().thumb
        override val durationMs: Long get() = photos.size * SLIDE_MS
    }

    /** A real video from the phone. */
    data class DeviceVideo(
        override val id: String,
        override val title: String,
        override val channel: String,
        val uri: String,
        override val durationMs: Long,
    ) : MediaVideo {
        override val thumb: ImageSource get() = ImageSource.ContentUri(uri)
    }

    companion object {
        /** How long each photo of a slideshow stays on screen. */
        const val SLIDE_MS = 3_000L
    }
}

@Immutable
data class MediaLibraryState(
    val loading: Boolean = true,
    val albums: List<MediaAlbum> = emptyList(),
    val videos: List<MediaVideo> = emptyList(),
) {
    fun album(id: String): MediaAlbum? = albums.firstOrNull { it.id == id }
}

/**
 * Everything Photos and YouTube can show: the bundled photo pack plus, once the grown-up allows
 * it, the phone's own pictures and videos. Read-only — nothing is ever deleted or shared.
 */
interface MediaLibrary {
    val state: StateFlow<MediaLibraryState>

    /** Re-reads the phone's media, e.g. after the media permission was granted. */
    fun refresh()
}

/** For previews and platforms without media yet. */
class EmptyMediaLibrary : MediaLibrary {
    private val _state = MutableStateFlow(MediaLibraryState(loading = false))
    override val state: StateFlow<MediaLibraryState> = _state.asStateFlow()
    override fun refresh() = Unit
}

/** "0:09", "3:05", "1:02:07" — like a YouTube duration badge. */
fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = (total % 60).toString().padStart(2, '0')
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:$seconds" else "$minutes:$seconds"
}
