package io.github.kabirnayeem99.totpocket.online

import androidx.compose.runtime.Immutable

/** One photo found on Wikimedia Commons, with what its licence requires us to show. */
@Immutable
data class OnlinePhoto(
    /** The Commons file title, e.g. "File:Red apple.jpg" — unique, so it also marks what's been added. */
    val id: String,
    val title: String,
    /** A small copy for the results grid. */
    val thumbUrl: String,
    /** A copy big enough for the full-screen viewer; this is what gets downloaded. */
    val imageUrl: String,
    val author: String,
    val license: String,
    val pageUrl: String,
)

/**
 * Live photo search, for the grown-ups' "Add photos" screen behind the PIN. The child never
 * reaches it: every photo is looked at by a grown-up before it's added.
 */
interface OnlinePhotoSearch {
    /** Throws on network failure. */
    suspend fun search(query: String): List<OnlinePhoto>
}

object NoOnlinePhotoSearch : OnlinePhotoSearch {
    override suspend fun search(query: String): List<OnlinePhoto> = emptyList()
}
