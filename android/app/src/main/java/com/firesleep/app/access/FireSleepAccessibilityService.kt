package com.firesleep.app.access

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.firesleep.app.MainActivity

/**
 * Lets the user open FireSleep from any app by triple-pressing the Fire TV
 * remote's Menu button (the "≡" hamburger key, KEYCODE_MENU = 82). The user
 * must enable this once under Settings → Accessibility → FireSleep.
 *
 * KEYCODE_MENU is rare in TV apps (Netflix, YouTube, etc. don't use it for
 * playback), so consuming it on the third press is safe.
 */
class FireSleepAccessibilityService : AccessibilityService() {

    private val pressTimes = LongArray(WINDOW)
    private var pressIdx = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* unused */ }
    override fun onInterrupt() { /* unused */ }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.keyCode != TRIGGER_KEY) return false
        if (event.repeatCount != 0) return false

        val now = SystemClock.uptimeMillis()
        pressTimes[pressIdx % WINDOW] = now
        pressIdx++
        if (pressIdx >= WINDOW) {
            val first = pressTimes[(pressIdx - WINDOW + WINDOW) % WINDOW]
            val last = pressTimes[(pressIdx - 1) % WINDOW]
            if (last - first <= WINDOW_MS) {
                launchApp()
                pressIdx = 0
                return true
            }
        }
        return false
    }

    private fun launchApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(intent)
    }

    private companion object {
        const val TRIGGER_KEY = KeyEvent.KEYCODE_MENU
        const val WINDOW = 3
        const val WINDOW_MS = 700L
    }
}
