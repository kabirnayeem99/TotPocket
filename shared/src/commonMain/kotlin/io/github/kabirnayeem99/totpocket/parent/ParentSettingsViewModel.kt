package io.github.kabirnayeem99.totpocket.parent

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kabirnayeem99.totpocket.device.DeviceController
import io.github.kabirnayeem99.totpocket.settings.ParentSettings
import io.github.kabirnayeem99.totpocket.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@Immutable
data class ParentSettingsUiState(
    val settings: ParentSettings,
    val isPinned: Boolean,
)

sealed interface ParentSettingsAction {
    data class VolumeChanged(val fraction: Float) : ParentSettingsAction
    data class PlayLimitChosen(val minutes: Int) : ParentSettingsAction
    data object PinToggled : ParentSettingsAction
    data object KeepPinnedToggled : ParentSettingsAction
    data object ShowSystemBarsToggled : ParentSettingsAction
    data object HomeAppToggled : ParentSettingsAction
    /** Forget the PIN; the gate asks for a new one next time. */
    data object ChangePin : ParentSettingsAction
    /** The screen came back to the front — the pin state may have changed outside the app. */
    data object Refresh : ParentSettingsAction
    data object ExitApp : ParentSettingsAction
}

class ParentSettingsViewModel(
    private val store: SettingsStore,
    private val device: DeviceController,
) : ViewModel() {

    private val pinned = MutableStateFlow(device.isPinned)

    val state: StateFlow<ParentSettingsUiState> = combine(store.settings, pinned, ::ParentSettingsUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ParentSettingsUiState(store.settings.value, device.isPinned),
        )

    fun onAction(action: ParentSettingsAction) {
        when (action) {
            is ParentSettingsAction.VolumeChanged -> store.update {
                it.copy(volumeCeiling = action.fraction.coerceIn(ParentSettings.MIN_VOLUME, 1f))
            }
            is ParentSettingsAction.PlayLimitChosen -> {
                if (action.minutes in ParentSettings.PlayLimitChoices) store.update { it.copy(playLimitMinutes = action.minutes) }
            }
            ParentSettingsAction.PinToggled -> {
                if (pinned.value) device.unpin() else device.pin()
                // The system confirms pinning asynchronously; show the intent now, Refresh corrects it.
                pinned.update { !it }
            }
            ParentSettingsAction.KeepPinnedToggled -> {
                store.update { it.copy(keepPinned = !it.keepPinned) }
                if (store.settings.value.keepPinned && !pinned.value) {
                    device.pin()
                    pinned.update { true }
                }
            }
            ParentSettingsAction.ShowSystemBarsToggled -> {
                store.update { it.copy(showSystemBars = !it.showSystemBars) }
                device.setSystemBarsAllowed(store.settings.value.showSystemBars)
            }
            ParentSettingsAction.HomeAppToggled -> {
                store.update { it.copy(homeApp = !it.homeApp) }
                device.setHomeApp(store.settings.value.homeApp, askToChoose = true)
            }
            ParentSettingsAction.ChangePin -> store.update { it.copy(pin = null) }
            ParentSettingsAction.Refresh -> pinned.update { device.isPinned }
            ParentSettingsAction.ExitApp -> device.exitApp()
        }
    }
}
