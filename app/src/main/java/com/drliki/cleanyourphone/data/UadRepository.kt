package com.drliki.cleanyourphone.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.drliki.cleanyourphone.model.CategorizedPackages
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
        const val CLEANYOURTV_REMOTE_URL = AppSettings.DEFAULT_TV_REPO_URL
        private const val CACHE_FILE_NAME = "uad_lists.json"
        private const val CACHE_TV_FILE_NAME = "cleanyourtv_lists.json"
    }

    private val cacheFile: File
        get() = File(context.filesDir, CACHE_FILE_NAME)

    private val cacheTvFile: File
        get() = File(context.filesDir, CACHE_TV_FILE_NAME)

    private var tvCatalog: Map<String, UadEntry> = emptyMap()

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

    suspend fun loadTvCatalog(): Map<String, UadEntry> = withContext(Dispatchers.IO) {
        if (tvCatalog.isNotEmpty()) return@withContext tvCatalog

        // Check auto-update on launch if configured
        if (settings.autoUpdateTvOnLaunch) {
            val lastUpdate = settings.lastTvUpdateTime
            val oneDayMs = 24 * 60 * 60 * 1000L
            if (System.currentTimeMillis() - lastUpdate > oneDayMs) {
                runCatching { updateTvFromGitHub() }
            }
        }

        // Try local downloaded cache first
        if (cacheTvFile.exists() && cacheTvFile.length() > 0) {
            try {
                val raw = cacheTvFile.readText()
                tvCatalog = json.decodeFromString<Map<String, UadEntry>>(raw)
                if (tvCatalog.isNotEmpty()) return@withContext tvCatalog
            } catch (_: Exception) { }
        }

        // Fallback to bundled asset
        try {
            val raw = context.assets.open("cleanyourtv_lists_seed.json").bufferedReader().use { it.readText() }
            tvCatalog = json.decodeFromString<Map<String, UadEntry>>(raw)
        } catch (_: Exception) {
            tvCatalog = emptyMap()
        }
        tvCatalog
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

    suspend fun updateTvFromGitHub(customUrl: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val targetUrl = customUrl?.ifBlank { null } ?: settings.tvRepoUrl
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
                throw IllegalStateException("Downloaded CleanYourTV catalog is empty")
            }

            // Save to internal storage
            cacheTvFile.writeText(body)
            tvCatalog = downloaded
            settings.lastTvUpdateTime = System.currentTimeMillis()
            downloaded.size
        }
    }

    fun getCatalogSize(): Int = catalog.size

    fun getTvCatalogSize(): Int = if (tvCatalog.isNotEmpty()) tvCatalog.size else 2427

    fun isUsingRemoteCache(): Boolean = cacheFile.exists() && cacheFile.length() > 0

    fun isUsingRemoteTvCache(): Boolean = cacheTvFile.exists() && cacheTvFile.length() > 0

    /** Scan packages installed on the local device. */
    suspend fun scanLocalPackages(): List<DevicePackage> = withContext(Dispatchers.IO) {
        val cat = loadCatalog()
        val pm = context.packageManager
        pm.getInstalledApplications(PackageManager.GET_META_DATA).map { appInfo ->
            toDevicePackage(appInfo.packageName, cat, pm, appInfo)
        }.sortedWith(compareBy<DevicePackage> { it.tier.ordinal }.thenBy { it.packageName })
    }

    /** Resolve categorized remote packages against UAD-ng or CleanYourTV catalog with real system/user/disabled status. */
    suspend fun resolveRemotePackages(categorized: CategorizedPackages, isTv: Boolean = false): List<DevicePackage> =
        withContext(Dispatchers.IO) {
            val tvCat = if (isTv) loadTvCatalog() else emptyMap()
            val phoneCat = loadCatalog()
            categorized.allPackages.map { pkgName ->
                val tvEntry = if (isTv) tvCat[pkgName] else null
                val phoneEntry = phoneCat[pkgName]
                val entry = tvEntry ?: phoneEntry
                val tier = entry?.let { RemovalTier.from(it.removal) } ?: RemovalTier.UNLISTED
                val category = entry?.let { PackageCategory.from(it.list) } ?: PackageCategory.UNLISTED
                val isProtected = pkgName in PROTECTED_PACKAGES || tier == RemovalTier.UNSAFE
                val description = tvEntry?.description ?: phoneEntry?.description
                    ?: if (isTv) "Not cataloged in CleanYourTV database." else "Not cataloged in UAD-ng database."

                val isSystem = pkgName in categorized.systemPackages || (pkgName !in categorized.userPackages)
                val isEnabled = pkgName !in categorized.disabledPackages

                DevicePackage(
                    packageName = pkgName,
                    appLabel = pkgName.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                    tier = tier,
                    category = category,
                    description = description,
                    isEnabled = isEnabled,
                    isSystemApp = isSystem,
                    isSafeguardLocked = isProtected,
                    safeguardReason = safeguardReason(pkgName, tier)
                )
            }.sortedWith(compareBy<DevicePackage> { it.tier.ordinal }.thenBy { it.packageName })
        }

    /** Resolve a list of remote package names against UAD-ng or CleanYourTV catalog. */
    suspend fun resolveRemotePackages(names: List<String>, isTv: Boolean = false): List<DevicePackage> =
        withContext(Dispatchers.IO) {
            val tvCat = if (isTv) loadTvCatalog() else emptyMap()
            val phoneCat = loadCatalog()
            names.map { pkgName ->
                val tvEntry = if (isTv) tvCat[pkgName] else null
                val phoneEntry = phoneCat[pkgName]
                val entry = tvEntry ?: phoneEntry
                val tier = entry?.let { RemovalTier.from(it.removal) } ?: RemovalTier.UNLISTED
                val category = entry?.let { PackageCategory.from(it.list) } ?: PackageCategory.UNLISTED
                val isProtected = pkgName in PROTECTED_PACKAGES || tier == RemovalTier.UNSAFE
                val description = tvEntry?.description ?: phoneEntry?.description
                    ?: if (isTv) "Not cataloged in CleanYourTV database." else "Not cataloged in UAD-ng database."
                DevicePackage(
                    packageName = pkgName,
                    appLabel = pkgName.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                    tier = tier,
                    category = category,
                    description = description,
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
