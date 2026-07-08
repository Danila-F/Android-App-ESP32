package com.danilaf.esp32controller.data

import android.content.Context
import androidx.core.content.edit

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("gateway_settings", Context.MODE_PRIVATE)

    fun loadGateway(): GatewayConfig = GatewayConfig(
        name = prefs.getString("name", "Home Gateway") ?: "Home Gateway",
        url = prefs.getString("url", "http://192.168.1.100") ?: "http://192.168.1.100",
        token = prefs.getString("token", "") ?: ""
    )

    fun saveGateway(config: GatewayConfig) {
        prefs.edit {
            putString("name", config.name)
            putString("url", config.url.trimEnd('/'))
            putString("token", config.token)
        }
    }
}
