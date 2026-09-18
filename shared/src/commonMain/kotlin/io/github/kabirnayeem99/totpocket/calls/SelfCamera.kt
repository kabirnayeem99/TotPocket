package io.github.kabirnayeem99.totpocket.calls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The front camera's live picture, for the self-view of pretend video calls. Only a preview —
 * nothing is recorded or saved. Shows [fallback] when there's no camera or no permission.
 */
@Composable
expect fun SelfCamera(modifier: Modifier = Modifier, fallback: @Composable () -> Unit)
