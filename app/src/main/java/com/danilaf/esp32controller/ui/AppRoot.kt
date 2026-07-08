package com.danilaf.esp32controller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.danilaf.esp32controller.data.*
import com.danilaf.esp32controller.network.GatewayApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppRoot(settings: SettingsRepository, api: GatewayApi = remember { GatewayApi() }) {
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(settings.loadGateway()) }
    var status by remember { mutableStateOf(GatewayStatus()) }
    var nodes by remember { mutableStateOf<List<EspNowNode>>(emptyList()) }
    var eventLog by remember { mutableStateOf(listOf("Waiting for gateway events")) }
    var error by remember { mutableStateOf<String?>(null) }
    var pairing by remember { mutableStateOf(false) }

    fun refresh() = scope.launch {
        error = null
        api.getStatus(config).onSuccess { status = it }.onFailure { error = it.message; status = status.copy(online = false) }
        api.getNodes(config).onSuccess { nodes = it }.onFailure { if (error == null) error = it.message }
    }

    LaunchedEffect(config) {
        refresh()
        launch {
            while (true) { delay(30_000); refresh() }
        }
        launch {
            api.events(config).collect { event ->
                eventLog = (listOf("${event.type}: ${event.message}") + eventLog).take(30)
                if (event.type.contains("node") || event.type.contains("gateway") || event.type.contains("migration")) refresh()
            }
        }
    }

    Scaffold(topBar = { TopBar(status.online, refresh = ::refresh) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { GatewayCard(config, status, error, onSave = { config = it; settings.saveGateway(it); refresh() }) }
            item { MigrationCard(status) }
            item { ActionsCard(status, onAction = { path -> scope.launch { api.gatewayAction(config, path); refresh() } }) }
            item { PairingCard(pairing, onStart = { pairing = true; scope.launch { api.startPairing(config) } }, onCancel = { pairing = false; scope.launch { api.cancelPairing(config) } }) }
            item { Text("ESP-NOW devices", style = MaterialTheme.typography.titleLarge) }
            items(nodes, key = { it.deviceId }) { node ->
                NodeCard(node, migrationBlocked = status.migrationState in listOf("migrating", "recovering")) { command, value ->
                    scope.launch { api.sendCommand(config, node.deviceId, command, value); refresh() }
                }
            }
            item { DiagnosticsCard(status, nodes, eventLog) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(online: Boolean, refresh: () -> Unit) {
    TopAppBar(title = { Text(if (online) "ESP32 Gateway Online" else "ESP32 Gateway Offline") }, actions = {
        IconButton(onClick = refresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh status") }
    })
}

@Composable
private fun GatewayCard(config: GatewayConfig, status: GatewayStatus, error: String?, onSave: (GatewayConfig) -> Unit) {
    var name by remember(config) { mutableStateOf(config.name) }
    var url by remember(config) { mutableStateOf(config.url) }
    var token by remember(config) { mutableStateOf(config.token) }
    ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Gateway", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(name, { name = it }, label = { Text("Gateway name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(url, { url = it }, label = { Text("Gateway URL / IP / hostname") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(token, { token = it }, label = { Text("API token") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Button(onClick = { onSave(GatewayConfig(name, url.trimEnd('/'), token)) }) { Text("Save and check availability") }
        error?.let { Text("Gateway unavailable: $it", color = MaterialTheme.colorScheme.error) }
        Text("Wi‑Fi: ${yesNo(status.wifiConnected)} · SSID ${status.ssid} · BSSID ${status.bssid}")
        Text("IP ${status.ipAddress} · Router channel ${status.routerChannel ?: "—"} · ESP‑NOW channel ${status.espNowChannel ?: "—"}")
        Text("Nodes ${status.nodesOnline}/${status.nodesTotal} · Firmware ${status.firmwareVersion} · Uptime ${status.uptime}")
        Text("RSSI ${status.rssi ?: "—"} · Last sync ${status.lastSyncTime}")
    } }
}

@Composable
private fun MigrationCard(status: GatewayStatus) = ElevatedCard { Column(Modifier.padding(16.dp)) {
    Text("Channel migration", style = MaterialTheme.typography.titleMedium)
    Text("State: ${status.migrationState}")
    if (status.migrationState in listOf("scanning", "migrating", "recovering")) Text("Commands are temporarily blocked while the gateway synchronizes ESP‑NOW nodes.")
} }

@Composable
private fun ActionsCard(status: GatewayStatus, onAction: (String) -> Unit) = ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Advanced / Diagnostics actions", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onAction("/api/gateway/rescan-channel") }) { Text("Rescan channel") }
        Button(onClick = { onAction("/api/gateway/resync") }) { Text("Restart sync") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onAction("/api/gateway/reannounce") }) { Text("Reannounce") }
        OutlinedButton(onClick = { onAction("/api/gateway/reboot") }, enabled = status.online) { Text("Reboot") }
    }
} }

@Composable
private fun PairingCard(pairing: Boolean, onStart: () -> Unit, onCancel: () -> Unit) = ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Pairing ESP‑NOW device", style = MaterialTheme.typography.titleMedium)
    Text(if (pairing) "Pairing window is open. Confirm discovered devices on the gateway event stream." else "Add devices only through the gateway; the app never talks ESP‑NOW directly.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onStart, enabled = !pairing) { Text("Add ESP‑NOW device") }
        OutlinedButton(onClick = onCancel, enabled = pairing) { Text("Cancel pairing") }
    }
} }

@Composable
private fun NodeCard(node: EspNowNode, migrationBlocked: Boolean, onCommand: (String, Boolean?) -> Unit) = ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(node.name, style = MaterialTheme.typography.titleMedium)
    Text("${node.deviceId} · ${node.room} · ${node.type} · ${node.status}")
    Text("Last seen ${node.lastSeen} · RSSI ${node.rssi ?: "—"} · Channel ${node.channel ?: "—"} · Migration ${node.migrationStatus}")
    Text("Firmware ${node.firmwareVersion} · State ${node.state.ifEmpty { mapOf("—" to "—") }}")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onCommand("toggle_power", null) }, enabled = node.canSwitch && node.isOnline && !migrationBlocked) { Text("Toggle") }
        OutlinedButton(onClick = { onCommand("identify", null) }, enabled = node.isOnline && !migrationBlocked) { Text("Identify") }
        OutlinedButton(onClick = { onCommand("reboot", null) }, enabled = node.isOnline && !migrationBlocked) { Text("Reboot") }
    }
    if (!node.isOnline) Text("Device is offline or searching for gateway channel.", color = MaterialTheme.colorScheme.error)
} }

@Composable
private fun DiagnosticsCard(status: GatewayStatus, nodes: List<EspNowNode>, events: List<String>) = ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text("Diagnostics", style = MaterialTheme.typography.titleLarge)
    Text("Router BSSID ${status.bssid}; router channel ${status.routerChannel ?: "—"}; saved ESP‑NOW channel ${status.espNowChannel ?: "—"}")
    Text("Known nodes: ${nodes.size}; online: ${nodes.count { it.isOnline }}")
    Text("Recent events and errors")
    events.take(8).forEach { Text("• $it") }
} }

private fun yesNo(value: Boolean) = if (value) "yes" else "no"
