package io.github.kabirnayeem99.totpocket.settings

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** What a grown-up can change. */
@Immutable
data class ParentSettings(
    /** 0.2..1 multiplier on every sound TotPocket makes. */
    val volumeCeiling: Float = DEFAULT_VOLUME,
    /** Foreground play time before bedtime, in minutes; 0 = no limit. */
    val playLimitMinutes: Int = 0,
    /** The grown-up PIN guarding settings and exit; `null` until one is created. */
    val pin: String? = null,
    /** Keep TotPocket pinned (lock task): pin on open, and ask again whenever it gets unpinned. */
    val keepPinned: Boolean = true,
    /** Offer TotPocket as the phone's home app, so the Home button always comes back to it. */
    val homeApp: Boolean = false,
    /** Let the status bar and notifications show. Off: they're hidden and blocked. */
    val showSystemBars: Boolean = false,
) {
    companion object {
        const val DEFAULT_VOLUME = 0.6f
        const val MIN_VOLUME = 0.2f
        val PlayLimitChoices = listOf(0, 10, 15, 20)
        const val PIN_LENGTH = 4
    }
}

interface SettingsStore {
    val settings: StateFlow<ParentSettings>
    fun update(transform: (ParentSettings) -> ParentSettings)
}

/** Keeps settings for this process only — for previews, tests and platforms without storage yet. */
class InMemorySettingsStore(initial: ParentSettings = ParentSettings()) : SettingsStore {
    private val _settings = MutableStateFlow(initial)
    override val settings: StateFlow<ParentSettings> = _settings.asStateFlow()
    override fun update(transform: (ParentSettings) -> ParentSettings) = _settings.update(transform)
}
