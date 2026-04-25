package com.firesleep.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp

/**
 * Draws a crescent moon. `phase` is 0f..1f; 0 = wide crescent, 1 = nearly eaten.
 * Implemented by drawing a filled accent circle, then a `cutoutColor` circle
 * offset slightly to carve out the crescent — matches the CSS
 * `box-shadow: inset X px -Y px 0 0 accent` trick in the mocks.
 */
@Composable
fun Moon(
    size: Dp,
    phase: Float = 0f,
    color: Color = Tokens.Accent,
    cutoutColor: Color = Tokens.BgCanvas,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val d = this.size.minDimension
        val radius = d / 2f
        val center = Offset(d / 2f, d / 2f)
        val eat = 0.25f + 0.33f * phase.coerceIn(0f, 1f)
        val vOffset = -0.08f * d
        rotate(degrees = -15f, pivot = center) {
            drawCircle(color = color, radius = radius, center = center)
            drawCircle(
                color = cutoutColor,
                radius = radius,
                center = Offset(x = center.x + eat * d, y = center.y + vOffset),
            )
        }
    }
}
