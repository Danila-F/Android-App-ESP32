package com.danilaf.esp32controller.network

import android.content.Context
import android.net.Uri
import com.danilaf.esp32controller.data.Device
import com.danilaf.esp32controller.data.DeviceInfo
import com.danilaf.esp32controller.data.DeviceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class EspApiClient(
    private val context: Context? = null,
    private val client: OkHttpClient = defaultClient()
) {
    suspend fun getInfo(baseUrl: String, token: String? = null): DeviceInfo = withContext(Dispatchers.IO) {
        val normalized = normalizeBaseUrl(baseUrl)
        val json = getJson("$normalized/api/info", token)
        DeviceInfo(
            id = json.optString("deviceId"),
            name = json.optString("name", "ESP32 Device"),
            room = json.optString("room"),
            baseUrl = normalized,
            firmwareVersion = json.optString("firmwareVersion").ifBlank { null },
            token = json.optString("authToken").ifBlank { null }
        )
    }

    suspend fun provision(
        setupBaseUrl: String,
        wifiSsid: String,
        wifiPassword: String,
        deviceName: String,
        room: String
    ): DeviceInfo = withContext(Dispatchers.IO) {
        val normalized = normalizeBaseUrl(setupBaseUrl)
        val payload = JSONObject()
            .put("ssid", wifiSsid)
            .put("password", wifiPassword)
            .put("name", deviceName.ifBlank { "ESP32 Device" })
            .put("room", room)

        val request = Request.Builder()
            .url("$normalized/api/provision")
            .post(payload.toString().toRequestBody(JSON))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Provisioning failed: HTTP ${response.code} $body")
            val json = JSONObject(body)
            DeviceInfo(
                id = json.optString("deviceId"),
                name = json.optString("name", deviceName.ifBlank { "ESP32 Device" }),
                room = room,
                baseUrl = normalized,
                firmwareVersion = null,
                token = json.optString("authToken").ifBlank { null }
            )
        }
    }

    suspend fun getState(device: Device): DeviceState = withContext(Dispatchers.IO) {
        val json = getJson("${device.baseUrl}/api/state", device.token)
        DeviceState(
            online = json.optBoolean("online", false),
            networkOnline = json.optBoolean("networkOnline", false),
            power = json.optBoolean("power", false),
            firmwareVersion = json.optString("firmwareVersion").ifBlank { null },
            ip = json.optString("ip").ifBlank { null }
        )
    }

    suspend fun setPower(device: Device, enabled: Boolean): DeviceState = withContext(Dispatchers.IO) {
        postCommand(device, JSONObject().put("command", "set_power").put("value", enabled))
        getState(device)
    }

    suspend fun togglePower(device: Device): DeviceState = withContext(Dispatchers.IO) {
        postCommand(device, JSONObject().put("command", "toggle_power"))
        getState(device)
    }

    suspend fun uploadFirmware(device: Device, fileUri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context?.contentResolver ?: error("ContentResolver is unavailable")
        val bytes = resolver.openInputStream(fileUri)?.use { it.readBytes() }
            ?: error("Unable to read selected firmware file")

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "firmware",
                "firmware.bin",
                bytes.toRequestBody("application/octet-stream".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("${device.baseUrl}/api/ota")
            .headers(authHeaders(device.token))
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("OTA failed: HTTP ${response.code} $body")
            JSONObject(body).optString("message", "Firmware uploaded")
        }
    }

    private fun postCommand(device: Device, body: JSONObject) {
        val request = Request.Builder()
            .url("${device.baseUrl}/api/command")
            .headers(authHeaders(device.token))
            .post(body.toString().toRequestBody(JSON))
            .build()
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Command failed: HTTP ${response.code} $responseBody")
        }
    }

    private fun getJson(url: String, token: String?): JSONObject {
        val request = Request.Builder()
            .url(url)
            .headers(authHeaders(token))
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("HTTP ${response.code}: $body")
            return JSONObject(body)
        }
    }

    private fun authHeaders(token: String?): okhttp3.Headers {
        val values = buildMap {
            if (!token.isNullOrBlank()) put("X-Device-Token", token)
        }
        return values.toHeaders()
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun normalizeBaseUrl(value: String): String {
            val trimmed = value.trim().removeSuffix("/")
            return when {
                trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
                else -> "http://$trimmed"
            }
        }

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
