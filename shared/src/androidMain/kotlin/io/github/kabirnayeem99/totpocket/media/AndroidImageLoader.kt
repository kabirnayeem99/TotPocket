package io.github.kabirnayeem99.totpocket.media

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import coil3.video.VideoFrameDecoder
import io.github.kabirnayeem99.totpocket.online.CommonsPhotoSearch
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import java.util.concurrent.TimeUnit

/**
 * TotPocket's one Coil image loader, shared by every picture on screen:
 * - memory cache: a fifth of the app's memory, so scrolling back and swiping photos is instant;
 * - disk cache: 100 MB for web pictures (the grown-ups' Commons search), so nothing downloads twice;
 * - video frames decode as thumbnails for the phone's own videos;
 * - web requests name the app, as Wikimedia asks.
 * Coil loads and decodes on its own background threads, never the main one.
 */
private fun totPocketCoil(context: Context): coil3.ImageLoader {
    val appContext = context.applicationContext
    val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", CommonsPhotoSearch.USER_AGENT).build())
        }
        .build()
    return coil3.ImageLoader.Builder(appContext)
        .memoryCache { MemoryCache.Builder().maxSizePercent(appContext, 0.2).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(appContext.cacheDir.resolve("image_cache").toOkioPath())
                .maxSizeBytes(100L * 1024 * 1024)
                .build()
        }
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { http }))
            add(VideoFrameDecoder.Factory())
        }
        .crossfade(true)
        .build()
}

/**
 * Sets up TotPocket's Coil loader (every `MediaImage` uses it) and, as the app's [ImageLoader],
 * gives code that needs a picture's pixels — e.g. a slideshow photo's main colour — the same caches.
 * Create once, at start-up.
 */
class AndroidImageLoader(context: Context) : ImageLoader {

    private val appContext = context.applicationContext
    private val coil = totPocketCoil(appContext).also { loader -> SingletonImageLoader.setSafe { loader } }

    override suspend fun load(source: ImageSource, maxPx: Int): ImageBitmap? {
        val request = ImageRequest.Builder(appContext)
            .data(source.coilModel())
            .size(maxPx)
            // Readable pixels: hardware bitmaps can't be sampled for their colour.
            .allowHardware(false)
            .build()
        val result = coil.execute(request) as? SuccessResult ?: return null
        return result.image.toBitmap().asImageBitmap()
    }
}
