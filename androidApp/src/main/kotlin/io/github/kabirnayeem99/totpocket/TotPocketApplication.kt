package io.github.kabirnayeem99.totpocket

import android.app.Application
import io.github.kabirnayeem99.totpocket.audio.AndroidSoundPlayer
import io.github.kabirnayeem99.totpocket.audio.Sounds

class TotPocketApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val soundPlayer = AndroidSoundPlayer(this).apply { preload(Sounds.Effects) }
        container = AppContainer(soundPlayer = soundPlayer)
    }
}
