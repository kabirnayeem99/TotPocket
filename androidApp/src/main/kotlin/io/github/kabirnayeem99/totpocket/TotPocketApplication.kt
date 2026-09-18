package io.github.kabirnayeem99.totpocket

import android.app.Application
import android.content.ComponentName
import io.github.kabirnayeem99.totpocket.audio.AndroidSoundPlayer
import io.github.kabirnayeem99.totpocket.audio.Sounds
import io.github.kabirnayeem99.totpocket.device.AndroidDeviceController
import io.github.kabirnayeem99.totpocket.settings.AndroidSettingsStore

class TotPocketApplication : Application() {

    lateinit var container: AppContainer
        private set

    // Lazy: the Application has no context until onCreate, and ComponentName needs one.
    val deviceController by lazy { AndroidDeviceController(ComponentName(this, TotPocketAdminReceiver::class.java)) }

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
