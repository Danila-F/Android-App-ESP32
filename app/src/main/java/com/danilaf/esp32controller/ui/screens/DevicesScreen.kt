package com.danilaf.esp32controller.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.danilaf.esp32controller.data.Device

@Composable
fun DevicesScreen(
    devices: List<Device>,
    onOpenDevice: (Device) -> Unit,
    onAddDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Devices", style = MaterialTheme.typography.headlineSmall)
                Text("Tap a device to control it.", style = MaterialTheme.typography.bodyMedium)
            }
            ExtendedFloatingActionButton(
                onClick = onAddDevice,
                icon = { androidx.compose.material3.Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add") }
            )
        }

        if (devices.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No devices yet", style = MaterialTheme.typography.titleMedium)
                    Text("Tap Add to set up a new ESP32 or add an existing device by IP address.")
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(devices, key = { it.id }) { device ->
                    DeviceListItem(device = device, onClick = { onOpenDevice(device) })
                }
            }
        }
    }
}

@Composable
private fun DeviceListItem(device: Device, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(device.name, style = MaterialTheme.typography.titleMedium)
            if (device.room.isNotBlank()) Text(device.room, style = MaterialTheme.typography.bodyMedium)
            Text(device.baseUrl, style = MaterialTheme.typography.bodySmall)
            val powerText = when (device.lastKnownPower) {
                true -> "Last known state: On"
                false -> "Last known state: Off"
                null -> "Last known state: Unknown"
            }
            Text(powerText, style = MaterialTheme.typography.bodySmall)
        }
    }
}
