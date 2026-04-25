package com.firesleep.app.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

fun Modifier.drawLeftBorder(color: Color, widthDp: Dp): Modifier = this.drawBehind {
    if (color.alpha == 0f) return@drawBehind
    val strokePx = widthDp.toPx()
    drawRect(
        color = color,
        topLeft = Offset(0f, 0f),
        size = androidx.compose.ui.geometry.Size(strokePx, size.height),
    )
}
