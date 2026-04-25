package com.firesleep.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OverlayScreen(
    secondsRemaining: Long,
    onSnooze: () -> Unit,
    onCancel: () -> Unit,
) {
    var focus by remember { mutableIntStateOf(0) } // 0 = +10 min, 1 = keep watching
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val phase = ((60f - secondsRemaining.coerceIn(0L, 60L)) / 60f).coerceIn(0f, 1f)
    val dimAlpha = 0.55f + phase * 0.30f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1A1510), Color(0xFF0A0807), Color(0xFF150F08)),
                ),
            )
            .background(Color.Black.copy(alpha = dimAlpha))
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { focus = 0; true }
                    Key.DirectionRight -> { focus = 1; true }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (focus == 0) onSnooze() else onCancel(); true
                    }
                    else -> false
                }
            },
    ) {
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 72.dp)) {
            OverlayCard(
                seconds = secondsRemaining,
                phase = phase,
                focus = focus,
            )
        }
    }
}

@Composable
private fun OverlayCard(
    seconds: Long,
    phase: Float,
    focus: Int,
) {
    Column(
        modifier = Modifier
            .widthIn(min = 520.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Tokens.OverlayCardBg)
            .border(
                width = 1.dp,
                color = Tokens.Accent.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 32.dp, vertical = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Moon(size = 72.dp, phase = phase, cutoutColor = Tokens.OverlayCardBg)
            Column(modifier = Modifier.padding(start = 24.dp)) {
                Text(
                    "Fading to sleep".uppercase(),
                    style = eyebrowSmall(Tokens.Accent),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "0:${seconds.toString().padStart(2, '0')}",
                    style = body(Tokens.TextPrimary)
                        .copy(fontSize = 44.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(
                text = "+ 10 minutes",
                focused = focus == 0,
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                text = "Keep watching",
                focused = focus == 1,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.padding(horizontal = 0.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                "◀ ▶ choose · ● confirm · ignore to sleep",
                style = hintMono(Tokens.TextDim).copy(fontSize = 15.sp),
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Tokens.Accent else Tokens.SurfaceSoft)
            .border(
                width = if (focused) 0.dp else 1.dp,
                color = if (focused) Color.Transparent else Tokens.Divider,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = body(if (focused) Tokens.AccentOn else Tokens.TextBody)
                .copy(
                    fontSize = 20.sp,
                    fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
                ),
        )
    }
}

