package com.drliki.cleanyourphone.engine

import com.drliki.cleanyourphone.model.DiscoveredDevice
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

class AdbEngine {

    private var connection: Dadb? = null
    var connectedDevice: DiscoveredDevice? = null
        private set

    val isConnected: Boolean get() = connection != null

    // ── Network scanning ─────────────────────────────────────────────────────
    suspend fun scanNetwork(port: Int = 5555): List<DiscoveredDevice> =
        withContext(Dispatchers.IO) {
            val localIp = localIpAddress() ?: return@withContext emptyList()
            val subnet = localIp.substringBeforeLast(".")

            (1..254).map { i ->
                async {
                    val host = "$subnet.$i"
                    if (host != localIp && portOpen(host, port, 350)) {
                        val name = try {
                            val tmp = Dadb.create(host, port)
                            val n = tmp.shell("getprop ro.product.model").output.trim()
                                .ifBlank { "Android Device" }
                            try { tmp.close() } catch (_: Exception) { }
                            n
                        } catch (_: Exception) { "Android Device" }
                        DiscoveredDevice(host, port, name, name)
                    } else null
                }
            }.awaitAll().filterNotNull()
        }

    // ── Connection ───────────────────────────────────────────────────────────
    suspend fun connect(device: DiscoveredDevice): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                disconnect()
                val adb = Dadb.create(device.host, device.port)
                val resp = adb.shell("echo ok")
                if (!resp.output.contains("ok")) {
                    adb.close()
                    throw IllegalStateException("ADB handshake failed")
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
