package io.github.kabirnayeem99.totpocket.online

import androidx.compose.runtime.Immutable
import io.github.kabirnayeem99.totpocket.media.MediaPhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How many photos a grown-up may download on one calendar day. Checked on the phone only.
 *
 * The count resets when the day moves forward; setting the clock back doesn't reset it, so
 * changing the date back and forth gains nothing.
 */
@Immutable
data class DownloadQuota(
    /** The day of the last download, as days since 1970-01-01 in the phone's time zone. */
    val day: Long = 0,
    val used: Int = 0,
) {
    fun left(today: Long): Int = if (today > day) LIMIT else (LIMIT - used).coerceAtLeast(0)

    /** The quota after one more download today. */
    fun afterDownload(today: Long): DownloadQuota =
        if (today > day) DownloadQuota(today, 1) else copy(used = used + 1)

    companion object {
        const val LIMIT = 5
    }
}

/** A photo a grown-up added, and the Commons file it came from. */
@Immutable
data class DownloadedPhoto(val sourceId: String, val photo: MediaPhoto)

sealed interface DownloadResult {
    data object Added : DownloadResult
    data object AlreadyAdded : DownloadResult
    data object LimitReached : DownloadResult
}

/** Photos grown-ups downloaded from Commons, kept on the phone and shown in Photos like the pack. */
interface PhotoDownloads {
    val photos: StateFlow<List<DownloadedPhoto>>

    /** Downloads left today, out of [DownloadQuota.LIMIT]. */
    val leftToday: StateFlow<Int>

    /** Re-reads the quota, e.g. when the screen opens on a new day. */
    fun refresh()

    /** Throws on network failure; the quota is only spent when the photo is saved. */
    suspend fun download(photo: OnlinePhoto): DownloadResult

    /** Removing a photo doesn't give a download back. */
    suspend fun remove(sourceId: String)
}

object NoPhotoDownloads : PhotoDownloads {
    override val photos: StateFlow<List<DownloadedPhoto>> = MutableStateFlow(emptyList<DownloadedPhoto>()).asStateFlow()
    override val leftToday: StateFlow<Int> = MutableStateFlow(0).asStateFlow()
    override fun refresh() = Unit
    override suspend fun download(photo: OnlinePhoto): DownloadResult = DownloadResult.LimitReached
    override suspend fun remove(sourceId: String) = Unit
}
