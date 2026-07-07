package com.danilaf.esp32controller.network

import com.danilaf.esp32controller.data.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GatewayApi(private val client: OkHttpClient = defaultClient()) {
    private var socket: WebSocket? = null

    suspend fun getStatus(config: GatewayConfig): Result<GatewayStatus> = request(config, "/api/gateway/status") { body ->
        val json = JSONObject(body)
        GatewayStatus(
            online = true,
            wifiConnected = json.optBoolean("wifiConnected", json.optBoolean("wifi_connected")),
            ssid = json.optString("ssid", "—"),
            bssid = json.optString("bssid", "—"),
            ipAddress = json.optString("ipAddress", json.optString("ip", "—")),
            routerChannel = json.optNullableInt("routerChannel", "router_channel"),
            espNowChannel = json.optNullableInt("espNowChannel", "esp_now_channel"),
            migrationState = json.optString("migrationState", json.optString("migration_state", "idle")),
            nodesOnline = json.optInt("nodesOnline", json.optInt("nodes_online")),
            nodesTotal = json.optInt("nodesTotal", json.optInt("nodes_total")),
            firmwareVersion = json.optString("firmwareVersion", json.optString("firmware", "—")),
            uptime = json.optString("uptime", "—"),
            rssi = json.optNullableInt("rssi"),
            lastSyncTime = json.optString("lastSyncTime", json.optString("last_sync_time", "—"))
        )
    }

    suspend fun getNodes(config: GatewayConfig): Result<List<EspNowNode>> = request(config, "/api/nodes") { body ->
        val array = if (body.trim().startsWith("[")) JSONArray(body) else JSONObject(body).optJSONArray("nodes") ?: JSONArray()
        (0 until array.length()).map { index ->
            val json = array.getJSONObject(index)
            EspNowNode(
                deviceId = json.optString("deviceId", json.optString("id")),
                name = json.optString("name", json.optString("deviceId", "ESP-NOW node")),
                room = json.optString("room", "—"),
                type = json.optString("type", "unknown"),
                capabilities = json.optJSONArray("capabilities").toStringList(),
                status = json.optString("status", if (json.optBoolean("online")) "online" else "offline"),
                lastSeen = json.optString("lastSeen", json.optString("last_seen", "—")),
                state = json.optJSONObject("state").toStringMap(),
                rssi = json.optNullableInt("rssi"),
                channel = json.optNullableInt("channel"),
                migrationStatus = json.optString("migrationStatus", json.optString("migration_status", "unknown")),
                firmwareVersion = json.optString("firmwareVersion", json.optString("firmware", "—"))
            )
        }
    }

    suspend fun sendCommand(config: GatewayConfig, deviceId: String, command: String, value: Boolean? = null): Result<Unit> = post(config, "/api/nodes/$deviceId/command", JSONObject().apply {
        put("command", command)
        value?.let { put("value", it) }
    }) { }

    suspend fun startPairing(config: GatewayConfig): Result<Unit> = post(config, "/api/pairing/start", JSONObject()) { }
    suspend fun cancelPairing(config: GatewayConfig): Result<Unit> = post(config, "/api/pairing/cancel", JSONObject()) { }
    suspend fun confirmPairing(config: GatewayConfig, request: PairRequest, name: String, room: String): Result<Unit> = post(config, "/api/pairing/confirm", JSONObject().apply {
        put("deviceId", request.deviceId); put("name", name); put("room", room)
    }) { }

    suspend fun gatewayAction(config: GatewayConfig, path: String): Result<Unit> = post(config, path, JSONObject()) { }

    fun events(config: GatewayConfig): Flow<GatewayEvent> = callbackFlow {
        val wsUrl = config.url.replaceFirst("http://", "ws://").replaceFirst("https://", "wss://") + "/api/events"
        val request = Request.Builder().url(wsUrl).headers(config.headers()).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = runCatching { JSONObject(text) }.getOrNull()
                trySend(GatewayEvent(json?.optString("type") ?: "event", json?.optString("message", text) ?: text, json?.optString("deviceId")))
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { trySend(GatewayEvent("gateway_disconnected", t.message ?: "Gateway unavailable")) }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { trySend(GatewayEvent("gateway_disconnected", reason)) }
        })
        awaitClose { socket?.close(1000, null) }
    }

    private suspend fun <T> request(config: GatewayConfig, path: String, parser: (String) -> T): Result<T> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            client.newCall(Request.Builder().url(config.url + path).headers(config.headers()).build()).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                parser(response.body?.string().orEmpty())
            }
        }
    }

    private suspend fun <T> post(config: GatewayConfig, path: String, json: JSONObject, parser: (String) -> T): Result<T> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            client.newCall(Request.Builder().url(config.url + path).headers(config.headers()).post(json.toString().toRequestBody(JSON)).build()).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                parser(response.body?.string().orEmpty())
            }
        }
    }

    private fun GatewayConfig.headers(): Headers = Headers.Builder().apply { if (token.isNotBlank()) add("Authorization", "Bearer $token") }.build()

    private fun JSONObject.optNullableInt(vararg names: String): Int? = names.firstNotNullOfOrNull { if (has(it) && !isNull(it)) optInt(it) else null }
    private fun JSONArray?.toStringList(): List<String> = if (this == null) emptyList() else (0 until length()).map { optString(it) }
    private fun JSONObject?.toStringMap(): Map<String, String> = if (this == null) emptyMap() else keys().asSequence().associateWith { opt(it).toString() }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private fun defaultClient() = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
    }
}
