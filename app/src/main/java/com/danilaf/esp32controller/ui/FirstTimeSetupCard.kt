package com.danilaf.esp32controller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun FirstTimeSetupCard(
    setupSsid: String,
    setupPassword: String,
    setupBaseUrl: String,
    homeWifiSsid: String,
    homeWifiPassword: String,
    deviceName: String,
    room: String,
    status: String,
    inProgress: Boolean,
    onSetupSsidChange: (String) -> Unit,
    onSetupPasswordChange: (String) -> Unit,
    onSetupBaseUrlChange: (String) -> Unit,
    onHomeWifiSsidChange: (String) -> Unit,
    onHomeWifiPasswordChange: (String) -> Unit,
    onDeviceNameChange: (String) -> Unit,
    onRoomChange: (String) -> Unit,
    onConnectSetupWifi: () -> Unit,
    onProvision: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("First-time ESP32 setup", style = MaterialTheme.typography.titleMedium)
            Text(
                "Use this when ESP32 is still broadcasting its setup Wi-Fi network. The app will connect to the setup network, send Wi-Fi credentials to /api/provision, and store the returned device token.",
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedTextField(
                value = setupSsid,
                onValueChange = onSetupSsidChange,
                label = { Text("ESP32 setup Wi-Fi SSID") },
                placeholder = { Text("ESP32-Setup-xxxxxx") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = setupPassword,
                onValueChange = onSetupPasswordChange,
                label = { Text("ESP32 setup Wi-Fi password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            OutlinedTextField(
                value = setupBaseUrl,
                onValueChange = onSetupBaseUrlChange,
                label = { Text("ESP32 setup API URL") },
                placeholder = { Text("http://192.168.4.1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedButton(
                onClick = onConnectSetupWifi,
                enabled = setupSsid.isNotBlank() && setupPassword.isNotBlank() && !inProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connect to setup Wi-Fi")
            }

            OutlinedTextField(
                value = homeWifiSsid,
                onValueChange = onHomeWifiSsidChange,
                label = { Text("Home Wi-Fi SSID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = homeWifiPassword,
                onValueChange = onHomeWifiPasswordChange,
                label = { Text("Home Wi-Fi password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            OutlinedTextField(
                value = deviceName,
                onValueChange = onDeviceNameChange,
                label = { Text("Device name") },
                placeholder = { Text("Desk lamp") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = room,
                onValueChange = onRoomChange,
                label = { Text("Room") },
                placeholder = { Text("Bedroom") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = onProvision,
                enabled = homeWifiSsid.isNotBlank() && setupBaseUrl.isNotBlank() && !inProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (inProgress) "Provisioning..." else "Provision ESP32 and save token")
            }

            if (status.isNotBlank()) {
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
