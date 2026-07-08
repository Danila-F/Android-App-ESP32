package com.danilaf.esp32controller.data

data class GatewayConfig(
    val name: String = "Home Gateway",
    val url: String = "http://192.168.1.100",
    val token: String = ""
)

data class GatewayStatus(
    val online: Boolean = false,
    val wifiConnected: Boolean = false,
    val ssid: String = "—",
    val bssid: String = "—",
    val ipAddress: String = "—",
    val routerChannel: Int? = null,
    val espNowChannel: Int? = null,
    val migrationState: String = "unknown",
    val nodesOnline: Int = 0,
    val nodesTotal: Int = 0,
    val firmwareVersion: String = "—",
    val uptime: String = "—",
    val rssi: Int? = null,
    val lastSyncTime: String = "—"
)

data class EspNowNode(
    val deviceId: String,
    val name: String,
    val room: String = "—",
    val type: String = "unknown",
    val capabilities: List<String> = emptyList(),
    val status: String = "unknown",
    val lastSeen: String = "—",
    val state: Map<String, String> = emptyMap(),
    val rssi: Int? = null,
    val channel: Int? = null,
    val migrationStatus: String = "unknown",
    val firmwareVersion: String = "—"
) {
    val canSwitch: Boolean get() = capabilities.contains("switch") || type.contains("switch", true) || state.containsKey("power")
    val isOnline: Boolean get() = status == "online" || status == "synced"
}

data class PairRequest(
    val deviceId: String,
    val mac: String,
    val capabilities: List<String> = emptyList(),
    val rssi: Int? = null
)

data class GatewayEvent(
    val type: String,
    val message: String,
    val deviceId: String? = null
)
