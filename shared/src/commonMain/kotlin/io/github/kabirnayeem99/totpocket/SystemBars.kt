package io.github.kabirnayeem99.totpocket

import androidx.compose.runtime.Composable

/**
 * Keeps the status and navigation bars hidden, hiding them again whenever something reveals
 * them — unless a grown-up has [allowed] them. No-op where there are none.
 */
@Composable
expect fun KeepSystemBarsHidden(allowed: Boolean)
