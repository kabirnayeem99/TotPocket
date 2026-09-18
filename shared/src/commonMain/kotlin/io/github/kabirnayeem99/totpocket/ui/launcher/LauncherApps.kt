package io.github.kabirnayeem99.totpocket.ui.launcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp
import io.github.kabirnayeem99.totpocket.navigation.Route
import io.github.kabirnayeem99.totpocket.calls.CallApp

/**
 * The apps on TotPocket's pretend home screen, styled like the real icons the child knows:
 * a HyperOS squircle in the app's brand colour with a solid glyph. Glyph outlines come from the
 * SVG Repo line icons (48×48 viewBox), filled rather than traced so they read like app icons.
 */
enum class LauncherApp(
    val label: String,
    /** Squircle colour. */
    val background: Color,
    val route: Route,
) {
    Phone("Phone", BrandColors.Phone, Route.Calls.Contacts(CallApp.Phone)),
    WhatsApp("WhatsApp", BrandColors.WhatsApp, Route.Calls.Contacts(CallApp.WhatsApp)),
    Imo("imo", BrandColors.Imo, Route.Calls.Contacts(CallApp.Imo)),
    Gallery("Gallery", BrandColors.Gallery, Route.Gallery.Categories),
    YouTube("YouTube", Color.White, Route.YouTube),
    Games("Games", BrandColors.Games, Route.Games.Picker);

    val glyph: ImageVector
        get() = when (this) {
            Phone -> LauncherGlyphs.Phone
            WhatsApp -> LauncherGlyphs.WhatsApp
            Imo -> LauncherGlyphs.Imo
            Gallery -> LauncherGlyphs.Gallery
            YouTube -> LauncherGlyphs.YouTube
            Games -> LauncherGlyphs.Games
        }
}

object BrandColors {
    val WhatsApp = Color(0xFF25D366)
    val YouTube = Color(0xFFFF0000)
    val Imo = Color(0xFF1A8CFF)
    /** Google blue, as on Gallery Go. */
    val Gallery = Color(0xFF4285F4)
    /** HyperOS Phone green. */
    val Phone = Color(0xFF34C759)
    /** Google Play Games green. */
    val Games = Color(0xFF01875F)
}

object LauncherGlyphs {

    /** The handset from the phone SVG, without the call-blocker shield around it. */
    val Phone: ImageVector by lazy {
        icon(
            "Phone",
            scale = 1.5f,
            Layer(
                "M34.71,33.38a1.37,1.37,0,0,1-1.37,1.35h-.07a21.74,21.74,0,0,1-20-20,1.34,1.34,0,0,1,1.27-1.43h3a2.69,2.69," +
                    "0,0,1,2.63,2.3,13.31,13.31,0,0,0,.51,2.14,1.58,1.58,0,0,1-.4,1.56L18.62,21A19.22,19.22,0,0,0,27,29.34" +
                    "l1.77-1.75a1.32,1.32,0,0,1,1.38-.33,14.73,14.73,0,0,0,2.25.54,2.8,2.8,0,0,1,2.32,2.66Z",
                fill = Color.White,
                stroke = Color.White,
                width = 1.5f,
            ),
        )
    }

    /** White speech-bubble ring with a solid handset, like the WhatsApp logo. */
    val WhatsApp: ImageVector by lazy {
        icon(
            "WhatsApp",
            scale = 1f,
            Layer(
                "M24,2.5A21.52,21.52,0,0,0,5.15,34.36L2.5,45.5l11.14-2.65A21.5,21.5,0,1,0,24,2.5Z",
                stroke = Color.White,
                width = 3.6f,
            ),
            Layer(
                "M13.25,12.27h5.86a1,1,0,0,1,1,1,10.4,10.4,0,0,0,.66,3.91,1.93,1.93,0,0,1-.66,2.44l-2.05,2a18.6,18.6,0,0,0," +
                    "3.52,4.79A18.6,18.6,0,0,0,26.35,30l2-2.05c1-1,1.46-1,2.44-.66a10.4,10.4,0,0,0,3.91.66,1.05,1.05,0,0,1," +
                    "1,1v5.86a1.05,1.05,0,0,1-1,1,23.68,23.68,0,0,1-15.64-6.84,23.6,23.6,0,0,1-6.84-15.64A1.07,1.07,0,0,1," +
                    "13.25,12.27Z",
                fill = Color.White,
                stroke = Color.White,
                width = 1f,
                scale = 0.78f,
            ),
        )
    }

    /** Solid white bubble with "imo" lettered in the brand blue. */
    val Imo: ImageVector by lazy {
        val letters = BrandColors.Imo
        icon(
            "imo",
            scale = 1f,
            Layer(
                "m24,2.5C12.1259,2.5,2.5,12.1259,2.5,24c0,5.4902,2.0752,10.4837,5.4617,14.2834-.971,2.8007-2.9933,3.6747" +
                    "-5.4617,4.5671,2.2605,2.1812,6.6677,2.7591,10.6226-.3278,3.1938,1.8799,6.9033,2.9773,10.8774,2.9773," +
                    "11.8741,0,21.5-9.6259,21.5-21.5S35.8741,2.5,24,2.5Z",
                fill = Color.White,
            ),
            Layer(
                "M34.5488,19.8877a2.909,2.909,0,0,1,2.909,2.909v1.891a2.909,2.909,0,0,1-2.909,2.909h0a2.909,2.909,0,0,1" +
                    "-2.909-2.909v-1.891a2.909,2.909,0,0,1,2.909-2.909Z",
                stroke = letters,
            ),
            Layer("m16.9179,22.7967c0-1.6066,1.3024-2.909,2.909-2.909h0c1.6066,0,2.909,1.3024,2.909,2.909v4.7999", stroke = letters),
            Layer("M16.9179,19.8877L16.9179,27.5967", stroke = letters),
            Layer("m22.736,22.7967c0-1.6066,1.3024-2.909,2.909-2.909h0c1.6066,0,2.909,1.3024,2.909,2.909v4.7999", stroke = letters),
            Layer("M13.5602,19.8877L13.5602,27.5967", stroke = letters),
            Layer("M13.5602,16.6L13.5602,16.61", stroke = letters),
        )
    }

