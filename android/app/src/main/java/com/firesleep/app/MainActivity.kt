package com.firesleep.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.firesleep.app.ui.body
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
                DesignCanvas { App(intent) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setContent {
            FireSleepTheme {
                DesignCanvas { App(intent) }
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
private fun App(intent: Intent?) {
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
    var confirmingMinutes by rememberSaveable { mutableIntStateOf(0) }

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

    Box(modifier = Modifier.fillMaxSize().background(Tokens.BgCanvas)) {
        when (screen) {
            Screen.Settings -> SettingsScreen(onSaved = { screen = Screen.Home })
            Screen.Home -> HomeScreen(
                initialFocusIndex = focusIndexForPreset(prefs.lastPresetMinutes),
                onPresetSelected = { mins ->
                    prefs.lastPresetMinutes = mins
                    confirmingMinutes = mins
                    TimerController.start(context, mins)
                    screen = Screen.Confirming
                },
                onCustomSelected = { screen = Screen.Custom },
            )
            Screen.Custom -> CustomScreen(
                onBegin = { mins ->
                    confirmingMinutes = mins
                    TimerController.start(context, mins)
                    screen = Screen.Confirming
                },
                onBack = { screen = Screen.Home },
            )
            Screen.Confirming -> ConfirmScreen(
                minutes = confirmingMinutes,
                onDismiss = { moveToBackground(context); screen = Screen.Running },
            )
            Screen.Running -> {
                // Intentionally empty — user's content is playing; we just need
                // the activity alive so alarms keep firing through the service.
            }
            Screen.Warning -> OverlayScreen(
                secondsRemaining = secondsRemaining,
                onSnooze = {
                    TimerController.addMinutes(context, 10)
                    screen = Screen.Running
                    moveToBackground(context)
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

private fun focusIndexForPreset(minutes: Int): Int = when (minutes) {
    15 -> 0; 30 -> 1; 45 -> 2; 60 -> 3; 90 -> 4
    else -> 2
}

private fun moveToBackground(context: android.content.Context) {
    val home = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(home)
}
