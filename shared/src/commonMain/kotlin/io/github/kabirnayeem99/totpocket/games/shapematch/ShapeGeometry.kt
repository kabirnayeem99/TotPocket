package io.github.kabirnayeem99.totpocket.games.shapematch

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Compose outline for each game shape, drawn to fill its bounds. */
fun ShapeKind.outline(): Shape = when (this) {
    ShapeKind.Circle -> CircleShape
    ShapeKind.Square -> RoundedCornerShape(16)
    ShapeKind.Triangle -> TriangleShape
    ShapeKind.Star -> StarShape
    ShapeKind.Heart -> HeartShape
    ShapeKind.Diamond -> DiamondShape
}

/** Each shape keeps one colour every round, so colour can help a child find its hole. */
fun ShapeKind.fill(): Color = when (this) {
    ShapeKind.Circle -> TotPocketColors.Red
    ShapeKind.Square -> TotPocketColors.Blue
    ShapeKind.Triangle -> TotPocketColors.Green
    ShapeKind.Star -> TotPocketColors.Yellow
    ShapeKind.Heart -> TotPocketColors.Red
    ShapeKind.Diamond -> TotPocketColors.Blue
}

fun ShapeKind.displayName(): String = name

private val TriangleShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, size.height * 0.06f)
    lineTo(size.width * 0.96f, size.height * 0.92f)
    lineTo(size.width * 0.04f, size.height * 0.92f)
    close()
}

private val DiamondShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width * 0.92f, size.height / 2f)
    lineTo(size.width / 2f, size.height)
    lineTo(size.width * 0.08f, size.height / 2f)
    close()
}

private val StarShape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height * 0.53f
    val outer = minOf(size.width, size.height) / 2f
    val inner = outer * 0.48f
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = -PI / 2 + i * PI / 5
        val x = cx + (radius * cos(angle)).toFloat()
        val y = cy + (radius * sin(angle)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private val HeartShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.5f, h * 0.92f)
    cubicTo(w * 0.08f, h * 0.66f, w * -0.02f, h * 0.34f, w * 0.2f, h * 0.16f)
    cubicTo(w * 0.35f, h * 0.04f, w * 0.5f, h * 0.14f, w * 0.5f, h * 0.28f)
    cubicTo(w * 0.5f, h * 0.14f, w * 0.65f, h * 0.04f, w * 0.8f, h * 0.16f)
    cubicTo(w * 1.02f, h * 0.34f, w * 0.92f, h * 0.66f, w * 0.5f, h * 0.92f)
    close()
}
