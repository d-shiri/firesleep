package com.firesleep.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Centers content in a 67% × 67% card over a translucent scrim, so the app
 * feels overlaid on whatever's behind (launcher, or the previous app).
 */
@Composable
fun WindowedShell(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Tokens.OverlayScrim),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.67f)
                .clip(RoundedCornerShape(28.dp))
                .background(Tokens.BgCanvas),
        ) {
            content()
        }
    }
}
