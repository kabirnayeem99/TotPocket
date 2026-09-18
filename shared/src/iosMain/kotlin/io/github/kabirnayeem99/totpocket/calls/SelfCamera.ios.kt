package io.github.kabirnayeem99.totpocket.calls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun SelfCamera(modifier: Modifier, fallback: @Composable () -> Unit) = fallback()
