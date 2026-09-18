package io.github.kabirnayeem99.totpocket.ui.launcher

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * The continuous-curvature "squircle" HyperOS uses for app icons: a superellipse
 * |x|ⁿ + |y|ⁿ = 1. Unlike a rounded rectangle, the curve eases into the straight edges.
 */
class SquircleShape(private val exponent: Double = 4.6) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val rx = size.width / 2f
        val ry = size.height / 2f
        val path = Path()
        for (i in 0..SEGMENTS) {
            val t = 2 * PI * i / SEGMENTS
            val c = cos(t)
            val s = sin(t)
            val x = rx + rx * (sign(c) * abs(c).pow(2 / exponent)).toFloat()
            val y = ry + ry * (sign(s) * abs(s).pow(2 / exponent)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }

    private companion object {
        const val SEGMENTS = 96
    }
}

val HyperOsIconShape = SquircleShape()
