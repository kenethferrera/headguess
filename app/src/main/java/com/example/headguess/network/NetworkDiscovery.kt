package com.example.headguess.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.example.headguess.utils.PermissionManager
import android.os.Handler
import android.os.Looper

data class DiscoveredHost(
    val ip: String,
    val serviceName: String,
    val timestamp: Long = System.currentTimeMillis()
)

object NetworkDiscovery {
    // Exposed so client knows what port to connect; NSD gives host+port.
    // We'll resolve it from NSD. Fallback to 8888 if needed.
    var port: Int = 8888
    var applicationContext: Context? = null

    // Track the most recent discovery so callers can stop it on navigation
    private var lastDiscovery: Pair<NsdManager, NsdManager.DiscoveryListener>? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    fun discoverHost(onFound: (ip: String) -> Unit, onLost: (() -> Unit)? = null, gameType: String = "guessword") {
        val context = applicationContext ?: return
        
        // Check if nearby devices permission is granted
        if (!PermissionManager.checkNearbyDevicesPermission(context)) {
            android.util.Log.w("NetworkDiscovery", "Nearby devices permission not granted, cannot discover hosts")
            return
        }
        
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                android.util.Log.d("NetworkDiscovery", "Discovery started")
            }
            override fun onDiscoveryStopped(serviceType: String) {
                android.util.Log.d("NetworkDiscovery", "Discovery stopped")
            }
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                android.util.Log.e("NetworkDiscovery", "Discovery failed: $errorCode")
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                android.util.Log.e("NetworkDiscovery", "Stop discovery failed: $errorCode")
            }
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                android.util.Log.d("NetworkDiscovery", "Service found: name=${serviceInfo.serviceName}, type=${serviceInfo.serviceType}")
                val expectedServiceType = when (gameType) {
                    "charades" -> "_charades._tcp."
                    "impostor" -> "_impostor._tcp."
                    "custom" -> "_custom._tcp."
                    else -> "_headguess._tcp."
                }
                // Some devices append domain suffixes (e.g., .local.) or change casing; use contains + ignoreCase
                if (serviceInfo.serviceType.contains(expectedServiceType, ignoreCase = true)) {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onServiceResolved(resolved: NsdServiceInfo) {
                            port = resolved.port
                            val hostAddress = resolved.host.hostAddress
                            android.util.Log.d("NetworkDiscovery", "Resolved: $hostAddress:$port")
                            onFound(hostAddress)
                        }
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            android.util.Log.e("NetworkDiscovery", "Resolve failed: $errorCode")
                        }
                    })
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                android.util.Log.d("NetworkDiscovery", "Service lost: ${serviceInfo.serviceName}")
                onLost?.invoke()
            }
        }
        val serviceType = when (gameType) {
            "charades" -> "_charades._tcp."
            "impostor" -> "_impostor._tcp."
            "custom" -> "_custom._tcp."
            else -> "_headguess._tcp."
        }
        // Remember this discovery so we can stop it later
        lastDiscovery = Pair(nsdManager, discoveryListener)
        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun discoverMultipleHosts(onHostFound: (ip: String, serviceName: String) -> Unit, onHostLost: (ip: String) -> Unit, gameType: String = "guessword") {
        val context = applicationContext ?: return
        
        // Check if nearby devices permission is granted
        if (!PermissionManager.checkNearbyDevicesPermission(context)) {
            android.util.Log.w("NetworkDiscovery", "Nearby devices permission not granted, cannot discover hosts")
            return
        }
        
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val serviceToIpMap = mutableMapOf<String, String>() // Track service name to IP mapping
        val handler = Handler(Looper.getMainLooper())
        val pendingRemovalByService = mutableMapOf<String, Runnable>()
        val ipLastSeen = mutableMapOf<String, Long>() // For impostor: track last seen per IP
        val pendingRemovalByIp = mutableMapOf<String, Runnable>()
        val debounceMillis = if (gameType == "impostor") 5000L else 0L
        val resolveRetryCount = if (gameType == "impostor") 2 else 0
        
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                android.util.Log.e("NetworkDiscovery", "Discovery failed: $errorCode")
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                android.util.Log.e("NetworkDiscovery", "Stop discovery failed: $errorCode")
            }
            override fun onDiscoveryStarted(serviceType: String) {
                android.util.Log.d("NetworkDiscovery", "Discovery started")
            }
            override fun onDiscoveryStopped(serviceType: String) {
                android.util.Log.d("NetworkDiscovery", "Discovery stopped")
            }
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                android.util.Log.d("NetworkDiscovery", "Service found: name=${serviceInfo.serviceName}, type=${serviceInfo.serviceType}")
                val expectedServiceType = when (gameType) {
                    "charades" -> "_charades._tcp."
                    "impostor" -> "_impostor._tcp."
                    "custom" -> "_custom._tcp."
                    else -> "_headguess._tcp."
                }
                // Some devices append domain suffixes (e.g., .local.) or change casing; use contains + ignoreCase
                if (serviceInfo.serviceType.contains(expectedServiceType, ignoreCase = true)) {
                    // Cancel any pending removal for this service name
                    pendingRemovalByService.remove(serviceInfo.serviceName)?.let { handler.removeCallbacks(it) }

                    // Resolve with optional retries
                    fun resolveTry(remaining: Int) {
                        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onServiceResolved(resolved: NsdServiceInfo) {
                                port = resolved.port
                                val hostAddress = resolved.host.hostAddress
                                val serviceName = serviceInfo.serviceName
                                android.util.Log.d("NetworkDiscovery", "Resolved: $hostAddress:$port (service: $serviceName)")
                                serviceToIpMap[serviceName] = hostAddress
                                if (gameType == "impostor") {
                                    ipLastSeen[hostAddress] = System.currentTimeMillis()
                                    // Cancel any pending removal by IP
                                    pendingRemovalByIp.remove(hostAddress)?.let { handler.removeCallbacks(it) }
                                }
                                onHostFound(hostAddress, serviceName)
                            }
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                                android.util.Log.e("NetworkDiscovery", "Resolve failed: $errorCode (remain=$remaining)")
                                if (remaining > 0) handler.postDelayed({ resolveTry(remaining - 1) }, 200L)
                            }
                        })
                    }
                    resolveTry(resolveRetryCount)
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val serviceName = serviceInfo.serviceName
                android.util.Log.d("NetworkDiscovery", "Service lost: $serviceName")
                val removeAction = Runnable {
                    val hostAddress = serviceToIpMap[serviceName]
                    if (hostAddress != null) {
                        // Remove mapping for this service
                        serviceToIpMap.remove(serviceName)
                        if (gameType == "impostor") {
                            // Debounce by IP: only remove if not seen again recently
                            val lastSeen = ipLastSeen[hostAddress] ?: 0L
                            val tooOld = System.currentTimeMillis() - lastSeen >= debounceMillis
                            if (tooOld) {
                                android.util.Log.d("NetworkDiscovery", "Removing host by IP (debounced): $hostAddress")
                                ipLastSeen.remove(hostAddress)
                                onHostLost(hostAddress)
                            } else {
                                android.util.Log.d("NetworkDiscovery", "Skip removal; IP $hostAddress seen recently")
                            }
                            pendingRemovalByIp.remove(hostAddress)
                        } else {
                            // Other game types: immediate remove if no other service maps
                            val ipStillPresent = serviceToIpMap.values.any { it == hostAddress }
                            if (!ipStillPresent) onHostLost(hostAddress)
                        }
                    } else {
                        android.util.Log.d("NetworkDiscovery", "Service lost but no IP mapping found for: $serviceName")
                    }
                    pendingRemovalByService.remove(serviceName)
                }
                if (debounceMillis > 0L && gameType == "impostor") {
                    // Schedule by IP to ensure stability across service name changes
                    val ip = serviceToIpMap[serviceName]
                    if (ip != null) {
                        pendingRemovalByIp[ip]?.let { handler.removeCallbacks(it) }
                        pendingRemovalByIp[ip] = removeAction
                        handler.postDelayed(removeAction, debounceMillis)
                    } else {
                        pendingRemovalByService[serviceName] = removeAction
                        handler.postDelayed(removeAction, debounceMillis)
                    }
                } else if (debounceMillis > 0L) {
                    pendingRemovalByService[serviceName] = removeAction
                    handler.postDelayed(removeAction, debounceMillis)
                } else {
                    removeAction.run()
                }
            }
        }
        val serviceType = when (gameType) {
            "charades" -> "_charades._tcp."
            "impostor" -> "_impostor._tcp."
            "custom" -> "_custom._tcp."
            else -> "_headguess._tcp."
        }
        // Remember this discovery so we can stop it later
        lastDiscovery = Pair(nsdManager, discoveryListener)
        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    /**
     * Stop the most recently started discovery session, if any.
     * This mirrors the behavior used by Guess the Word join screens.
     */
    fun stopAllDiscoveries() {
        try {
            lastDiscovery?.let { (nsdManager, listener) ->
                nsdManager.stopServiceDiscovery(listener)
            }
        } catch (_: Exception) {
        } finally {
            lastDiscovery = null
        }
    }
}




