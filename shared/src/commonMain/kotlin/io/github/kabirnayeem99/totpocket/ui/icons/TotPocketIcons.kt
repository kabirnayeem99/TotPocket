package io.github.kabirnayeem99.totpocket.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Chunky filled glyphs (Material Symbols path data, Apache 2.0) built in code so `commonMain`
 * needs no icon artifact. Tint at the call site.
 */
object TotPocketIcons {

    val Phone: ImageVector by lazy {
        icon(
            "Phone",
            "M20.01,15.38c-1.23,0 -2.42,-0.2 -3.53,-0.56 -0.35,-0.12 -0.74,-0.03 -1.01,0.24" +
                "l-1.57,1.97c-2.83,-1.35 -5.48,-3.9 -6.89,-6.83l1.95,-1.66c0.27,-0.28 0.35,-0.67 0.24,-1.02" +
                " -0.37,-1.11 -0.56,-2.3 -0.56,-3.53 0,-0.54 -0.45,-0.99 -0.99,-0.99H4.19C3.65,3 3,3.24 3,3.99" +
                " 3,13.28 10.73,21 20.01,21c0.71,0 0.99,-0.63 0.99,-1.18v-3.45c0,-0.54 -0.45,-0.99 -0.99,-0.99z",
        )
    }

    val Paw: ImageVector by lazy {
        icon(
            "Paw",
            "M4.5,9.5m-2.5,0a2.5,2.5 0,1 1,5 0a2.5,2.5 0,1 1,-5 0",
            "M9,5.5m-2.5,0a2.5,2.5 0,1 1,5 0a2.5,2.5 0,1 1,-5 0",
            "M15,5.5m-2.5,0a2.5,2.5 0,1 1,5 0a2.5,2.5 0,1 1,-5 0",
            "M19.5,9.5m-2.5,0a2.5,2.5 0,1 1,5 0a2.5,2.5 0,1 1,-5 0",
            "M17.34,14.86c-0.87,-1.02 -1.6,-1.89 -2.48,-2.91 -0.46,-0.54 -1.05,-1.08 -1.75,-1.32" +
                " -0.11,-0.04 -0.22,-0.07 -0.33,-0.09 -0.25,-0.04 -0.52,-0.04 -0.78,-0.04s-0.53,0 -0.79,0.05" +
                "c-0.11,0.02 -0.22,0.05 -0.33,0.09 -0.7,0.24 -1.28,0.78 -1.75,1.32 -0.87,1.02 -1.6,1.89 -2.48,2.91" +
                " -1.31,1.31 -2.92,2.76 -2.62,4.79 0.29,1.02 1.02,2.03 2.33,2.32 0.73,0.15 3.06,-0.44 5.54,-0.44" +
                "h0.18c2.48,0 4.81,0.58 5.54,0.44 1.31,-0.29 2.04,-1.31 2.33,-2.32 0.31,-2.04 -1.3,-3.49 -2.61,-4.8z",
        )
    }

    val Puzzle: ImageVector by lazy {
        icon(
            "Puzzle",
            "M20.5,11H19V7c0,-1.1 -0.9,-2 -2,-2h-4V3.5C13,2.12 11.88,1 10.5,1S8,2.12 8,3.5V5H4" +
                "c-1.1,0 -1.99,0.9 -1.99,2v3.8H3.5c1.49,0 2.7,1.21 2.7,2.7s-1.21,2.7 -2.7,2.7H2V20" +
                "c0,1.1 0.9,2 2,2h3.8v-1.5c0,-1.49 1.21,-2.7 2.7,-2.7 1.49,0 2.7,1.21 2.7,2.7V22H17" +
                "c1.1,0 2,-0.9 2,-2v-4h1.5c1.38,0 2.5,-1.12 2.5,-2.5S21.88,11 20.5,11z",
        )
    }

    val Home: ImageVector by lazy {
        icon("Home", "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z")
    }

    /** Solid triangles read as "go this way" better than thin arrows for pre-readers. */
    val ArrowRight: ImageVector by lazy { icon("ArrowRight", "M8,4.5v15l11.5,-7.5z") }

    val ArrowLeft: ImageVector by lazy { icon("ArrowLeft", "M16,4.5v15L4.5,12z") }

    val HangUp: ImageVector by lazy {
        icon(
            "HangUp",
            "M12,9c-1.6,0 -3.15,0.25 -4.6,0.72v3.1c0,0.39 -0.23,0.74 -0.56,0.9 -0.98,0.49 -1.87,1.12 -2.66,1.85" +
                " -0.18,0.18 -0.43,0.28 -0.7,0.28 -0.28,0 -0.53,-0.11 -0.71,-0.29L0.29,13.08c-0.18,-0.17 -0.29,-0.42" +
                " -0.29,-0.7 0,-0.28 0.11,-0.53 0.29,-0.71C3.34,8.78 7.46,7 12,7s8.66,1.78 11.71,4.67c0.18,0.18 0.29,0.43" +
                " 0.29,0.71 0,0.28 -0.11,0.53 -0.29,0.71l-2.48,2.48c-0.18,0.18 -0.43,0.29 -0.71,0.29 -0.27,0 -0.52,-0.11" +
                " -0.7,-0.28 -0.79,-0.74 -1.69,-1.36 -2.67,-1.85 -0.33,-0.16 -0.56,-0.5 -0.56,-0.9v-3.1C15.15,9.25 13.6,9 12,9z",
        )
    }

    val Back: ImageVector by lazy { icon("Back", "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z") }

    val Video: ImageVector by lazy {
        icon(
            "Video",
            "M17,10.5V7c0,-0.55 -0.45,-1 -1,-1H4c-0.55,0 -1,0.45 -1,1v10c0,0.55 0.45,1 1,1h12c0.55,0 1,-0.45 1,-1v-3.5l4,4v-11l-4,4z",
        )
    }

    val Mic: ImageVector by lazy {
        icon(
            "Mic",
            "M12,14c1.66,0 2.99,-1.34 2.99,-3L15,5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3z" +
                "M17.3,11c0,3 -2.54,5.1 -5.3,5.1S6.7,14 6.7,11L5,11c0,3.41 2.72,6.23 6,6.72L11,21h2v-3.28" +
                "c3.28,-0.48 6,-3.3 6,-6.72h-1.7z",
        )
    }

    val Speaker: ImageVector by lazy {
        icon(
            "Speaker",
            "M3,9v6h4l5,5V4L7,9H3z" +
                "M16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02z" +
                "M14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z",
        )
    }

    val Lock: ImageVector by lazy {
        icon(
            "Lock",
            "M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10" +
                "c0,-1.1 -0.9,-2 -2,-2zm-6,9c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2 -0.9,2 -2,2z" +
                "m3.1,-9H8.9V6c0,-1.71 1.39,-3.1 3.1,-3.1 1.71,0 3.1,1.39 3.1,3.1v2z",
        )
    }

    /** 3×3 dots plus a centred bottom dot, like a phone keypad. */
    val Keypad: ImageVector by lazy {
        val dots = buildList {
            for (row in 0..3) for (column in 0..2) {
                if (row == 3 && column != 1) continue
                val x = 6f + 6f * column
                val y = 3f + 6f * row
                add("M${x - 2.4f},${y}a2.4,2.4 0,1 0,4.8 0a2.4,2.4 0,1 0,-4.8 0z")
            }
        }
        icon("Keypad", *dots.toTypedArray())
    }

    private fun icon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            paths.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) }
        }.build()
}
