package com.danilaf.esp32controller

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.danilaf.esp32controller.controls.DeviceControlsRequester
import com.danilaf.esp32controller.data.Device
import com.danilaf.esp32controller.data.DeviceRepository
import com.danilaf.esp32controller.data.DeviceState
import com.danilaf.esp32controller.data.DiscoveredDevice
import com.danilaf.esp32controller.discovery.EspDiscoveryManager
import com.danilaf.esp32controller.network.EspApiClient
import com.danilaf.esp32controller.ui.theme.Esp32ControllerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Esp32ControllerTheme {
                Esp32ControllerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Esp32ControllerApp() {
    val context = LocalContext.current
    val repository = remember { DeviceRepository(context) }
    val api = remember { EspApiClient(context) }
    val discovery = remember { EspDiscoveryManager(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var devices by remember { mutableStateOf(repository.getDevices()) }
    var manualUrl by remember { mutableStateOf("") }
    var manualToken by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    val discovered = remember { mutableStateListOf<DiscoveredDevice>() }
    var selectedDevice by remember { mutableStateOf<Device?>(devices.firstOrNull()) }
    var selectedState by remember { mutableStateOf<DeviceState?>(null) }
    var selectedFirmwareUri by remember { mutableStateOf<Uri?>(null) }

    val firmwarePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedFirmwareUri = uri
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            scope.launch { snackbarHostState.showSnackbar("Nearby Wi-Fi permission was not granted") }
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }

    DisposableEffect(Unit) {
        onDispose { discovery.stop() }
    }

    fun reloadDevices() {
        devices = repository.getDevices()
        selectedDevice = selectedDevice?.let { current -> devices.firstOrNull { it.id == current.id } } ?: devices.firstOrNull()
    }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun refreshSelected() {
        val device = selectedDevice ?: return
        scope.launch {
            runCatching { api.getState(device) }
                .onSuccess { state ->
                    selectedState = state
                    repository.updatePower(device.id, state.power)
                    reloadDevices()
                }
                .onFailure { showMessage(it.message ?: "Refresh failed") }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ESP32 Controller") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AddDeviceCard(
                    manualUrl = manualUrl,
                    manualToken = manualToken,
                    manualName = manualName,
                    onUrlChange = { manualUrl = it },
                    onTokenChange = { manualToken = it },
                    onNameChange = { manualName = it },
                    onAdd = {
                        scope.launch {
                            runCatching {
                                val info = api.getInfo(manualUrl, manualToken.ifBlank { null })
                                val device = Device(
                                    id = info.id.ifBlank { manualUrl },
                                    name = manualName.ifBlank { info.name },
                                    room = info.room,
                                    baseUrl = info.baseUrl,
                                    token = manualToken,
                                    firmwareVersion = info.firmwareVersion
                                )
                                repository.saveDevice(device)
                                reloadDevices()
                                selectedDevice = device
                            }.onSuccess {
                                manualUrl = ""
                                manualName = ""
                                showMessage("Device saved")
                            }.onFailure {
                                showMessage(it.message ?: "Unable to add device")
                            }
                        }
                    }
                )
            }

            item {
                DiscoveryCard(
                    scanning = scanning,
                    discovered = discovered,
                    onStartScan = {
                        discovered.clear()
                        scanning = true
                        discovery.start(
                            onDevice = { device ->
                                if (discovered.none { it.baseUrl == device.baseUrl }) discovered.add(device)
                            },
                            onError = { showMessage(it) }
                        )
                    },
                    onStopScan = {
                        scanning = false
                        discovery.stop()
                    },
                    onUse = { found ->
                        manualUrl = found.baseUrl
                        manualName = found.name
                        showMessage("Discovery result copied to manual form")
                    }
                )
            }

            item {
                SavedDevicesCard(
                    devices = devices,
                    selectedDevice = selectedDevice,
                    onSelect = {
                        selectedDevice = it
                        selectedState = null
                    },
                    onRemove = {
                        repository.removeDevice(it.id)
                        reloadDevices()
                    }
                )
            }

            item {
                DeviceControlCard(
                    device = selectedDevice,
                    state = selectedState,
                    firmwareUri = selectedFirmwareUri,
                    onRefresh = { refreshSelected() },
                    onSetPower = { enabled ->
                        val device = selectedDevice ?: return@DeviceControlCard
                        scope.launch {
                            runCatching { api.setPower(device, enabled) }
                                .onSuccess {
                                    selectedState = it
                                    repository.updatePower(device.id, it.power)
                                    reloadDevices()
                                }
                                .onFailure { showMessage(it.message ?: "Command failed") }
                        }
                    },
                    onToggle = {
                        val device = selectedDevice ?: return@DeviceControlCard
                        scope.launch {
                            runCatching { api.togglePower(device) }
                                .onSuccess {
                                    selectedState = it
                                    repository.updatePower(device.id, it.power)
                                    reloadDevices()
                                }
                                .onFailure { showMessage(it.message ?: "Command failed") }
                        }
                    },
                    onAddToSystemControls = {
                        val device = selectedDevice ?: return@DeviceControlCard
                        val requested = DeviceControlsRequester.requestAdd(context, device)
                        showMessage(
                            if (requested) "Android Device Controls prompt requested"
                            else "Android Device Controls require Android 11 or newer"
                        )
                    },
                    onPickFirmware = { firmwarePicker.launch(arrayOf("application/octet-stream", "application/bin", "*/*")) },
                    onUploadFirmware = {
                        val device = selectedDevice ?: return@DeviceControlCard
                        val uri = selectedFirmwareUri ?: return@DeviceControlCard
                        scope.launch {
                            runCatching { api.uploadFirmware(device, uri) }
                                .onSuccess { showMessage(it) }
                                .onFailure { showMessage(it.message ?: "OTA upload failed") }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AddDeviceCard(
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
            OutlinedTextField(
                value = manualUrl,
                onValueChange = onUrlChange,
                label = { Text("Device URL or IP") },
                placeholder = { Text("http://192.168.1.50") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = manualToken,
                onValueChange = onTokenChange,
                label = { Text("Device token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = manualName,
                onValueChange = onNameChange,
                label = { Text("Display name override") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(onClick = onAdd, enabled = manualUrl.isNotBlank()) {
                Text("Add device")
            }
        }
    }
}

@Composable
private fun DiscoveryCard(
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
private fun SavedDevicesCard(
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
private fun DeviceControlCard(
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
