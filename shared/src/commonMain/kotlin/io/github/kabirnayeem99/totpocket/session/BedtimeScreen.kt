package io.github.kabirnayeem99.totpocket.session

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import io.github.kabirnayeem99.totpocket.ui.components.HoldToActivate

/**
 * Shown over everything when play time is up: a dim night sky and a slowly breathing moon.
 * Taps do nothing. Only a grown-up holding the top-right corner gets past it.
 */
@Composable
fun BedtimeScreen(onParentHold: () -> Unit, modifier: Modifier = Modifier) {
    val breathing = rememberInfiniteTransition(label = "moon")
    val scale by breathing.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(3_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "moonScale",
    )
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0E1330), Color(0xFF1F2A55))))
            // Swallow every tap so nothing underneath can be reached.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .semantics { contentDescription = "Bedtime" },
    ) {
        Text(
            "🌙",
            fontSize = 160.sp,
            modifier = Modifier.align(Alignment.Center).graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        )
        Text("✨", fontSize = 36.sp, modifier = Modifier.align(Alignment.TopStart).graphicsLayer { translationX = 120f; translationY = 360f; alpha = 0.7f })
        Text("✨", fontSize = 28.sp, modifier = Modifier.align(Alignment.BottomEnd).graphicsLayer { translationX = -140f; translationY = -420f; alpha = 0.6f })
        HoldToActivate(onActivated = onParentHold, modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp))
    }
}

@Preview
@Composable
private fun BedtimePreview() {
    BedtimeScreen(onParentHold = {})
}
