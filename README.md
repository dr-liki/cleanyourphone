<div align="center">

# 📱 CleanYourPhone

**Modern, open-source Android debloater for smartphones, tablets, and Android/Google TVs.**  
*Debloat directly on-device with privileged Shizuku access, or wirelessly across your local network over Wireless ADB.*

[![Release](https://img.shields.io/badge/Release-v3.2.0-0284C7?style=flat-square&logo=github)](https://github.com/dr-liki/cleanyourphone/releases/tag/v3.2.0)
[![Android Compatibility](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-059669?style=flat-square&logo=android)](#compatibility)
[![Privilege Architecture](https://img.shields.io/badge/Engines-Shizuku%20%7C%20Wireless%20ADB-4F46E5?style=flat-square)](#-core-architecture--modes)
[![Debloat Databases](https://img.shields.io/badge/Catalogs-UAD--ng%20%26%20CleanYourTV-D97706?style=flat-square)](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation)
[![Security](https://img.shields.io/badge/Pairing-SPAKE2%20%26%20TLS%201.3-059669?style=flat-square)](#-wireless-connection-engine)
[![UI Framework](https://img.shields.io/badge/UI-Material%20Design%203-DB2777?style=flat-square)](#-modern-material-design-3-interface)
[![License](https://img.shields.io/badge/License-Apache--2.0-475569?style=flat-square)](LICENSE)

</div>

---

## 📑 Table of Contents

- [Overview](#-overview)
- [Core Architecture & Modes](#-core-architecture--modes)
- [Key Capabilities](#-key-capabilities)
- [Debloat Catalogs & Brand Intelligence](#-debloat-catalogs--brand-intelligence)
- [Safety Tiers & Safeguards](#-safety-tiers--safeguards)
- [User Guides](#-user-guides)
  - [1. Debloating This Device via Shizuku](#1-debloating-this-device-via-shizuku)
  - [2. Debloating Remote Phones via Wireless ADB (Android 11+)](#2-debloating-remote-phones-via-wireless-adb-android-11)
  - [3. Debloating Android TV / Google TV (Port 5555)](#3-debloating-android-tv--google-tv-port-5555)
  - [4. Search, Navigation & Filtering](#4-search-navigation--filtering)
  - [5. Action History & Package Restoration](#5-action-history--package-restoration)
  - [6. JSON Snapshot Backup & Migration](#6-json-snapshot-backup--migration)
- [Technical Architecture](#-technical-architecture)
- [Building from Source & Signing](#-building-from-source--signing)
- [Open Source Credits & Acknowledgements](#-open-source-credits--acknowledgements)
- [License](#-license)

---

## 📖 Overview

**CleanYourPhone** is a standalone, rootless Android debloater designed to eliminate OEM bloatware, carrier analytics, tracking services, and redundant background packages without requiring root access or a desktop computer.

Built with Kotlin and Jetpack Compose under Material Design 3, CleanYourPhone operates through two dedicated privilege engines:

1. **This Device (Shizuku Mode)**: Manages local packages using Shizuku's privileged system binder interface (`UID 2000` / `com.android.shell`).
2. **Other Phone / TV (Wireless ADB Mode)**: Discovers, securely pairs, connects, and debloats other Android devices and TVs across local Wi-Fi using native Android Wireless Debugging (AOSP SPAKE2 over TLS 1.3) or classic ADB port 5555.

---

## ⚙️ Core Architecture & Modes

```
                                  ┌─────────────────────────────┐
                                  │       CleanYourPhone        │
                                  │   (Jetpack Compose MD3)     │
                                  └──────────────┬──────────────┘
                                                 │
                         ┌───────────────────────┴───────────────────────┐
                         ▼                                               ▼
         ┌───────────────────────────────┐               ┌───────────────────────────────┐
         │     This Device (Shizuku)     │               │   Other Phone / TV (ADB)      │
         ├───────────────────────────────┤               ├───────────────────────────────┤
         │ • Privileged Binder (UID 2000)│               │ • AOSP SPAKE2 + TLS 1.3       │
         │ • Zero Cables, Zero PC Setup  │               │ • Classic TCP Port 5555 (TV)  │
         │ • Hardware & OS Diagnostics   │               │ • mDNS Service Discovery      │
         │ • UAD-ng Catalog Integration  │               │ • UAD-ng + CleanYourTV Lists  │
         │ • Sticky Binder Listeners     │               │ • 15s Keepalive Ping Loop     │
         └───────────────────────────────┘               └───────────────────────────────┘
```

### Shizuku Engine (Local Device)
- Interfaces directly with Android's system service binder as `com.android.shell`.
- Instant, non-blocking execution with no computer or cable required after initial Shizuku setup.
- Automatic binder lifecycle management with sticky dead/received listeners for uninterrupted operation.

### Wireless ADB Engine (Remote Device & TV)
- **Native Wireless Pairing (Android 11+)**: Implements AOSP SPAKE2 over TLS 1.3 with X.509 mutual certificate authentication for seamless Wi-Fi pairing.
- **Classic ADB (Port 5555)**: Direct one-tap connection for Android TV, Google TV, emulators, and legacy devices.
- **mDNS Auto-Discovery**: Automatically listens for `_adb-tls-connect._tcp` and `_adb-tls-pairing._tcp` network services.
- **Active Keepalive Heartbeat**: Runs a background 15-second heartbeat ping loop (`echo -n`) to prevent Android `adbd` idle socket dropouts, with automatic reconnect on network changes.

---

## ✨ Key Capabilities

### 🛡️ Non-Destructive Package Management
- Debloating disables packages for the current user (`pm disable-user --user 0`).
- System partition files on `/system`, `/vendor`, or `/product` are never modified or deleted, guaranteeing zero bootloop risk and complete reversibility.
- Essential system components (`android`, `com.android.systemui`, `com.android.phone`, `com.android.settings`, etc.) are hard-locked against accidental modification.

### 🏷️ Smart App Label Resolver
- In Wireless ADB mode, package labels are resolved through an intelligent multi-stage resolver:
  - Local package manager inspection (when available).
  - CleanYourTV and UAD-ng catalog descriptions.
  - Built-in 150+ OEM application dictionary covering Samsung, Xiaomi/HyperOS, Google Pixel, BBK (OnePlus, OPPO, Vivo, Realme), Sony, Motorola, and Android TV vendors.
  - Fallback camelCase / snake_case title formatter for clean presentation instead of raw reversed domain names.

### 🔍 Full-Width Keyboard-Activated Search
- Top header search action available across all screens.
- Clean Material 3 design with autofocus and automatic software keyboard activation upon tapping.
- Search instantly by application label, package identifier, or catalog description.

### 🎯 Multi-Facet Persistent Filtering
- Filter packages by **App Type**: `All`, `System`, `User`, `Disabled`.
- Filter by **Removal Tier**: `All Tiers`, `Brand Specific`, `Recommended`, `Advanced`, `Expert`, `Unsafe`, `Unlisted`.
- Quick **Sorting**: Name (A→Z / Z→A), Package ID, Safety Tier, Status.
- Filter and sorting states persist across navigation tabs and configuration changes.

### 📊 Deep System & Hardware Diagnostics
- **Tab 1** in both modes displays comprehensive hardware and OS specifications:
  - Manufacturer, brand, model, codename, board, hardware platform.
  - Android version, SDK API level, security patch level, build fingerprint.
  - Display resolution, density (`dpi`), CPU ABI architectures.
  - Real-time package distribution statistics (Total, System, User, Disabled, Recommended, Brand-Specific).

### 📜 Device-Isolated Action History
- Centralized action ledger records every disabled package with timestamp, package ID, and device identity.
- When inside Wireless ADB mode for a connected device, the Action Log is strictly isolated to that specific device.
- Streamlined filter view toggles between **All Actions** and **Restored**.
- Single-tap individual undo or batch package restoration.
- Tapping any saved device profile in the disconnected state opens that device's historical Action Log directly.

### 💾 Snapshot Backup & Migration
- Complete JSON snapshot manager accessible from the top menu.
- Export and import remembered device profiles, action log entries, and repository configurations via clipboard or Android share sheet.
- Safe, non-destructive merging of imported data.

---

## 📚 Debloat Catalogs & Brand Intelligence

CleanYourPhone integrates two community-maintained debloat catalogs:

| Catalog | Primary Scope | Package Entries | Upstream Repository |
| :--- | :--- | :--- | :--- |
| **UAD-ng** | Android Phones & Tablets | 1,800+ packages | [Universal-Debloater-Alliance/universal-android-debloater-next-generation](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation) |
| **CleanYourTV** | Android TV & Google TV | 2,427+ packages (492 models) | [gathin23/cleanyourtv](https://github.com/gathin23/cleanyourtv) |

### Automatic Brand-Specific Detection
When connected, CleanYourPhone reads target device build properties (`ro.product.manufacturer`, `ro.product.brand`, `ro.product.model`) and automatically flags manufacturer-specific bloatware:

- **Samsung**: Knox telemetry, Bixby daemons, AR Zone, partner apps.
- **Xiaomi / Redmi / POCO**: HyperOS/MIUI analytics, MSA ad services, Mi Cloud, Joyose.
- **Google Pixel**: Ambient Services, VR core, secondary analytics.
- **OnePlus / OPPO / Realme**: HeyTap services, HeyMelody, carrier packages.
- **Vivo / iQOO**: Jovi assistant, vivo push services, analytics.
- **Motorola**: Moto care analytics, partner preloads.
- **Sony**: Xperia diagnostics, Cinema Pro telemetry.
- **Android TV / Google TV**: Sponsored launcher telemetry, OEM channel overlays, TV brand analytics.

Brand-specific apps display a clean **Brand Specific** badge and can be isolated with one tap using the **Brand Specific** filter chip.

---

## 🛡️ Safety Tiers & Safeguards

| Safety Tier | Badge | Classification | Safeguard Policy |
| :--- | :---: | :--- | :--- |
| **Recommended** | 🟢 `Recommended` | Verified safe to disable for all users. Bloatware, analytics, telemetry, ad daemons. | Enabled for selection |
| **Advanced** | 🟡 `Advanced` | Stock applications or non-essential features (e.g., calendar, calculator, live wallpapers). | Enabled for selection |
| **Expert** | 🟠 `Expert` | Deeper system services. Disabling may impact specific secondary features. | Requires deliberate selection |
| **Unsafe** | 🔴 `Unsafe` | Critical OS components, telephony, emergency dialer, core system framework. | **Hard-locked. Cannot be selected or disabled.** |
| **Unlisted** | ⚪ `Unlisted` | Packages not cataloged in UAD-ng or CleanYourTV. | Filterable for manual inspection |

---

## 🚀 User Guides

### 1. Debloating This Device via Shizuku

1. **Install Shizuku**: Download [Shizuku from GitHub](https://github.com/RikkaApps/Shizuku/releases) or Google Play.
2. **Start Shizuku**:
   - **Android 11+**: Pair via *Wireless Debugging* inside Shizuku, then tap **Start**.
   - **Rooted**: Tap **Start** under Root.
   - **Android 8.0 - 10**: Connect to PC once and execute `adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh`.
3. **Open CleanYourPhone**: Select **This Device (Shizuku)** and grant permission when prompted.
4. **Debloat**:
   - **Tab 1 (Status)**: Inspect device specifications, display info, and package distribution.
   - **Tab 2 (Packages)**: Browse apps, filter by **Recommended** or **Brand Specific**, select targets, and tap **Disable Selected**.
   - **Tab 3 (Action Log)**: Review disabled packages and restore any app with one tap.

---

### 2. Debloating Remote Phones via Wireless ADB (Android 11+)

1. **Connect to Same Wi-Fi**: Connect both devices to the same local network.
2. **Enable Wireless Debugging on Target Device**:
   - Navigate to `Settings` → `About phone` → tap `Build number` 7 times.
   - Navigate to `Settings` → `System` → `Developer options` → enable **Wireless debugging**.
   - Tap **Pair device with pairing code**. Note the 6-digit code and pairing IP/port.
3. **Pair in CleanYourPhone**:
   - Select **Other Phone / TV (Wireless ADB)** on the host device.
   - CleanYourPhone detects the pairing broadcast and displays a pairing banner (with device name and network address).
   - Tap **Pair**, enter the 6-digit code, and tap **Complete Pairing**.
   - Once paired, tap **Connect Device** from the saved profiles list.
4. **Manage Packages**: Switch to **Packages** (Tab 1) to inspect and debloat the remote device.

---

### 3. Debloating Android TV / Google TV (Port 5555)

1. **Connect TV & Phone to Same Wi-Fi**.
2. **Enable Network Debugging on TV**:
   - Open TV `Settings` → `System` → `About` → tap `Android TV OS build` 7 times.
   - Navigate to `Settings` → `System` → `Developer options` → enable **Network debugging** (or **USB debugging**).
   - Note the TV's IP address (e.g., `192.168.1.120`).
3. **Connect in CleanYourPhone**:
   - Open **Other Phone / TV (Wireless ADB)** → tap **Pair / Connect** → select the **Direct (5555)** tab.
   - Enter the TV IP and port `5555`, then tap **Connect**.
   - On the TV screen, accept the *"Allow Network/USB debugging?"* dialog.
4. **Debloat with CleanYourTV**:
   - CleanYourPhone automatically detects the TV platform and activates the **CleanYourTV** catalog with 2,427+ TV debloat definitions.

---

### 4. Search, Navigation & Filtering

- **Header Search**: Tap the **Search (🔍)** icon in the top header. The search bar expands across the header, focuses immediately, and opens the software keyboard.
- **Bottom Navigation**:
  - **Shizuku Mode**: `Status` (Tab 0) → `Packages` (Tab 1) → `Action Log` (Tab 2).
  - **Wireless Mode (Connected)**: `Device` (Tab 0) → `Packages` (Tab 1) → `Action Log` (Tab 2).
- **Network Scan & Profiles**:
  - Use **Re-scan** to refresh local network mDNS discovery, or **Clear** to wipe discovered services.
  - Saved device profiles display an **Available** status badge when live on the network.
  - Delete saved profiles with confirmation or tap the profile card to open its historical Action Log.

---

### 5. Action History & Package Restoration

- **Single App Restore**: On the **Action Log** tab, locate the entry and tap **Undo**. The package is re-enabled immediately.
- **Batch Restoration**: Select multiple entries in the Action Log and tap **Restore Selected** from the bottom bar.
- **Re-enable from Packages**: Filter the packages list by `Disabled`, select any package, and tap **Re-enable**.

---

### 6. File-Based Backup & Migration (.json)

- **Create Backup File**:
  - Open `Menu (⋮)` → **Backup & Restore**.
  - Inspect the snapshot summary (saved profiles, action logs, catalog repository URLs).
  - Tap **Save Backup File (.json)** to store directly to your device downloads or cloud drive via the system Storage Access Framework (SAF).
- **Restore from Backup File**:
  - Open `Menu (⋮)` → **Backup & Restore** → **Import File**.
  - Tap **Select Backup File (.json)** and pick your backup from storage.
  - Device profiles and action history logs are validated and safely merged.

---

## 🛠️ Technical Architecture

CleanYourPhone is built using modern Android architecture principles:

- **Language & Runtime**: 100% Kotlin with Coroutines and StateFlow.
- **UI Toolkit**: Jetpack Compose with Material Design 3, dynamic theme colors, and adaptive monochrome launcher icon.
- **Navigation**: Jetpack Navigation Compose with integrated bottom navigation bar.
- **Privilege Engines**:
  - *Local*: Shizuku API (UID 2000 shell binder access).
  - *Remote*: DADb (Kotlin ADB client) over TCP and TLS 1.3.
- **Cryptography**: BouncyCastle implementation of AOSP SPAKE2 and TLS 1.3 mutual certificate authentication.
- **Barcode & QR Scanner**: Embedded ZXing scanner for camera-based pairing.
- **State & Persistence**: Jetpack DataStore / SharedPreferences with JSON snapshot serialization.

---

## 🏗️ Building from Source & Signing

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or newer
- JDK 17
- Android SDK Platform API 34 (UpsideDownCake)

### Build Release APK
```bash
git clone https://github.com/dr-liki/cleanyourphone.git
cd cleanyourphone
./gradlew assembleRelease
```

### Official Release Signing Certificate
All official release builds are signed with a permanent release keystore:
- **Keystore Alias**: `cleanyourphone`
- **Certificate SHA-256 Fingerprint**:
  ```text
  7C:7B:09:B5:2D:57:BF:82:56:BE:DD:68:71:9C:D6:F0:9C:88:B5:AA:C4:07:19:3E:52:1A:21:F3:A4:D3:12:76
  ```
- **Verify APK Signature**:
  ```bash
  apksigner verify --print-certs CleanYourPhone-v3.2.0.apk
  ```

---

## 🤝 Open Source Credits & Acknowledgements

CleanYourPhone is made possible thanks to these open-source projects:

- **[Universal Android Debloater Next Generation (UAD-ng)](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation)**: Curated package classifications, safety tiers, and debloat definitions for phones and tablets.
- **[CleanYourTV by gathin23](https://github.com/gathin23/cleanyourtv)**: Curated package definitions and debloat lists for Android TV and Google TV platforms.
- **[Shizuku by Rikka](https://shizuku.rikka.app/)**: On-device privileged Android system service binder access.
- **[DADb by mobile.dev](https://github.com/mobile-dev-inc/DADb)**: Lightweight Kotlin ADB protocol implementation.
- **[Bouncy Castle](https://www.bouncycastle.org/)**: Cryptographic provider enabling AOSP SPAKE2 and TLS 1.3 key exchange.
- **[ZXing ("Zebra Crossing")](https://github.com/zxing/zxing)**: Multi-format barcode and QR code image processing.

---

## 📄 License

```text
Copyright (C) 2026 CleanYourPhone Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
