package com.drliki.cleanyourphone.engine

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.drliki.cleanyourphone.model.DiscoveredDevice
import dadb.AdbKeyPair
import dadb.Dadb
import io.github.rhythmcache.adb.AdbClient
import io.github.rhythmcache.adb.FileKeyProvider
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

    private var client: AdbClient? = null
    private var dadbConnection: Dadb? = null
    var connectedDevice: DiscoveredDevice? = null
        private set

    val isConnected: Boolean get() = (client != null && !(client?.isClosed ?: true)) || dadbConnection != null

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
     * Returns an AdbKeyProvider backed by internal writable storage.
     */
    fun getKeyProvider(): FileKeyProvider {
        val dotAndroid = File(context.filesDir, ".android").apply { if (!exists()) mkdirs() }
        val priv = File(dotAndroid, "adbkey")
        val pub = File(dotAndroid, "adbkey.pub")
        return FileKeyProvider(priv, pub, identityComment = "CleanYourPhone")
    }

    /**
     * Programmatically ensures valid RSA 2048-bit ADB private/public keys exist in app-private storage.
     * Prevents "android/adbkey: open failed: ENOENT" caused by missing keys or unmapped user.home.
     */
    fun getOrCreateKeyPair(): AdbKeyPair {
        val dotAndroid = File(context.filesDir, ".android").apply { if (!exists()) mkdirs() }
        val priv = File(dotAndroid, "adbkey")
        val pub = File(dotAndroid, "adbkey.pub")

        if (!priv.exists() || !pub.exists() || priv.length() == 0L) {
            AdbKeyPair.generate(priv, pub)
        }

        return AdbKeyPair.read(priv, pub)
    }

    // ── Connection creation for dadb fallback ────────────────────────────────
    private fun createDadb(host: String, port: Int): Dadb {
        val keyPair = getOrCreateKeyPair()
        return Dadb.create(host, port, keyPair)
    }

    // ── Android 11+ SPAKE2 Pairing over TLS ──────────────────────────────────
    /**
     * Pairs with the target phone using the 6-digit PIN and the temporary pairing port.
     * Android 11+ uses SPAKE2 over TLS on the pairing port advertised by _adb-tls-pairing._tcp.
     */
    suspend fun pair(host: String, pairingPort: Int, pairingCode: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val keyProvider = getKeyProvider()
                val result = AdbClient.pairTls(
                    host = host,
                    port = pairingPort,
                    pairingCode = pairingCode.trim(),
                    keyProvider = keyProvider
                )
                if (result.isFailure) {
                    val err = result.exceptionOrNull()?.message ?: "Pairing handshake failed"
                    throw IllegalStateException("Pairing failed: $err. Make sure the 'Pair device with pairing code' popup remains open on the other phone and verify the 6-digit code.")
                }
                "Device paired successfully with 6-digit code!"
            }
        }

    // ── Connection with TLS and TCP Multi-Mode Support ───────────────────────
    suspend fun connect(device: DiscoveredDevice): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                disconnect()

                val keyProvider = getKeyProvider()
                var activeClient: AdbClient? = null
                var activeDadb: Dadb? = null
                var lastError: Throwable? = null

                val isPort5555 = device.port == 5555

                // Polling retry loop: up to 8 attempts (with 1.2s delay between attempts)
                for (attempt in 1..8) {
                    try {
                        if (!isPort5555) {
                            // On dynamic port, attempt TLS 1.3 first (standard Android 11+ Wireless Debugging)
                            try {
                                val c = AdbClient.connectTls(device.host, device.port, keyProvider, handshakeTimeoutMs = 6000)
                                val resp = c.shell("echo ok")
                                if (resp.stdoutText.contains("ok")) {
                                    activeClient = c
                                    break
                                } else {
                                    try { c.close() } catch (_: Exception) {}
                                }
                            } catch (tlsEx: Exception) {
                                lastError = tlsEx
                                // Fall back to plain TCP in case the custom port is unencrypted
                                try {
                                    val c = AdbClient.connectTcp(device.host, device.port, keyProvider, handshakeTimeoutMs = 4000)
                                    val resp = c.shell("echo ok")
                                    if (resp.stdoutText.contains("ok")) {
                                        activeClient = c
                                        break
                                    } else {
                                        try { c.close() } catch (_: Exception) {}
                                    }
                                } catch (_: Exception) {}
                            }
                        } else {
                            // Port 5555: plain TCP
                            try {
                                val c = AdbClient.connectTcp(device.host, device.port, keyProvider, handshakeTimeoutMs = 5000)
                                val resp = c.shell("echo ok")
                                if (resp.stdoutText.contains("ok")) {
                                    activeClient = c
                                    break
                                } else {
                                    try { c.close() } catch (_: Exception) {}
                                }
                            } catch (e: Exception) {
                                lastError = e
                                // Fallback to dadb for port 5555 if needed
                                try {
                                    val d = createDadb(device.host, device.port)
                                    val resp = d.shell("echo ok")
                                    if (resp.output.contains("ok")) {
                                        activeDadb = d
                                        break
                                    } else {
                                        try { d.close() } catch (_: Exception) {}
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    } catch (e: Exception) {
                        lastError = e
                        val msg = e.message ?: ""
                        if (attempt == 1 && (msg.contains("ECONNREFUSED", ignoreCase = true) || msg.contains("Connection refused", ignoreCase = true))) {
                            throw IllegalStateException("Connection refused on ${device.host}:${device.port}. Verify Wireless Debugging is turned ON in Developer Options.")
                        }
                    }
                    delay(1200)
                }

                if (activeClient == null && activeDadb == null) {
                    val rawErr = lastError?.message ?: "Handshake timed out"
                    if (!isPort5555) {
                        throw IllegalStateException("Connection failed on dynamic port ${device.host}:${device.port}. Android 11+ dynamic ports require pairing with the 6-digit code first. Open 'Pair device with pairing code' on the other phone and tap 'Pair Device' in CleanYourPhone. (Details: $rawErr)")
                    } else {
                        throw IllegalStateException("Connection failed on ${device.host}:${device.port}: $rawErr. Please check target phone screen and tap 'Always allow'.")
                    }
                }

                client = activeClient
                dadbConnection = activeDadb
                connectedDevice = device

                val model = try {
                    if (activeClient != null) {
                        val res = activeClient.shell("getprop ro.product.model")
                        res.stdoutText.trim().ifBlank { device.name }
                    } else {
                        activeDadb?.shell("getprop ro.product.model")?.output?.trim()?.ifBlank { device.name } ?: device.name
                    }
                } catch (_: Exception) { device.name }

                "Connected to $model (${device.host}:${device.port})"
            }
        }

    fun disconnect() {
        try { client?.close() } catch (_: Exception) { }
        try { dadbConnection?.close() } catch (_: Exception) { }
        client = null
        dadbConnection = null
        connectedDevice = null
    }

    // ── Shell commands on connected device ───────────────────────────────────
    suspend fun shell(command: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val c = client
            if (c != null) {
                val resp = c.shell(command)
                if (resp.exitCode != 0) {
                    throw IllegalStateException("Command failed (${resp.exitCode}): ${resp.stderrText.trim().ifBlank { resp.stdoutText.trim() }}")
                }
                resp.stdoutText.trim()
            } else {
                val d = dadbConnection ?: throw IllegalStateException("Not connected")
                val resp = d.shell(command)
                if (resp.exitCode != 0) {
                    throw IllegalStateException("Command failed (${resp.exitCode}): ${resp.output.trim()}")
                }
                resp.output.trim()
            }
        }
    }

    suspend fun listPackages(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val output = shell("pm list packages").getOrThrow()
            output.lines()
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
