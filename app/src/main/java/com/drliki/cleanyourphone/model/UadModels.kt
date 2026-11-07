package com.drliki.cleanyourphone.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Matches Universal Android Debloater Next Generation (UAD-ng) `uad_lists.json` entry schema.
 * Reference: https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation
 */
@Serializable
data class UadRawEntry(
    val list: String = "Pending",
    val description: String = "",
    val dependencies: List<String> = emptyList(),
    val neededBy: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val removal: String = "Pending"
)

enum class RemovalTier(val label: String, val summary: String) {
    RECOMMENDED("Recommended", "Safe to disable — bloatware, ads, telemetry, and carrier junk"),
    ADVANCED("Advanced", "Stock apps or minor features replaceable by better alternatives"),
    EXPERT("Expert", "Important system features — review side effects carefully before disabling"),
    UNSAFE("Unsafe", "Vital OS component — locked by CleanYourPhone safeguards to prevent bootloops"),
    UNLISTED("Unlisted", "Not yet documented in UAD-ng uad_lists.json");

    companion object {
        fun fromUadString(value: String): RemovalTier = when (value.lowercase()) {
            "recommended" -> RECOMMENDED
            "advanced" -> ADVANCED
            "expert" -> EXPERT
            "unsafe" -> UNSAFE
            else -> UNLISTED
        }
    }
}

enum class PackageListCategory(val label: String) {
    ALL("All"),
    OEM("OEM"),
    CARRIER("Carrier"),
    GOOGLE("Google"),
    AOSP("AOSP"),
    MISC("Misc"),
    UNLISTED("Unlisted");

    companion object {
        fun fromUadString(value: String): PackageListCategory = when (value.lowercase()) {
            "oem" -> OEM
            "carrier" -> CARRIER
            "google" -> GOOGLE
            "aosp" -> AOSP
            "misc" -> MISC
            else -> UNLISTED
        }
    }
}

data class PhonePackageItem(
    val packageName: String,
    val appLabel: String,
    val listCategory: PackageListCategory,
    val removalTier: RemovalTier,
    val description: String,
    val dependencies: List<String>,
    val neededBy: List<String>,
    val labels: List<String>,
    val isEnabled: Boolean,
    val isSystemApp: Boolean,
    val estimatedRamMb: Int,
    val estimatedStorageMb: Int,
    val isSafeguardLocked: Boolean = false,
    val safeguardReason: String? = null
)

data class DebloatProfile(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val selector: (PhonePackageItem) -> Boolean
)

@Serializable
data class ActionLogEntry(
    val id: String,
    val timestampIso: String,
    val packageName: String,
    val appLabel: String,
    val tier: String,
    val commandExecuted: String,
    val undoCommand: String,
    val undone: Boolean = false
)

enum class PrivilegeMode(val title: String, val detail: String) {
    SHIZUKU_READY("Shizuku Active (User 0)", "Connected to Shizuku privileged binder"),
    WIRELESS_ADB("Wireless ADB (127.0.0.1)", "Connected over local encrypted Wireless Debugging"),
    DEMO_PREVIEW("Preview Mode", "Grant Shizuku or connect Wireless ADB to execute live pm commands")
}
