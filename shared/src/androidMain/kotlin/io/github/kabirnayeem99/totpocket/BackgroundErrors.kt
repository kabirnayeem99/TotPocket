package io.github.kabirnayeem99.totpocket

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * For TotPocket's long-lived background scopes (media, downloads, sounds, settings): a failure
 * there is logged and dropped, never allowed to take the app down. Carrying on matters more than
 * any one background job.
 */
val SwallowBackgroundErrors = CoroutineExceptionHandler { _, error ->
    Log.w("TotPocket", "Background work failed; carrying on", error)
}
