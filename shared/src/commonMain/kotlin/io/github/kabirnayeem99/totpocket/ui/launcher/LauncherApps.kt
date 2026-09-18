package io.github.kabirnayeem99.totpocket.ui.launcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import io.github.kabirnayeem99.totpocket.navigation.Route
import io.github.kabirnayeem99.totpocket.calls.CallApp
import org.jetbrains.compose.resources.DrawableResource
import totpocket.shared.generated.resources.Res
import totpocket.shared.generated.resources.icon_google_photos
import totpocket.shared.generated.resources.icon_imo
import totpocket.shared.generated.resources.icon_truecaller

/** What's drawn inside an app's squircle: a vector glyph or the app's own logo image. */
sealed interface LauncherArt {
    /** Fraction of the squircle the art fills. */
    val scale: Float

    data class Glyph(val vector: ImageVector, override val scale: Float = 0.62f) : LauncherArt
    data class Logo(val image: DrawableResource, override val scale: Float) : LauncherArt
}

/**
 * The apps on TotPocket's pretend home screen, drawn like the real icons the child knows: a
 * HyperOS squircle with the app's own logo and brand colours.
 */
enum class LauncherApp(
    val label: String,
    /** Squircle colour. */
    val background: Color,
    val route: Route,
) {
    Phone("Truecaller", Color.White, Route.Calls.Contacts(CallApp.Phone)),
    WhatsApp("WhatsApp", BrandColors.WhatsApp, Route.Calls.Contacts(CallApp.WhatsApp)),
    Imo("imo", Color.White, Route.Calls.Contacts(CallApp.Imo)),
    Gallery("Photos", Color.White, Route.Gallery.Categories),
    YouTube("YouTube", Color.White, Route.YouTube),
    Games("Games", BrandColors.Games, Route.Games.Picker);

    val art: LauncherArt
        get() = when (this) {
            // The Truecaller square icon is full-bleed; the squircle crops it like the launcher does.
            Phone -> LauncherArt.Logo(Res.drawable.icon_truecaller, scale = 1f)
            WhatsApp -> LauncherArt.Glyph(LauncherGlyphs.WhatsApp, scale = 0.66f)
            Imo -> LauncherArt.Logo(Res.drawable.icon_imo, scale = 0.74f)
            Gallery -> LauncherArt.Logo(Res.drawable.icon_google_photos, scale = 0.68f)
            YouTube -> LauncherArt.Glyph(LauncherGlyphs.YouTube, scale = 0.8f)
            Games -> LauncherArt.Glyph(LauncherGlyphs.Games)
        }
}

object BrandColors {
    /** WhatsApp green, from the WhatsApp brand glyph. */
    val WhatsApp = Color(0xFF00AC55)
    val YouTube = Color(0xFFEE3124)
    val Imo = Color(0xFF35A0F9)
    /** Google blue, from the Google Photos pinwheel. */
    val Gallery = Color(0xFF4285F4)
    /** Truecaller blue. */
    val Phone = Color(0xFF0027FF)
    /** Google Play Games green. */
    val Games = Color(0xFF01875F)
}

object LauncherGlyphs {

    /** The WhatsApp brand glyph (WhatsApp Brand Resource Center), in white. */
    val WhatsApp: ImageVector by lazy {
        icon(
            name = "WhatsApp",
            viewport = 720f,
            layers = listOf(
                Layer(
                    "M360,0C161.18,0,0,161.18,0,360c0,65.41,17.45,126.75,47.94,179.61L0,720l187.02-44.21c51.34,28.18,110.28," +
                        "44.21,172.98,44.21,198.82,0,360-161.18,360-360S558.82,0,360,0ZM360,655.52c-60.17,0-116.13-17.98" +
                        "-162.82-48.87l-110.49,28.14,30.99-105.61c-33.53-47.93-53.2-106.26-53.2-169.19,0-163.21,132.31-295.52," +
                        "295.52-295.52s295.52,132.31,295.52,295.52-132.31,295.52-295.52,295.52Z",
                    fill = Color.White,
                ),
                Layer(
                    "M444.35,407.52l87.1,41.06c4,1.88,6.56,5.94,6.2,10.34-.94,11.46-5.54,34.43-26.13,55.02-58.12,58.12" +
                        "-162.49-7.64-166.74-10.18-25.67-13.79-50.06-32.24-73.19-55.36-23.12-23.12-41.58-47.52-55.37-73.19" +
                        "-2.55-4.24-68.31-108.61-10.18-166.74,20.59-20.59,43.56-25.19,55.02-26.13,4.41-.36,8.46,2.2,10.34,6.2" +
                        "l41.07,87.1c1.94,4.12,1.09,9.02-2.13,12.24l-30.61,30.61c-6.62,6.62-8.56,16.93-4,25.11,11.17,20.03," +
                        "26.19,39.32,43.59,57.07,17.75,17.4,37.04,32.43,57.07,43.59,8.18,4.56,18.48,2.62,25.11-4l30.61-30.61" +
                        "c3.22-3.22,8.12-4.08,12.24-2.13Z",
                    fill = Color.White,
                ),
            ),
        )
    }

    /** The YouTube 2017 icon (seeklogo SVG): red rounded play button with a white triangle. */
    val YouTube: ImageVector by lazy {
        icon(
            name = "YouTube",
            viewport = 192f,
            layers = listOf(
                Layer(
                    "M180.3,53.4c-2-7.6-8-13.6-15.6-15.7C151,34,96,34,96,34s-55,0-68.8,3.7c-7.6,2-13.5,8-15.6,15.7" +
                        "C8,67.2,8,96,8,96s0,28.8,3.7,42.6c2,7.6,8,13.6,15.6,15.7C41,158,96,158,96,158s55,0,68.8-3.7" +
                        "c7.6-2,13.5-8,15.6-15.7C184,124.8,184,96,184,96S184,67.2,180.3,53.4z",
                    fill = BrandColors.YouTube,
                ),
                Layer("M78,122.2L78,69.8L124,96Z", fill = Color.White),
            ),
        )
    }

    /** The Google Play Games gamepad, solid white with its buttons and d-pad cut out. */
    val Games: ImageVector by lazy {
        icon(
            name = "Games",
            viewport = 48f,
            layers = listOf(
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
            ),
        )
    }

    /** One path of a glyph: filled, stroked or both. */
    private class Layer(
        val pathData: String,
        val fill: Color? = null,
        val stroke: Color? = null,
        val width: Float = 0f,
        /** Fill inner sub-paths as holes (buttons cut out of a body). */
        val evenOdd: Boolean = false,
    )

    private fun icon(name: String, viewport: Float, layers: List<Layer>): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = viewport,
            viewportHeight = viewport,
        ).apply {
            layers.forEach { layer ->
                addPath(
                    pathData = addPathNodes(layer.pathData),
                    pathFillType = if (layer.evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
                    fill = layer.fill?.let(::SolidColor),
                    stroke = layer.stroke?.let(::SolidColor),
                    strokeLineWidth = layer.width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}
