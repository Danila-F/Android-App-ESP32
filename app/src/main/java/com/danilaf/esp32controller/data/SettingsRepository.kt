package com.danilaf.esp32controller.data

import android.content.Context

data class MqttSettings(
    val enabled: Boolean,
    val brokerUri: String,
    val username: String,
    val password: String,
    val baseTopic: String
)

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("esp32_settings", Context.MODE_PRIVATE)

    fun getLanguage(): String = prefs.getString("lang", "en") ?: "en"

    fun setLanguage(v: String) {
        prefs.edit().putString("lang", v).apply()
    }

    fun getMqttSettings(): MqttSettings = MqttSettings(
        enabled = prefs.getBoolean("mqtt_enabled", false),
        brokerUri = prefs.getString("mqtt_broker_uri", "tcp://192.168.1.2:1883") ?: "tcp://192.168.1.2:1883",
        username = prefs.getString("mqtt_username", "") ?: "",
        password = prefs.getString("mqtt_password", "") ?: "",
        baseTopic = prefs.getString("mqtt_base_topic", "esp32") ?: "esp32"
    )

    fun saveMqttSettings(settings: MqttSettings) {
        prefs.edit()
            .putBoolean("mqtt_enabled", settings.enabled)
            .putString("mqtt_broker_uri", settings.brokerUri)
            .putString("mqtt_username", settings.username)
            .putString("mqtt_password", settings.password)
            .putString("mqtt_base_topic", settings.baseTopic)
            .apply()
    }

    fun getVersion(): String = "1.0"
}
