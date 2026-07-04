package com.danilaf.esp32controller.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.danilaf.esp32controller.data.Device
import com.danilaf.esp32controller.data.DeviceState
import com.danilaf.esp32controller.ui.DeviceControlCard

@Composable
fun DeviceDetailsScreen(
    device: Device?,
    state: DeviceState?,
    firmwareUri: Uri?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSetPower: (Boolean) -> Unit,
    onToggle: () -> Unit,
    onAddToSystemControls: () -> Unit,
    onPickFirmware: () -> Unit,
    onUploadFirmware: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            if (device != null) {
                Button(onClick = onRemove) { Text("Remove") }
            }
        }
        DeviceControlCard(
            device = device,
            state = state,
            firmwareUri = firmwareUri,
            onRefresh = onRefresh,
            onSetPower = onSetPower,
            onToggle = onToggle,
            onAddToSystemControls = onAddToSystemControls,
            onPickFirmware = onPickFirmware,
            onUploadFirmware = onUploadFirmware
        )
    }
}
