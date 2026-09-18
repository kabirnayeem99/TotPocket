package io.github.kabirnayeem99.totpocket.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import io.github.kabirnayeem99.totpocket.ui.icons.TotPocketIcons
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketColors
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketDimens

/** Green house in a white circle: the same place on every screen, always back to Home. */
@Composable
fun HomeButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    RoundIconButton(
        onClick = onClick,
        icon = TotPocketIcons.Home,
        contentDescription = "Home",
        color = TotPocketColors.Surface,
        iconTint = TotPocketColors.Green,
        size = TotPocketDimens.HomeButtonSize,
        modifier = modifier,
    )
}

@Composable
fun RoundIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    color: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    size: Dp = TotPocketDimens.RoundActionSize,
    playTapSound: Boolean = true,
) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        shape = CircleShape,
        color = color,
        playTapSound = playTapSound,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(size * 0.55f))
    }
}

/** A big coloured card with a glyph and an (adult-facing, optional-to-read) label. */
@Composable
fun SectionCard(
    onClick: () -> Unit,
    label: String,
    color: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    picture: @Composable () -> Unit,
) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = label,
        modifier = modifier,
        color = color,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            picture()
            Spacer(Modifier.height(TotPocketDimens.TightSpacing))
            Text(
                text = label,
                color = contentColor,
                fontSize = TotPocketDimens.LabelSize,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/** The glyph used on [SectionCard]s. */
@Composable
fun SectionGlyph(icon: ImageVector, tint: Color, size: Dp = TotPocketDimens.IconSize) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size))
}

/**
 * A picture tile (an emoji stands in for a photo): grows slightly and gets a thick ring while
 * [highlighted] — e.g. while its sound is playing.
 */
@Composable
fun PictureTile(
    onClick: () -> Unit,
    emoji: String,
    contentDescription: String,
    background: Color,
    highlightColor: Color,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    playTapSound: Boolean = false,
) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        shape = RoundedCornerShape(TotPocketDimens.TileCorner),
        color = background,
        outline = if (highlighted) highlightColor else TotPocketColors.Outline,
        outlineWidth = if (highlighted) TotPocketDimens.CardOutline * 2 else TotPocketDimens.CardOutline,
        playTapSound = playTapSound,
    ) {
        Emoji(emoji, grow = highlighted)
    }
}

@Composable
fun Emoji(emoji: String, modifier: Modifier = Modifier, grow: Boolean = false) {
    Text(
        text = emoji,
        fontSize = if (grow) TotPocketDimens.EmojiSize * 1.15f else TotPocketDimens.EmojiSize,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
