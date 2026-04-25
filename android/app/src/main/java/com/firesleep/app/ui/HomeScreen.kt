package com.firesleep.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

data class Preset(
    val minutes: Int,
    val seconds: Int,
    val display: String,   // big number, e.g. "30" or "15"
    val unit: String,      // "s" or "m"
    val label: String,
    val sub: String,
)

private fun preset(minutes: Int, label: String, sub: String) =
    Preset(
        minutes = minutes,
        seconds = minutes * 60,
        display = minutes.toString(),
        unit = "m",
        label = label,
        sub = sub,
    )

private val PRESETS = listOf(
    // TODO: remove the TEST row before shipping — it's only here to make the
    //   end-to-end flow (start → countdown → overlay → power-off) testable
    //   in 30 seconds instead of 15 minutes.
    Preset(minutes = 0, seconds = 30, display = "30", unit = "s", label = "TEST", sub = "30 seconds — remove for prod"),
    preset(15, "Quick doze", "one ad break"),
    preset(30, "Short nap", "half an hour"),
    preset(45, "One episode", "usual length"),
    preset(60, "One hour", "a whole movie act"),
    preset(90, "Long movie", "full film length"),
)

private enum class Region { Close, Settings, Preset }

@Composable
fun HomeScreen(
    initialFocusIndex: Int,
    onPresetSelected: (Preset) -> Unit,
    onCustomSelected: () -> Unit,
    onSettings: () -> Unit,
    onClose: () -> Unit,
) {
    var presetIndex by remember { mutableIntStateOf(initialFocusIndex.coerceIn(0, PRESETS.size)) }
    var region by remember { mutableStateOf(Region.Close) }
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
                        when (region) {
                            Region.Close -> {}
                            Region.Settings -> region = Region.Close
                            Region.Preset -> if (presetIndex == 0) region = Region.Close else presetIndex -= 1
                        }
                        true
                    }
                    Key.DirectionDown -> {
                        when (region) {
                            Region.Close -> region = Region.Preset
                            Region.Settings -> {} // single button — nowhere to go down
                            Region.Preset -> presetIndex = (presetIndex + 1).coerceAtMost(PRESETS.size)
                        }
                        true
                    }
                    Key.DirectionLeft -> {
                        if (region == Region.Preset || region == Region.Close) region = Region.Settings
                        true
                    }
                    Key.DirectionRight -> {
                        if (region == Region.Settings) region = Region.Preset
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        when (region) {
                            Region.Close -> onClose()
                            Region.Settings -> onSettings()
                            Region.Preset -> {
                                if (presetIndex < PRESETS.size) onPresetSelected(PRESETS[presetIndex])
                                else onCustomSelected()
                            }
                        }
                        true
                    }
                    Key.Back, Key.Escape -> { onClose(); true }
                    else -> false
                }
            },
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            LeftColumn(
                settingsFocused = region == Region.Settings,
                modifier = Modifier.width(420.dp),
            )
            PresetLadder(
                focusIndex = if (region == Region.Preset) presetIndex else -1,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
            )
        }
        CloseButton(
            focused = region == Region.Close,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 36.dp, end = 48.dp),
        )
    }
}

@Composable
private fun LeftColumn(settingsFocused: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(start = 80.dp, top = 80.dp, end = 40.dp, bottom = 60.dp),
    ) {
        LogoEyebrow()
        Spacer(Modifier.weight(1f))
        SettingsButton(focused = settingsFocused)
    }
}

@Composable
private fun SettingsButton(focused: Boolean) {
    val bg = if (focused) Tokens.Accent else Tokens.SurfaceSoft
    val fg = if (focused) Tokens.AccentOn else Tokens.TextBody
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Text(
            "⚙  Settings",
            style = body(fg).copy(
                fontSize = 22.sp,
                fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
                letterSpacing = 1.sp,
            ),
        )
    }
}

@Composable
private fun CloseButton(focused: Boolean, modifier: Modifier = Modifier) {
    val bg = if (focused) Tokens.Accent else Tokens.SurfaceSoft
    val fg = if (focused) Tokens.AccentOn else Tokens.TextMuted
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        Text(
            "✕  Close",
            style = body(fg).copy(
                fontSize = 22.sp,
                fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
                letterSpacing = 1.sp,
            ),
        )
    }
}

@Composable
private fun PresetLadder(
    focusIndex: Int,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    // Approximate row pitch (number + label + padding). Used to bring the
    // focused row into view so users on a small windowed canvas can still
    // reach the bottom presets and Custom.
    val rowPitchPx = with(androidx.compose.ui.platform.LocalDensity.current) { 150.dp.toPx() }.toInt()
    LaunchedEffect(focusIndex) {
        if (focusIndex < 0) return@LaunchedEffect
        val target = (focusIndex * rowPitchPx - rowPitchPx).coerceAtLeast(0)
        scrollState.animateScrollTo(target.coerceAtMost(scrollState.maxValue))
    }
    Column(
        modifier = modifier
            .padding(start = 40.dp, top = 80.dp, end = 80.dp, bottom = 60.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PRESETS.forEachIndexed { i, preset ->
            PresetRow(preset = preset, focused = focusIndex == i)
        }
        Spacer(Modifier.height(20.dp))
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
            .padding(horizontal = 32.dp, vertical = 20.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(modifier = Modifier.width(160.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = preset.display,
                    style = presetNumber(focused),
                )
                Text(
                    text = preset.unit,
                    style = presetNumber(focused).copy(fontSize = 26.sp, color = Tokens.TextDim, letterSpacing = 0.sp),
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
                )
            }
        }
        Spacer(Modifier.width(32.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                preset.label,
                style = body(if (focused) Tokens.TextPrimary else Tokens.TextMuted)
                    .copy(fontSize = 30.sp, fontWeight = FontWeight.Normal),
            )
            Spacer(Modifier.height(4.dp))
            Text(preset.sub, style = body(Tokens.TextDim).copy(fontSize = 18.sp))
        }
        Text(
            text = "● BEGIN",
            style = hintMono(if (focused) Tokens.Accent else Color.Transparent).copy(letterSpacing = 2.sp),
        )
    }
}
