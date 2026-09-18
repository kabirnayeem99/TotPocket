package io.github.kabirnayeem99.totpocket.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kabirnayeem99.totpocket.audio.SoundPlayer
import io.github.kabirnayeem99.totpocket.audio.Sounds
import io.github.kabirnayeem99.totpocket.settings.SettingsStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

sealed interface SessionAction {
    data object Resumed : SessionAction
    data object Paused : SessionAction
    /** The bedtime screen is up and everything else has been closed — time for the lullaby. */
    data object BedtimeShown : SessionAction
    /** A grown-up passed the gate: bedtime is lifted and the play clock starts again. */
    data object ParentUnlocked : SessionAction
}

/**
 * App-wide play time. Counts only while TotPocket is in front; when the grown-up's limit is
 * reached it's bedtime: the bedtime screen covers everything, a lullaby plays once, and it stays
 * until a grown-up unlocks it. Also keeps the sound player at the grown-up's volume limit.
 */
class SessionViewModel(
    private val store: SettingsStore,
    private val player: SoundPlayer,
) : ViewModel() {

    private var played: Duration = Duration.ZERO
    private var clock: Job? = null

    private val _bedtime = MutableStateFlow(false)
    val bedtime: StateFlow<Boolean> = _bedtime.asStateFlow()

    init {
        viewModelScope.launch {
            store.settings.collect { player.setCeiling(it.volumeCeiling) }
        }
    }

    fun onAction(action: SessionAction) {
        when (action) {
            SessionAction.Resumed -> startClock()
            SessionAction.Paused -> {
                clock?.cancel()
                clock = null
            }
            SessionAction.BedtimeShown -> if (_bedtime.value) player.play(Sounds.Lullaby)
            SessionAction.ParentUnlocked -> {
                played = Duration.ZERO
                _bedtime.update { false }
                if (clock == null) startClock()
            }
        }
    }

    private fun startClock() {
        if (clock?.isActive == true || _bedtime.value) return
        clock = viewModelScope.launch {
            while (isActive) {
                delay(Tick)
                played += Tick
                val limit = store.settings.value.playLimitMinutes
                if (limit > 0 && played >= limit.minutes) {
                    _bedtime.update { true }
                    clock = null
                    return@launch
                }
            }
        }
    }

    companion object {
        val Tick: Duration = 1.seconds
    }
}
