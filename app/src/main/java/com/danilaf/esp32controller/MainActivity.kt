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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.danilaf.esp32controller.provisioning.WifiProvisioningConnector
import com.danilaf.esp32controller.ui.AddDeviceCard
import com.danilaf.esp32controller.ui.DeviceControlCard
import com.danilaf.esp32controller.ui.DiscoveryCard
import com.danilaf.esp32controller.ui.FirstTimeSetupCard
import com.danilaf.esp32controller.ui.SavedDevicesCard
import com.danilaf.esp32controller.ui.theme.Esp32ControllerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Esp32ControllerTheme { Esp32ControllerApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun Esp32ControllerApp() {
    val context = LocalContext.current
    val repository = remember { DeviceRepository(context) }
    val api = remember { EspApiClient(context) }
    val discovery = remember { EspDiscoveryManager(context) }
    val setupConnector = remember { WifiProvisioningConnector(context) }
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

    var setupSsid by remember { mutableStateOf("ESP32-Setup-") }
    var setupPassphrase by remember { mutableStateOf("esp32setup") }
    var setupBaseUrl by remember { mutableStateOf("http://192.168.4.1") }
    var homeSsid by remember { mutableStateOf("") }
    var homePassphrase by remember { mutableStateOf("") }
    var provisionName by remember { mutableStateOf("ESP32 Device") }
    var provisionRoom by remember { mutableStateOf("") }
    var provisionStatus by remember { mutableStateOf("") }
    var provisioning by remember { mutableStateOf(false) }

    val firmwarePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedFirmwareUri = uri
        uri?.let { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) scope.launch { snackbarHostState.showSnackbar("Wi-Fi permission was not granted") }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            discovery.stop()
            setupConnector.release()
        }
    }

    fun reloadDevices() {
        devices = repository.getDevices()
        selectedDevice = selectedDevice?.let { current -> devices.firstOrNull { it.id == current.id } } ?: devices.firstOrNull()
    }

    fun showMessage(message: String) = scope.launch { snackbarHostState.showSnackbar(message) }

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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FirstTimeSetupCard(
                    setupSsid = setupSsid,
                    setupPassword = setupPassphrase,
                    setupBaseUrl = setupBaseUrl,
                    homeWifiSsid = homeSsid,
                    homeWifiPassword = homePassphrase,
                    deviceName = provisionName,
                    room = provisionRoom,
                    status = provisionStatus,
                    inProgress = provisioning,
                    onSetupSsidChange = { setupSsid = it },
                    onSetupPasswordChange = { setupPassphrase = it },
                    onSetupBaseUrlChange = { setupBaseUrl = it },
                    onHomeWifiSsidChange = { homeSsid = it },
                    onHomeWifiPasswordChange = { homePassphrase = it },
                    onDeviceNameChange = { provisionName = it },
                    onRoomChange = { provisionRoom = it },
                    onConnectSetupWifi = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            provisionStatus = "Requesting Android connection to $setupSsid..."
                            setupConnector.requestSetupNetwork(
                                ssid = setupSsid,
                                passphrase = setupPassphrase,
                                onAvailable = { scope.launch { provisionStatus = "Connected to ESP32 setup Wi-Fi. Now tap Provision." } },
                                onUnavailable = { scope.launch { provisionStatus = "Setup Wi-Fi connection was not completed." } },
                                onLost = { scope.launch { provisionStatus = "Setup Wi-Fi disconnected." } },
                                onError = { message -> scope.launch { provisionStatus = message; snackbarHostState.showSnackbar(message) } }
                            )
                        } else {
                            provisionStatus = "Automatic setup Wi-Fi connection requires Android 10+. Connect to ESP32 setup Wi-Fi manually, then tap Provision."
                        }
                    },
                    onProvision = {
                        if (homeSsid.isBlank()) {
                            showMessage("Home Wi-Fi SSID is required")
                        } else {
                            provisioning = true
                            provisionStatus = "Sending home Wi-Fi credentials to ESP32..."
                            scope.launch {
                                runCatching { api.provision(setupBaseUrl, homeSsid, homePassphrase, provisionName, provisionRoom) }
                                    .onSuccess { info ->
                                        provisioning = false
                                        manualToken = info.token.orEmpty()
                                        manualName = info.name
                                        setupConnector.release()
                                        provisionStatus = "Provisioned ${info.name}. Token copied below. Reconnect to home Wi-Fi if needed, scan for devices, then add it."
                                        showMessage("ESP32 provisioned")
                                    }
                                    .onFailure { error ->
                                        provisioning = false
                                        provisionStatus = error.message ?: "Provisioning failed"
                                        showMessage(error.message ?: "Provisioning failed")
                                    }
                            }
                        }
                    }
                )
            }

            item {
                AddDeviceCard(manualUrl, manualToken, manualName, { manualUrl = it }, { manualToken = it }, { manualName = it }) {
                    scope.launch {
                        runCatching {
                            val info = api.getInfo(manualUrl, manualToken.ifBlank { null })
                            val device = Device(info.id.ifBlank { manualUrl }, manualName.ifBlank { info.name }, info.room, info.baseUrl, manualToken, info.firmwareVersion)
                            repository.saveDevice(device)
                            reloadDevices()
                            selectedDevice = device
                        }.onSuccess {
                            manualUrl = ""
                            manualName = ""
                            showMessage("Device saved")
                        }.onFailure { showMessage(it.message ?: "Unable to add device") }
                    }
                }
            }

            item {
                DiscoveryCard(
                    scanning = scanning,
                    discovered = discovered,
                    onStartScan = {
                        discovered.clear()
                        scanning = true
                        discovery.start(
                            onDevice = { device -> if (discovered.none { it.baseUrl == device.baseUrl }) discovered.add(device) },
                            onError = { showMessage(it) }
                        )
                    },
                    onStopScan = { scanning = false; discovery.stop() },
                    onUse = { found -> manualUrl = found.baseUrl; manualName = found.name; showMessage("Discovery result copied to manual form") }
                )
            }

            item {
                SavedDevicesCard(devices, selectedDevice, onSelect = { selectedDevice = it; selectedState = null }, onRemove = { repository.removeDevice(it.id); reloadDevices() })
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
                                .onSuccess { selectedState = it; repository.updatePower(device.id, it.power); reloadDevices() }
                                .onFailure { showMessage(it.message ?: "Command failed") }
                        }
                    },
                    onToggle = {
                        val device = selectedDevice ?: return@DeviceControlCard
                        scope.launch {
                            runCatching { api.togglePower(device) }
                                .onSuccess { selectedState = it; repository.updatePower(device.id, it.power); reloadDevices() }
                                .onFailure { showMessage(it.message ?: "Command failed") }
                        }
                    },
                    onAddToSystemControls = {
                        val device = selectedDevice ?: return@DeviceControlCard
                        val requested = DeviceControlsRequester.requestAdd(context, device)
                        showMessage(if (requested) "Android Device Controls prompt requested" else "Android Device Controls require Android 11 or newer")
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
