package io.github.kabirnayeem99.totpocket.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketDimens
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * An invisible corner a grown-up holds for [holdFor] to reach the parent area. A ring fills while
 * the finger stays down, so an adult can see it working; lifting early resets it. A toddler's
 * tap or brief press never gets there.
 */
@Composable
fun HoldToActivate(
    onActivated: () -> Unit,
    modifier: Modifier = Modifier,
    holdFor: Duration = 3.seconds,
    ringColor: Color = Color.White,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentOnActivated by rememberUpdatedState(onActivated)

    Box(
        modifier
            .size(TotPocketDimens.MinTouchTarget)
            .pointerInput(holdFor) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val hold = scope.launch {
                        progress.animateTo(1f, tween(holdFor.inWholeMilliseconds.toInt(), easing = LinearEasing))
                        currentOnActivated()
                    }
                    waitForUpOrCancellation()
                    hold.cancel()
                    scope.launch { progress.snapTo(0f) }
                }
            }
            .drawBehind {
                if (progress.value > 0f) {
                    val stroke = 6.dp.toPx()
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress.value,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(stroke * 2, stroke * 2),
                        size = androidx.compose.ui.geometry.Size(size.width - stroke * 4, size.height - stroke * 4),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            },
    )
}
