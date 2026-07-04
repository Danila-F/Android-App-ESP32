package com.danilaf.esp32controller.data

import android.content.Context

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("esp32_settings", Context.MODE_PRIVATE)

    fun getLanguage(): String = prefs.getString("lang", "en") ?: "en"

    fun setLanguage(v: String) {
        prefs.edit().putString("lang", v).apply()
    }

    fun getVersion(): String = "1.0"
}