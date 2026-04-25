package com.firesleep.app.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.firesleep.app.MainActivity

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WARN -> onWarn(context)
            ACTION_EXPIRE -> onExpire(context)
        }
    }

    private fun onWarn(context: Context) {
        TimerController.markWarningShown()
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            putExtra(MainActivity.EXTRA_SHOW_OVERLAY, true)
        }
        context.startActivity(launch)
    }

    private fun onExpire(context: Context) {
        val svc = Intent(context, PowerOffService::class.java).apply {
            action = PowerOffService.ACTION_EXECUTE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc)
        } else {
            context.startService(svc)
        }
    }

    companion object {
        const val ACTION_WARN = "com.firesleep.app.WARN"
        const val ACTION_EXPIRE = "com.firesleep.app.EXPIRE"
    }
}
