package io.github.kabirnayeem99.totpocket.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Plays a phone video from its content [uri] once, pausing while [paused], calling [onFinished] at the
 * end. Nothing plays next on its own. Stops when it leaves the screen.
 */
@Composable
expect fun VideoSurface(uri: String, playKey: Int, paused: Boolean, onFinished: () -> Unit, modifier: Modifier = Modifier)
