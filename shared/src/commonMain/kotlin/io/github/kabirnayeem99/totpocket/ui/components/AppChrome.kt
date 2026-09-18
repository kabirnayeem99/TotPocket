package io.github.kabirnayeem99.totpocket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kabirnayeem99.totpocket.ui.icons.TotPocketIcons
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketDimens

/** How an app's chrome looks — the child recognises apps by these colours. */
data class AppChromeStyle(
    val background: Color,
    val barColor: Color,
    val barContent: Color,
    /** The HyperOS gesture pill: dark on light screens, light on dark ones. */
    val gestureBar: Color,
)

object AppChromeStyles {
    /** HyperOS system apps: plain white with black titles. */
    val System = AppChromeStyle(Color.White, Color.White, Color(0xFF1B1B1B), Color(0xFF1B1B1B))
}

/**
 * Frame for every screen inside a pretend app: a real-looking top bar (large back arrow + title),
 * [content], and the HyperOS gesture pill at the bottom that goes Home. Nothing scrolls.
 */
@Composable
fun AppScaffold(
    title: String,
    style: AppChromeStyle,
    onBack: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().background(style.background)) {
        if (showTopBar) AppTopBar(title, style, onBack, actions)
        Box(Modifier.weight(1f).fillMaxWidth(), content = content)
        GestureBar(color = style.gestureBar, onHome = onHome)
    }
}

@Composable
fun AppTopBar(
    title: String,
    style: AppChromeStyle,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    // The drawn status strip, then a standard 56dp app bar — like any real app.
    Column(Modifier.fillMaxWidth().background(style.barColor)) {
        StatusStrip(contentColor = style.barContent)
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToddlerButton(
                onClick = onBack,
                contentDescription = "Back",
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.Transparent,
                outline = Color.Transparent,
                outlineWidth = 1.dp,
                minSize = 48.dp,
            ) {
                Icon(TotPocketIcons.Back, contentDescription = null, tint = style.barContent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                color = style.barContent,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
    }
}

/**
 * The HyperOS navigation pill. System bars are hidden, so this stands in for them: tapping
 * anywhere along the bottom strip goes Home, just as swiping up would on the real phone.
 */
@Composable
fun GestureBar(color: Color, onHome: () -> Unit, modifier: Modifier = Modifier) {
    // As thin as the real gesture area; the pill is the same size as HyperOS's.
    ToddlerButton(
        onClick = onHome,
        contentDescription = "Home",
        modifier = modifier.fillMaxWidth().height(GestureBarHeight),
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent,
        outline = Color.Transparent,
        outlineWidth = 1.dp,
        pressedScale = 1f,
        minSize = GestureBarHeight,
    ) {
        Box(Modifier.width(110.dp).height(4.dp).background(color, CircleShape))
    }
}

val GestureBarHeight = 20.dp

/** A round button with an icon and a small caption, as on real in-call screens. */
@Composable
fun CallControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    caption: String,
    background: Color,
    iconTint: Color,
    captionColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = TotPocketDimens.MinTouchTarget,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ToddlerButton(
            onClick = onClick,
            contentDescription = caption,
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = background,
            outline = Color.Transparent,
            outlineWidth = 1.dp,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(size * 0.42f))
        }
        Spacer(Modifier.height(8.dp))
        Text(caption, color = captionColor, fontSize = 14.sp)
    }
}
