package io.github.kabirnayeem99.totpocket.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.kabirnayeem99.totpocket.SwallowBackgroundErrors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [SettingsStore] kept in SharedPreferences, so a grown-up's choices survive restarts.
 *
 * Opening and reading the file happen on one background thread, started as the app starts; the
 * UI waits for [loaded]. Writes go through the same thread, after the read, so an early change is
 * never lost under the loaded values.
 */
class AndroidSettingsStore(context: Context) : SettingsStore {

    private val appContext = context.applicationContext

    // One thread for the read and every write, so they happen in order.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1) + SwallowBackgroundErrors)
    private lateinit var prefs: SharedPreferences

    private val _settings = MutableStateFlow(ParentSettings())
    override val settings: StateFlow<ParentSettings> = _settings.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    override val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        scope.launch {
            // Whatever happens, the app must open: a failed read leaves the defaults.
            try {
                prefs = appContext.getSharedPreferences("parent_settings", Context.MODE_PRIVATE)
                _settings.value = ParentSettings(
                    volumeCeiling = prefs.getFloat(KEY_VOLUME, ParentSettings.DEFAULT_VOLUME),
                    playLimitMinutes = prefs.getInt(KEY_PLAY_LIMIT, 0),
                    pin = prefs.getString(KEY_PIN, null),
                    keepPinned = prefs.getBoolean(KEY_KEEP_PINNED, true),
                    homeApp = prefs.getBoolean(KEY_HOME_APP, false),
                    showSystemBars = prefs.getBoolean(KEY_SHOW_BARS, false),
                )
            } finally {
                _loaded.value = true
            }
        }
    }

    override fun update(transform: (ParentSettings) -> ParentSettings) {
        if (_loaded.value) {
            _settings.update(transform)
            save()
        } else {
            scope.launch {
                _settings.update(transform)
                save()
            }
        }
    }

    private fun save() {
        val saved = _settings.value
        scope.launch {
            if (!::prefs.isInitialized) return@launch
            // apply(): the file write itself also happens off the main thread.
            prefs.edit {
                putFloat(KEY_VOLUME, saved.volumeCeiling)
                putInt(KEY_PLAY_LIMIT, saved.playLimitMinutes)
                putString(KEY_PIN, saved.pin)
                putBoolean(KEY_KEEP_PINNED, saved.keepPinned)
                putBoolean(KEY_HOME_APP, saved.homeApp)
                putBoolean(KEY_SHOW_BARS, saved.showSystemBars)
            }
        }
    }

    private companion object {
        const val KEY_VOLUME = "volume_ceiling"
        const val KEY_PLAY_LIMIT = "play_limit_minutes"
        const val KEY_PIN = "grown_up_pin"
        const val KEY_KEEP_PINNED = "keep_pinned"
        const val KEY_HOME_APP = "home_app"
        const val KEY_SHOW_BARS = "show_system_bars"
    }
}
