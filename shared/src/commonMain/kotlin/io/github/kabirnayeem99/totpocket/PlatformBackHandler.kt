package io.github.kabirnayeem99.totpocket

import androidx.compose.runtime.Composable

/** Intercepts the platform back gesture/button. No-op where the platform has none (iOS). */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
