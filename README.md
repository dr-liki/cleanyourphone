<div align="center">

# 📱 CleanYourPhone v2.0

**Debloat Android phones — directly on this device via Shizuku, or wirelessly connect to other phones over Wi-Fi.**

[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-38BDF8?style=flat-square)](#compatibility)
[![Privilege](https://img.shields.io/badge/Modes-Shizuku%20%7C%20Wireless%20ADB-10B981?style=flat-square)](#operation-modes)
[![Database](https://img.shields.io/badge/Database-UAD--ng%20uad__lists.json-818CF8?style=flat-square)](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation)
[![No Root](https://img.shields.io/badge/Root-Not%20Needed-22C55E?style=flat-square)](#safeguards)
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
- **Local Wi-Fi Subnet Scanner**: Auto-probes your local network (e.g. `192.168.1.1`–`254` on port `5555`) to discover other Android phones with Wireless Debugging enabled.
- **Manual Connect**: Connect directly by IP and Port (supports custom or dynamic ports).
- **Remote Debloater**: Queries packages installed on the remote phone, evaluates them against UAD-ng safety tiers, and disables them remotely over ADB.
- Perfect for setting up family members' phones, secondary devices, or test units completely wirelessly!

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

- Dynamic color theming with dark & light mode support.
- Fast, instant search and filter chips across removal tiers.
- Selection counter and floating action bar.
- Pre-flight confirmation dialogs before executing batch actions.
- Animated transitions and modal bottom sheets for action history.

---

## 📥 Downloads

Download the latest signed release APK from [GitHub Releases](https://github.com/dr-liki/cleanyourphone/releases).
