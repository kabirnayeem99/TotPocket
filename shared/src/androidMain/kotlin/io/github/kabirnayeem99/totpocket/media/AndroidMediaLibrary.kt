package io.github.kabirnayeem99.totpocket.media

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import io.github.kabirnayeem99.totpocket.SwallowBackgroundErrors
import io.github.kabirnayeem99.totpocket.online.PhotoDownloads
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import totpocket.shared.generated.resources.Res
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlin.coroutines.cancellation.CancellationException

/**
 * [MediaLibrary] on Android: the bundled photo pack (unzipped once into app storage), the photos
 * grown-ups added from Commons, and the phone's own pictures and videos from MediaStore when read
 * access is granted. Never changes the phone's media.
 */
class AndroidMediaLibrary(context: Context, private val downloads: PhotoDownloads) : MediaLibrary {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + SwallowBackgroundErrors)
    private val _state = MutableStateFlow(MediaLibraryState())
    override val state: StateFlow<MediaLibraryState> = _state.asStateFlow()

    private var packAlbums: List<MediaAlbum> = emptyList()
    private var packVideos: List<MediaVideo> = emptyList()
    private var deviceAlbums: List<MediaAlbum> = emptyList()
    private var deviceVideos: List<MediaVideo> = emptyList()
    private var loaded = false
    private var loadJob: Job? = null

    init {
        load(includePack = true)
        // Added photos appear as soon as they're saved; the publish runs on this IO scope.
        scope.launch { downloads.photos.collect { loadJob?.join(); publish() } }
    }

    override fun refresh() = load(includePack = false)

    private fun load(includePack: Boolean) {
        val previous = loadJob
        loadJob = scope.launch {
            previous?.join()
            if (includePack) loadPack()
            deviceAlbums = runCatching { readDeviceAlbums() }.getOrDefault(emptyList())
            deviceVideos = runCatching { readDeviceVideos() }.getOrDefault(emptyList())
            loaded = true
            publish()
        }
    }

    @Synchronized
    private fun publish() {
        if (!loaded) return
        val added = downloads.photos.value.map { it.photo }
        val addedAlbum = if (added.isEmpty()) emptyList() else listOf(MediaAlbum(ADDED_ALBUM, "New photos", added.asReversed(), onDevice = false))
        _state.value = MediaLibraryState(
            loading = false,
            albums = addedAlbum + packAlbums + deviceAlbums,
            videos = packVideos + deviceVideos,
        )
    }

    // ---------------------------------------------------------------- bundled pack

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadPack() {
        try {
            val root = File(appContext.filesDir, "media/pack")
            val catalogFile = File(root, "catalog.json")
            val bundled = Res.readBytes(PACK_PATH)
            val installedStamp = File(root, ".size")
            // Unzip only when the bundled pack changed (a new app version), not on every launch.
            if (!catalogFile.exists() || installedStamp.takeIf { it.exists() }?.readText() != bundled.size.toString()) {
                root.deleteRecursively()
                root.mkdirs()
                ZipInputStream(bundled.inputStream()).use { zip ->
                    generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
                        val out = File(root, entry.name).canonicalFile
                        require(out.path.startsWith(root.canonicalPath)) { "Bad pack entry ${entry.name}" }
                        out.parentFile?.mkdirs()
                        out.outputStream().use { zip.copyTo(it) }
                    }
                }
                installedStamp.writeText(bundled.size.toString())
            }
            val (albums, videos) = PhotoPackCatalog.parse(catalogFile.readText()).toLibrary(root.path)
            packAlbums = albums
            packVideos = videos
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Photo pack unavailable", e)
        }
    }

    // ---------------------------------------------------------------- phone media

    private fun canRead(permission: String) =
        appContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun canReadImages() = if (Build.VERSION.SDK_INT >= 33) {
        canRead(Manifest.permission.READ_MEDIA_IMAGES) ||
            (Build.VERSION.SDK_INT >= 34 && canRead(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
    } else {
        canRead(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun canReadVideos() = if (Build.VERSION.SDK_INT >= 33) {
        canRead(Manifest.permission.READ_MEDIA_VIDEO) ||
            (Build.VERSION.SDK_INT >= 34 && canRead(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
    } else {
        canRead(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /** The phone's picture folders ("Camera", "WhatsApp Images", …), newest pictures first. */
    private fun readDeviceAlbums(): List<MediaAlbum> {
        if (!canReadImages()) return emptyList()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
        )
        val byBucket = LinkedHashMap<String, Pair<String, MutableList<MediaPhoto>>>()
        appContext.contentResolver.query(
            collection, projection, null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketId = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketName = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val taken = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val added = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(collection, cursor.getLong(id)).toString()
                val millis = cursor.getLong(taken).takeIf { it > 0 } ?: (cursor.getLong(added) * 1000)
                val bucket = cursor.getString(bucketId) ?: "other"
                val entry = byBucket.getOrPut(bucket) { (cursor.getString(bucketName) ?: "Pictures") to mutableListOf() }
                entry.second += MediaPhoto(
                    id = "device:$uri",
                    title = DateFormat.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())),
                    image = ImageSource.ContentUri(uri),
                    thumb = ImageSource.ContentUri(uri),
                )
            }
        }
        return byBucket.map { (bucket, entry) ->
            MediaAlbum(id = "device:$bucket", title = entry.first, photos = entry.second, onDevice = true)
        }
    }

    /** The phone's videos, newest first, as YouTube videos from a "channel" named after their folder. */
    private fun readDeviceVideos(): List<MediaVideo> {
        if (!canReadVideos()) return emptyList()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
        )
        val videos = mutableListOf<MediaVideo>()
        appContext.contentResolver.query(
            collection, projection, null, null, "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val name = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val bucket = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val duration = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(collection, cursor.getLong(id)).toString()
                videos += MediaVideo.DeviceVideo(
                    id = "device:$uri",
                    title = cursor.getString(name)?.substringBeforeLast('.') ?: "Video",
                    channel = cursor.getString(bucket) ?: "My videos",
                    uri = uri,
                    durationMs = cursor.getLong(duration),
                )
            }
        }
        return videos
    }

    private companion object {
        const val TAG = "TotPocketMedia"
        const val PACK_PATH = "files/media/pack.zip"
        const val ADDED_ALBUM = "added"
        val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    }
}
