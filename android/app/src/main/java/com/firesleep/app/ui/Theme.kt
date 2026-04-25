@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.firesleep.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.firesleep.app.R

private fun interFont(weight: Int, fontWeight: FontWeight) =
    Font(
        resId = R.font.inter,
        weight = fontWeight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

private val Inter = FontFamily(
    interFont(200, FontWeight.ExtraLight),
    interFont(300, FontWeight.Light),
    interFont(400, FontWeight.Normal),
    interFont(500, FontWeight.Medium),
    interFont(600, FontWeight.SemiBold),
)

object Tokens {
    val BgCanvas = Color(0xFF0A0907)
    val TextPrimary = Color(0xFFF2EAD8)
    val TextBody = Color(0xFFE8E0D4)
    val TextMuted = Color(0xFFE8E0D4).copy(alpha = 0.55f)
    val TextDim = Color(0xFFE8E0D4).copy(alpha = 0.40f)
    val TextFaint = Color(0xFFE8E0D4).copy(alpha = 0.25f)
    val SurfaceSoft = Color(0xFFE8E0D4).copy(alpha = 0.06f)
    val Divider = Color(0xFFE8E0D4).copy(alpha = 0.10f)
    val Accent = Color(0xFFD9A46A)
    val AccentOn = BgCanvas
    val AccentBorderFaint = Accent.copy(alpha = 0.20f)
    val OverlayCardBg = Color(0xCC0C0A08)
    val OverlayScrim = Color(0x8C000000)

    val Mono = FontFamily.Monospace
    val Sans = Inter
}

@Composable
fun FireSleepTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        background = Tokens.BgCanvas,
        surface = Tokens.BgCanvas,
        primary = Tokens.Accent,
        onPrimary = Tokens.AccentOn,
        onBackground = Tokens.TextBody,
        onSurface = Tokens.TextBody,
    )
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}

fun heroHeadline() = TextStyle(
    fontFamily = Tokens.Sans,
    fontWeight = FontWeight.ExtraLight,
    fontSize = 76.sp,
    lineHeight = 80.sp,
    letterSpacing = (-2).sp,
    color = Tokens.TextPrimary,
)

fun heroNumeric() = TextStyle(
    fontFamily = Tokens.Sans,
    fontWeight = FontWeight.ExtraLight,
    fontSize = 260.sp,
    lineHeight = 260.sp,
    letterSpacing = (-10).sp,
    color = Tokens.TextPrimary,
)

fun reelDigit() = TextStyle(
    fontFamily = Tokens.Sans,
    fontWeight = FontWeight.ExtraLight,
    fontSize = 160.sp,
    lineHeight = 160.sp,
    letterSpacing = (-6).sp,
    color = Tokens.TextPrimary,
)

fun presetNumber(focused: Boolean) = TextStyle(
    fontFamily = Tokens.Sans,
    fontWeight = FontWeight.ExtraLight,
    fontSize = 88.sp,
    lineHeight = 88.sp,
    letterSpacing = (-3).sp,
    color = if (focused) Tokens.TextPrimary else Tokens.TextDim,
)

fun eyebrow(color: Color) = TextStyle(
    fontFamily = Tokens.Sans,
    fontWeight = FontWeight.Medium,
    fontSize = 20.sp,
    lineHeight = 20.sp,
    letterSpacing = 5.sp,
    color = color,
)

fun eyebrowSmall(color: Color) = TextStyle(
    fontFamily = Tokens.Sans,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    letterSpacing = 3.sp,
    color = color,
)

fun body(color: Color) = TextStyle(
    fontFamily = Tokens.Sans,
    fontWeight = FontWeight.Light,
    fontSize = 24.sp,
    lineHeight = 32.sp,
    color = color,
)

fun hintMono(color: Color) = TextStyle(
    fontFamily = Tokens.Mono,
    fontWeight = FontWeight.Normal,
    fontSize = 20.sp,
    color = color,
)
