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
