package io.github.kabirnayeem99.totpocket.media

import io.github.kabirnayeem99.totpocket.audio.SoundRef
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The `catalog.json` inside the bundled photo pack (written by tools/build_media_pack.py). */
@Serializable
data class PhotoPackCatalog(
    val version: Int,
    val categories: List<Category>,
    val photos: List<Photo>,
    val videos: List<Video>,
) {
    @Serializable
    data class Category(val id: String, val title: String, val cover: String)

    @Serializable
    data class Photo(
        val id: String,
        val category: String,
        val subject: String,
        val title: String,
        val image: String,
        val thumb: String,
        val width: Int,
        val height: Int,
        val sound: String? = null,
    )

    @Serializable
    data class Video(
        val id: String,
        val title: String,
        val category: String,
        val channel: String,
        val photos: List<String>,
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): PhotoPackCatalog = json.decodeFromString(serializer(), text)
    }
}

/** Turns the catalog into albums and slideshows, with files found under [root]. */
fun PhotoPackCatalog.toLibrary(root: String): Pair<List<MediaAlbum>, List<MediaVideo>> {
    val photosById = photos.associate { photo ->
        photo.id to MediaPhoto(
            id = "pack:${photo.id}",
            title = photo.title,
            image = ImageSource.FilePath("$root/${photo.image}"),
            thumb = ImageSource.FilePath("$root/${photo.thumb}"),
            sound = photo.sound?.let(::SoundRef),
        )
    }
    val albums = categories.map { category ->
        MediaAlbum(
            id = "pack:${category.id}",
            title = category.title,
            photos = photos.filter { it.category == category.id }.mapNotNull { photosById[it.id] },
            onDevice = false,
        )
    }
    val slideshows = videos.map { video ->
        MediaVideo.Slideshow(
            id = "pack:${video.id}",
            title = video.title,
            channel = video.channel,
            photos = video.photos.mapNotNull { photosById[it] },
        )
    }.filter { it.photos.isNotEmpty() }
    return albums to slideshows
}
