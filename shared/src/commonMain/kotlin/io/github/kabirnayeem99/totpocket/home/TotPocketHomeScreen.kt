package io.github.kabirnayeem99.totpocket.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import io.github.kabirnayeem99.totpocket.navigation.Route
import io.github.kabirnayeem99.totpocket.ui.components.SectionCard
import io.github.kabirnayeem99.totpocket.ui.components.SectionGlyph
import io.github.kabirnayeem99.totpocket.ui.components.isLandscape
import io.github.kabirnayeem99.totpocket.ui.icons.TotPocketIcons
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketColors
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketDimens
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketTheme

@Immutable
private data class HomeSection(
    val route: Route,
    val label: String,
    val icon: ImageVector,
    val container: Color,
    val content: Color,
)

private val Sections = listOf(
    HomeSection(Route.Calls.Contacts, "Calls", TotPocketIcons.Phone, TotPocketColors.Red, TotPocketColors.OnDark),
    HomeSection(Route.Gallery.Categories, "Gallery", TotPocketIcons.Paw, TotPocketColors.Blue, TotPocketColors.OnDark),
    HomeSection(Route.Games.Picker, "Games", TotPocketIcons.Puzzle, TotPocketColors.Yellow, TotPocketColors.OnLight),
)

/**
 * Three full-bleed cards that split the whole screen — no scrolling, nothing off-screen.
 * Stacks vertically in portrait, side by side in landscape. [overlay] sits on top (the hidden
 * parent-gate corner).
 */
@Composable
fun TotPocketHomeScreen(
    onOpen: (Route) -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = {},
) {
    Box(modifier.fillMaxSize().background(TotPocketColors.Background)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(TotPocketDimens.ScreenPadding)) {
            val spacing = Arrangement.spacedBy(TotPocketDimens.CardSpacing)
            if (isLandscape(maxWidth, maxHeight)) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = spacing) {
                    Sections.forEach { HomeCard(it, onOpen, Modifier.weight(1f).fillMaxSize()) }
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = spacing) {
                    Sections.forEach { HomeCard(it, onOpen, Modifier.weight(1f).fillMaxSize()) }
                }
            }
        }
        overlay()
    }
}

@Composable
private fun HomeCard(section: HomeSection, onOpen: (Route) -> Unit, modifier: Modifier) {
    SectionCard(
        onClick = { onOpen(section.route) },
        label = section.label,
        color = section.container,
        contentColor = section.content,
        modifier = modifier,
    ) {
        SectionGlyph(section.icon, section.content)
    }
}

@Preview
@Composable
private fun TotPocketHomeScreenPreview() {
    TotPocketTheme { TotPocketHomeScreen(onOpen = {}) }
}
