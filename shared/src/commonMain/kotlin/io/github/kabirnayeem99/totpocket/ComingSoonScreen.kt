package io.github.kabirnayeem99.totpocket

import androidx.compose.runtime.Composable
import io.github.kabirnayeem99.totpocket.ui.components.AppChromeStyles
import io.github.kabirnayeem99.totpocket.ui.components.AppScaffold

/** An empty screen with just the app chrome — for places not built yet. */
@Composable
internal fun ComingSoonScreen(onBack: () -> Unit, onHome: () -> Unit) {
    AppScaffold(title = "", style = AppChromeStyles.System, onBack = onBack, onHome = onHome) {}
}
