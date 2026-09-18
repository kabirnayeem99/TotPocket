package io.github.kabirnayeem99.totpocket.media

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import io.github.kabirnayeem99.totpocket.LocalAppContainer

/** Decodes pictures to at most a given size, caching recent ones. */
interface ImageLoader {
    suspend fun load(source: ImageSource, maxPx: Int): ImageBitmap?
}

object NoImageLoader : ImageLoader {
    override suspend fun load(source: ImageSource, maxPx: Int): ImageBitmap? = null
}

/**
 * Shows [source] decoded to about [maxPx] on its long edge, fading in once it's ready. A soft grey
 * placeholder holds the space meanwhile, like a real gallery.
 */
@Composable
fun MediaImage(
    source: ImageSource,
    maxPx: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Color = Color(0xFFE9EAEE),
) {
    val loader = LocalAppContainer.current.imageLoader
    val bitmap by produceState<ImageBitmap?>(null, source, maxPx) { value = loader.load(source, maxPx) }
    val alpha by animateFloatAsState(if (bitmap != null) 1f else 0f, tween(200), label = "imageFade")
    Box(modifier.background(placeholder)) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.matchParentSize().graphicsLayer { this.alpha = alpha },
            )
        }
    }
}
