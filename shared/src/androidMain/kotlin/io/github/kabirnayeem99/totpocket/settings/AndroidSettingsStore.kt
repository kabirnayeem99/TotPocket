package io.github.kabirnayeem99.totpocket.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** [SettingsStore] kept in SharedPreferences, so a grown-up's choices survive restarts. */
class AndroidSettingsStore(context: Context) : SettingsStore {

    private val prefs = context.applicationContext.getSharedPreferences("parent_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        ParentSettings(
            volumeCeiling = prefs.getFloat(KEY_VOLUME, ParentSettings.DEFAULT_VOLUME),
            playLimitMinutes = prefs.getInt(KEY_PLAY_LIMIT, 0),
            pin = prefs.getString(KEY_PIN, null),
            keepPinned = prefs.getBoolean(KEY_KEEP_PINNED, true),
            homeApp = prefs.getBoolean(KEY_HOME_APP, false),
            showSystemBars = prefs.getBoolean(KEY_SHOW_BARS, false),
        ),
    )
    override val settings: StateFlow<ParentSettings> = _settings.asStateFlow()

    override fun update(transform: (ParentSettings) -> ParentSettings) {
        _settings.update(transform)
        val saved = _settings.value
        prefs.edit {
            putFloat(KEY_VOLUME, saved.volumeCeiling)
            putInt(KEY_PLAY_LIMIT, saved.playLimitMinutes)
            putString(KEY_PIN, saved.pin)
            putBoolean(KEY_KEEP_PINNED, saved.keepPinned)
            putBoolean(KEY_HOME_APP, saved.homeApp)
            putBoolean(KEY_SHOW_BARS, saved.showSystemBars)
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
