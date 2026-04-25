package com.firesleep.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.firesleep.app.R

/**
 * Branded eyebrow: the flame logo + "FireSleep" wordmark.
 * Used on Home, Settings, and Confirm screens in place of the old "◐ FireSleep"
 * text glyph.
 */
@Composable
fun LogoEyebrow(
    wordmark: String = "FireSleep",
    style: TextStyle = eyebrow(Tokens.Accent),
    flameSize: Dp = 32.dp,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_flame),
            contentDescription = null,
            modifier = Modifier.height(flameSize),
        )
        Text(wordmark, style = style)
    }
}
