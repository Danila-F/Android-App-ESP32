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
            Text("Add device", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(manualUrl, onUrlChange, label = { Text("Device URL or IP") }, placeholder = { Text("http://192.168.1.50") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(manualToken, onTokenChange, label = { Text("Device token") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(manualName, onNameChange, label = { Text("Display name override") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = onAdd, enabled = manualUrl.isNotBlank()) { Text("Add device") }
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
            Text("Local discovery", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartScan, enabled = !scanning) { Text("Scan for devices") }
                OutlinedButton(onClick = onStopScan, enabled = scanning) { Text("Stop") }
            }
            if (discovered.isEmpty()) {
                Text("No devices discovered yet.")
            } else {
                discovered.forEach { device ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.name, style = MaterialTheme.typography.bodyLarge)
                            Text(device.baseUrl, style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = { onUse(device) }) { Text("Use") }
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.name, style = MaterialTheme.typography.bodyLarge)
                            Text(device.baseUrl, style = MaterialTheme.typography.bodySmall)
                            if (selectedDevice?.id == device.id) Text("Selected", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onSelect(device) }) { Text("Select") }
                            OutlinedButton(onClick = { onRemove(device) }) { Text("Remove") }
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
            Text("Device control", style = MaterialTheme.typography.titleMedium)
            if (device == null) {
                Text("Select or add a device first.")
                return@Column
            }
            Text(device.name, style = MaterialTheme.typography.bodyLarge)
            Text(device.baseUrl, style = MaterialTheme.typography.bodySmall)
            Text("Power: ${state?.power?.let { if (it) "On" else "Off" } ?: "Unknown"}")
            state?.firmwareVersion?.let { Text("Firmware: $it") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefresh) { Text("Refresh") }
                Button(onClick = { onSetPower(true) }) { Text("Turn on") }
                Button(onClick = { onSetPower(false) }) { Text("Turn off") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onToggle) { Text("Toggle") }
                OutlinedButton(onClick = onAddToSystemControls) { Text("Add to Android Device Controls") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Firmware update", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPickFirmware) { Text("Select firmware.bin") }
                Button(onClick = onUploadFirmware, enabled = firmwareUri != null) { Text("Upload OTA") }
            }
            firmwareUri?.let { Text("Selected: ${it.lastPathSegment ?: it}", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
