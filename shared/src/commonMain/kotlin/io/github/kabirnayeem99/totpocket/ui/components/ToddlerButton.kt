package io.github.kabirnayeem99.totpocket.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketColors
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketDimens
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/** Short UI sound played on every toddler tap. Provided at the app root; silent by default. */
val LocalTapSound = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * The one tappable surface every child-facing control is built on: a big outlined shape that
 * bounces on press, ticks the vibrator, plays the tap sound, and ignores repeat taps within
 * [debounce] (toddlers mash). Never smaller than [TotPocketDimens.MinTouchTarget].
 */
@Composable
fun ToddlerButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(TotPocketDimens.CardCorner),
    color: Color = TotPocketColors.Surface,
    outline: Color = TotPocketColors.Outline,
    outlineWidth: Dp = TotPocketDimens.CardOutline,
    playTapSound: Boolean = true,
    debounce: Duration = 400.milliseconds,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "toddlerPressScale",
    )
    val haptics = LocalHapticFeedback.current
    val tapSound = LocalTapSound.current
    val click = rememberDebouncedClick(debounce) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (playTapSound) tapSound()
        onClick()
    }

    Box(
        modifier = modifier
            .sizeIn(minWidth = TotPocketDimens.MinTouchTarget, minHeight = TotPocketDimens.MinTouchTarget)
            // Scale is read in the draw phase only — no recomposition per animation frame.
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(color)
            .border(outlineWidth, outline, shape)
            .clickable(
                interactionSource = interactionSource,
                // The bounce is the feedback; a ripple across a big shape reads as visual noise.
                indication = null,
                role = Role.Button,
                onClick = click,
            )
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Wraps [onClick] so taps closer together than [window] are dropped. */
@Composable
fun rememberDebouncedClick(window: Duration = 400.milliseconds, onClick: () -> Unit): () -> Unit {
    val latest by rememberUpdatedState(onClick)
    return remember(window) {
        var last: TimeSource.Monotonic.ValueTimeMark? = null
        val debounced: () -> Unit = {
            val mark = last
            if (mark == null || mark.elapsedNow() >= window) {
                last = TimeSource.Monotonic.markNow()
                latest()
            }
        }
        debounced
    }
}
