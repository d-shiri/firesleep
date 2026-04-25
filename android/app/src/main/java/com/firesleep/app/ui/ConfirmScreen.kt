package com.firesleep.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")

@Composable
fun ConfirmScreen(
    minutes: Int,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(minutes) {
        delay(3_000)
        onDismiss()
    }
    val offAt = LocalTime.now().plusMinutes(minutes.toLong()).format(timeFmt)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Tokens.BgCanvas)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0x22D29650), Color.Transparent),
                    radius = 1400f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Moon(size = 120.dp, phase = 0f)
            Spacer(Modifier.height(48.dp))
            LogoEyebrow(
                wordmark = "FireSleep set",
                style = eyebrow(Tokens.Accent).copy(fontSize = 22.sp),
            )
            Spacer(Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(minutes.toString(), style = heroNumeric())
                Spacer(Modifier.size(16.dp))
                Text(
                    "min",
                    style = body(Tokens.TextMuted).copy(fontSize = 72.sp, letterSpacing = 0.sp),
                    modifier = Modifier.padding(bottom = 48.dp),
                )
            }
            Spacer(Modifier.height(40.dp))
            Text(
                buildAnnotatedString {
                    append("Good night. TV off at ")
                    withStyle(SpanStyle(color = Tokens.TextPrimary)) { append(offAt) }
                    append(".")
                },
                style = body(Tokens.TextMuted).copy(fontSize = 28.sp),
            )
        }
    }
}
