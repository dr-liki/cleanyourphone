<div align="center">

# 📱 CleanYourPhone v2.1

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
- Uses Shizuku's privileged system binder to execute commands.
- Live status detection (Ready, Permission Needed, or Not Running).
- 1-click batch disabling for safe bloatware, carrier apps, and telemetry services.

### 2. 📶 Other Phone (Wireless ADB Mode)
- **Automatic mDNS / NSD Discovery**: Continuous discovery of other phones advertising `_adb-tls-connect._tcp` and `_adb._tcp` on local Wi-Fi, discovering their exact dynamic ports with zero guesswork.
- **Dynamic Port Subnet Scanner**: Auto-probes local Wi-Fi subnet across any target port (default 5555 or custom dynamic ports).
- **Pair Device with QR & PIN**: Seamless pairing dialog supporting AOSP wireless pairing QR codes and 6-digit Wi-Fi pairing codes.
- **Manual Connect**: Connect directly by IP and dynamic port without file permission issues.
- **Remote Debloater**: Queries packages installed on the remote phone, evaluates them against UAD-ng safety tiers, and disables them remotely over ADB.

---

## 🔄 Live UAD-ng Database Updates

- Sync the latest community-curated definitions directly from the official [Universal Android Debloater (UAD-ng)](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation) repository on GitHub with one tap from the menu.
- Persisted locally in internal app storage with fallback to bundled seed definitions.

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

## 🎨 Professional Material Design 3 UI/UX

- Custom-crafted adaptive vector icon with obsidian-sky aesthetics.
- Dynamic color theming with dark & light mode support.
- Fast, instant search and filter chips across removal tiers.
- Selection counter and floating action bar.
- Dedicated **About Dialog** with open-source credits and license details.

---

## 📥 Downloads

Download the latest signed release APK from [GitHub Releases](https://github.com/dr-liki/cleanyourphone/releases).
