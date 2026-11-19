package com.drliki.cleanyourphone.engine

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.drliki.cleanyourphone.model.DiscoveredDevice
import com.flyfishxu.kadb.Kadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

data class DiscoveredPairingService(
    val host: String,
    val port: Int,
    val serviceName: String
)

class AdbEngine(private val context: Context) {

    private var connection: Kadb? = null
    var connectedDevice: DiscoveredDevice? = null
        private set

    val isConnected: Boolean get() = connection != null

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private var connectDiscoveryListener: NsdManager.DiscoveryListener? = null
    private var pairingDiscoveryListener: NsdManager.DiscoveryListener? = null
    private val nsdDiscovered = ConcurrentHashMap<String, DiscoveredDevice>()

    init {
        // Ensure user.home points to internal writable storage across all threads
        System.setProperty("user.home", context.filesDir.absolutePath)
        File(context.filesDir, ".android").apply { if (!exists()) mkdirs() }
    }

    // ── Pairing (Android 11+ Wireless Debugging with SPAKE2 / TLS) ────────────
    suspend fun pair(host: String, port: Int, code: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanHost = host.trim()
                val cleanCode = code.trim()
                if (cleanHost.isBlank() || port <= 0 || cleanCode.isBlank()) {
                    throw IllegalArgumentException("Invalid host ($cleanHost), port ($port), or pairing code")
                }
                val res: Any = Kadb.pair(cleanHost, port, cleanCode)
                if (res is Boolean && !res) {
                    throw IllegalStateException("Pairing failed: Device rejected code or session expired. Make sure 'Pair with pairing code' popup is still open on target phone.")
                }
                true
            }
        }

    // ── Connection with Authorization Polling Retry Loop ─────────────────────
    suspend fun connect(device: DiscoveredDevice): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                disconnect()

                var lastError: Throwable? = null
                var activeKadb: Kadb? = null

                // Retry loop: up to 10 attempts (15 seconds total)
                // Allows user time to see prompt and tap "Always allow" on target phone
                for (attempt in 1..10) {
                    try {
                        val candidate = Kadb.create(device.host, device.port)
                        val resp = candidate.shell("echo ok")
                        if (resp.output.contains("ok")) {
                            activeKadb = candidate
                            break
                        } else {
                            try { candidate.close() } catch (_: Exception) { }
                        }
                    } catch (e: Exception) {
                        lastError = e
                        val msg = e.message ?: ""
                        // If refused on first attempt, the port is simply closed / wrong
                        if (attempt == 1 && (msg.contains("ECONNREFUSED", ignoreCase = true) ||
                                             msg.contains("Connection refused", ignoreCase = true))) {
                            throw IllegalStateException("Connection refused on ${device.host}:${device.port}. Verify Wireless Debugging is turned ON in Developer Options and check the port.")
                        }
                    }
                    delay(1500)
                }

                if (activeKadb == null) {
                    val rawErr = lastError?.message ?: "Handshake timed out"
                    if (rawErr.contains("ECONNRESET", ignoreCase = true) || rawErr.contains("Connection reset", ignoreCase = true)) {
                        throw IllegalStateException("Connection reset by ${device.host}:${device.port}. Note: Make sure to enter the Connect Port (e.g. 41255) from the main Wireless Debugging screen, NOT the temporary pairing port.")
                    }
                    throw IllegalStateException("Connection failed on ${device.host}:${device.port}: $rawErr. Please check target phone screen and tap 'Always allow'.")
                }

                connection = activeKadb
                connectedDevice = device

                val model = try {
                    activeKadb.shell("getprop ro.product.model").output.trim().ifBlank { device.name }
                } catch (_: Exception) { device.name }

                "Connected to $model (${device.host}:${device.port})"
            }
        }

    fun disconnect() {
        try { connection?.close() } catch (_: Exception) { }
        connection = null
        connectedDevice = null
    }

    // ── Shell commands on connected device ───────────────────────────────────
    suspend fun shell(command: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val adb = connection ?: throw IllegalStateException("Not connected")
            val resp = adb.shell(command)
            if (resp.exitCode != 0) {
                throw IllegalStateException("Command failed (${resp.exitCode}): ${resp.output.trim()}")
            }
            resp.output.trim()
        }
    }

    suspend fun listPackages(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val adb = connection ?: throw IllegalStateException("Not connected")
            adb.shell("pm list packages").output.lines()
                .filter { it.startsWith("package:") }
                .map { it.removePrefix("package:").trim() }
                .filter { it.isNotBlank() }
        }
    }

    suspend fun disablePackage(pkg: String): Result<String> =
        shell("pm disable-user --user 0 $pkg")

    suspend fun enablePackage(pkg: String): Result<String> =
        shell("pm enable --user 0 $pkg")

    // ── Pairing Verification Helper ──────────────────────────────────────────
    suspend fun verifyPairingPort(host: String, pairingPort: Int): Boolean =
        withContext(Dispatchers.IO) {
            portOpen(host, pairingPort, 2000)
        }

    // ── Network scanning (Dynamic port & subnet scan) ────────────────────────
    suspend fun scanNetwork(ports: List<Int> = listOf(5555)): List<DiscoveredDevice> =
        withContext(Dispatchers.IO) {
            val localIp = localIpAddress() ?: return@withContext emptyList()
            val subnet = localIp.substringBeforeLast(".")
            val found = mutableListOf<DiscoveredDevice>()

            // Include any devices already discovered via mDNS / NSD
            found.addAll(nsdDiscovered.values)

            // Probe subnet on requested ports (e.g. 5555 or dynamic port)
            for (port in ports) {
                val scanned = (1..254).map { i ->
                    async {
                        val host = "$subnet.$i"
                        if (host != localIp && portOpen(host, port, 280)) {
                            val name = try {
                                val tmp = Kadb.create(host, port)
                                val n = tmp.shell("getprop ro.product.model").output.trim()
                                    .ifBlank { "Android Device" }
                                try { tmp.close() } catch (_: Exception) { }
                                n
                            } catch (_: Exception) { "Android Device ($port)" }
                            DiscoveredDevice(host, port, name, name)
                        } else null
                    }
                }.awaitAll().filterNotNull()
                found.addAll(scanned)
            }

            // Deduplicate by host:port
            found.distinctBy { "${it.host}:${it.port}" }
        }

    // ── mDNS / NSD Service Discovery for Wireless Debugging ──────────────────
    fun startNsdDiscovery(
        onDeviceFound: (DiscoveredDevice) -> Unit,
        onPairingServiceFound: ((DiscoveredPairingService) -> Unit)? = null
    ) {
        stopNsdDiscovery()
        val manager = nsdManager ?: return

        // 1. Listen for connectable wireless debugging services (_adb-tls-connect._tcp)
        connectDiscoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                try {
                    manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

                        override fun onServiceResolved(resolved: NsdServiceInfo) {
                            val host = resolved.host?.hostAddress ?: return
                            val port = resolved.port
                            val name = resolved.serviceName ?: "Android Phone"
                            val device = DiscoveredDevice(host, port, name, name)
                            nsdDiscovered["$host:$port"] = device
                            onDeviceFound(device)
                        }
                    })
                } catch (_: Exception) { }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                nsdDiscovered.remove(serviceInfo.serviceName)
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                try { manager.stopServiceDiscovery(this) } catch (_: Exception) { }
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        try {
            manager.discoverServices("_adb-tls-connect._tcp", NsdManager.PROTOCOL_DNS_SD, connectDiscoveryListener)
        } catch (_: Exception) { }

        // 2. Listen for active pairing services (_adb-tls-pairing._tcp) if callback provided
        if (onPairingServiceFound != null) {
            pairingDiscoveryListener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) {}

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    try {
                        manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

                            override fun onServiceResolved(resolved: NsdServiceInfo) {
                                val host = resolved.host?.hostAddress ?: return
                                val port = resolved.port
                                val name = resolved.serviceName ?: "Pairing Phone"
                                onPairingServiceFound(DiscoveredPairingService(host, port, name))
                            }
                        })
                    } catch (_: Exception) { }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    try { manager.stopServiceDiscovery(this) } catch (_: Exception) { }
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            }

            try {
                manager.discoverServices("_adb-tls-pairing._tcp", NsdManager.PROTOCOL_DNS_SD, pairingDiscoveryListener)
            } catch (_: Exception) { }
        }
    }

    fun stopNsdDiscovery() {
        connectDiscoveryListener?.let {
            try { nsdManager?.stopServiceDiscovery(it) } catch (_: Exception) { }
        }
        connectDiscoveryListener = null

        pairingDiscoveryListener?.let {
            try { nsdManager?.stopServiceDiscovery(it) } catch (_: Exception) { }
        }
        pairingDiscoveryListener = null
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private fun portOpen(host: String, port: Int, timeout: Int): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(host, port), timeout); true }
    } catch (_: Exception) { false }

    private fun localIpAddress(): String? {
        for (intf in NetworkInterface.getNetworkInterfaces()) {
            for (addr in intf.inetAddresses) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) return addr.hostAddress
            }
        }
        return null
    }
}
