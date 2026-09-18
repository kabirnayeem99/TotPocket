package io.github.kabirnayeem99.totpocket

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.kabirnayeem99.totpocket.home.HomeSection
import io.github.kabirnayeem99.totpocket.home.TotPocketHomeScreen
import io.github.kabirnayeem99.totpocket.ui.icons.TotPocketIcons
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketColors
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketDimens
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketTheme

/** Root of the shared UI. `null` section = home. */
@Composable
@Preview
fun App() {
    var section by rememberSaveable { mutableStateOf<HomeSection?>(null) }

    // Always enabled: system Back never leaves the app. Inside a section it returns home;
    // on home it's swallowed.
    PlatformBackHandler(enabled = true) { section = null }

    TotPocketTheme {
        when (val current = section) {
            null -> TotPocketHomeScreen(onSectionClick = { section = it })
            else -> SectionPlaceholder(current, onHome = { section = null })
        }
    }
}

/** Stand-in until Calls / Gallery / Games land: a solid colour field and one big home button. */
@Composable
private fun SectionPlaceholder(section: HomeSection, onHome: () -> Unit) {
    val color = when (section) {
        HomeSection.Calls -> TotPocketColors.Red
        HomeSection.Gallery -> TotPocketColors.Blue
        HomeSection.Games -> TotPocketColors.Yellow
    }
    Box(
        modifier = Modifier.fillMaxSize().background(color).padding(TotPocketDimens.ScreenPadding),
        contentAlignment = Alignment.TopStart,
    ) {
        HomeButton(onHome)
    }
}

@Composable
private fun HomeButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(TotPocketDimens.MinTouchTarget * 1.25f)
            .clip(CircleShape)
            .background(Color.White)
            .border(TotPocketDimens.CardOutline, TotPocketColors.Outline, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = TotPocketIcons.Home,
            contentDescription = "Home",
            tint = TotPocketColors.Green,
            modifier = Modifier.size(72.dp),
        )
    }
}
