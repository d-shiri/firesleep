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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun CustomScreen(
    onBegin: (minutes: Int) -> Unit,
    onBack: () -> Unit,
) {
    var hours by remember { mutableIntStateOf(1) }
    var minutes by remember { mutableIntStateOf(20) }
    var focusedCol by remember { mutableIntStateOf(1) } // 0 = hours, 1 = minutes
    val total = hours * 60 + minutes

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Tokens.BgCanvas)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        focusedCol = 0; true
                    }
                    Key.DirectionRight -> {
                        focusedCol = 1; true
                    }
                    Key.DirectionUp -> {
                        if (focusedCol == 0) hours = (hours + 1).coerceAtMost(8)
                        else minutes = (minutes + 5).coerceAtMost(55)
                        true
                    }
                    Key.DirectionDown -> {
                        if (focusedCol == 0) hours = (hours - 1).coerceAtLeast(0)
                        else minutes = (minutes - 5).coerceAtLeast(0)
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (total > 0) onBegin(total); true
                    }
                    Key.Back, Key.Escape -> {
                        onBack(); true
                    }
                    else -> false
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 120.dp, vertical = 96.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("◀ Back", style = eyebrow(Tokens.TextDim).copy(fontSize = 20.sp, letterSpacing = 4.sp))
                Spacer(Modifier.width(20.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Tokens.TextDim),
                )
                Spacer(Modifier.width(20.dp))
                Text("Custom length", style = eyebrow(Tokens.Accent))
            }
            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Reel(
                    value = hours, min = 0, max = 8, step = 1, pad = 1,
                    unit = "hours", focused = focusedCol == 0,
                )
                Text(
                    ":",
                    style = reelDigit().copy(color = Tokens.TextFaint, letterSpacing = (-4).sp),
                    modifier = Modifier.padding(bottom = 24.dp),
                )
                Reel(
                    value = minutes, min = 0, max = 55, step = 5, pad = 2,
                    unit = "minutes", focused = focusedCol == 1,
                )

                Spacer(Modifier.width(80.dp))

                Column(
                    modifier = Modifier
                        .widthIn(min = 360.dp)
                        .drawLeftBorder(Tokens.Divider, 1.dp)
                        .padding(start = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column {
                        Text("Total".uppercase(), style = eyebrowSmall(Tokens.TextDim))
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                total.toString(),
                                style = heroNumeric().copy(fontSize = 64.sp, lineHeight = 64.sp, letterSpacing = (-2).sp),
                            )
                            Text(
                                " min",
                                style = body(Tokens.TextMuted).copy(fontSize = 28.sp),
                                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
                            )
                        }
                    }
                    Column {
                        Text("Lights out".uppercase(), style = eyebrowSmall(Tokens.TextDim))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            lightsOutString(total),
                            style = body(Tokens.TextPrimary)
                                .copy(fontSize = 40.sp, fontWeight = FontWeight.Light),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Tokens.Accent)
                            .padding(horizontal = 28.dp, vertical = 18.dp),
                    ) {
                        Text(
                            "● Begin",
                            style = body(Tokens.AccentOn).copy(fontSize = 24.sp, fontWeight = FontWeight.Medium),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "◀ ▶ Switch column  ·  ▲ ▼ Adjust value  ·  ● Begin",
                    style = hintMono(Tokens.TextDim),
                )
            }
        }
    }
}

@Composable
private fun Reel(
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    pad: Int,
    unit: String,
    focused: Boolean,
) {
    val above = if (value - step >= min) (value - step) else null
    val below = if (value + step <= max) (value + step) else null
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (focused) Tokens.SurfaceSoft else Color.Transparent)
            .border(
                width = 2.dp,
                color = if (focused) Tokens.Accent else Color.Transparent,
                shape = RoundedCornerShape(20.dp),
            )
            .widthIn(min = 260.dp)
            .padding(horizontal = 32.dp, vertical = 20.dp),
    ) {
        Text(
            above?.toString()?.padStart(pad, '0') ?: "—",
            style = body(Tokens.TextFaint).copy(fontSize = 36.sp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            value.toString().padStart(pad, '0'),
            style = reelDigit(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            below?.toString()?.padStart(pad, '0') ?: "—",
            style = body(Tokens.TextFaint).copy(fontSize = 36.sp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            unit.uppercase(),
            style = eyebrowSmall(Tokens.TextDim.copy(alpha = 0.45f))
                .copy(fontSize = 18.sp, letterSpacing = 4.sp),
        )
    }
}

private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")

private fun lightsOutString(totalMinutes: Int): String {
    val now = LocalTime.now()
    val then = now.plusMinutes(totalMinutes.toLong())
    return then.format(timeFmt)
}
