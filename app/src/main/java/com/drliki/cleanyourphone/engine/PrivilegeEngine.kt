package com.drliki.cleanyourphone.engine

import android.content.pm.PackageManager
import com.drliki.cleanyourphone.model.ActionLogEntry
import com.drliki.cleanyourphone.model.PrivilegeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PrivilegeEngine {

    fun detectPrivilegeMode(): PrivilegeMode {
        return try {
            if (!Shizuku.pingBinder()) {
                PrivilegeMode.SIMULATION_PREVIEW
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                PrivilegeMode.SHIZUKU_READY
            } else {
                PrivilegeMode.SHIZUKU_PERMISSION_NEEDED
            }
        } catch (_: Throwable) {
            PrivilegeMode.SIMULATION_PREVIEW
        }
    }

    fun requestShizukuPermissionIfNeeded(requestCode: Int = 1001) {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (_: Throwable) {}
    }

    suspend fun disablePackageUser0(
        packageName: String,
        appLabel: String,
        tierLabel: String
    ): Result<ActionLogEntry> = withContext(Dispatchers.IO) {
        val disableCmd = "pm disable-user --user 0 $packageName"
        val undoCmd = "pm enable --user 0 $packageName"
        val mode = detectPrivilegeMode()

        runCatching {
            if (mode == PrivilegeMode.SHIZUKU_READY) {
                executeShizukuShell(disableCmd)
            }
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            ActionLogEntry(
                id = UUID.randomUUID().toString(),
                timestamp = timestamp,
                packageName = packageName,
                appLabel = appLabel,
                tierLabel = tierLabel,
                commandExecuted = disableCmd,
                undoCommand = undoCmd,
                undone = false
            )
        }
    }

    suspend fun enablePackageUser0(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        val enableCmd = "pm enable --user 0 $packageName"
        val mode = detectPrivilegeMode()

        runCatching {
            if (mode == PrivilegeMode.SHIZUKU_READY) {
                executeShizukuShell(enableCmd)
            } else {
                "Simulated: $enableCmd"
            }
        }
    }

    private fun executeShizukuShell(command: String): String {
        val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        ).apply { isAccessible = true }

        val process = newProcessMethod.invoke(
            null,
            arrayOf("sh", "-c", command),
            null,
            null
        ) as Process

        val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
        val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
        val exitCode = process.waitFor()

        if (exitCode != 0 && stderr.isNotBlank()) {
            throw IllegalStateException("Shizuku shell failed ($exitCode): ${stderr.trim()}")
        }
        return stdout.trim()
    }
}
