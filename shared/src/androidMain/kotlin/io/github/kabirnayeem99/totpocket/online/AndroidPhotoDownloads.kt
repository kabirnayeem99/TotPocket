package io.github.kabirnayeem99.totpocket.online

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.content.edit
import io.github.kabirnayeem99.totpocket.media.ImageSource
import io.github.kabirnayeem99.totpocket.media.MediaPhoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

/**
 * [PhotoDownloads] kept in app storage: each photo as a 1080px WebP plus a 360px thumbnail, listed
 * in an index file, and the day's count in SharedPreferences. Every file, network and preference
 * access runs on [Dispatchers.IO]; the lock keeps two downloads from racing past the limit.
 */
class AndroidPhotoDownloads(
    context: Context,
    private val today: () -> Long = { LocalDate.now().toEpochDay() },
) : PhotoDownloads {

    private val appContext = context.applicationContext
    // filesDir touches the disk the first time, so the folder is only worked out on the IO thread.
    private val dir by lazy { File(appContext.filesDir, "media/downloads") }
    private val indexFile by lazy { File(dir, "index.json") }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    // Opening SharedPreferences reads a file, so it happens on the first IO call, never on main.
    private val prefs: SharedPreferences by lazy { appContext.getSharedPreferences("photo_downloads", Context.MODE_PRIVATE) }

    private var entries: List<Entry> = emptyList()
    private val _photos = MutableStateFlow<List<DownloadedPhoto>>(emptyList())
    override val photos: StateFlow<List<DownloadedPhoto>> = _photos.asStateFlow()
    private val _leftToday = MutableStateFlow(DownloadQuota.LIMIT)
    override val leftToday: StateFlow<Int> = _leftToday.asStateFlow()

    init {
        scope.launch {
            lock.withLock {
                entries = runCatching { json.decodeFromString<List<Entry>>(indexFile.readText()) }.getOrDefault(emptyList())
                publish()
            }
        }
    }

    override fun refresh() {
        scope.launch { lock.withLock { _leftToday.value = quota().left(today()) } }
    }

    override suspend fun download(photo: OnlinePhoto): DownloadResult = withContext(Dispatchers.IO) {
        lock.withLock {
            if (entries.any { it.sourceId == photo.id }) return@withLock DownloadResult.AlreadyAdded
            val day = today()
            val quota = quota()
            if (quota.left(day) <= 0) return@withLock DownloadResult.LimitReached

            val bitmap = CommonsPhotoSearch.open(photo.imageUrl).use { BitmapFactory.decodeStream(it) }
                ?: throw java.io.IOException("Not an image: ${photo.imageUrl}")
            dir.mkdirs()
            val name = "${System.currentTimeMillis()}"
            val image = File(dir, "$name.webp")
            val thumb = File(dir, "$name-thumb.webp")
            try {
                writeWebp(bitmap, FULL_EDGE, FULL_QUALITY, image)
                writeWebp(bitmap, THUMB_EDGE, THUMB_QUALITY, thumb)
            } finally {
                bitmap.recycle()
            }

            entries = entries + Entry(photo.id, photo.title, image.name, thumb.name, photo.author, photo.license, photo.pageUrl)
            saveIndex()
            prefs.edit { quota.afterDownload(day).let { putLong(KEY_DAY, it.day); putInt(KEY_USED, it.used) } }
            publish()
            DownloadResult.Added
        }
    }

    override suspend fun remove(sourceId: String) = withContext(Dispatchers.IO) {
        lock.withLock {
            val gone = entries.filter { it.sourceId == sourceId }
            gone.forEach { File(dir, it.image).delete(); File(dir, it.thumb).delete() }
            entries = entries - gone.toSet()
            saveIndex()
            publish()
        }
    }

    private fun quota() = DownloadQuota(prefs.getLong(KEY_DAY, 0), prefs.getInt(KEY_USED, 0))

    private fun publish() {
        _photos.value = entries.map { entry ->
            DownloadedPhoto(
                sourceId = entry.sourceId,
                photo = MediaPhoto(
                    id = "download:${entry.image}",
                    title = entry.title,
                    image = ImageSource.FilePath(File(dir, entry.image).path),
                    thumb = ImageSource.FilePath(File(dir, entry.thumb).path),
                    credit = "Photo: ${entry.author} · ${entry.license}",
                ),
            )
        }
        _leftToday.value = quota().left(today())
    }

    private fun saveIndex() {
        dir.mkdirs()
        val temp = File(dir, "index.json.tmp")
        temp.writeText(json.encodeToString(entries))
        temp.renameTo(indexFile)
    }

    private fun writeWebp(source: Bitmap, edge: Int, quality: Int, out: File) {
        val scale = minOf(1f, edge.toFloat() / maxOf(source.width, source.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true)
        } else {
            source
        }
        val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
        out.outputStream().use { scaled.compress(format, quality, it) }
        if (scaled !== source) scaled.recycle()
    }

    @Serializable
    private data class Entry(
        val sourceId: String,
        val title: String,
        val image: String,
        val thumb: String,
        val author: String,
        val license: String,
        val page: String,
    )

    private companion object {
        const val KEY_DAY = "quota_day"
        const val KEY_USED = "quota_used"
        const val FULL_EDGE = 1080
        const val FULL_QUALITY = 75
        const val THUMB_EDGE = 360
        const val THUMB_QUALITY = 65
    }
}
