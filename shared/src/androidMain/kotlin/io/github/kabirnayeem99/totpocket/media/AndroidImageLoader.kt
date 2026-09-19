package io.github.kabirnayeem99.totpocket.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import io.github.kabirnayeem99.totpocket.online.CommonsPhotoSearch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Decodes pack files with BitmapFactory (sub-sampled to the requested size) and phone media with
 * MediaStore thumbnails, which also works for video frames. Keeps an eighth of the heap as cache.
 */
class AndroidImageLoader(context: Context) : ImageLoader {

    private val resolver = context.applicationContext.contentResolver
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    override suspend fun load(source: ImageSource, maxPx: Int): ImageBitmap? {
        val key = "$source@$maxPx"
        cache.get(key)?.let { return it.asImageBitmap() }
        val bitmap = withContext(Dispatchers.IO) {
            try {
                when (source) {
                    is ImageSource.FilePath -> decodeFile(source.path, maxPx)
                    is ImageSource.ContentUri -> resolver.loadThumbnail(Uri.parse(source.uri), Size(maxPx, maxPx), null)
                    is ImageSource.Url -> decodeUrl(source.url, maxPx)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        } ?: return null
        cache.put(key, bitmap)
        return bitmap.asImageBitmap()
    }

    /** Only the grown-ups' Commons search shows URLs. Read once, then sub-sampled like a file. */
    private fun decodeUrl(url: String, maxPx: Int): Bitmap? {
        val bytes = CommonsPhotoSearch.open(url).use { it.readBytes() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds, maxPx) })
    }

    private fun decodeFile(path: String, maxPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds, maxPx) })
    }

    private fun sampleSize(bounds: BitmapFactory.Options, maxPx: Int): Int {
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
        return sample
    }
}
