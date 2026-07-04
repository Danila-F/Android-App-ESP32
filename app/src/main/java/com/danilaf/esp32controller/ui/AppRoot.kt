package com.danilaf.esp32controller.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.danilaf.esp32controller.controls.DeviceControlsRequester
import com.danilaf.esp32controller.data.Device
import com.danilaf.esp32controller.data.DeviceRepository
import com.danilaf.esp32controller.data.DeviceState
import com.danilaf.esp32controller.data.DiscoveredDevice
import com.danilaf.esp32controller.data.SettingsRepository
import com.danilaf.esp32controller.discovery.EspDiscoveryManager
import com.danilaf.esp32controller.mqtt.MqttClientManager
import com.danilaf.esp32controller.network.EspApiClient
import com.danilaf.esp32controller.provisioning.WifiProvisioningConnector
import com.danilaf.esp32controller.ui.navigation.AppScreen
import com.danilaf.esp32controller.ui.screens.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    repository: DeviceRepository,
    settings: SettingsRepository,
    mqtt: MqttClientManager
) {
    val context = LocalContext.current
    val api = remember { EspApiClient(context) }
    val discovery = remember { EspDiscoveryManager(context) }
    val setupConnector = remember { WifiProvisioningConnector(context) }
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Devices) }
    var devices by remember { mutableStateOf(repository.getDevices()) }
    var selectedDevice by remember { mutableStateOf<Device?>(devices.firstOrNull()) }
    var selectedState by remember { mutableStateOf<DeviceState?>(null) }
    var firmwareUri by remember { mutableStateOf<Uri?>(null) }

    var setupSsid by remember { mutableStateOf("ESP32-Setup-") }
    var setupPassword by remember { mutableStateOf("esp32setup") }
    var setupBaseUrl by remember { mutableStateOf("http://192.168.4.1") }
    var homeWifiSsid by remember { mutableStateOf("") }
    var homeWifiPassword by remember { mutableStateOf("") }
    var newDeviceName by remember { mutableStateOf("ESP32 Device") }
    var newDeviceRoom by remember { mutableStateOf("") }
    var setupStatus by remember { mutableStateOf("") }
    var setupInProgress by remember { mutableStateOf(false) }

    var manualUrl by remember { mutableStateOf("") }
    var manualToken by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    val discovered = remember { mutableStateListOf<DiscoveredDevice>() }

    val snackbarHostState = remember { SnackbarHostState() }
    val mqttSettings = settings.getMqttSettings()

    val firmwarePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        firmwareUri = uri
        uri?.let { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    LaunchedEffect(mqttSettings) {
        runCatching { mqtt.connect(mqttSettings) }
    }

    DisposableEffect(Unit) {
        onDispose {
            discovery.stop()
            setupConnector.release()
            mqtt.disconnect()
        }
    }

    fun reload() {
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
                .onSuccess {
                    selectedState = it
                    repository.updatePower(device.id, it.power)
                    reload()
                }
                .onFailure { showMessage(it.message ?: "Refresh failed") }
        }
    }

    fun publishMqttCommand(device: Device, command: String, value: Boolean? = null) {
        val topic = "${mqttSettings.baseTopic}/${device.id}/command"
        val payload = JSONObject().put("command", command).also { if (value != null) it.put("value", value) }.toString()
        scope.launch {
            runCatching { mqtt.publish(topic, payload) }
                .onSuccess { showMessage("MQTT command published") }
                .onFailure { showMessage(it.message ?: "MQTT command failed") }
        }
    }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Text("ESP32 Controller", style = MaterialTheme.typography.titleLarge)
                NavigationDrawerItem(
                    label = { Text("Devices") },
                    selected = currentScreen is AppScreen.Devices,
                    onClick = { currentScreen = AppScreen.Devices }
                )
                NavigationDrawerItem(
                    label = { Text("Add Device") },
                    selected = currentScreen is AppScreen.AddDevice,
                    onClick = { currentScreen = AppScreen.AddDevice }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = currentScreen is AppScreen.Settings,
                    onClick = { currentScreen = AppScreen.Settings }
                )
            }
        }
    ) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
            when (currentScreen) {
                is AppScreen.Devices -> DevicesScreen(
                    devices = devices,
                    onOpenDevice = {
                        selectedDevice = it
                        selectedState = null
                        currentScreen = AppScreen.DeviceDetails(it.id, it.name)
                    },
                    onAddDevice = { currentScreen = AppScreen.AddDevice },
                    modifier = Modifier.padding(paddingValues)
                )

                is AppScreen.AddDevice -> AddDeviceScreen(
                    setupSsid = setupSsid,
                    setupPassword = setupPassword,
                    setupBaseUrl = setupBaseUrl,
                    homeWifiSsid = homeWifiSsid,
                    homeWifiPassword = homeWifiPassword,
                    deviceName = newDeviceName,
                    room = newDeviceRoom,
                    status = setupStatus,
                    inProgress = setupInProgress,
                    manualUrl = manualUrl,
                    manualToken = manualToken,
                    manualName = manualName,
                    scanning = scanning,
                    discovered = discovered,
                    onSetupSsidChange = { setupSsid = it },
                    onSetupPasswordChange = { setupPassword = it },
                    onSetupBaseUrlChange = { setupBaseUrl = it },
                    onHomeWifiSsidChange = { homeWifiSsid = it },
                    onHomeWifiPasswordChange = { homeWifiPassword = it },
                    onDeviceNameChange = { newDeviceName = it },
                    onRoomChange = { newDeviceRoom = it },
                    onConnectSetupWifi = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            setupStatus = "Requesting Android connection to $setupSsid..."
                            setupConnector.requestSetupNetwork(
                                ssid = setupSsid,
                                passphrase = setupPassword,
                                onAvailable = { scope.launch { setupStatus = "Connected to setup Wi-Fi. Tap Provision." } },
                                onUnavailable = { scope.launch { setupStatus = "Setup Wi-Fi connection was not completed." } },
                                onLost = { scope.launch { setupStatus = "Setup Wi-Fi disconnected." } },
                                onError = { message -> scope.launch { setupStatus = message; snackbarHostState.showSnackbar(message) } }
                            )
                        } else {
                            setupStatus = "Automatic setup Wi-Fi connection requires Android 10+."
                        }
                    },
                    onProvision = {
                        setupInProgress = true
                        setupStatus = "Provisioning ESP32..."
                        scope.launch {
                            runCatching { api.provision(setupBaseUrl, homeWifiSsid, homeWifiPassword, newDeviceName, newDeviceRoom) }
                                .onSuccess {
                                    setupInProgress = false
                                    manualToken = it.token.orEmpty()
                                    manualName = it.name
                                    setupStatus = "Provisioned. Reconnect to home Wi-Fi, scan, then add device."
                                    setupConnector.release()
                                }
                                .onFailure {
                                    setupInProgress = false
                                    setupStatus = it.message ?: "Provisioning failed"
                                }
                        }
                    },
                    onManualUrlChange = { manualUrl = it },
                    onManualTokenChange = { manualToken = it },
                    onManualNameChange = { manualName = it },
                    onManualAdd = {
                        scope.launch {
                            runCatching {
                                val info = api.getInfo(manualUrl, manualToken.ifBlank { null })
                                val device = Device(info.id.ifBlank { manualUrl }, manualName.ifBlank { info.name }, info.room, info.baseUrl, manualToken, info.firmwareVersion)
                                repository.saveDevice(device)
                                reload()
                                selectedDevice = device
                                currentScreen = AppScreen.Devices
                            }.onFailure { showMessage(it.message ?: "Unable to add device") }
                        }
                    },
                    onStartScan = {
                        discovered.clear()
                        scanning = true
                        discovery.start(
                            onDevice = { device -> if (discovered.none { it.baseUrl == device.baseUrl }) discovered.add(device) },
                            onError = { showMessage(it) }
                        )
                    },
                    onStopScan = { scanning = false; discovery.stop() },
                    onUseDiscovered = { found -> manualUrl = found.baseUrl; manualName = found.name },
                    onBack = { currentScreen = AppScreen.Devices },
                    modifier = Modifier.padding(paddingValues)
                )

                is AppScreen.Settings -> SettingsScreen(
                    language = settings.getLanguage(),
                    versionName = com.danilaf.esp32controller.BuildConfig.VERSION_NAME,
                    versionCode = com.danilaf.esp32controller.BuildConfig.VERSION_CODE,
                    repositoryUrl = "https://github.com/Danila-F/Android-App-ESP32",
                    onLanguageChange = { settings.setLanguage(it) },
                    modifier = Modifier.padding(paddingValues)
                )

                is AppScreen.DeviceDetails -> DeviceDetailsScreen(
                    device = selectedDevice,
                    state = selectedState,
                    firmwareUri = firmwareUri,
                    onBack = { currentScreen = AppScreen.Devices },
                    onRefresh = { refreshSelected() },
                    onSetPower = { enabled ->
                        val device = selectedDevice
                        if (device != null) {
                            if (mqttSettings.enabled) publishMqttCommand(device, "set_power", enabled)
                            else scope.launch {
                                runCatching { api.setPower(device, enabled) }
                                    .onSuccess { selectedState = it; repository.updatePower(device.id, it.power); reload() }
                                    .onFailure { showMessage(it.message ?: "Command failed") }
                            }
                        }
                    },
                    onToggle = {
                        val device = selectedDevice
                        if (device != null) {
                            if (mqttSettings.enabled) publishMqttCommand(device, "toggle_power")
                            else scope.launch {
                                runCatching { api.togglePower(device) }
                                    .onSuccess { selectedState = it; repository.updatePower(device.id, it.power); reload() }
                                    .onFailure { showMessage(it.message ?: "Command failed") }
                            }
                        }
                    },
                    onAddToSystemControls = {
                        selectedDevice?.let { device ->
                            val requested = DeviceControlsRequester.requestAdd(context, device)
                            showMessage(if (requested) "Android Device Controls prompt requested" else "Android Device Controls require Android 11 or newer")
                        }
                    },
                    onPickFirmware = { firmwarePicker.launch(arrayOf("application/octet-stream", "application/bin", "*/*")) },
                    onUploadFirmware = {
                        val device = selectedDevice
                        val uri = firmwareUri
                        if (device != null && uri != null) {
                            scope.launch {
                                runCatching { api.uploadFirmware(device, uri) }
                                    .onSuccess { showMessage(it) }
                                    .onFailure { showMessage(it.message ?: "OTA upload failed") }
                            }
                        }
                    },
                    onRemove = {
                        selectedDevice?.let { repository.removeDevice(it.id) }
                        reload()
                        currentScreen = AppScreen.Devices
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}
