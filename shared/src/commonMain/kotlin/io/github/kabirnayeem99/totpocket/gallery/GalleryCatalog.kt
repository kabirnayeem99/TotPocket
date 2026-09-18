package io.github.kabirnayeem99.totpocket.gallery

import androidx.compose.runtime.Immutable
import io.github.kabirnayeem99.totpocket.audio.SoundRef
import kotlin.jvm.JvmInline

@JvmInline
value class GalleryItemId(val value: String)

@Immutable
data class GalleryItem(
    val id: GalleryItemId,
    val name: String,
    val emoji: String,
    val sound: SoundRef,
    /** A soft, photo-like backdrop colour (ARGB) for this picture. */
    val backdrop: Long,
)

@Immutable
data class GalleryCategory(
    val id: String,
    val name: String,
    val emoji: String,
    val items: List<GalleryItem>,
)

/**
 * Everything the gallery shows. Pictures are emoji for now; each item's sound is expected at
 * `composeResources/files/gallery/<category>/<item>.ogg` and is simply silent until it's added.
 */
object GalleryCatalog {

    // Declared first: the categories below read it while initialising.
    private val Backdrops = listOf(
        0xFFC8E6C9, 0xFFBBDEFB, 0xFFFFE0B2, 0xFFF8BBD0, 0xFFD1C4E9, 0xFFFFF9C4,
        0xFFB2EBF2, 0xFFFFCCBC, 0xFFDCEDC8, 0xFFE1BEE7, 0xFFB3E5FC, 0xFFFFECB3,
    )

    val Animals = category(
        "animals", "Animals", "🐮",
        "cow" to "🐮", "dog" to "🐶", "cat" to "🐱", "duck" to "🦆", "sheep" to "🐑", "pig" to "🐷",
        "horse" to "🐴", "chicken" to "🐔", "frog" to "🐸", "lion" to "🦁", "elephant" to "🐘", "bee" to "🐝",
    )

    val Nature = category(
        "nature", "Nature", "🌧️",
        "rain" to "🌧️", "ocean" to "🌊", "wind" to "🌬️", "birds" to "🐦", "stream" to "💧", "leaves" to "🍃",
    )

    val Flowers = category(
        "flowers", "Flowers", "🌼",
        "sunflower" to "🌻", "rose" to "🌹", "tulip" to "🌷", "daisy" to "🌼", "hibiscus" to "🌺", "blossom" to "🌸",
    )

    val categories: List<GalleryCategory> = listOf(Animals, Nature, Flowers)

    fun category(id: String): GalleryCategory = categories.firstOrNull { it.id == id } ?: Animals

    private fun category(id: String, name: String, emoji: String, vararg items: Pair<String, String>) =
        GalleryCategory(
            id = id,
            name = name,
            emoji = emoji,
            items = items.mapIndexed { index, (itemId, itemEmoji) ->
                GalleryItem(
                    id = GalleryItemId(itemId),
                    name = itemId.replaceFirstChar { it.uppercase() },
                    emoji = itemEmoji,
                    sound = SoundRef("files/gallery/$id/$itemId.ogg"),
                    backdrop = Backdrops[index % Backdrops.size],
                )
            },
        )
}
