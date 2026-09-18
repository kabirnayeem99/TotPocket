package io.github.kabirnayeem99.totpocket.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kabirnayeem99.totpocket.navigation.Route
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerButton
import io.github.kabirnayeem99.totpocket.ui.components.isLandscape
import io.github.kabirnayeem99.totpocket.ui.launcher.HyperOsIconShape
import io.github.kabirnayeem99.totpocket.ui.launcher.LauncherApp
import io.github.kabirnayeem99.totpocket.ui.launcher.LauncherArt
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Apps on the page, with labels — like the first page of a Xiaomi home screen. */
private val PageApps = listOf(LauncherApp.YouTube, LauncherApp.Imo, LauncherApp.Games)

/** Apps in the dock, without labels — the dialer, WhatsApp and Photos are what she reaches for most. */
private val DockApps = listOf(LauncherApp.Phone, LauncherApp.WhatsApp, LauncherApp.Gallery)

/** Real-launcher icon size; the whole icon-plus-label cell is the touch target. */
private val IconSize = 60.dp
private const val Columns = 4

/**
 * TotPocket's home, dressed as a HyperOS home screen on a playful sky wallpaper: a status line with
 * the time, a clock widget, labelled app icons on a 4-column grid and a frosted dock. [overlay]
 * sits on top (the hidden parent-gate corner).
 */
@Composable
fun LauncherHomeScreen(
    onOpen: (Route) -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = {},
) {
    var clock by remember { mutableStateOf(wallClockNow()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            clock = wallClockNow()
        }
    }
    Box(modifier.fillMaxSize().background(SkyGradient)) {
        SkyDecorations(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            StatusLine(clock.time)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 20.dp)) {
                if (isLandscape(maxWidth, maxHeight)) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        ClockWidget(clock, Modifier.weight(1f).padding(start = 16.dp))
                        Column(Modifier.weight(1.4f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                            AppGrid(PageApps, showLabels = true, onOpen = onOpen)
                            Spacer(Modifier.height(16.dp))
                            Dock(onOpen)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        ClockWidget(clock, Modifier.padding(start = 12.dp, top = 20.dp))
                        Spacer(Modifier.weight(1f))
                        AppGrid(PageApps, showLabels = true, onOpen = onOpen)
                        Spacer(Modifier.height(24.dp))
                        PageDots()
                        Spacer(Modifier.height(14.dp))
                        Dock(onOpen)
                    }
                }
            }
        }
        overlay()
    }
}

/** The real status bar is hidden, so the home screen draws its own: time on the left, battery right. */
@Composable
private fun StatusLine(time: String) {
    Row(
        Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(time, style = LabelStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.weight(1f))
        // Battery: outline, fill and the little cap.
        Box(
            Modifier.size(width = 24.dp, height = 12.dp).border(1.5.dp, Color.White, RoundedCornerShape(3.dp)).padding(2.dp),
        ) {
            Box(Modifier.fillMaxHeight().width(14.dp).background(Color.White, RoundedCornerShape(1.dp)))
        }
        Box(Modifier.padding(start = 1.dp).size(width = 2.dp, height = 5.dp).background(Color.White, RoundedCornerShape(1.dp)))
    }
}

@Composable
private fun AppGrid(apps: List<LauncherApp>, showLabels: Boolean, onOpen: (Route) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        repeat(Columns) { index ->
            val app = apps.getOrNull(index)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (app != null) LauncherIcon(app, showLabel = showLabels, onClick = { onOpen(app.route) })
            }
        }
    }
}

@Composable
private fun Dock(onOpen: (Route) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(30.dp))
            .padding(vertical = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            DockApps.forEach { app -> LauncherIcon(app, showLabel = false, onClick = { onOpen(app.route) }) }
        }
    }
}

