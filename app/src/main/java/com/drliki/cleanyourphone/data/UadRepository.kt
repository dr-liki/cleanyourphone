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

        private val KNOWN_APP_LABELS = mapOf(
            // Samsung
            "com.sec.android.app.sbrowser" to "Samsung Internet",
            "com.sec.android.app.popupcalculator" to "Samsung Calculator",
            "com.sec.android.app.myfiles" to "Samsung My Files",
            "com.sec.android.app.voicenote" to "Voice Recorder",
            "com.sec.android.app.camera" to "Samsung Camera",
            "com.sec.android.gallery3d" to "Samsung Gallery",
            "com.sec.android.app.clockpackage" to "Samsung Clock",
            "com.samsung.android.app.notes" to "Samsung Notes",
            "com.samsung.android.calendar" to "Samsung Calendar",
            "com.samsung.android.messaging" to "Samsung Messages",
            "com.samsung.android.dialer" to "Samsung Phone",
            "com.samsung.android.contacts" to "Samsung Contacts",
            "com.samsung.android.voc" to "Samsung Members",
            "com.samsung.android.app.spage" to "Samsung Free",
            "com.samsung.android.game.gos" to "Game Optimizing Service",
            "com.samsung.android.game.gamehome" to "Gaming Hub",
            "com.samsung.android.bixby.agent" to "Bixby Voice",
            "com.samsung.android.bixby.wakeup" to "Bixby Wakeup",
            "com.samsung.android.bixby.service" to "Bixby Service",
            "com.samsung.android.visionintelligence" to "Bixby Vision",
            "com.samsung.android.smartswitchassistant" to "Smart Switch",
            "com.samsung.android.oneconnect" to "SmartThings",
            "com.samsung.android.rubin.app" to "Customization Service",
            "com.samsung.android.kidsinstaller" to "Samsung Kids",
            "com.samsung.android.app.sharelive" to "Quick Share",
            "com.samsung.android.ardrawing" to "AR Doodle",
            "com.samsung.android.aremoji" to "AR Emoji",
            "com.samsung.android.arzone" to "AR Zone",
            "com.samsung.android.spay" to "Samsung Wallet",
            "com.samsung.android.samsungpass" to "Samsung Pass",
            "com.samsung.android.app.tips" to "Tips",
            "com.samsung.android.weather" to "Samsung Weather",
            "com.samsung.android.mdecservice" to "Call & Text on other devices",

            // Xiaomi / MIUI / HyperOS
            "com.miui.analytics" to "Xiaomi Analytics",
            "com.miui.msa.global" to "MIUI System Ads (MSA)",
            "com.miui.cleanmaster" to "Cleaner",
            "com.miui.cloudservice" to "Mi Cloud",
            "com.miui.yellowpage" to "Yellow Pages",
            "com.miui.videoplayer" to "Mi Video",
            "com.miui.player" to "Mi Music",
            "com.miui.weather2" to "Mi Weather",
            "com.miui.compass" to "Compass",
            "com.miui.calculator" to "Mi Calculator",
            "com.miui.notes" to "Mi Notes",
            "com.miui.gallery" to "Mi Gallery",
            "com.miui.bugreport" to "Feedback",
            "com.xiaomi.joyose" to "Joyose",
            "com.xiaomi.midrop" to "ShareMe",
            "com.xiaomi.mipicks" to "GetApps",
            "com.xiaomi.payment" to "Mi Pay",
            "com.xiaomi.scanner" to "Scanner",

            // Google
            "com.google.android.gms" to "Google Play Services",
            "com.google.android.gsf" to "Google Services Framework",
            "com.android.vending" to "Google Play Store",
            "com.google.android.youtube" to "YouTube",
            "com.google.android.apps.youtube.music" to "YouTube Music",
            "com.google.android.videos" to "Google TV",
            "com.google.android.apps.photos" to "Google Photos",
            "com.google.android.apps.docs" to "Google Drive",
            "com.google.android.apps.maps" to "Google Maps",
            "com.google.android.gm" to "Gmail",
            "com.google.android.googlequicksearchbox" to "Google App",
            "com.google.android.inputmethod.latin" to "Gboard",
            "com.android.chrome" to "Google Chrome",
            "com.google.android.apps.messaging" to "Google Messages",
            "com.google.android.dialer" to "Google Phone",
            "com.google.android.contacts" to "Google Contacts",
            "com.google.android.calendar" to "Google Calendar",
            "com.google.android.keep" to "Google Keep",
            "com.google.android.apps.tachyon" to "Google Meet",
            "com.google.android.apps.wellbeing" to "Digital Wellbeing",

            // Meta
            "com.facebook.katana" to "Facebook",
            "com.facebook.orca" to "Messenger",
            "com.facebook.services" to "Meta Services",
            "com.facebook.appmanager" to "Meta App Manager",
            "com.facebook.system" to "Meta App Installer",
            "com.instagram.android" to "Instagram",
            "com.whatsapp" to "WhatsApp",

            // Microsoft
            "com.microsoft.office.officehubrow" to "Microsoft 365 (Office)",
            "com.microsoft.office.outlook" to "Microsoft Outlook",
            "com.microsoft.skydrive" to "Microsoft OneDrive",
            "com.microsoft.appmanager" to "Link to Windows",
            "com.linkedin.android" to "LinkedIn",

            // Streaming / Media / TV
            "com.netflix.ninja" to "Netflix",
            "com.netflix.mediaclient" to "Netflix",
            "com.amazon.amazonvideo.livingroom" to "Prime Video",
            "com.amazon.mp3" to "Amazon Music",
            "com.amazon.mShop.android.shopping" to "Amazon Shopping",
            "com.spotify.music" to "Spotify",
            "com.spotify.tv.android" to "Spotify on TV",
            "com.disney.disneyplus" to "Disney+",
            "com.hulu.plus" to "Hulu",
            "com.google.android.tv.launcher" to "Android TV Home",
            "com.google.android.tungsten.setupwraith" to "Android TV Setup",

            // Other OEMs
            "com.coloros.athena" to "Athena Engine",
            "com.oplus.cosa" to "OPPO System Assistant",
            "com.heytap.mcs" to "HeyTap Cloud",
            "com.vivo.browser" to "Vivo Browser",
            "com.vivo.easyshare" to "EasyShare",
            "com.motorola.ccc.notification" to "Motorola Notifications",
            "com.sonyericsson.organizer" to "Sony Organizer"
        )
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

    // ── Brand & Model Tailored Recommendation Matcher ───────────────────────
    data class BrandMatcher(
        val displayName: String,
        val keywords: List<String>,
        val packagePrefixes: List<String>
    )

    private val brandMatchers = listOf(
        BrandMatcher("Samsung", listOf("samsung", "sec"), listOf("com.samsung.", "com.sec.", "com.osp.")),
        BrandMatcher("Xiaomi", listOf("xiaomi", "redmi", "poco", "miui", "hyperos"), listOf("com.miui.", "com.xiaomi.", "com.mi.")),
        BrandMatcher("Google Pixel", listOf("google", "pixel"), listOf("com.google.android.apps.")),
        BrandMatcher("OnePlus", listOf("oneplus", "one plus"), listOf("com.oneplus.", "net.oneplus.")),
        BrandMatcher("OPPO", listOf("oppo", "coloros", "heytap"), listOf("com.oppo.", "com.coloros.", "com.heytap.")),
        BrandMatcher("Realme", listOf("realme"), listOf("com.realme.", "com.oppo.", "com.coloros.")),
        BrandMatcher("Vivo", listOf("vivo", "iqoo", "funtouch", "origin"), listOf("com.vivo.", "com.iqoo.", "com.bbk.")),
        BrandMatcher("Motorola", listOf("motorola", "moto", "lenovo"), listOf("com.motorola.", "com.lenovo.")),
        BrandMatcher("Sony", listOf("sony", "xperia", "bravia"), listOf("com.sony.", "com.sonymobile.")),
        BrandMatcher("Huawei/Honor", listOf("huawei", "honor", "emui", "magicui"), listOf("com.huawei.", "com.honor.", "com.hihonor.")),
        BrandMatcher("Transsion (Tecno/Infinix)", listOf("infinix", "tecno", "itel", "transsion", "xos", "hios"), listOf("com.transsion.", "com.infinix.", "com.tecno.")),
        BrandMatcher("Amazon", listOf("amazon", "fire"), listOf("com.amazon.")),
        BrandMatcher("TCL", listOf("tcl"), listOf("com.tcl.")),
        BrandMatcher("ASUS", listOf("asus", "rog", "zenfone"), listOf("com.asus.")),
        BrandMatcher("LG", listOf("lg", "lge"), listOf("com.lge.")),
        BrandMatcher("NVIDIA", listOf("nvidia", "shield"), listOf("com.nvidia."))
    )

    fun findBrandMatcher(brand: String?, manufacturer: String?, model: String?): BrandMatcher? {
        val combined = listOfNotNull(brand, manufacturer, model).joinToString(" ").lowercase()
        if (combined.isBlank()) return null
        return brandMatchers.firstOrNull { matcher ->
            matcher.keywords.any { k -> combined.contains(k) }
        }
    }

    private fun checkTailored(
        pkgName: String,
        entry: UadEntry?,
        matcher: BrandMatcher?,
        model: String?
    ): Pair<Boolean, String?> {
        if (matcher == null) return false to null
        val matchesPrefix = matcher.packagePrefixes.any { pkgName.startsWith(it) }
        val matchesDesc = entry?.description?.contains(matcher.displayName, ignoreCase = true) == true ||
            matcher.keywords.any { entry?.description?.contains(it, ignoreCase = true) == true }
        val matchesModel = !model.isNullOrBlank() && entry?.description?.contains(model, ignoreCase = true) == true

        if (matchesPrefix || matchesDesc || matchesModel) {
            val reason = if (matchesModel) "Specifically tailored for your $model"
                else "Tailored recommendation for ${matcher.displayName} devices"
            return true to reason
        }
        return false to null
    }

    /** Scan packages installed on the local device. */
    suspend fun scanLocalPackages(
        brand: String? = android.os.Build.BRAND,
        manufacturer: String? = android.os.Build.MANUFACTURER,
        model: String? = android.os.Build.MODEL
    ): List<DevicePackage> = withContext(Dispatchers.IO) {
        val cat = loadCatalog()
        val pm = context.packageManager
        val matcher = findBrandMatcher(brand, manufacturer, model)
        pm.getInstalledApplications(PackageManager.GET_META_DATA).map { appInfo ->
            toDevicePackage(appInfo.packageName, cat, pm, appInfo, matcher, model)
        }.sortedWith(
            compareBy<DevicePackage> { if (it.isTailored) 0 else 1 }
                .thenBy { it.tier.ordinal }
                .thenBy { it.packageName }
        )
    }

    /** Resolve categorized remote packages against UAD-ng or CleanYourTV catalog with real system/user/disabled status. */
    suspend fun resolveRemotePackages(
        categorized: CategorizedPackages,
        isTv: Boolean = false,
        brand: String? = null,
        manufacturer: String? = null,
        model: String? = null
    ): List<DevicePackage> =
        withContext(Dispatchers.IO) {
            val tvCat = if (isTv) loadTvCatalog() else emptyMap()
            val phoneCat = loadCatalog()
            val matcher = findBrandMatcher(brand, manufacturer, model)
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
                val (isTailored, tailoredReason) = checkTailored(pkgName, entry, matcher, model)

                val appLabel = resolveAppLabel(pkgName, entry, isTv)

                DevicePackage(
                    packageName = pkgName,
                    appLabel = appLabel,
                    tier = tier,
                    category = category,
                    description = description,
                    isEnabled = isEnabled,
                    isSystemApp = isSystem,
                    isSafeguardLocked = isProtected,
                    safeguardReason = safeguardReason(pkgName, tier),
                    isTailored = isTailored,
                    tailoredReason = tailoredReason
                )
            }.sortedWith(
                compareBy<DevicePackage> { if (it.isTailored) 0 else 1 }
                    .thenBy { it.tier.ordinal }
                    .thenBy { it.packageName }
            )
        }

    /** Resolve a list of remote package names against UAD-ng or CleanYourTV catalog. */
    suspend fun resolveRemotePackages(
        names: List<String>,
        isTv: Boolean = false,
        brand: String? = null,
        manufacturer: String? = null,
        model: String? = null
    ): List<DevicePackage> =
        withContext(Dispatchers.IO) {
            val tvCat = if (isTv) loadTvCatalog() else emptyMap()
            val phoneCat = loadCatalog()
            val matcher = findBrandMatcher(brand, manufacturer, model)
            names.map { pkgName ->
                val tvEntry = if (isTv) tvCat[pkgName] else null
                val phoneEntry = phoneCat[pkgName]
                val entry = tvEntry ?: phoneEntry
                val tier = entry?.let { RemovalTier.from(it.removal) } ?: RemovalTier.UNLISTED
                val category = entry?.let { PackageCategory.from(it.list) } ?: PackageCategory.UNLISTED
                val isProtected = pkgName in PROTECTED_PACKAGES || tier == RemovalTier.UNSAFE
                val description = tvEntry?.description ?: phoneEntry?.description
                    ?: if (isTv) "Not cataloged in CleanYourTV database." else "Not cataloged in UAD-ng database."
                val (isTailored, tailoredReason) = checkTailored(pkgName, entry, matcher, model)
                val appLabel = resolveAppLabel(pkgName, entry, isTv)
                DevicePackage(
                    packageName = pkgName,
                    appLabel = appLabel,
                    tier = tier,
                    category = category,
                    description = description,
                    isEnabled = true,
                    isSystemApp = true,
                    isSafeguardLocked = isProtected,
                    safeguardReason = safeguardReason(pkgName, tier),
                    isTailored = isTailored,
                    tailoredReason = tailoredReason
                )
            }.sortedWith(
                compareBy<DevicePackage> { if (it.isTailored) 0 else 1 }
                    .thenBy { it.tier.ordinal }
                    .thenBy { it.packageName }
            )
        }

    private fun toDevicePackage(
        pkgName: String, cat: Map<String, UadEntry>,
        pm: PackageManager, appInfo: ApplicationInfo,
        matcher: BrandMatcher? = null,
        model: String? = null
    ): DevicePackage {
        val uad = cat[pkgName]
        val tier = uad?.let { RemovalTier.from(it.removal) } ?: RemovalTier.UNLISTED
        val category = uad?.let { PackageCategory.from(it.list) } ?: PackageCategory.UNLISTED
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isProtected = pkgName in PROTECTED_PACKAGES || tier == RemovalTier.UNSAFE
        val (isTailored, tailoredReason) = checkTailored(pkgName, uad, matcher, model)
        return DevicePackage(
            packageName = pkgName,
            appLabel = pm.getApplicationLabel(appInfo).toString(),
            tier = tier,
            category = category,
            description = uad?.description ?: "Not cataloged in UAD-ng database.",
            isEnabled = appInfo.enabled,
            isSystemApp = isSystem,
            isSafeguardLocked = isProtected,
            safeguardReason = safeguardReason(pkgName, tier),
            isTailored = isTailored,
            tailoredReason = tailoredReason
        )
    }

    private fun safeguardReason(pkgName: String, tier: RemovalTier): String? = when {
        pkgName in PROTECTED_PACKAGES -> "Protected core system package"
        tier == RemovalTier.UNSAFE -> "Classified Unsafe by UAD-ng — high bootloop risk"
        else -> null
    }

    /** Resolve clean, human-readable application label for packages across local and remote devices */
    fun resolveAppLabel(pkgName: String, entry: UadEntry?, isTv: Boolean = false): String {
        // 1. Try local PackageManager if installed on host device
        val localLabel = runCatching {
            val appInfo = context.packageManager.getApplicationInfo(pkgName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        }.getOrNull()
        if (!localLabel.isNullOrBlank() && localLabel != pkgName) {
            return localLabel
        }

        // 2. Check well-known package dictionary
        KNOWN_APP_LABELS[pkgName]?.let { return it }

        // 3. Extract title from UAD-ng / CleanYourTV description if present
        entry?.description?.let { desc ->
            val clean = desc.trim()
            if (clean.isNotBlank() &&
                !clean.startsWith("Not cataloged", ignoreCase = true) &&
                !clean.startsWith("(CleanYourTV", ignoreCase = true)
            ) {
                val firstDelim = clean.indexOfAny(charArrayOf('.', ':', '—', '-'))
                if (firstDelim in 3..45) {
                    val candidate = clean.substring(0, firstDelim).trim()
                    if (!candidate.startsWith("This ", ignoreCase = true) &&
                        !candidate.startsWith("The ", ignoreCase = true) &&
                        !candidate.startsWith("A ", ignoreCase = true) &&
                        !candidate.startsWith("An ", ignoreCase = true) &&
                        !candidate.startsWith("Used ", ignoreCase = true) &&
                        !candidate.startsWith("Disabling ", ignoreCase = true) &&
                        !candidate.startsWith("Pre-installed ", ignoreCase = true) &&
                        !candidate.startsWith("Preinstalled ", ignoreCase = true) &&
                        !candidate.startsWith("System ", ignoreCase = true)
                    ) {
                        return candidate
                    }
                }
            }
        }

        // 4. Parse friendly title from package segments
        return formatFriendlyPackageName(pkgName)
    }

    private fun formatFriendlyPackageName(pkgName: String): String {
        val parts = pkgName.split('.').filter { it.isNotBlank() }
        val ignored = setOf("com", "org", "net", "android", "google", "app", "apps", "vendor", "service", "services", "system")
        val meaningful = parts.filter { it.lowercase() !in ignored }
        val target = meaningful.lastOrNull() ?: parts.lastOrNull() ?: pkgName
        // Split camelCase and underscores into title-cased words
        val words = target.replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace('_', ' ')
            .replace('-', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .map { it.replaceFirstChar { c -> c.uppercase() } }

        return if (words.isNotEmpty()) words.joinToString(" ") else target.replaceFirstChar { it.uppercase() }
    }
}
