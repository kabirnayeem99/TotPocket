package io.github.kabirnayeem99.totpocket

import androidx.compose.runtime.Composable
import io.github.kabirnayeem99.totpocket.navigation.Route
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerScaffold
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketColors

/** A calm, empty screen in the section's tint with just the Home button — for sections not built yet. */
@Composable
internal fun ComingSoonScreen(route: Route, onHome: () -> Unit) {
    val tint = when (route) {
        is Route.Calls -> TotPocketColors.RedTint
        is Route.Gallery -> TotPocketColors.BlueTint
        is Route.Games -> TotPocketColors.YellowTint
        else -> TotPocketColors.Background
    }
    ToddlerScaffold(onHome = onHome, background = tint) {}
}
