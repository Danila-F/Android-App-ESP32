package com.danilaf.esp32controller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("Back to devices") }
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
        DiscoveryCard(scanning, discovered, onStartScan, onStopScan, onUseDiscovered)
        AddDeviceCard(manualUrl, manualToken, manualName, onManualUrlChange, onManualTokenChange, onManualNameChange, onManualAdd)
    }
}
