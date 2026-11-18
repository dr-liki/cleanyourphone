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

class UadRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private var catalog: Map<String, UadEntry> = emptyMap()

    suspend fun loadCatalog(): Map<String, UadEntry> = withContext(Dispatchers.IO) {
        if (catalog.isNotEmpty()) return@withContext catalog
        val raw = context.assets.open("uad_lists_seed.json").bufferedReader().use { it.readText() }
        catalog = json.decodeFromString<Map<String, UadEntry>>(raw)
        catalog
    }

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
