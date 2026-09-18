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
        ),
    )
    override val settings: StateFlow<ParentSettings> = _settings.asStateFlow()

    override fun update(transform: (ParentSettings) -> ParentSettings) {
        _settings.update(transform)
        val saved = _settings.value
        prefs.edit {
            putFloat(KEY_VOLUME, saved.volumeCeiling)
            putInt(KEY_PLAY_LIMIT, saved.playLimitMinutes)
        }
    }

    private companion object {
        const val KEY_VOLUME = "volume_ceiling"
        const val KEY_PLAY_LIMIT = "play_limit_minutes"
    }
}
