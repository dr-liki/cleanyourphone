<div align="center">

# 📱 CleanYourPhone v2.4

**Debloat Android phones — directly on this device via Shizuku, or wirelessly connect to other phones over Wi-Fi.**

[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-38BDF8?style=flat-square)](#compatibility)
[![Privilege](https://img.shields.io/badge/Modes-Shizuku%20%7C%20Wireless%20ADB-10B981?style=flat-square)](#operation-modes)
[![Database](https://img.shields.io/badge/Database-UAD--ng%20uad__lists.json-818CF8?style=flat-square)](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation)
[![Pairing](https://img.shields.io/badge/Wireless%20Pairing-QR%20%26%20PIN-10B981?style=flat-square)](#wireless-pairing)
[![Design](https://img.shields.io/badge/UI-Material%20Design%203-F59E0B?style=flat-square)](#user-interface)
[![License](https://img.shields.io/badge/License-Apache--2.0-64748B?style=flat-square)](LICENSE)

</div>

---

**CleanYourPhone** is a modern, open-source Android debloater built from the ground up with **Material Design 3**. It offers a dual-engine architecture that lets you optimize your current device or connect wirelessly across your local network to debloat other phones.

---

## 🚀 Two Operation Modes

### 1. 🛡️ This Device (Shizuku Mode)
- Debloat your phone directly without root access or a PC.
- Uses Shizuku's privileged system binder to execute commands safely.
- Real-time application icon rendering with high-speed memory caching.
- Non-destructive package disabling via `pm disable-user --user 0`.

### 2. 📶 Other Phone (Wireless ADB Mode)
- **Automatic Pairing Discovery**: Listens for Android 11+ `_adb-tls-pairing._tcp` and `_adb-tls-connect._tcp` broadcasts. Automatically populates the target phone's IP and dynamic pairing port so you only have to enter the 6-digit PIN.
- **Remembered Device Profiles**: Saves connected phones, their last known IP and port, and tracks all packages modified per device profile for instant one-tap reconnection.
- **Dynamic Port Subnet Scanner**: Probes local Wi-Fi subnet across any target port (default 5555 or custom dynamic ports).
- **Pair Device with QR & PIN**: Seamless pairing dialog supporting AOSP wireless pairing QR codes and 6-digit Wi-Fi pairing codes.
- **Robust RSA Key Pair Generation**: Generates and manages internal 2048-bit RSA keys, resolving `android/adbkey: open failed: ENOENT` permanently.
- **Remote Debloater**: Queries packages installed on the remote phone, evaluates them against UAD-ng safety tiers, and disables them remotely over ADB.

---

## 🌟 What's New in v2.4

- **🎨 Material Theme Launcher Icon**: Added `<monochrome>` adaptive launcher icon vector supporting Android 13+ Material You dynamic color theming across all compliant launchers.
- **📱 Repositioned Selection & Status Controls**: Checkbox selection, safeguard lock icon, and disabled checkmark badge are now cleanly centered directly beneath the app icon on each package card.
- **🔄 Versatile App Sorting**: Introduced quick-sort dropdown options: Sort by Name (A→Z, Z→A), Package ID (A→Z), Safety Tier (Safest first), and Status (Disabled first, Enabled first).
- **📐 Uniform Wireless Controls & Grid Sizing**: Port input, Scan Subnet button, and Pair button now share identical 56dp height, 12dp rounded corners, and equal grid proportions. Manual Connect and Pairing dialog boxes now also feature equal-sized side-by-side Pairing Port and Connect Port inputs.
- **✨ Polished Material Design 3 UI/UX**: Unified component spacing, touch targets, elevation, and typography across This Device (Shizuku), Wireless ADB, Settings, and Action Log.

---

## 🌟 What's New in v2.3

- **🔍 Dual-Row Advanced App Filtering**: Filter packages simultaneously by app type (`All Apps`, `System Apps`, `User Apps`, `Disabled Apps`) and removal tier (`Recommended`, `Advanced`, `Expert`, `Unsafe`).
- **🖼️ Real Application Icons**: App icons loaded directly from `PackageManager` for local apps, with clean Material 3 avatars for remote packages.
- **📝 Complete Unrestricted Details**: Displays full app name, full package ID, and full description with no truncation or ellipses.
- **🏷️ Badges Below Description**: Status (`Disabled`), Tier (`Recommended`, etc.), Type (`System` / `User`), and Safeguard badges are positioned cleanly below the description line.
- **⚙️ Configurable Bloat Repositories**: Customize the raw JSON repository link for debloat lists, enable automatic background updates on app launch, or trigger manual updates with timestamp tracking.
- **📜 Full-Page Action Log with Search**: Moved from bottom sheet to a dedicated full-page screen with instantaneous search, active/restored filtering, and one-tap "Restore All".
- **📱 Remembered Device Profiles**: Separate profiles for each connected phone with connection history and disabled package tracking.
- **✨ Clean Header Branding**: Removed version numbers and subtitle noise from the header across all screens for a clean MD3 look.

---

## 🛡️ Built-in Safeguards & UAD-ng Classification

Every package is cross-referenced with the **Universal Android Debloater Next Generation (UAD-ng)** catalog:

| Safety Tier | Badge | Meaning & Behavior |
| :--- | :--- | :--- |
| **Recommended** | 🟢 `Recommended` | Safe to disable — OEM ad daemons, telemetry, carrier bloat, and easily replaceable apps. |
| **Advanced** | 🟡 `Advanced` | Stock apps or minor features. Review before disabling. |
| **Expert** | 🟠 `Expert` | Deeper system components. Requires conscious review. |
| **Unsafe** | 🔴 `Unsafe (Locked)` | Vital OS components or emergency services. **Hard-locked by CleanYourPhone.** |

- **Non-Destructive:** Packages are disabled with `pm disable-user --user 0`, never uninstalled from `/system`.
- **Protected Core:** Core packages (`android`, `com.android.systemui`, `com.android.phone`, `com.android.settings`, etc.) can never be disabled.
- **Reversible Action Log:** Every action is recorded with a single-tap **Undo** button.

---

## 📥 Downloads

Download the latest signed release APK from [GitHub Releases](https://github.com/dr-liki/cleanyourphone/releases).
