package com.danilaf.esp32controller.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.danilaf.esp32controller.data.Device
import com.danilaf.esp32controller.data.DeviceState
import com.danilaf.esp32controller.data.DiscoveredDevice

@Composable
fun AddDeviceCard(
    manualUrl: String,
    manualToken: String,
    manualName: String,
    onUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add by address", style = MaterialTheme.typography.titleMedium)
            Text("Use this for an ESP32 that is already connected to the same home network as this phone.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(manualUrl, onUrlChange, label = { Text("Device URL, IP, or .local host") }, placeholder = { Text("http://192.168.1.50 or http://esp32-xxxx.local") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(manualToken, onTokenChange, label = { Text("Device token") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(manualName, onNameChange, label = { Text("Display name override") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = onAdd, enabled = manualUrl.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Add existing device") }
        }
    }
}

@Composable
fun DiscoveryCard(
    scanning: Boolean,
    discovered: List<DiscoveredDevice>,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onUse: (DiscoveredDevice) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Find devices on this network", style = MaterialTheme.typography.titleMedium)
            Text("Scans for ESP32 devices advertising the local _espctrl._tcp service. This does not run first-time setup.", style = MaterialTheme.typography.bodySmall)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartScan, enabled = !scanning, modifier = Modifier.fillMaxWidth()) { Text("Scan local network") }
                OutlinedButton(onClick = onStopScan, enabled = scanning, modifier = Modifier.fillMaxWidth()) { Text("Stop") }
            }
            if (discovered.isEmpty()) {
                Text("No devices discovered yet. You can still add the device manually by IP address below.")
            } else {
                discovered.forEach { device ->
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column {
                            Text(device.name, style = MaterialTheme.typography.bodyLarge)
                            Text(device.baseUrl, style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = { onUse(device) }, modifier = Modifier.fillMaxWidth()) { Text("Use address") }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedDevicesCard(
    devices: List<Device>,
    selectedDevice: Device?,
    onSelect: (Device) -> Unit,
    onRemove: (Device) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Saved devices", style = MaterialTheme.typography.titleMedium)
            if (devices.isEmpty()) {
                Text("No saved devices.")
            } else {
                devices.forEach { device ->
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column {
                            Text(device.name, style = MaterialTheme.typography.bodyLarge)
                            Text(device.baseUrl, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onSelect(device) }, enabled = selectedDevice?.id != device.id, modifier = Modifier.weight(1f)) { Text("Select") }
                            OutlinedButton(onClick = { onRemove(device) }, modifier = Modifier.weight(1f)) { Text("Remove") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceControlCard(
    device: Device?,
    state: DeviceState?,
    firmwareUri: Uri?,
    onRefresh: () -> Unit,
    onSetPower: (Boolean) -> Unit,
    onToggle: () -> Unit,
    onAddToSystemControls: () -> Unit,
    onPickFirmware: () -> Unit,
    onUploadFirmware: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Device controls", style = MaterialTheme.typography.titleMedium)
            if (device == null) {
                Text("Select a saved device first.")
                return@Column
            }
            Text(device.name, style = MaterialTheme.typography.bodyLarge)
            Text(device.baseUrl, style = MaterialTheme.typography.bodySmall)
            Text("Power: ${state?.power?.let { if (it) "On" else "Off" } ?: "Unknown"}")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Refresh") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { onSetPower(true) }, modifier = Modifier.weight(1f)) { Text("On") }
                    OutlinedButton(onClick = { onSetPower(false) }, modifier = Modifier.weight(1f)) { Text("Off") }
                }
                OutlinedButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) { Text("Toggle") }
            }
            OutlinedButton(onClick = onAddToSystemControls, modifier = Modifier.fillMaxWidth()) { Text("Add to Android Device Controls") }
            Spacer(modifier = Modifier.height(8.dp))
            Text("OTA update", style = MaterialTheme.typography.titleSmall)
            Text(firmwareUri?.toString() ?: "No firmware selected", style = MaterialTheme.typography.bodySmall)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPickFirmware, modifier = Modifier.fillMaxWidth()) { Text("Pick firmware") }
                Button(onClick = onUploadFirmware, enabled = firmwareUri != null, modifier = Modifier.fillMaxWidth()) { Text("Upload OTA") }
            }
        }
    }
}
