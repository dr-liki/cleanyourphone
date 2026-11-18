package com.drliki.cleanyourphone.engine

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.drliki.cleanyourphone.model.DiscoveredDevice
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    private var connection: Dadb? = null
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
        // Pre-generate ADB RSA 2048 keypair in internal storage so it's instantly ready
        getOrCreateKeyPair()
    }

    /**
     * Programmatically ensures valid RSA 2048-bit ADB private/public keys exist in app-private storage.
     * Prevents "android/adbkey: open failed: ENOENT" caused by missing keys or unmapped user.home.
     */
    private fun getOrCreateKeyPair(): AdbKeyPair {
        val dotAndroid = File(context.filesDir, ".android").apply { if (!exists()) mkdirs() }
        val priv = File(dotAndroid, "adbkey")
        val pub = File(dotAndroid, "adbkey.pub")

        if (!priv.exists() || !pub.exists() || priv.length() == 0L) {
            AdbKeyPair.generate(priv, pub)
        }

        return AdbKeyPair.read(priv, pub)
    }

    // ── Connection creation ──────────────────────────────────────────────────
    private fun createDadb(host: String, port: Int): Dadb {
        val keyPair = getOrCreateKeyPair()
        return Dadb.create(host, port, keyPair)
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
                                val tmp = createDadb(host, port)
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

    // ── Connection ───────────────────────────────────────────────────────────
    suspend fun connect(device: DiscoveredDevice): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                disconnect()
                val adb = createDadb(device.host, device.port)
                val resp = adb.shell("echo ok")
                if (!resp.output.contains("ok")) {
                    adb.close()
                    throw IllegalStateException("ADB handshake failed on ${device.host}:${device.port}. Check prompt on target phone.")
                }
                connection = adb
                connectedDevice = device
                val model = try {
                    adb.shell("getprop ro.product.model").output.trim()
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
