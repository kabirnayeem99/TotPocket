package io.github.kabirnayeem99.totpocket

import androidx.compose.ui.window.ComposeUIViewController
import io.github.kabirnayeem99.totpocket.audio.SilentSoundPlayer

private val container by lazy { AppContainer(soundPlayer = SilentSoundPlayer()) }

fun MainViewController() = ComposeUIViewController { App(container) }
