package io.github.kabirnayeem99.totpocket.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.kabirnayeem99.totpocket.ui.icons.TotPocketIcons
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketColors
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketDimens
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketTheme

enum class HomeSection { Calls, Gallery, Games }

@Immutable
private data class HomeCardSpec(
    val section: HomeSection,
    val label: String,
    val icon: ImageVector,
    val container: Color,
    val content: Color,
)

private val HomeCards = listOf(
    HomeCardSpec(HomeSection.Calls, "Calls", TotPocketIcons.Phone, TotPocketColors.Red, TotPocketColors.OnDark),
    HomeCardSpec(HomeSection.Gallery, "Gallery", TotPocketIcons.Paw, TotPocketColors.Blue, TotPocketColors.OnDark),
    HomeCardSpec(HomeSection.Games, "Games", TotPocketIcons.Puzzle, TotPocketColors.Yellow, TotPocketColors.OnLight),
)

/**
 * Three full-bleed cards that split the whole screen — no scrolling, nothing off-screen.
 * Stacks vertically in portrait, side by side in landscape.
 */
@Composable
fun TotPocketHomeScreen(
    onSectionClick: (HomeSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(TotPocketColors.Background)
            .padding(TotPocketDimens.ScreenPadding),
    ) {
        val spacing = Arrangement.spacedBy(TotPocketDimens.CardSpacing)
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = spacing) {
                HomeCards.forEach { spec ->
                    HomeCard(spec, onSectionClick, Modifier.weight(1f).fillMaxSize())
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = spacing) {
                HomeCards.forEach { spec ->
                    HomeCard(spec, onSectionClick, Modifier.weight(1f).fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun HomeCard(
    spec: HomeCardSpec,
    onSectionClick: (HomeSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "homeCardScale",
    )
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(TotPocketDimens.CardCorner)

    Column(
        modifier = modifier
            .sizeIn(minWidth = TotPocketDimens.MinTouchTarget, minHeight = TotPocketDimens.MinTouchTarget)
            // Scale read in the draw phase only — no recomposition per animation frame.
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(spec.container)
            .border(BorderStroke(TotPocketDimens.CardOutline, TotPocketColors.Outline), shape)
            .clickable(
                interactionSource = interactionSource,
                // The bounce is the feedback; a ripple on a huge card reads as visual noise.
                indication = null,
                role = Role.Button,
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onSectionClick(spec.section)
            }
            .semantics(mergeDescendants = true) { contentDescription = spec.label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = spec.icon,
            contentDescription = null,
            tint = spec.content,
            modifier = Modifier.size(TotPocketDimens.IconSize),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = spec.label,
            color = spec.content,
            fontSize = TotPocketDimens.LabelSize,
            fontWeight = FontWeight.Black,
        )
    }
}

@Preview
@Composable
private fun TotPocketHomeScreenPreview() {
    TotPocketTheme { TotPocketHomeScreen(onSectionClick = {}) }
}
