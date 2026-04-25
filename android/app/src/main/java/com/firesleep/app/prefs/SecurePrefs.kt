package com.firesleep.app.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePrefs(context: Context) {
    private val prefs: SharedPreferences = run {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE,
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var piIp: String
        get() = prefs.getString(KEY_IP, "") ?: ""
        set(value) = prefs.edit().putString(KEY_IP, value).apply()

    var lastPresetMinutes: Int
        get() = prefs.getInt(KEY_LAST_PRESET, 45)
        set(value) = prefs.edit().putInt(KEY_LAST_PRESET, value).apply()

    fun isConfigured(): Boolean = piIp.isNotBlank()

    companion object {
        private const val FILE = "firesleep_secure"
        private const val KEY_IP = "pi_ip"
        private const val KEY_LAST_PRESET = "last_preset"
    }
}
