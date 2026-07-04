package com.danilaf.esp32controller.mqtt

import android.content.Context
import com.danilaf.esp32controller.data.MqttSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttClientManager(private val context: Context) {

    private var client: MqttClient? = null

    suspend fun connect(settings: MqttSettings) = withContext(Dispatchers.IO) {
        if (!settings.enabled) return@withContext

        val mqttClient = MqttClient(settings.brokerUri, "esp32-android-${System.currentTimeMillis()}", MemoryPersistence())

        val options = MqttConnectOptions().apply {
            isCleanSession = true
            if (settings.username.isNotBlank()) {
                userName = settings.username
                password = settings.password.toCharArray()
            }
        }

        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {}
            override fun messageArrived(topic: String?, message: MqttMessage?) {}
            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        mqttClient.connect(options)
        client = mqttClient
    }

    suspend fun publish(topic: String, payload: String, qos: Int = 1) = withContext(Dispatchers.IO) {
        val msg = MqttMessage(payload.toByteArray()).apply {
            this.qos = qos
        }
        client?.publish(topic, msg)
    }

    fun disconnect() {
        runCatching { client?.disconnect() }
        client = null
    }
}
