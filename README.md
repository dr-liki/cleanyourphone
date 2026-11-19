<div align="center">

# 📱 CleanYourPhone

**Modern, rootless Android debloater for phones, tablets, and Android/Google TVs.**  
*Debloat directly on-device with Shizuku, or wirelessly across your local network via Wireless ADB.*

[![Release](https://img.shields.io/badge/Release-v3.2.0-0284C7?style=flat-square&logo=github)](https://github.com/dr-liki/cleanyourphone/releases/tag/v3.2.0)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-059669?style=flat-square&logo=android)](#-prerequisites--compatibility)
[![Modes](https://img.shields.io/badge/Engines-Shizuku%20%7C%20Wireless%20ADB-4F46E5?style=flat-square)](#-two-operating-modes)
[![Catalogs](https://img.shields.io/badge/Catalogs-UAD--ng%20%26%20CleanYourTV-D97706?style=flat-square)](#-curated-debloat-catalogs)
[![UI](https://img.shields.io/badge/UI-Material%20Design%203-DB2777?style=flat-square)](#-key-features)
[![License](https://img.shields.io/badge/License-Apache--2.0-475569?style=flat-square)](LICENSE)

</div>

---

## ⚡ Overview

**CleanYourPhone** is a rootless Android debloater engineered with Jetpack Compose and Material Design 3. It safely disables unwanted OEM bloatware, carrier telemetry, and background services without modifying system partitions (`/system`) and without requiring a computer.

Everything is completely reversible with one-tap undo.

---

## 🔌 Two Operating Modes

| Mode | Target | Connection Method | Best For |
| :--- | :--- | :--- | :--- |
| **This Device** | Local phone or tablet | **Shizuku API** (`UID 2000`) | Instant on-device debloating without PC or cables. |
| **Other Phone / TV** | Remote phones & Android/Google TVs | **Wireless ADB** (Wi-Fi) | Debloating other devices over Wi-Fi via QR/pairing code or Port 5555. |

---

## ✨ Features (v1.0.0 – v3.2.0)

### 🛡️ Non-Destructive Package Management
- Disables packages safely for user 0 (`pm disable-user --user 0`).
- System partition files are never deleted or modified—zero bootloop risk.
- Core system packages (System UI, Settings, Phone, Permission Controller) are **hard-locked** to prevent accidental disabling.

### 🏷️ Intelligent App Names & Branded Icons *(v3.1 – v3.2)*
- **High-Accuracy Name Resolution**: Built-in dictionary covering 350+ OEM services and 150+ popular user-installed apps (VLC, MX Player, Kodi, Plex, Stremio, SmartTube, ReVanced, X-plore, Solid Explorer, Termux, F-Droid, Brave, Firefox, Steam Link, Moonlight, emulators, VPNs).
- **Smart Compound Word Formatting**: Automatically cleans package names (`rootexplorer` → *Root Explorer*, `videoplayer` → *Video Player*, `filemanager` → *File Manager*).
- **Uppercase Acronym Formatting**: Displays clean acronyms (`VLC`, `MX`, `TV`, `UI`, `FX`, `CX`, `VPN`, `DNS`, `AI`, `FTP`).
- **Branded Icons & Glyphs**: Signature vector glyphs and brand colors across media players, browsers, file managers, tools, and games.
- **Dynamic Multi-Line Badges**: Badges wrap to a clean second line when an app has more than 3 badges.

### 🎯 Filtering, Search & Sorting *(v2.9 – v3.2)*
- **Top Header Search**: Keyboard-activated full-width search by app label, package ID, or catalog description.
- **App Type Filter**: View `All Apps`, `System Apps`, `User Apps`, or `Disabled Apps`.
- **Removal Tier Filter**: Filter by `All Tiers`, `Brand Specific`, `Recommended`, `Advanced`, `Expert`, `Unsafe`, or `Unlisted`.
- **Persistent State**: Filter and sort selections stay preserved across tabs until changed.

### 📚 Curated Debloat Catalogs
- **Universal Android Debloater (UAD-ng)**: 1,800+ community-classified package definitions for smartphones and tablets.
- **CleanYourTV**: 2,400+ package definitions across 490+ Android TV and Google TV models.
- **Brand-Specific Detection**: Automatically flags bloatware tailored to connected device brand (Samsung, Xiaomi/HyperOS, Google Pixel, OnePlus, OPPO, Vivo, Motorola, Sony, TCL).

### 📊 Comprehensive Hardware & OS Diagnostics
- Displays manufacturer, brand, model, codename, board, Android version, SDK level, security patch, display resolution/density, CPU ABI architecture, and package distribution breakdown.

### 📜 Action Log & One-Tap Restoration
- Centralized ledger records every action with timestamp, package ID, and device identity.
- In Wireless ADB mode, actions are scoped specifically to the connected device.
- Filter actions by **All Actions** or **Restored**.
- **Undo single packages** or tap **Restore Selected** for batch re-enabling.
- Top header **Restore All** button dynamically hides when devices are disconnected.
- Saved device profiles can be tapped in disconnected state to view that device's action history directly.

### 📁 File-Based Backup & Restore (.json) *(v3.2)*
- Native integration with Android Storage Access Framework (SAF).
- **Export**: Save complete backup snapshots directly as `.json` files to Downloads, SD card, or Google Drive via `CreateDocument`.
- **Import**: Pick `.json` backup files directly via `OpenDocument` to safely restore saved device profiles, action history, and configuration.

### 📶 Wireless Connection & Profile Management *(v2.8 – v3.0)*
- **Continuous Keepalive**: 15-second background heartbeat ping loop prevents idle socket dropouts.
- **Saved Profiles**: Remembers paired devices with real-time **Available** network indicators and quick reconnect.
- **Discovered Devices**: mDNS network scanner with dedicated **Re-scan** and **Clear** controls.
- **Direct Port 5555**: One-tap connection for Android TV and Google TV.

---

## 🚦 Safety Tiers

| Tier | Badge | Description | Selection Policy |
| :--- | :---: | :--- | :--- |
| **Recommended** | 🟢 `Recommended` | Telemetry, ad trackers, and vendor bloatware safe for all users. | Selectable |
| **Brand Specific** | 🏷️ `Brand Specific` | Tailored bloatware specific to connected device manufacturer. | Selectable |
| **Advanced** | 🟡 `Advanced` | Non-essential apps (e.g., stock calculators, live wallpapers). | Selectable |
| **Expert** | 🟠 `Expert` | Deeper services; disabling may affect secondary features. | Deliberate selection |
| **Unsafe** | 🔴 `Unsafe` | Critical OS components and telephony. | **Hard-locked (Cannot disable)** |
| **Unlisted** | ⚪ `Unlisted` | Packages not cataloged in UAD-ng or CleanYourTV. | Selectable for manual inspection |

---

## 🚀 Quick Start Guide

### Option A: Local Device via Shizuku
1. Install and start [Shizuku](https://shizuku.rikka.app/) (via Wireless Debugging on Android 11+, or via PC on older versions).
2. Open CleanYourPhone and tap **This Device (Shizuku)**.
3. Grant permission when prompted.
4. Browse packages in **Packages (Tab 1)**, select bloatware, and tap **Disable Selected**.

### Option B: Remote Phone via Wireless ADB (Android 11+)
1. Connect both devices to the same Wi-Fi.
2. On target phone: Enable **Developer Options** → **Wireless debugging** → **Pair device with pairing code**.
3. In CleanYourPhone: Open **Other Phone / TV (Wireless ADB)**.
4. Tap pairing notification banner or pair dialog, enter 6-digit code, and tap **Complete Pairing**.
5. Tap **Connect Device** from saved profiles.

### Option C: Android TV / Google TV (Port 5555)
1. Connect TV and phone to the same Wi-Fi.
2. On TV: Enable **Developer Options** → **Network debugging** (note TV's IP address).
3. In CleanYourPhone: Tap **Pair / Connect** → **Direct (5555)** → enter IP and connect.
4. Accept the debugging prompt on your TV screen.

---

## 🛠️ Tech Stack

- **Language**: 100% Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **Local Privileges**: Shizuku Binder Privileged API (`UID 2000`)
- **Remote Privileges**: DADb (Kotlin ADB client) over TCP & TLS 1.3
- **Crypto**: BouncyCastle (SPAKE2 + TLS 1.3 mutual authentication)
- **Scanning**: ZXing QR code camera scanner + Android NsdManager (mDNS)
- **Architecture**: MVVM with Coroutines & StateFlow

---

## 🏗️ Build & Release

```bash
git clone https://github.com/dr-liki/cleanyourphone.git
cd cleanyourphone
./gradlew assembleRelease
```

Official releases are signed with permanent certificate:
- **SHA-256 Fingerprint**: `7C:7B:09:B5:2D:57:BF:82:56:BE:DD:68:71:9C:D6:F0:9C:88:B5:AA:C4:07:19:3E:52:1A:21:F3:A4:D3:12:76`
- **Verify**: `apksigner verify --print-certs CleanYourPhone-v3.2.0.apk`

---

## 📄 License

Licensed under the [Apache License, Version 2.0](LICENSE).
