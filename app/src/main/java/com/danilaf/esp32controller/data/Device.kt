package com.danilaf.esp32controller.data

data class Device(
    val id: String,
    val name: String,
    val room: String,
    val baseUrl: String,
    val token: String,
    val firmwareVersion: String? = null,
    val lastKnownPower: Boolean? = null
)

data class DeviceState(
    val online: Boolean,
    val networkOnline: Boolean,
    val power: Boolean,
    val firmwareVersion: String?,
    val ip: String?
)

data class DeviceInfo(
    val id: String,
    val name: String,
    val room: String,
    val baseUrl: String,
    val firmwareVersion: String?,
    val token: String? = null
)

data class DiscoveredDevice(
    val name: String,
    val host: String,
    val port: Int,
    val serviceName: String,
    val baseUrl: String
)
