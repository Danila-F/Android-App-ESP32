package com.danilaf.esp32controller.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class DeviceRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDevices(): List<Device> {
        val raw = prefs.getString(KEY_DEVICES, "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id")
                val baseUrl = item.optString("baseUrl")
                if (id.isBlank() || baseUrl.isBlank()) continue
                add(
                    Device(
                        id = id,
                        name = item.optString("name", id),
                        room = item.optString("room"),
                        baseUrl = baseUrl,
                        token = item.optString("token"),
                        firmwareVersion = item.optString("firmwareVersion").ifBlank { null },
                        lastKnownPower = if (item.has("lastKnownPower")) item.optBoolean("lastKnownPower") else null
                    )
                )
            }
        }
    }

    fun findDevice(id: String): Device? = getDevices().firstOrNull { it.id == id }

    fun saveDevice(device: Device) {
        val updated = getDevices().filterNot { it.id == device.id } + device
        persist(updated)
    }

    fun removeDevice(id: String) {
        persist(getDevices().filterNot { it.id == id })
    }

    fun updatePower(id: String, power: Boolean) {
        val updated = getDevices().map { device ->
            if (device.id == id) device.copy(lastKnownPower = power) else device
        }
        persist(updated)
    }

    private fun persist(devices: List<Device>) {
        val array = JSONArray()
        devices.forEach { device ->
            array.put(
                JSONObject()
                    .put("id", device.id)
                    .put("name", device.name)
                    .put("room", device.room)
                    .put("baseUrl", device.baseUrl)
                    .put("token", device.token)
                    .put("firmwareVersion", device.firmwareVersion)
                    .put("lastKnownPower", device.lastKnownPower)
            )
        }
        prefs.edit().putString(KEY_DEVICES, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "devices"
        private const val KEY_DEVICES = "device_list"
    }
}
