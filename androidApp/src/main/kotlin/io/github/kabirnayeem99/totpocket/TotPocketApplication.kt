package io.github.kabirnayeem99.totpocket

import android.app.Application
import io.github.kabirnayeem99.totpocket.audio.AndroidSoundPlayer
import io.github.kabirnayeem99.totpocket.audio.Sounds
import io.github.kabirnayeem99.totpocket.device.AndroidDeviceController
import io.github.kabirnayeem99.totpocket.settings.AndroidSettingsStore

class TotPocketApplication : Application() {

    lateinit var container: AppContainer
        private set

    val deviceController = AndroidDeviceController()

    override fun onCreate() {
        super.onCreate()
        val soundPlayer = AndroidSoundPlayer(this).apply { preload(Sounds.Effects) }
        container = AppContainer(
            soundPlayer = soundPlayer,
            device = deviceController,
            settingsStore = AndroidSettingsStore(this),
        )
    }
}
