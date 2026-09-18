package io.github.kabirnayeem99.totpocket

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.kabirnayeem99.totpocket.audio.SoundPlayer
import io.github.kabirnayeem99.totpocket.device.DeviceController
import io.github.kabirnayeem99.totpocket.media.ImageLoader
import io.github.kabirnayeem99.totpocket.media.MediaLibrary
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
    val random: Random = Random.Default,
)

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided — wrap content in App(container)")
}
