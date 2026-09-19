package io.github.kabirnayeem99.totpocket.media

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap

/**
 * The colour most of [image] is made of: pixels are grouped into coarse colour boxes, the fullest
 * box wins, and its pixels are averaged. Meant for a small thumbnail (a few dozen pixels a side);
 * call it off the main thread. Nearly-white and nearly-black pixels count less, so a photo on a
 * white table still gives the table's subject a say.
 */
fun dominantColor(image: ImageBitmap): Color {
    val pixels = image.toPixelMap()
    val weight = FloatArray(BINS)
    val red = FloatArray(BINS)
    val green = FloatArray(BINS)
    val blue = FloatArray(BINS)
    for (y in 0 until pixels.height) {
        for (x in 0 until pixels.width) {
            val c = pixels[x, y]
            val bin = (c.red * 7.99f).toInt() * 64 + (c.green * 7.99f).toInt() * 8 + (c.blue * 7.99f).toInt()
            val brightness = (c.red + c.green + c.blue) / 3f
            val w = if (brightness > 0.92f || brightness < 0.06f) 0.25f else 1f
            weight[bin] += w
            red[bin] += c.red * w
            green[bin] += c.green * w
            blue[bin] += c.blue * w
        }
    }
    val best = weight.indices.maxByOrNull { weight[it] } ?: return Color.Black
    val total = weight[best].takeIf { it > 0f } ?: return Color.Black
    return Color(red[best] / total, green[best] / total, blue[best] / total)
}

private const val BINS = 8 * 8 * 8
