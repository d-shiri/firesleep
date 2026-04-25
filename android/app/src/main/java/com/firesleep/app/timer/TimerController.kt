package com.firesleep.app.timer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TimerController {

    data class State(
        val running: Boolean = false,
        val endAtElapsedMs: Long = 0L,
        val totalSeconds: Int = 0,
        val warningActive: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun secondsRemaining(): Long {
        val s = _state.value
        if (!s.running) return 0L
        val rem = s.endAtElapsedMs - SystemClock.elapsedRealtime()
        return (rem / 1000L).coerceAtLeast(0L)
    }

    @SuppressLint("MissingPermission")
    fun start(context: Context, totalSeconds: Int) {
        cancelAlarms(context)
        val now = SystemClock.elapsedRealtime()
        val endAt = now + totalSeconds * 1000L
        _state.value = State(
            running = true,
            endAtElapsedMs = endAt,
            totalSeconds = totalSeconds,
            warningActive = false,
        )
        scheduleAlarms(context, endAt, totalSeconds)
        ContextCompat_startForegroundService(context)
    }

    @SuppressLint("MissingPermission")
    fun addMinutes(context: Context, minutes: Int) {
        val s = _state.value
        if (!s.running) return
        val newEnd = s.endAtElapsedMs + minutes * 60_000L
        val newTotal = s.totalSeconds + minutes * 60
        _state.value = s.copy(
            endAtElapsedMs = newEnd,
            totalSeconds = newTotal,
            warningActive = false,
        )
        cancelAlarms(context)
        scheduleAlarms(context, newEnd, newTotal)
    }

    fun cancel(context: Context) {
        cancelAlarms(context)
        _state.value = State()
        context.stopService(Intent(context, PowerOffService::class.java))
    }

    fun markWarningShown() {
        _state.value = _state.value.copy(warningActive = true)
    }

    fun markExpired() {
        _state.value = State()
    }

    /**
     * Warning fires `warningWindowMs(totalSeconds)` before expiry. For normal
     * timers this is a flat 60s. For the short test preset (≤ 60s) we scale it
     * down to half the duration so the user can actually see the overlay.
     */
    private fun warningWindowMs(totalSeconds: Int): Long {
        val totalMs = totalSeconds * 1000L
        return minOf(60_000L, totalMs / 2)
    }

    private fun scheduleAlarms(context: Context, endAtElapsedMs: Long, totalSeconds: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val warnAt = endAtElapsedMs - warningWindowMs(totalSeconds)

        val warnPi = pending(context, TimerReceiver.ACTION_WARN, REQ_WARN)
        val expirePi = pending(context, TimerReceiver.ACTION_EXPIRE, REQ_EXPIRE)

        val now = SystemClock.elapsedRealtime()
        if (warnAt > now) {
            setExact(am, warnAt, warnPi)
        }
        setExact(am, endAtElapsedMs, expirePi)
    }

    private fun setExact(am: AlarmManager, triggerAt: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
        } else {
            am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
        }
    }

    private fun cancelAlarms(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context, TimerReceiver.ACTION_WARN, REQ_WARN))
        am.cancel(pending(context, TimerReceiver.ACTION_EXPIRE, REQ_EXPIRE))
    }

    private fun pending(context: Context, action: String, reqCode: Int): PendingIntent {
        val intent = Intent(context, TimerReceiver::class.java).setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getBroadcast(context, reqCode, intent, flags)
    }

    private fun ContextCompat_startForegroundService(context: Context) {
        val intent = Intent(context, PowerOffService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private const val REQ_WARN = 1001
    private const val REQ_EXPIRE = 1002
}
