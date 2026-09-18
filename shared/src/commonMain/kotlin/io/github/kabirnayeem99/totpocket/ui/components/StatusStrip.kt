package io.github.kabirnayeem99.totpocket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kabirnayeem99.totpocket.home.wallClockNow
import kotlinx.coroutines.delay

/** Height of the drawn status strip — the space a real status bar takes at the top of every app. */
val StatusStripHeight = 28.dp

/**
 * The real status bar stays hidden, so every screen draws its own at the top: the time on the
 * left and a battery on the right, in the screen's [contentColor].
 */
@Composable
fun StatusStrip(contentColor: Color, modifier: Modifier = Modifier) {
    var time by remember { mutableStateOf(wallClockNow().time) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            time = wallClockNow().time
        }
    }
    Row(
        modifier.fillMaxWidth().height(StatusStripHeight).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(time, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(width = 22.dp, height = 11.dp).border(1.3.dp, contentColor, RoundedCornerShape(3.dp)).padding(2.dp),
        ) {
            Box(Modifier.fillMaxHeight().width(13.dp).background(contentColor, RoundedCornerShape(1.dp)))
        }
        Box(Modifier.padding(start = 1.dp).size(width = 2.dp, height = 4.dp).background(contentColor, RoundedCornerShape(1.dp)))
    }
}
