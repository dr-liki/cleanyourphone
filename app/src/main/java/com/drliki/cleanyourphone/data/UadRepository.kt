package com.drliki.cleanyourphone.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.drliki.cleanyourphone.model.DevicePackage
import com.drliki.cleanyourphone.model.PROTECTED_PACKAGES
import com.drliki.cleanyourphone.model.PackageCategory
import com.drliki.cleanyourphone.model.RemovalTier
import com.drliki.cleanyourphone.model.UadEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class UadRepository(private val context: Context) {

    val settings = AppSettings(context)
    private val json = Json { ignoreUnknownKeys = true }
    private var catalog: Map<String, UadEntry> = emptyMap()

    companion object {
        const val UAD_NG_REMOTE_URL = AppSettings.DEFAULT_UAD_REPO_URL
        private const val CACHE_FILE_NAME = "uad_lists.json"
    }

    private val cacheFile: File
        get() = File(context.filesDir, CACHE_FILE_NAME)

    suspend fun loadCatalog(): Map<String, UadEntry> = withContext(Dispatchers.IO) {
        if (catalog.isNotEmpty()) return@withContext catalog

        // Check auto-update on launch if configured
        if (settings.autoUpdateOnLaunch) {
            val lastUpdate = settings.lastUpdateTime
            val oneDayMs = 24 * 60 * 60 * 1000L
            if (System.currentTimeMillis() - lastUpdate > oneDayMs) {
                runCatching { updateFromGitHub() }
            }
        }

        // Try local downloaded cache first
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                val raw = cacheFile.readText()
                catalog = json.decodeFromString<Map<String, UadEntry>>(raw)
                if (catalog.isNotEmpty()) return@withContext catalog
            } catch (_: Exception) { }
        }

        // Fallback to bundled asset
        try {
            val raw = context.assets.open("uad_lists_seed.json").bufferedReader().use { it.readText() }
            catalog = json.decodeFromString<Map<String, UadEntry>>(raw)
        } catch (_: Exception) {
            catalog = emptyMap()
        }
        catalog
    }

    suspend fun updateFromGitHub(customUrl: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val targetUrl = customUrl?.ifBlank { null } ?: settings.uadRepoUrl
            val url = URL(targetUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 15000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "CleanYourPhone-Android")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val downloaded = json.decodeFromString<Map<String, UadEntry>>(body)
            if (downloaded.isEmpty()) {
                throw IllegalStateException("Downloaded catalog is empty")
            }

            // Save to internal storage
            cacheFile.writeText(body)
            catalog = downloaded
            settings.lastUpdateTime = System.currentTimeMillis()
            downloaded.size
        }
    }

    fun getCatalogSize(): Int = catalog.size

    fun isUsingRemoteCache(): Boolean = cacheFile.exists() && cacheFile.length() > 0

    /** Scan packages installed on the local device. */
    suspend fun scanLocalPackages(): List<DevicePackage> = withContext(Dispatchers.IO) {
        val cat = loadCatalog()
        val pm = context.packageManager
        pm.getInstalledApplications(PackageManager.GET_META_DATA).map { appInfo ->
            toDevicePackage(appInfo.packageName, cat, pm, appInfo)
        }.sortedWith(compareBy<DevicePackage> { it.tier.ordinal }.thenBy { it.packageName })
    }

    /** Resolve a list of remote package names against the UAD-ng catalog. */
    suspend fun resolveRemotePackages(names: List<String>): List<DevicePackage> =
        withContext(Dispatchers.IO) {
            val cat = loadCatalog()
            names.map { pkgName ->
                val uad = cat[pkgName]
                val tier = uad?.let { RemovalTier.from(it.removal) } ?: RemovalTier.UNLISTED
                val category = uad?.let { PackageCategory.from(it.list) } ?: PackageCategory.UNLISTED
                val isProtected = pkgName in PROTECTED_PACKAGES || tier == RemovalTier.UNSAFE
                DevicePackage(
                    packageName = pkgName,
                    appLabel = pkgName.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                    tier = tier,
                    category = category,
                    description = uad?.description ?: "Not cataloged in UAD-ng database.",
                    isEnabled = true,
                    isSystemApp = true,
                    isSafeguardLocked = isProtected,
                    safeguardReason = safeguardReason(pkgName, tier)
                )
            }.sortedWith(compareBy<DevicePackage> { it.tier.ordinal }.thenBy { it.packageName })
        }

    private fun toDevicePackage(
        pkgName: String, cat: Map<String, UadEntry>,
        pm: PackageManager, appInfo: ApplicationInfo
    ): DevicePackage {
        val uad = cat[pkgName]
        val tier = uad?.let { RemovalTier.from(it.removal) } ?: RemovalTier.UNLISTED
        val category = uad?.let { PackageCategory.from(it.list) } ?: PackageCategory.UNLISTED
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isProtected = pkgName in PROTECTED_PACKAGES || tier == RemovalTier.UNSAFE
        return DevicePackage(
            packageName = pkgName,
            appLabel = pm.getApplicationLabel(appInfo).toString(),
            tier = tier,
            category = category,
            description = uad?.description ?: "Not cataloged in UAD-ng database.",
            isEnabled = appInfo.enabled,
            isSystemApp = isSystem,
            isSafeguardLocked = isProtected,
            safeguardReason = safeguardReason(pkgName, tier)
        )
    }

    private fun safeguardReason(pkgName: String, tier: RemovalTier): String? = when {
        pkgName in PROTECTED_PACKAGES -> "Protected core system package"
        tier == RemovalTier.UNSAFE -> "Classified Unsafe by UAD-ng — high bootloop risk"
        else -> null
    }
}
