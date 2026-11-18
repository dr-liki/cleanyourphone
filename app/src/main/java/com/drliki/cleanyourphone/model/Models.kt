package com.drliki.cleanyourphone.model

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ── UAD-ng raw entry from uad_lists.json ────────────────────────────────────
@Serializable
data class UadEntry(
    val list: String = "",
    val description: String = "",
    val dependencies: List<String> = emptyList(),
    val neededBy: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val removal: String = "Unlisted"
)

// ── Removal tier from UAD-ng ─────────────────────────────────────────────────
enum class RemovalTier(val label: String, val colorValue: Long) {
    RECOMMENDED("Recommended", 0xFF10B981),
    ADVANCED("Advanced", 0xFFF59E0B),
    EXPERT("Expert", 0xFFF97316),
    UNSAFE("Unsafe", 0xFFEF4444),
    UNLISTED("Unlisted", 0xFF94A3B8);

    companion object {
        fun from(s: String): RemovalTier = when (s.lowercase().trim()) {
            "recommended" -> RECOMMENDED
            "advanced" -> ADVANCED
            "expert" -> EXPERT
            "unsafe" -> UNSAFE
            else -> UNLISTED
        }
    }
}

// ── Package list category from UAD-ng ────────────────────────────────────────
enum class PackageCategory(val label: String) {
    ALL("All"), OEM("OEM"), CARRIER("Carrier"), GOOGLE("Google"),
    AOSP("AOSP"), MISC("Misc"), UNLISTED("Unlisted");

    companion object {
        fun from(s: String): PackageCategory = when (s.lowercase().trim()) {
            "oem" -> OEM; "carrier" -> CARRIER; "google" -> GOOGLE
            "aosp" -> AOSP; "misc" -> MISC; else -> UNLISTED
        }
    }
}

// ── Package on a device (local or remote) ────────────────────────────────────
data class DevicePackage(
    val packageName: String,
    val appLabel: String,
    val tier: RemovalTier,
    val category: PackageCategory,
    val description: String,
    val isEnabled: Boolean,
    val isSystemApp: Boolean = false,
    val isSafeguardLocked: Boolean = false,
    val safeguardReason: String? = null
)

// ── Discovered remote device on the network ──────────────────────────────────
data class DiscoveredDevice(
    val host: String,
    val port: Int,
    val name: String = "Android Device",
    val model: String = ""
)

// ── Reversible action log entry ──────────────────────────────────────────────
data class ActionLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()),
    val packageName: String,
    val appLabel: String,
    val action: String,
    val undoCommand: String,
    val deviceName: String = "Local Device",
    val isUndone: Boolean = false
)

// ── Core protected packages that must never be disabled ──────────────────────
val PROTECTED_PACKAGES = setOf(
    "android", "com.android.systemui", "com.android.phone",
    "com.android.settings", "com.android.providers.settings",
    "com.android.providers.telephony", "com.android.permissioncontroller",
    "com.google.android.permissioncontroller", "com.android.packageinstaller",
    "com.google.android.packageinstaller", "com.android.shell",
    "moe.shizuku.privileged.api", "com.drliki.cleanyourphone"
)
