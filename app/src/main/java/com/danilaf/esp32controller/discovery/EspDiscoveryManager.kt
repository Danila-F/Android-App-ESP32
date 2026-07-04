package com.danilaf.esp32controller.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.danilaf.esp32controller.data.DiscoveredDevice

class EspDiscoveryManager(context: Context) {
    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun start(
        onDevice: (DiscoveredDevice) -> Unit,
        onError: (String) -> Unit
    ) {
        stop()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != SERVICE_TYPE) return
                resolve(serviceInfo, onDevice, onError)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                onError("Discovery start failed: $errorCode")
                stop()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                onError("Discovery stop failed: $errorCode")
                stop()
            }
        }
        discoveryListener = listener
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun stop() {
        val listener = discoveryListener ?: return
        runCatching { nsdManager.stopServiceDiscovery(listener) }
        discoveryListener = null
    }

    private fun resolve(
        serviceInfo: NsdServiceInfo,
        onDevice: (DiscoveredDevice) -> Unit,
        onError: (String) -> Unit
    ) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                onError("Resolve failed for ${serviceInfo.serviceName}: $errorCode")
            }

            override fun onServiceResolved(resolved: NsdServiceInfo) {
                val hostAddress = resolved.host?.hostAddress ?: return
                val port = resolved.port.takeIf { it > 0 } ?: 80
                onDevice(
                    DiscoveredDevice(
                        name = resolved.serviceName,
                        host = hostAddress,
                        port = port,
                        serviceName = resolved.serviceName,
                        baseUrl = "http://$hostAddress:$port"
                    )
                )
            }
        }
        nsdManager.resolveService(serviceInfo, resolveListener)
    }

    companion object {
        private const val SERVICE_TYPE = "_espctrl._tcp."
    }
}
