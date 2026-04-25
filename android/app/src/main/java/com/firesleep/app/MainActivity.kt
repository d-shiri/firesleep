package com.firesleep.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firesleep.app.prefs.SecurePrefs
import com.firesleep.app.timer.TimerController
import com.firesleep.app.ui.ConfirmScreen
import com.firesleep.app.ui.CustomScreen
import com.firesleep.app.ui.DesignCanvas
import com.firesleep.app.ui.FireSleepTheme
import com.firesleep.app.ui.HomeScreen
import com.firesleep.app.ui.OverlayScreen
import com.firesleep.app.ui.SettingsScreen
import com.firesleep.app.ui.Tokens
import com.firesleep.app.ui.WindowedShell
import com.firesleep.app.ui.body
import com.firesleep.app.ui.eyebrowSmall
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        setContent {
            FireSleepTheme {
                WindowedShell {
                    DesignCanvas { App(intent, dismiss = { finish() }) }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setContent {
            FireSleepTheme {
                WindowedShell {
                    DesignCanvas { App(intent, dismiss = { finish() }) }
                }
            }
        }
    }

    companion object {
        const val EXTRA_SHOW_OVERLAY = "show_overlay"
        const val EXTRA_ERROR = "error"
    }
}

private enum class Screen { Settings, Home, Custom, Confirming, Running, Warning }

private val ScreenSaver = Saver<Screen, Int>(
    save = { it.ordinal },
    restore = { Screen.values()[it] },
)

@Composable
private fun App(intent: Intent?, dismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { SecurePrefs(context) }
    val timerState by TimerController.state.collectAsState()

    val forceOverlay = intent?.getBooleanExtra(MainActivity.EXTRA_SHOW_OVERLAY, false) == true
    val errorText = intent?.getStringExtra(MainActivity.EXTRA_ERROR)

    var screen by rememberSaveable(stateSaver = ScreenSaver) {
        mutableStateOf(
            when {
                !prefs.isConfigured() -> Screen.Settings
                forceOverlay -> Screen.Warning
                timerState.running -> Screen.Running
                else -> Screen.Home
            },
        )
    }
    var confirmingSeconds by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(forceOverlay) {
        if (forceOverlay) screen = Screen.Warning
    }

    LaunchedEffect(timerState.warningActive) {
        if (timerState.warningActive && screen != Screen.Warning) {
            screen = Screen.Warning
        }
    }

    var secondsRemaining by remember { mutableLongStateOf(TimerController.secondsRemaining()) }
    LaunchedEffect(screen) {
        if (screen == Screen.Warning || screen == Screen.Running) {
            while (true) {
                secondsRemaining = TimerController.secondsRemaining()
                if (secondsRemaining <= 0L) break
                delay(500)
            }
        }
    }

    BackHandler(enabled = screen == Screen.Home || screen == Screen.Running) { dismiss() }

    Box(modifier = Modifier.fillMaxSize().background(Tokens.BgCanvas)) {
        when (screen) {
            Screen.Settings -> SettingsScreen(
                onSaved = { screen = Screen.Home },
                onBack = { if (prefs.isConfigured()) screen = Screen.Home else dismiss() },
            )
            Screen.Home -> HomeScreen(
                initialFocusIndex = focusIndexForPreset(prefs.lastPresetMinutes),
                onPresetSelected = { preset ->
                    if (preset.minutes > 0) prefs.lastPresetMinutes = preset.minutes
                    confirmingSeconds = preset.seconds
                    TimerController.start(context, preset.seconds)
                    screen = Screen.Confirming
                },
                onCustomSelected = { screen = Screen.Custom },
                onSettings = { screen = Screen.Settings },
                onClose = dismiss,
            )
            Screen.Custom -> CustomScreen(
                onBegin = { mins ->
                    confirmingSeconds = mins * 60
                    TimerController.start(context, mins * 60)
                    screen = Screen.Confirming
                },
                onBack = { screen = Screen.Home },
            )
            Screen.Confirming -> ConfirmScreen(
                seconds = confirmingSeconds,
                onDismiss = { screen = Screen.Running; dismiss() },
            )
            Screen.Running -> RunningStatus(
                secondsRemaining = secondsRemaining,
                onCancel = {
                    TimerController.cancel(context)
                    screen = Screen.Home
                },
                onClose = dismiss,
            )
            Screen.Warning -> OverlayScreen(
                secondsRemaining = secondsRemaining,
                onSnooze = {
                    TimerController.addMinutes(context, 10)
                    screen = Screen.Running
                    dismiss()
                },
                onCancel = {
                    TimerController.cancel(context)
                    screen = Screen.Home
                },
            )
        }

        if (errorText != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(
                    "Couldn't reach the Pi — TV will not turn off.\n$errorText",
                    style = body(Tokens.TextPrimary),
                )
            }
        }
    }
}

@Composable
private fun RunningStatus(
    secondsRemaining: Long,
    onCancel: () -> Unit,
    onClose: () -> Unit,
) {
    var focus by remember { mutableIntStateOf(0) } // 0 = close (default), 1 = cancel
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val mins = secondsRemaining / 60
    val secs = secondsRemaining % 60
    val timeStr = "%d:%02d".format(mins, secs)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { focus = 0; true }
                    Key.DirectionRight -> { focus = 1; true }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (focus == 0) onClose() else onCancel(); true
                    }
                    Key.Back, Key.Escape -> { onClose(); true }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Sleep timer running".uppercase(), style = eyebrowSmall(Tokens.Accent))
            Spacer(Modifier.height(20.dp))
            Text(
                timeStr,
                style = body(Tokens.TextPrimary)
                    .copy(fontSize = 120.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = (-4).sp, lineHeight = 120.sp),
            )
            Spacer(Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatusButton(text = "Close", focused = focus == 0)
                StatusButton(text = "Cancel timer", focused = focus == 1)
            }
        }
    }
}

@Composable
private fun StatusButton(text: String, focused: Boolean) {
    val bg = if (focused) Tokens.Accent else Tokens.SurfaceSoft
    val fg = if (focused) Tokens.AccentOn else Tokens.TextBody
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (focused) Color.Transparent else Tokens.Divider,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        Text(
            text,
            style = body(fg).copy(
                fontSize = 22.sp,
                fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            ),
        )
    }
}

private fun focusIndexForPreset(minutes: Int): Int = when (minutes) {
    // Index 0 is the TEST row — never default focus there.
    15 -> 1; 30 -> 2; 45 -> 3; 60 -> 4; 90 -> 5
    else -> 3
}
