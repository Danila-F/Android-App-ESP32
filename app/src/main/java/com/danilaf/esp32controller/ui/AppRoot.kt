package com.danilaf.esp32controller.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.danilaf.esp32controller.data.Device
import com.danilaf.esp32controller.data.DeviceRepository
import com.danilaf.esp32controller.data.DeviceState
import com.danilaf.esp32controller.data.DiscoveredDevice
import com.danilaf.esp32controller.data.SettingsRepository
import com.danilaf.esp32controller.mqtt.MqttClientManager
import com.danilaf.esp32controller.ui.navigation.AppScreen
import com.danilaf.esp32controller.ui.screens.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    repository: DeviceRepository,
    settings: SettingsRepository,
    mqtt: MqttClientManager
) {
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Devices) }
    var devices by remember { mutableStateOf(repository.getDevices()) }

    var selectedDevice by remember { mutableStateOf<Device?>(null) }
    var selectedState by remember { mutableStateOf<DeviceState?>(null) }
    var firmwareUri by remember { mutableStateOf<android.net.Uri?>(null) }

    var scanning by remember { mutableStateOf(false) }
    val discovered = remember { mutableStateListOf<DiscoveredDevice>() }

    var snackbarHostState = remember { SnackbarHostState() }

    val mqttSettings = settings.getMqttSettings()

    LaunchedEffect(mqttSettings) {
        runCatching { mqtt.connect(mqttSettings) }
    }

    fun reload() {
        devices = repository.getDevices()
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
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
            when (val screen = currentScreen) {
                is AppScreen.Devices -> DevicesScreen(
                    devices = devices,
                    onOpenDevice = {
                        selectedDevice = it
                        currentScreen = AppScreen.DeviceDetails(it.id, it.name)
                    },
                    onAddDevice = { currentScreen = AppScreen.AddDevice },
                    modifier = Modifier.padding(padding)
                )

                is AppScreen.AddDevice -> AddDeviceScreen(
                    setupSsid = "ESP32-Setup-",
                    setupPassword = "esp32setup",
                    setupBaseUrl = "http://192.168.4.1",
                    homeWifiSsid = "",
                    homeWifiPassword = "",
                    deviceName = "ESP32 Device",
                    room = "",
                    status = "",
                    inProgress = false,
                    manualUrl = "",
                    manualToken = "",
                    manualName = "",
                    scanning = scanning,
                    discovered = discovered,
                    onSetupSsidChange = {},
                    onSetupPasswordChange = {},
                    onSetupBaseUrlChange = {},
                    onHomeWifiSsidChange = {},
                    onHomeWifiPasswordChange = {},
                    onDeviceNameChange = {},
                    onRoomChange = {},
                    onConnectSetupWifi = {},
                    onProvision = {},
                    onManualUrlChange = {},
                    onManualTokenChange = {},
                    onManualNameChange = {},
                    onManualAdd = {},
                    onStartScan = {},
                    onStopScan = {},
                    onUseDiscovered = {},
                    onBack = { currentScreen = AppScreen.Devices },
                    modifier = Modifier.padding(padding)
                )

                is AppScreen.Settings -> SettingsScreen(
                    language = settings.getLanguage(),
                    versionName = "1.0",
                    versionCode = 1,
                    repositoryUrl = "https://github.com/Danila-F/Android-App-ESP32",
                    onLanguageChange = { settings.setLanguage(it) },
                    modifier = Modifier.padding(padding)
                )

                is AppScreen.DeviceDetails -> DeviceDetailsScreen(
                    device = selectedDevice,
                    state = selectedState,
                    firmwareUri = firmwareUri,
                    onBack = { currentScreen = AppScreen.Devices },
                    onRefresh = {},
                    onSetPower = { enabled -> },
                    onToggle = {},
                    onAddToSystemControls = {},
                    onPickFirmware = {},
                    onUploadFirmware = {},
                    onRemove = {
                        selectedDevice?.let { repository.removeDevice(it.id) }
                        reload()
                        currentScreen = AppScreen.Devices
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}
