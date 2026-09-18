package io.github.kabirnayeem99.totpocket

import androidx.compose.runtime.Composable

/** Hides the status and navigation bars again whenever something reveals them. No-op where there are none. */
@Composable
expect fun KeepSystemBarsHidden()