/** An app icon at real launcher size; the whole cell (icon and label) bounces and takes the tap. */
@Composable
fun LauncherIcon(
    app: LauncherApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = app.label,
        modifier = modifier.width(84.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        outline = Color.Transparent,
        outlineWidth = 1.dp,
        minSize = 76.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(IconSize).clip(HyperOsIconShape).background(app.background),
                contentAlignment = Alignment.Center,
            ) {
                when (val art = app.art) {
                    is LauncherArt.Glyph -> Icon(
                        art.vector,
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(IconSize * art.scale),
                    )
                    is LauncherArt.Logo -> Image(
                        painterResource(art.image),
                        contentDescription = null,
                        modifier = Modifier.size(IconSize * art.scale),
                    )
                }
            }
            if (showLabel) {
                Spacer(Modifier.height(6.dp))
                Text(app.label, style = LabelStyle.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ClockWidget(clock: WallClock, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(clock.time, style = LabelStyle.copy(fontSize = 72.sp, fontWeight = FontWeight.Light))
        Text(clock.date, style = LabelStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium))
    }
}

/** Decoration only — like the launcher's page indicator. There is just the one page. */
@Composable
private fun PageDots() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Box(Modifier.size(width = 14.dp, height = 6.dp).background(Color.White, CircleShape))
        Spacer(Modifier.width(6.dp))
        Box(Modifier.size(6.dp).background(Color.White.copy(alpha = 0.55f), CircleShape))
    }
}

/** A soft morning sky: blue at the top fading through mint to a warm sunny yellow. */
private val SkyGradient = Brush.verticalGradient(
    listOf(Color(0xFF6EC6FF), Color(0xFF8ED8F8), Color(0xFFA8E6CF), Color(0xFFFFE29A)),
)

/** Fluffy clouds, a smiling sun and a few stars, drawn once — nothing moves. */
@Composable
private fun SkyDecorations(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // Sun, top right.
        drawCircle(Color(0xFFFFF1B8), radius = w * 0.15f, center = Offset(w * 0.84f, h * 0.2f))
        drawCircle(Color(0xFFFFD54F), radius = w * 0.11f, center = Offset(w * 0.84f, h * 0.2f))
        cloud(Offset(w * 0.18f, h * 0.36f), w * 0.1f)
        cloud(Offset(w * 0.7f, h * 0.43f), w * 0.08f)
        cloud(Offset(w * 0.32f, h * 0.53f), w * 0.06f)
        listOf(Offset(0.12f, 0.46f), Offset(0.55f, 0.33f), Offset(0.9f, 0.55f), Offset(0.45f, 0.62f)).forEach {
            star(Offset(w * it.x, h * it.y), w * 0.022f)
        }
    }
}

private fun DrawScope.cloud(centre: Offset, r: Float) {
    // Opaque, so the overlapping puffs read as one cloud with no seams.
    val white = Color(0xFFF7FCFF)
    drawCircle(white, r, centre)
    drawCircle(white, r * 0.8f, centre + Offset(-r * 1.1f, r * 0.25f))
    drawCircle(white, r * 0.85f, centre + Offset(r * 1.1f, r * 0.2f))
    drawCircle(white, r * 0.6f, centre + Offset(r * 1.9f, r * 0.45f))
    drawRect(white, topLeft = centre + Offset(-r * 1.1f, r * 0.2f), size = androidx.compose.ui.geometry.Size(r * 3f, r * 0.85f))
}

private fun DrawScope.star(centre: Offset, r: Float) {
    val path = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) r else r * 0.45f
        val angle = -PI / 2 + i * PI / 5
        val point = centre + Offset((radius * cos(angle)).toFloat(), (radius * sin(angle)).toFloat())
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(path, Color.White.copy(alpha = 0.9f))
}

private val LabelStyle = TextStyle(
    color = Color.White,
    fontSize = 15.sp,
    fontWeight = FontWeight.Medium,
    shadow = Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 2f), blurRadius = 6f),
)

@Preview
@Composable
private fun LauncherHomeScreenPreview() {
    TotPocketTheme { LauncherHomeScreen(onOpen = {}) }
}
