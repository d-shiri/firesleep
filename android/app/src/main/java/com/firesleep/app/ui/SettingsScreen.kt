package com.firesleep.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firesleep.app.net.TvClient
import com.firesleep.app.prefs.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(onSaved: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { SecurePrefs(context) }
    var ip by remember { mutableStateOf(prefs.piIp) }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Tokens.BgCanvas),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.width(720.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            LogoEyebrow()
            Text("First-time setup", style = heroHeadline().copy(fontSize = 56.sp))
            Text(
                "Point FireSleep at the Raspberry Pi that powers your TV off.",
                style = body(Tokens.TextMuted),
            )

            LabeledField(
                label = "Pi IP address",
                hint = "e.g. 192.168.2.15",
                value = ip,
                onValueChange = { ip = it },
                autoFocus = true,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PrimaryButton("Save") {
                    prefs.piIp = ip.trim()
                    onSaved()
                }
                SecondaryButton("Test connection") {
                    scope.launch {
                        val r = withContext(Dispatchers.IO) {
                            TvClient(TvClient.baseUrlFor(ip.trim())).health()
                        }
                        status = r.fold(
                            onSuccess = { "Reached the Pi ✓" },
                            onFailure = { "Couldn't reach the Pi: ${it.message}" },
                        )
                    }
                }
            }
            if (status.isNotBlank()) {
                Text(status, style = body(Tokens.TextMuted))
            }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    autoFocus: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (autoFocus) focusRequester.requestFocus() }
    Column {
        Text(label.uppercase(), style = eyebrowSmall(Tokens.TextDim))
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Tokens.SurfaceSoft)
                .border(
                    width = 2.dp,
                    color = if (focused) Tokens.Accent else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Tokens.TextPrimary,
                    fontSize = 24.sp,
                ),
                cursorBrush = SolidColor(Tokens.Accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused },
            )
            if (value.isEmpty()) {
                Text(hint, style = body(Tokens.TextFaint).copy(fontSize = 24.sp))
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    FocusableButton(
        text = text,
        onClick = onClick,
        unfocusedBg = Tokens.Accent,
        unfocusedFg = Tokens.AccentOn,
        unfocusedBorder = Color.Transparent,
        weight = FontWeight.Medium,
    )
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    FocusableButton(
        text = text,
        onClick = onClick,
        unfocusedBg = Tokens.SurfaceSoft,
        unfocusedFg = Tokens.TextBody,
        unfocusedBorder = Tokens.Divider,
        weight = FontWeight.Normal,
    )
}

@Composable
private fun FocusableButton(
    text: String,
    onClick: () -> Unit,
    unfocusedBg: Color,
    unfocusedFg: Color,
    unfocusedBorder: Color,
    weight: FontWeight,
) {
    var focused by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val bg = if (focused) Tokens.Accent else unfocusedBg
    val fg = if (focused) Tokens.AccentOn else unfocusedFg
    val border = if (focused) Tokens.TextBody.copy(alpha = 0.7f) else unfocusedBorder
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                width = if (focused) 3.dp else 2.dp,
                color = border,
                shape = RoundedCornerShape(12.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable(interactionSource = source)
            .clickable(interactionSource = source, indication = null) { onClick() }
            .padding(horizontal = 28.dp, vertical = 18.dp),
    ) {
        Text(
            text,
            style = body(fg).copy(
                fontSize = 22.sp,
                fontWeight = if (focused) FontWeight.SemiBold else weight,
            ),
        )
    }
}
