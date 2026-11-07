package com.drliki.cleanyourphone.engine

import com.drliki.cleanyourphone.model.PhonePackageItem
import com.drliki.cleanyourphone.model.RemovalTier

/**
 * TVSlim-style non-bypassable safeguard engine.
 * Refuses to disable any package in the UAD-ng `Unsafe` tier, core Android boot packages,
 * or the currently active Home launcher / Input Method (keyboard).
 */
object SafeguardValidator {

    val PROTECTED_CORE_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.phone",
        "com.android.settings",
        "com.android.providers.settings",
        "com.android.providers.telephony",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.shell",
        "com.android.nfc",
        "moe.shizuku.privileged.api",
        "com.drliki.cleanyourphone"
    )

    fun evaluateSafeguard(
        pkgName: String,
        tier: RemovalTier,
        activeHomePackage: String?,
        activeImePackage: String?
    ): Pair<Boolean, String?> {
        if (pkgName in PROTECTED_CORE_PACKAGES) {
            return true to "Protected core Android / privilege package — never disable"
        }
        if (tier == RemovalTier.UNSAFE) {
            return true to "Classified as Unsafe by UAD-ng — high bootloop risk"
        }
        if (activeHomePackage != null && pkgName == activeHomePackage) {
            return true to "Currently active default Home screen launcher — install another launcher first"
        }
        if (activeImePackage != null && pkgName == activeImePackage) {
            return true to "Currently active default on-screen keyboard — enable another keyboard first"
        }
        return false to null
    }

    fun canUninstallUserApp(item: PhonePackageItem): Boolean {
        return !item.isSystemApp && !item.isSafeguardLocked
    }
}
