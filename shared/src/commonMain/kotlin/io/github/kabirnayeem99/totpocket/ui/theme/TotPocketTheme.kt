package io.github.kabirnayeem99.totpocket.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Toddler palette: saturated primaries on a warm off-white, outlined in near-black.
 * Every foreground/background pair clears WCAG AA (4.5:1) for large glyphs.
 */
object TotPocketColors {
    val Background = Color(0xFFFFF8E7)
    val Surface = Color(0xFFFFFFFF)
    val Outline = Color(0xFF1B1B1B)

    val Red = Color(0xFFD32F2F) // white on red ≈ 5.0:1
    val Blue = Color(0xFF1565C0) // white on blue ≈ 5.7:1
    val Yellow = Color(0xFFFFD600) // near-black on yellow ≈ 13:1
    val Green = Color(0xFF2E7D32) // white on green ≈ 5.1:1

    /** Soft tint of the Games colour, behind the shape board. */
    val YellowTint = Color(0xFFFFF4B8)

    val OnDark = Color.White
    val OnLight = Outline
}

object TotPocketDimens {
    val CardSpacing = 24.dp
    val TightSpacing = 12.dp
    val CardCorner = 48.dp
    val CardOutline = 6.dp

    /** Toddler touch target floor: ~2x Material's 48dp minimum. */
    val MinTouchTarget = 96.dp
    val LabelSize = 36.sp
}

@Composable
fun TotPocketTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = TotPocketColors.Red,
            onPrimary = TotPocketColors.OnDark,
            secondary = TotPocketColors.Blue,
            onSecondary = TotPocketColors.OnDark,
            tertiary = TotPocketColors.Yellow,
            onTertiary = TotPocketColors.OnLight,
            background = TotPocketColors.Background,
            onBackground = TotPocketColors.OnLight,
            surface = TotPocketColors.Background,
            onSurface = TotPocketColors.OnLight,
            outline = TotPocketColors.Outline,
        ),
        content = content,
    )
}
