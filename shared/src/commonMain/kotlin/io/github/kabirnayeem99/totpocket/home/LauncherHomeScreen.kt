package io.github.kabirnayeem99.totpocket.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kabirnayeem99.totpocket.navigation.Route
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerButton
import io.github.kabirnayeem99.totpocket.ui.components.isLandscape
import io.github.kabirnayeem99.totpocket.ui.launcher.HyperOsIconShape
import io.github.kabirnayeem99.totpocket.ui.launcher.LauncherApp
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketDimens
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketTheme
import kotlinx.coroutines.delay

/** Apps on the page, with labels — like the first page of a Xiaomi home screen. */
private val PageApps = listOf(LauncherApp.YouTube, LauncherApp.Imo, LauncherApp.Games)

/** Apps in the dock, without labels — Phone, WhatsApp and Gallery are what she reaches for most. */
private val DockApps = listOf(LauncherApp.Phone, LauncherApp.WhatsApp, LauncherApp.Gallery)

/**
 * TotPocket's home, dressed as a HyperOS home screen: wallpaper, clock widget, labelled app icons
 * and a frosted dock. Every icon is still a full-size toddler target. [overlay] sits on top (the
 * hidden parent-gate corner).
 */
@Composable
fun LauncherHomeScreen(
    onOpen: (Route) -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = {},
) {
    Box(modifier.fillMaxSize().background(Wallpaper)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 28.dp)) {
            if (isLandscape(maxWidth, maxHeight)) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    ClockWidget(Modifier.weight(1f).padding(start = 16.dp))
                    Column(
                        modifier = Modifier.weight(1.4f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        AppRow(PageApps, showLabels = true, onOpen = onOpen)
                        Spacer(Modifier.height(20.dp))
                        Dock(onOpen)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    ClockWidget(Modifier.padding(start = 12.dp, top = 24.dp))
                    Spacer(Modifier.weight(1f))
                    AppRow(PageApps, showLabels = true, onOpen = onOpen)
                    Spacer(Modifier.height(28.dp))
                    PageDots()
                    Spacer(Modifier.height(16.dp))
                    Dock(onOpen)
                }
            }
        }
        overlay()
    }
}

@Composable
private fun AppRow(apps: List<LauncherApp>, showLabels: Boolean, onOpen: (Route) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        apps.forEach { app -> LauncherIcon(app, showLabel = showLabels, onClick = { onOpen(app.route) }) }
    }
}

@Composable
private fun Dock(onOpen: (Route) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(36.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
    ) {
        AppRow(DockApps, showLabels = false, onOpen = onOpen)
    }
}

/** An app icon: brand-coloured squircle, white glyph, optional white label underneath. */
@Composable
fun LauncherIcon(
    app: LauncherApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    size: Dp = TotPocketDimens.MinTouchTarget,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ToddlerButton(
            onClick = onClick,
            contentDescription = app.label,
            modifier = Modifier.size(size),
            shape = HyperOsIconShape,
            color = app.background,
            outline = Color.Transparent,
            outlineWidth = 1.dp,
        ) {
            Icon(app.glyph, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(size * 0.62f))
        }
        if (showLabel) {
            Spacer(Modifier.height(8.dp))
            Text(app.label, style = LabelStyle, maxLines = 1)
        }
    }
}

@Composable
private fun ClockWidget(modifier: Modifier = Modifier) {
    var clock by remember { mutableStateOf(wallClockNow()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            clock = wallClockNow()
        }
    }
    Column(modifier) {
        Text(clock.time, style = LabelStyle.copy(fontSize = 76.sp, fontWeight = FontWeight.Light))
        Text(clock.date, style = LabelStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium))
    }
}

/** Decoration only — like the launcher's page indicator. There is just the one page. */
@Composable
private fun PageDots() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Box(Modifier.size(width = 14.dp, height = 6.dp).background(Color.White, CircleShape))
        Spacer(Modifier.width(6.dp))
        Box(Modifier.size(6.dp).background(Color.White.copy(alpha = 0.5f), CircleShape))
    }
}

/** A soft blue-violet-pink gradient in the spirit of the HyperOS default wallpapers. */
private val Wallpaper = Brush.linearGradient(
    colors = listOf(Color(0xFF2E5BD8), Color(0xFF6B5BD6), Color(0xFFC77DBA), Color(0xFFF2A28A)),
    start = Offset(0f, 0f),
    end = Offset(900f, 2400f),
)

private val LabelStyle = TextStyle(
    color = Color.White,
    fontSize = 15.sp,
    fontWeight = FontWeight.Medium,
    shadow = Shadow(color = Color.Black.copy(alpha = 0.35f), offset = Offset(0f, 2f), blurRadius = 6f),
)

@Preview
@Composable
private fun LauncherHomeScreenPreview() {
    TotPocketTheme { LauncherHomeScreen(onOpen = {}) }
}
