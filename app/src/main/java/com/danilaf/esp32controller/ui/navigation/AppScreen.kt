package com.danilaf.esp32controller.ui.navigation

sealed class AppScreen(val title: String) {
    object Devices : AppScreen("Devices")
    object AddDevice : AppScreen("Add device")
    object Settings : AppScreen("Settings")
    data class DeviceDetails(val deviceId: String, val deviceName: String) : AppScreen(deviceName)
}
