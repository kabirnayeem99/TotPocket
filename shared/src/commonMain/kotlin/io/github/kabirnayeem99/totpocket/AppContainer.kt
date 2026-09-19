package io.github.kabirnayeem99.totpocket

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.kabirnayeem99.totpocket.audio.SoundPlayer
import io.github.kabirnayeem99.totpocket.calls.CallSpeech
import io.github.kabirnayeem99.totpocket.calls.SilentCallSpeech
import io.github.kabirnayeem99.totpocket.device.DeviceController
import io.github.kabirnayeem99.totpocket.media.ImageLoader
import io.github.kabirnayeem99.totpocket.media.MediaLibrary
import io.github.kabirnayeem99.totpocket.online.NoOnlinePhotoSearch
import io.github.kabirnayeem99.totpocket.online.NoPhotoDownloads
import io.github.kabirnayeem99.totpocket.online.OnlinePhotoSearch
import io.github.kabirnayeem99.totpocket.online.PhotoDownloads
import io.github.kabirnayeem99.totpocket.settings.SettingsStore
import kotlin.random.Random

/**
 * Hand-wired dependencies, created once per process by the platform entry point.
 * Platform services arrive as interfaces; everything else is built here.
 */
class AppContainer(
    val soundPlayer: SoundPlayer,
    val device: DeviceController,
    val settingsStore: SettingsStore,
    val mediaLibrary: MediaLibrary,
    val imageLoader: ImageLoader,
    /** The grown-ups' Commons search and the photos they added — off where there's no network code. */
    val onlinePhotoSearch: OnlinePhotoSearch = NoOnlinePhotoSearch,
    val photoDownloads: PhotoDownloads = NoPhotoDownloads,
    /** A fresh Bangla voice for each pretend call, so one call can never cut into the next. */
    val newCallSpeech: () -> CallSpeech = { SilentCallSpeech },
    val random: Random = Random.Default,
)

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided — wrap content in App(container)")
}