    /** A solid white photo with the sun and hill cut out in blue. */
    val Gallery: ImageVector by lazy {
        val cutout = BrandColors.Gallery
        icon(
            "Gallery",
            scale = 1.05f,
            Layer(
                "M8.5,9H39.5A3,3,0,0,1,42.5,12V36A3,3,0,0,1,39.5,39H8.5A3,3,0,0,1,5.5,36V12A3,3,0,0,1,8.5,9Z",
                fill = Color.White,
            ),
            Layer("M26,20a5,5,0,1,0,10,0a5,5,0,1,0,-10,0Z", fill = cutout),
            Layer("M5.5,21.6A17.4,17.4,0,0,1,22.9,39H8.5A3,3,0,0,1,5.5,36Z", fill = cutout),
        )
    }

    /** The real YouTube mark: red rounded play button with a white triangle, on white. */
    val YouTube: ImageVector by lazy {
        icon(
            "YouTube",
            scale = 1.08f,
            Layer(
                "M43.1124,14.394a5.0056,5.0056,0,0,0-3.5332-3.5332c-2.3145-.8936-24.7326-1.3314-31.2358.0256A5.0059,5.0059," +
                    "0,0,0,4.81,14.42c-1.0446,4.583-1.1239,14.4914.0256,19.1767A5.006,5.006,0,0,0,8.369,37.13c4.5829,1.0548," +
                    "26.3712,1.2033,31.2358,0a5.0057,5.0057,0,0,0,3.5332-3.5333C44.2518,28.6037,44.3311,19.31,43.1124,14.394Z",
                fill = BrandColors.YouTube,
            ),
            Layer("M30.5669,23.9952,20.1208,18.004V29.9863Z", fill = Color.White, stroke = Color.White, width = 1.2f),
        )
    }

    /** The Google Play Games gamepad, solid white with its buttons and d-pad cut out. */
    val Games: ImageVector by lazy {
        icon(
            "Games",
            scale = 1.05f,
            Layer(
                "M41.59,18h0a7.28,7.28,0,0,0-12.24-4.34H18.67a7.29,7.29,0,0,0-5-2h0A7.3,7.3,0,0,0,6.42,18h0L4.51,33.24" +
                    "c-.32,3.89,6.53,4,7.81,1.32l4.17-8.92h15l4.17,8.92c1.28,2.66,8.13,2.57,7.81-1.32Z" +
                    "m-4.68-.47h0a1.42,1.42,0,1,1-1.41,1.42h0A1.42,1.42,0,0,1,36.91,17.49Z" +
                    "m-7.2,1.42a1.42,1.42,0,0,1,1.42-1.42h0a1.42,1.42,0,1,1-1.42,1.42Z" +
                    "M12.23,22.05V20.33H10.51a.87.87,0,0,1-.87-.87V18.37a.87.87,0,0,1,.87-.88h1.72V15.78a.87.87,0,0,1,.87-.87" +
                    "h1.09a.87.87,0,0,1,.88.87v1.71h1.71a.87.87,0,0,1,.87.88v1.09a.87.87,0,0,1-.87.87H15.07v1.72a.87.87,0,0,1" +
                    "-.88.87H13.1a.86.86,0,0,1-.23-.05l-.5-.38A.85.85,0,0,1,12.23,22.05Z" +
                    "m23.21-.24h0A1.42,1.42,0,1,1,34,20.39h0A1.42,1.42,0,0,1,35.44,21.81Z" +
                    "M34,14.6A1.43,1.43,0,0,1,35.44,16h0A1.42,1.42,0,1,1,34,14.6Z",
                fill = Color.White,
                stroke = Color.White,
                width = 0.6f,
                evenOdd = true,
            ),
        )
    }

    /** One path of a glyph: filled, stroked or both. [scale] shrinks/grows just this path about the centre. */
    private class Layer(
        val pathData: String,
        val fill: Color? = null,
        val stroke: Color? = null,
        val width: Float = LETTER_STROKE,
        val scale: Float = 1f,
        /** Fill inner sub-paths as holes (buttons cut out of a body). */
        val evenOdd: Boolean = false,
    )

    private fun icon(name: String, scale: Float, vararg layers: Layer): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            group(scaleX = scale, scaleY = scale, pivotX = 24f, pivotY = 24f) {
                layers.forEach { layer ->
                    group(scaleX = layer.scale, scaleY = layer.scale, pivotX = 24f, pivotY = 24f) {
                        addPath(
                            pathData = addPathNodes(layer.pathData),
                            pathFillType = if (layer.evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
                            fill = layer.fill?.let(::SolidColor),
                            stroke = layer.stroke?.let(::SolidColor),
                            strokeLineWidth = if (layer.stroke != null) layer.width else 0f,
                            strokeLineCap = StrokeCap.Round,
                            strokeLineJoin = StrokeJoin.Round,
                        )
                    }
                }
            }
        }.build()

    private const val LETTER_STROKE = 3.2f
}
