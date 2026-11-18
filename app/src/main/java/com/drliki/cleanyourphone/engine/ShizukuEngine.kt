package com.drliki.cleanyourphone.engine

import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

sealed class ShizukuStatus(val label: String) {
    object Ready : ShizukuStatus("Shizuku Active")
    object PermissionNeeded : ShizukuStatus("Grant Permission")
    object NotRunning : ShizukuStatus("Shizuku Not Running")
}

class ShizukuEngine {

    fun checkStatus(): ShizukuStatus = try {
        when {
            !Shizuku.pingBinder() -> ShizukuStatus.NotRunning
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> ShizukuStatus.Ready
            else -> ShizukuStatus.PermissionNeeded
        }
    } catch (_: Throwable) {
        ShizukuStatus.NotRunning
    }

    fun requestPermission(requestCode: Int = 1001) {
        try {
            if (Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED
            ) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (_: Throwable) { }
    }

    suspend fun shell(command: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }

            val process = method.invoke(
                null, arrayOf("sh", "-c", command), null, null
            ) as Process

            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exit = process.waitFor()
            if (exit != 0 && stderr.isNotBlank()) {
                throw IllegalStateException("Shell error ($exit): ${stderr.trim()}")
            }
            stdout.trim()
        }
    }

    suspend fun disablePackage(pkg: String): Result<String> =
        shell("pm disable-user --user 0 $pkg")

    suspend fun enablePackage(pkg: String): Result<String> =
        shell("pm enable --user 0 $pkg")
}
