package com.danilaf.esp32controller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.danilaf.esp32controller.data.DeviceRepository
import com.danilaf.esp32controller.data.SettingsRepository
import com.danilaf.esp32controller.mqtt.MqttClientManager
import com.danilaf.esp32controller.ui.AppRoot
import com.danilaf.esp32controller.ui.theme.Esp32ControllerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Esp32ControllerTheme {
                val context = this
                val repo = remember { DeviceRepository(context) }
                val settings = remember { SettingsRepository(context) }
                val mqtt = remember { MqttClientManager(context) }

                AppRoot(
                    repository = repo,
                    settings = settings,
                    mqtt = mqtt
                )
            }
        }
    }
}
