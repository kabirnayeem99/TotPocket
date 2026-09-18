package io.github.kabirnayeem99.totpocket

import androidx.compose.ui.window.ComposeUIViewController
import io.github.kabirnayeem99.totpocket.audio.SilentSoundPlayer
import io.github.kabirnayeem99.totpocket.device.NoDeviceController
import io.github.kabirnayeem99.totpocket.settings.InMemorySettingsStore

private val container by lazy { AppContainer(soundPlayer = SilentSoundPlayer(), device = NoDeviceController, settingsStore = InMemorySettingsStore()) }

fun MainViewController() = ComposeUIViewController { App(container) }
