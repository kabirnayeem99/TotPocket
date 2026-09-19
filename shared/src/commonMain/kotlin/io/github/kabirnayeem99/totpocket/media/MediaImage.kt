package io.github.kabirnayeem99.totpocket.media

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

/** Decodes pictures to at most a given size, for code that needs the pixels (e.g. a photo's colour). */
interface ImageLoader {
    suspend fun load(source: ImageSource, maxPx: Int): ImageBitmap?
}

object NoImageLoader : ImageLoader {
    override suspend fun load(source: ImageSource, maxPx: Int): ImageBitmap? = null
}

/** What Coil loads for [this]: an absolute file, a MediaStore content URI, or a web address. */
fun ImageSource.coilModel(): String = when (this) {
    is ImageSource.FilePath -> "file://$path"
    is ImageSource.ContentUri -> uri
    is ImageSource.Url -> url
}

/**
 * Shows [source] decoded to about [maxPx] on its long edge, fading in once it's ready. A soft grey
 * placeholder holds the space meanwhile, like a real gallery. Loading, decoding and caching (memory,
 * and disk for web pictures) are Coil's, set up once for the app.
 */
@Composable
fun MediaImage(
    source: ImageSource,
    maxPx: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Color = Color(0xFFE9EAEE),
) {
    val context = LocalPlatformContext.current
    val request = remember(context, source, maxPx) {
        ImageRequest.Builder(context).data(source.coilModel()).size(maxPx).crossfade(200).build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier.background(placeholder),
    )
}
