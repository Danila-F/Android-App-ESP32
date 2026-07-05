package com.danilaf.esp32controller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.danilaf.esp32controller.data.DiscoveredDevice
import com.danilaf.esp32controller.ui.AddDeviceCard
import com.danilaf.esp32controller.ui.DiscoveryCard
import com.danilaf.esp32controller.ui.FirstTimeSetupCard

@Composable
fun AddDeviceScreen(
    setupSsid: String,
    setupPassword: String,
    setupBaseUrl: String,
    homeWifiSsid: String,
    homeWifiPassword: String,
    deviceName: String,
    room: String,
    status: String,
    inProgress: Boolean,
    manualUrl: String,
    manualToken: String,
    manualName: String,
    scanning: Boolean,
    discovered: List<DiscoveredDevice>,
    onSetupSsidChange: (String) -> Unit,
    onSetupPasswordChange: (String) -> Unit,
    onSetupBaseUrlChange: (String) -> Unit,
    onHomeWifiSsidChange: (String) -> Unit,
    onHomeWifiPasswordChange: (String) -> Unit,
    onDeviceNameChange: (String) -> Unit,
    onRoomChange: (String) -> Unit,
    onConnectSetupWifi: () -> Unit,
    onProvision: () -> Unit,
    onManualUrlChange: (String) -> Unit,
    onManualTokenChange: (String) -> Unit,
    onManualNameChange: (String) -> Unit,
    onManualAdd: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onUseDiscovered: (DiscoveredDevice) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack) { Text("Back to devices") }
                Text("Add device", style = MaterialTheme.typography.headlineSmall)
                Text("Choose an already configured ESP32 on your home network, or run first-time setup for a new board.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Existing device on home Wi-Fi", style = MaterialTheme.typography.titleMedium)
                    Text("Use this when the ESP32 is already connected to your router. No setup AP connection or Wi-Fi provisioning is needed.", style = MaterialTheme.typography.bodyMedium)
                    Text("Scan local discovery first. If the device is not found, enter its IP address or .local hostname manually, then add the device token.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item { DiscoveryCard(scanning, discovered, onStartScan, onStopScan, onUseDiscovered) }
        item { AddDeviceCard(manualUrl, manualToken, manualName, onManualUrlChange, onManualTokenChange, onManualNameChange, onManualAdd) }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("First-time setup", style = MaterialTheme.typography.titleMedium)
                    Text("Use this only for a new or factory-reset ESP32 that is still broadcasting its setup Wi-Fi network.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            FirstTimeSetupCard(
                setupSsid = setupSsid,
                setupPassword = setupPassword,
                setupBaseUrl = setupBaseUrl,
                homeWifiSsid = homeWifiSsid,
                homeWifiPassword = homeWifiPassword,
                deviceName = deviceName,
                room = room,
                status = status,
                inProgress = inProgress,
                onSetupSsidChange = onSetupSsidChange,
                onSetupPasswordChange = onSetupPasswordChange,
                onSetupBaseUrlChange = onSetupBaseUrlChange,
                onHomeWifiSsidChange = onHomeWifiSsidChange,
                onHomeWifiPasswordChange = onHomeWifiPasswordChange,
                onDeviceNameChange = onDeviceNameChange,
                onRoomChange = onRoomChange,
                onConnectSetupWifi = onConnectSetupWifi,
                onProvision = onProvision
            )
        }
    }
}
