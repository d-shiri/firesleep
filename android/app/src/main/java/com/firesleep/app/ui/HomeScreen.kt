package com.firesleep.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.runtime.LaunchedEffect

data class Preset(val mins: Int, val label: String, val sub: String)

private val PRESETS = listOf(
    Preset(15, "Quick doze", "one ad break"),
    Preset(30, "Short nap", "half an hour"),
    Preset(45, "One episode", "usual length"),
    Preset(60, "One hour", "a whole movie act"),
    Preset(90, "Long movie", "full film length"),
)

@Composable
fun HomeScreen(
    initialFocusIndex: Int,
    onPresetSelected: (minutes: Int) -> Unit,
    onCustomSelected: () -> Unit,
) {
    var focusIndex by remember { mutableIntStateOf(initialFocusIndex.coerceIn(0, PRESETS.size)) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Tokens.BgCanvas)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0x1FD29650), Color.Transparent),
                    radius = 1200f,
                ),
            )
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> {
                        focusIndex = (focusIndex - 1).coerceAtLeast(0)
                        true
                    }
                    Key.DirectionDown -> {
                        focusIndex = (focusIndex + 1).coerceAtMost(PRESETS.size)
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (focusIndex < PRESETS.size) {
                            onPresetSelected(PRESETS[focusIndex].mins)
                        } else {
                            onCustomSelected()
                        }
                        true
                    }
                    else -> false
                }
            },
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            LeftColumn(modifier = Modifier.width(640.dp))
            PresetLadder(
                focusIndex = focusIndex,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
            )
        }
    }
}

@Composable
private fun LeftColumn(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(start = 120.dp, top = 120.dp, end = 80.dp, bottom = 80.dp),
    ) {
        Text("◐ FireSleep", style = eyebrow(Tokens.Accent))
        Spacer(Modifier.height(80.dp))
        Text(
            text = "Drift off.\nWe'll take\ncare of the\nTV.",
            style = heroHeadline().copy(fontWeight = FontWeight.ExtraLight),
        )
        Spacer(Modifier.height(48.dp))
        Text(
            "Audio and brightness fade over the final minute. No jarring shutoff.",
            style = body(Tokens.TextMuted),
            modifier = Modifier.width(400.dp),
        )
        Spacer(Modifier.weight(1f))
        Text("▲ ▼ Choose · ● Begin", style = hintMono(Tokens.TextDim))
    }
}

@Composable
private fun PresetLadder(
    focusIndex: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(start = 80.dp, top = 120.dp, end = 120.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        PRESETS.forEachIndexed { i, preset ->
            PresetRow(preset = preset, focused = focusIndex == i)
        }
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (focusIndex == PRESETS.size) Tokens.SurfaceSoft else Color.Transparent,
                )
                .padding(horizontal = 32.dp, vertical = 16.dp),
        ) {
            Text(
                "+ Custom length",
                style = body(
                    if (focusIndex == PRESETS.size) Tokens.TextPrimary else Tokens.TextDim,
                ).copy(fontSize = 22.sp, fontWeight = FontWeight.Light),
            )
        }
    }
}

@Composable
private fun PresetRow(preset: Preset, focused: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Tokens.SurfaceSoft else Color.Transparent)
            .drawLeftBorder(if (focused) Tokens.Accent else Color.Transparent, widthDp = 3.dp)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(modifier = Modifier.width(180.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = preset.mins.toString(),
                    style = presetNumber(focused),
                )
                Text(
                    text = "m",
                    style = presetNumber(focused).copy(fontSize = 26.sp, color = Tokens.TextDim, letterSpacing = 0.sp),
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
                )
            }
        }
        Spacer(Modifier.width(40.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                preset.label,
                style = body(if (focused) Tokens.TextPrimary else Tokens.TextMuted)
                    .copy(fontSize = 34.sp, fontWeight = FontWeight.Normal),
            )
            Spacer(Modifier.height(4.dp))
            Text(preset.sub, style = body(Tokens.TextDim).copy(fontSize = 20.sp))
        }
        Text(
            text = "● BEGIN",
            style = hintMono(if (focused) Tokens.Accent else Color.Transparent).copy(letterSpacing = 2.sp),
        )
    }
}
