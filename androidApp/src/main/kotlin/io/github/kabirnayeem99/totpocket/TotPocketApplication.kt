package io.github.kabirnayeem99.totpocket

import android.app.Application
import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import io.github.kabirnayeem99.totpocket.audio.AndroidSoundPlayer
import io.github.kabirnayeem99.totpocket.audio.Sounds
import io.github.kabirnayeem99.totpocket.device.AndroidDeviceController
import io.github.kabirnayeem99.totpocket.media.AndroidImageLoader
import io.github.kabirnayeem99.totpocket.media.AndroidMediaLibrary
import io.github.kabirnayeem99.totpocket.online.AndroidPhotoDownloads
import io.github.kabirnayeem99.totpocket.online.CommonsPhotoSearch
import io.github.kabirnayeem99.totpocket.settings.AndroidSettingsStore

class TotPocketApplication : Application() {

    lateinit var container: AppContainer
        private set

    // Lazy: the Application has no context until onCreate, and ComponentName needs one.
    val deviceController by lazy {
        AndroidDeviceController(
            admin = ComponentName(this, TotPocketAdminReceiver::class.java),
            homeAlias = ComponentName(this, "$packageName.HomeAlias"),
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Debug builds log any disk or network access on the main thread, so none sneaks in.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectLeakedClosableObjects().detectActivityLeaks().penaltyLog().build())
        }
        val soundPlayer = AndroidSoundPlayer(this).apply { preload(Sounds.Effects) }
        val photoDownloads = AndroidPhotoDownloads(this)
        container = AppContainer(
            soundPlayer = soundPlayer,
            device = deviceController,
            settingsStore = AndroidSettingsStore(this),
            mediaLibrary = AndroidMediaLibrary(this, photoDownloads),
            imageLoader = AndroidImageLoader(this),
            onlinePhotoSearch = CommonsPhotoSearch(),
            photoDownloads = photoDownloads,
        )
    }
}
