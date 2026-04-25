package com.firesleep.app.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Pins the composition to a fixed 1920×1080 design canvas regardless of the
 * device's actual density. Inside the lambda, 1.dp == 1 design pixel —
 * matching the 1:1 mapping the design spec assumes.
 *
 * Without this, Fire TV's xhdpi (1dp = 2px at 1080p) would scale everything
 * 2× larger than the mocks, blowing the layout off-screen.
 */
@Composable
fun DesignCanvas(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val pxHeight = constraints.maxHeight.toFloat()
        val systemDensity = LocalDensity.current
        val targetDensity = (pxHeight / DESIGN_HEIGHT_DP).coerceAtLeast(0.1f)
        val custom = Density(
            density = targetDensity,
            fontScale = systemDensity.fontScale,
        )
        CompositionLocalProvider(LocalDensity provides custom) {
            content()
        }
    }
}

private const val DESIGN_HEIGHT_DP = 1080f
