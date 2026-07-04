package com.danilaf.esp32controller.provisioning

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import androidx.annotation.RequiresApi

class WifiProvisioningConnector(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun release() {
        callback?.let { runCatching { connectivityManager.unregisterNetworkCallback(it) } }
        callback = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching { connectivityManager.bindProcessToNetwork(null) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestSetupNetwork(
        ssid: String,
        passphrase: String,
        onAvailable: () -> Unit,
        onUnavailable: () -> Unit,
        onLost: () -> Unit,
        onError: (String) -> Unit
    ) {
        release()

        if (ssid.isBlank()) {
            onError("ESP32 setup Wi-Fi SSID is required")
            return
        }
        if (passphrase.length !in 8..63) {
            onError("ESP32 setup Wi-Fi password must be 8–63 characters for WPA2")
            return
        }

        val specifier = try {
            WifiNetworkSpecifier.Builder()
                .setSsid(ssid.trim())
                .setWpa2Passphrase(passphrase)
                .build()
        } catch (error: RuntimeException) {
            onError("Invalid setup Wi-Fi request: ${error.message ?: error.javaClass.simpleName}")
            return
        }

        val request = try {
            NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifier)
                .build()
        } catch (error: RuntimeException) {
            onError("Unable to create Wi-Fi request: ${error.message ?: error.javaClass.simpleName}")
            return
        }

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val bound = runCatching { connectivityManager.bindProcessToNetwork(network) }.getOrDefault(false)
                if (bound) {
                    onAvailable()
                } else {
                    onError("Connected to setup Wi-Fi, but the app could not route requests through it")
                }
            }

            override fun onUnavailable() {
                onUnavailable()
            }

            override fun onLost(network: Network) {
                onLost()
            }
        }

        try {
            callback = networkCallback
            connectivityManager.requestNetwork(request, networkCallback)
        } catch (error: SecurityException) {
            callback = null
            onError("Wi-Fi setup permission error: ${error.message ?: error.javaClass.simpleName}")
        } catch (error: RuntimeException) {
            callback = null
            onError("Wi-Fi setup request failed: ${error.message ?: error.javaClass.simpleName}")
        }
    }
}
