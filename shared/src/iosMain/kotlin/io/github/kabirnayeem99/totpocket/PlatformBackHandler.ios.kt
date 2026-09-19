package io.github.kabirnayeem99.totpocket

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

@Composable
actual fun rememberSystemBack(): () -> Unit = {}
