<div align="center">

# 📱 CleanYourPhone

**Modern, open-source Android debloater for phones, tablets, and Android/Google TVs.**  
*Debloat directly on-device with privileged Shizuku access, or connect wirelessly to other phones and TVs over local Wi-Fi.*

[![Latest Release](https://img.shields.io/badge/Release-v2.9.0-38BDF8?style=flat-square&logo=github)](https://github.com/dr-liki/cleanyourphone/releases/tag/v2.9.0)
[![Android Compatibility](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-10B981?style=flat-square&logo=android)](#compatibility)
[![Privilege Architecture](https://img.shields.io/badge/Engines-Shizuku%20%7C%20Wireless%20ADB-818CF8?style=flat-square)](#core-architecture--modes)
[![Debloat Databases](https://img.shields.io/badge/Catalogs-UAD--ng%20%26%20CleanYourTV-F59E0B?style=flat-square)](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation)
[![Wireless Pairing](https://img.shields.io/badge/Wireless%20Pairing-SPAKE2%20%26%20TLS%201.3-10B981?style=flat-square)](#wireless-pairing--connection-engine)
[![Backup & Restore](https://img.shields.io/badge/Backup%20%26%20Restore-JSON%20Snapshot-8B5CF6?style=flat-square)](#backup--restore)
[![UI Framework](https://img.shields.io/badge/UI-Material%20Design%203-EC4899?style=flat-square)](#modern-material-design-3-ui)
[![License](https://img.shields.io/badge/License-Apache--2.0-64748B?style=flat-square)](LICENSE)

</div>

---

## 📑 Table of Contents

- [Overview](#-overview)
- [Core Architecture & Modes](#-core-architecture--modes)
- [Key Features](#-key-features)
- [Debloat Catalogs & Brand Tailoring](#-debloat-catalogs--brand-tailoring)
- [Safety Tiers & Non-Destructive Debloating](#-safety-tiers--non-destructive-debloating)
- [User Guides](#-user-guides)
  - [Mode 1: Debloating This Phone via Shizuku](#1-debloating-this-phone-via-shizuku)
  - [Mode 2: Debloating Another Phone Wirelessly (Android 11+)](#2-debloating-another-phone-wirelessly-android-11)
  - [Mode 3: Debloating Android TV / Google TV](#3-debloating-android-tv--google-tv)
  - [Managing, Searching & Filtering Apps](#4-managing-searching--filtering-apps)
  - [Restoring & Undoing Actions](#5-restoring--undoing-actions)
  - [Backup & Restore Snapshots](#6-backup--restore-snapshots)
- [Latest Release Highlights (v2.9.0)](#-latest-release-highlights-v290)
- [Technical Architecture](#-technical-architecture)
- [Building from Source & Signing](#-building-from-source--signing)
- [Open Source Credits & Acknowledgements](#-open-source-credits--acknowledgements)
- [License](#-license)

---

## 📖 Overview

**CleanYourPhone** is a rootless, open-source Android debloating suite built entirely in Kotlin with Jetpack Compose and **Material Design 3**. It solves the problem of pre-installed bloatware, carrier tracking services, OEM analytics daemons, and redundant background apps that consume battery, RAM, and storage.

CleanYourPhone requires **no root permissions** and **no desktop computer**. It functions as a standalone dual-mode manager:
1. **This Device (Shizuku Mode)**: Debloats the phone running the app using Shizuku's privileged system binder interface (`UID 2000`).
2. **Other Phone / TV (Wireless ADB Mode)**: Wirelessly discovers, pairs, and debloats other Android phones, tablets, and Android/Google TVs over the local Wi-Fi network using Android's native Wireless Debugging protocol (AOSP SPAKE2 over TLS 1.3 or classic ADB port 5555).

---

## ⚙️ Core Architecture & Modes

```
                                  ┌─────────────────────────────┐
                                  │       CleanYourPhone        │
                                  │   (Jetpack Compose MD3)     │
                                  └──────────────┬──────────────┘
                                                 │
                        ┌────────────────────────┴────────────────────────┐
                        ▼                                                 ▼
        ┌───────────────────────────────┐                 ┌───────────────────────────────┐
        │     This Device (Shizuku)     │                 │   Other Phone / TV (ADB)      │
        ├───────────────────────────────┤                 ├───────────────────────────────┤
        │ • Privileged Binder (UID 2000)│                 │ • AOSP SPAKE2 + TLS 1.3       │
        │ • Zero Cables, Zero PC        │                 │ • Plain TCP Port 5555         │
        │ • Local Hardware & OS Specs   │                 │ • mDNS / NSD Auto-Discovery   │
        │ • UAD-ng Catalog Integration  │                 │ • UAD-ng + CleanYourTV Lists  │
        │ • Asynchronous Process Streams│                 │ • 15s Keepalive Heartbeat     │
        └───────────────────────────────┘                 └───────────────────────────────┘
```

### 1. This Device Mode (Shizuku Engine)
- Leverages the [Shizuku API](https://shizuku.rikka.app/) to interact directly with Android's system service binder as `com.android.shell` (UID 2000).
- Provides instant, non-blocking package management without requiring USB cables or computer setups.
- Displays comprehensive local device specifications: Manufacturer, Brand, Model, Codename, Android Version, SDK API Level, Security Patch, Screen Resolution, Density (`dpi`), CPU ABI architectures, and Binder status.
- Implements sticky binder lifecycle listeners (`OnBinderReceivedListenerSticky`, `OnBinderDeadListener`) to maintain continuous binder connection.

### 2. Wireless Debloater Mode (Remote ADB Engine)
- Communicates directly with target devices over TCP/IP using an embedded, high-performance ADB client.
- **Android 11+ Native Wireless Pairing**: Implements AOSP SPAKE2 (Simple Password Exponential Key Exchange) over TLS 1.3 with X.509 client certificate authentication.
- **Classic ADB (Port 5555)**: Direct one-tap connection for Android TVs, emulators, and legacy devices.
- **mDNS Auto-Discovery**: Automatically listens for `_adb-tls-connect._tcp` and `_adb-tls-pairing._tcp` network service announcements.
- **Continuous Connection & Auto-Reconnect**: Runs an active 15-second keepalive heartbeat ping loop (`echo -n`) to prevent Android's `adbd` idle socket dropouts, with automatic silent reconnection on network hiccups.

---

## ✨ Key Features

- **🛡️ 100% Non-Destructive Debloating**: Disables packages for the current user (`pm disable-user --user 0`). System partition files on `/system` or `/product` are never modified or deleted, ensuring zero brick risk and full reversibility.
- **🔒 Hard-Locked Core Protection**: Essential operating system components (`android`, `com.android.systemui`, `com.android.phone`, `com.android.settings`, etc.) are permanently safeguarded against accidental disabling.
- **⭐ Intelligent Brand & Model Tailoring**: Detects the connected device manufacturer and model, automatically curating and highlighting recommendations tailored specifically to that brand (Samsung One UI, Xiaomi/POCO HyperOS, Google Pixel, OnePlus/OPPO, Vivo, Motorola, Sony, TCL, etc.).
- **📺 Dedicated Android & Google TV Catalog**: Integrates [CleanYourTV](https://github.com/gathin23/cleanyourtv) featuring **2,427+ curated TV package definitions** across 492 TV models (Chromecast, Xiaomi TV, TCL, Hisense, Sony Bravia, Fire OS, NVIDIA Shield, and more).
- **📊 Deep Device & OS Analytics**: Tab 1 in both modes displays complete hardware and OS specifications alongside live package breakdown metrics (Total, System, User, Disabled, Recommended).
- **🔍 Top Header Search Action**: Clean Material 3 top app bar search action button with instant real-time package and action log filtering across all screens.
- **🎯 Multi-Facet Persistent Filtering**: Filter simultaneously by App Type (`All`, `System`, `User`, `Disabled`), Removal Tier (`Recommended`, `Advanced`, `Expert`, `Unsafe`, `Unlisted`), Brand Tailoring (`Tailored`), and Quick Sorting (`Name A→Z`, `Name Z→A`, `Pkg A→Z`, `Safety`, `Status`). Filters remain persistent across tab switches and rotations.
- **📜 Persistent Cross-Device Action Log**: Centralized activity ledger records every disabled app with timestamp, package ID, and device identifier. Filter logs by connected device and restore individual or batch packages with a single tap.
- **💾 JSON Snapshot Backup & Restore**: Export and import complete snapshots of remembered device profiles, action logs, and custom debloat repository URLs via clipboard or Android's native share sheet.

---

## 📚 Debloat Catalogs & Brand Tailoring

CleanYourPhone combines two industry-standard, community-driven debloat databases:

| Database | Primary Target | Definitions | Source Repository |
| :--- | :--- | :--- | :--- |
| **UAD-ng** | Android Phones & Tablets | 1,800+ packages | [Universal-Debloater-Alliance/universal-android-debloater-next-generation](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation) |
| **CleanYourTV** | Android TV & Google TV | 2,427+ packages across 492 models | [gathin23/cleanyourtv](https://github.com/gathin23/cleanyourtv) |

### Automatic Brand & Model Tailoring
When CleanYourPhone connects to a device, it evaluates the `ro.product.manufacturer`, `ro.product.brand`, and `ro.product.model` properties to identify OEM software environments:

- **Samsung**: Identifies Knox telemetry, Bixby daemons, AR zone, and carrier companion apps.
- **Xiaomi / Redmi / POCO**: Identifies MIUI/HyperOS analytics, MSA ad services, Mi Cloud, and Joyose.
- **Google Pixel**: Identifies Ambient Services, VR core, and non-essential Google telemetry.
- **OnePlus / OPPO / Realme**: Identifies HeyTap services, HeyMelody, and ColorOS carrier bundles.
- **Vivo / iQOO**: Identifies FuntouchOS / OriginOS Jovi assistants and push daemons.
- **Motorola**: Identifies Moto care analytics and preloaded partner apps.
- **Sony**: Identifies Xperia diagnostics and Cinema Pro telemetry.
- **Android TV / Google TV**: Identifies TV manufacturer ad overlays, remote telemetry, and sponsored launcher feeds.

Tailored apps are badged with a prominent **⭐ Tailored** pill and can be filtered with a single tap using the **Tailored** filter chip.

---

## 🛡️ Safety Tiers & Non-Destructive Debloating

Every package is classified into a safety tier based on UAD-ng and CleanYourTV standards:

| Safety Tier | Badge | Definition & Recommended Action | Safeguard Behavior |
| :--- | :---: | :--- | :--- |
| **Recommended** | 🟢 `Recommended` | Safe to disable for all users. Bloatware, telemetry, analytics, ad networks, carrier apps. | Enabled for selection |
| **Advanced** | 🟡 `Advanced` | Stock applications or minor OS features (e.g., default calculator, calendar, live wallpapers). Disable if you use alternative apps. | Enabled for selection |
| **Expert** | 🟠 `Expert` | Deeper system services or framework components. Disabling may break specific OS features. | Requires conscious review |
| **Unsafe** | 🔴 `Unsafe` | Vital operating system components, telephony, emergency dialer, UI framework. | **Hard-locked by CleanYourPhone. Cannot be disabled.** |
| **Unlisted** | ⚪ `Unlisted` | Packages not cataloged in UAD-ng or CleanYourTV. | Filterable via `Unlisted` chip for custom inspection |

> [!NOTE]
> **Why `pm disable-user --user 0`?**
> Android's package manager retains APK files in the read-only `/system` partition while completely removing the app from the active user environment. The app cannot run in the background, consume battery, receive broadcasts, or access network sockets. If you ever want an app back, CleanYourPhone can re-enable it instantly.

---

## 🚀 User Guides

### 1. Debloating This Phone via Shizuku

1. **Install Shizuku**: Download and install [Shizuku from GitHub](https://github.com/RikkaApps/Shizuku/releases) or Google Play.
2. **Start Shizuku**:
   - **Rooted devices**: Tap *Start* under Root in the Shizuku app.
   - **Non-rooted devices (Android 11+)**: Tap *Pairing* in Shizuku, pair with Wireless Debugging in Developer Options, and tap *Start*.
   - **Non-rooted devices (Android 8.0 - 10)**: Connect phone to PC once and execute `adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh`.
3. **Open CleanYourPhone**: Select **This Device (Shizuku)** from the main screen.
4. **Grant Shizuku Permission**: When prompted by Shizuku, tap **Always allow**.
5. **Explore & Debloat**:
   - **Tab 1 (Status)**: View local hardware specifications, Android version, display resolution/density, CPU ABI, and package metrics.
   - **Tab 2 (Packages)**: Browse apps, filter by **Recommended** or **Tailored**, select packages, and tap **Disable Selected**.
   - **Tab 3 (Action Log)**: Inspect all disabled apps and undo anytime.

---

### 2. Debloating Another Phone Wirelessly (Android 11+)

1. **Connect to Same Wi-Fi**: Ensure both phones are connected to the same Wi-Fi network.
2. **Enable Wireless Debugging on Target Phone**:
   - Go to `Settings` → `About phone` → tap `Build number` 7 times to enable **Developer Options**.
   - Go to `Settings` → `System` → `Developer options`.
   - Enable **Wireless debugging**.
   - Tap **Pair device with pairing code**. A dialog will display:
     - Wi-Fi pairing code (e.g., `482910`)
     - IP address & Pairing Port (e.g., `192.168.1.55:38411`)
3. **Pair on CleanYourPhone**:
   - On the host phone, open CleanYourPhone and select **Other Phone / TV (Wireless ADB)**.
   - On the **Scan Network** tab, CleanYourPhone will automatically detect the pairing request and display a banner.
   - Tap **Pair**, enter the **6-digit pairing code**, and tap **Complete Pairing**.
   - Once paired, select the device and tap **Connect Device**.
4. **Debloat**: Use Tab 2 (**Packages**) to debloat the remote phone.

---

### 3. Debloating Android TV / Google TV

1. **Connect TV & Phone to Same Wi-Fi**.
2. **Enable Network Debugging on TV**:
   - Open TV `Settings` → `System` → `About` → tap `Android TV OS build` 7 times.
   - Go to `Settings` → `System` → `Developer options`.
   - Enable **Network debugging** (or **USB debugging**). Note the TV's IP address displayed in Network Settings (e.g., `192.168.1.120`).
3. **Connect on CleanYourPhone**:
   - Open CleanYourPhone → **Other Phone / TV (Wireless ADB)**.
   - Tap **Pair / Connect** → select the **Direct (5555)** tab.
   - Enter the TV's IP address and port `5555`. Tap **Connect**.
   - Look at the TV screen: a prompt saying *"Allow USB/Network debugging?"* will appear. Check *"Always allow from this computer"* and select **OK**.
4. **Debloat with CleanYourTV Catalog**:
   - CleanYourPhone automatically detects Android TV OS and loads the **CleanYourTV** catalog with 2,427+ TV debloat definitions.
   - Select and safely disable sponsored launchers, OEM telemetry, and TV bloatware.

---

### 4. Managing, Searching & Filtering Apps

- **Top Header Search**: Tap the **Search (🔍)** icon in the top header to filter packages across app names, package identifiers, and descriptions in real time.
- **App Type Filter**:
  - `All`: View every package on the device.
  - `System`: View pre-installed system apps.
  - `User`: View manually installed user apps.
  - `Disabled`: View currently disabled packages.
- **Removal Tier Filter**: Filter by `All Tiers`, `Tailored`, `Recommended`, `Advanced`, `Expert`, `Unsafe`, or `Unlisted`.
- **Sorting**: Tap the sort button to order by `Name (A→Z)`, `Name (Z→A)`, `Package ID`, `Safety Tier`, `Disabled First`, or `Enabled First`.

---

### 5. Restoring & Undoing Actions

- **Single App Restore**: Navigate to Tab 3 (**Action Log**), find the package, and tap **Undo**. The package will immediately be re-enabled on the device.
- **Batch Restore**: Check multiple items in the Action Log and tap **Restore Selected** from the bottom action bar.
- **Re-enable from Packages Tab**: Switch App Type filter to `Disabled`, select any disabled app, and tap **Re-enable**.

---

### 6. Backup & Restore Snapshots

CleanYourPhone includes a full JSON snapshot manager in the top menu:
- **Backup Snapshot**:
  - Tap `Menu (⋮)` → **Backup & Restore**.
  - Review the snapshot summary: remembered device profiles, action log entries, and repository settings.
  - Tap **Copy JSON to Clipboard** or **Share Snapshot** to save your backup securely.
- **Restore Snapshot**:
  - Paste your snapshot JSON string into the text field.
  - Tap **Restore from JSON**. CleanYourPhone validates the schema, non-destructively merges your device profiles and action logs, and refreshes the app state instantly.

---

## 🌟 Latest Release Highlights (v2.9.0)

- **🔍 Top Header Search Action**: Replaced inline search text boxes with a top app bar search action icon beside the menu button for a clean Material 3 header.
- **⭐ Tailored Brand & Model Recommendations**: Automatic OEM brand/model identification with brand badges, tailored filter chip, and prioritized safety sorting.
- **🏷️ Unlisted Tier Filter**: Added `Unlisted` filter chip to the Removal Tiers row for comprehensive cataloging and inspection.
- **📱 Comprehensive Device Specs in Shizuku Mode**: Tab 1 ("Status") in Shizuku mode now displays full hardware specifications, display resolution/density, CPU ABI architecture, and live package metrics.
- **🗑️ Saved Profile Deletion with Confirmation**: Added a Delete button with a Material 3 confirmation dialog right beside the Connect button on saved profile cards.
- **📌 Persistent Filter Selections**: Filters, search queries, safety tiers, and sort options remain persistent across tab switches and app sessions.
- **⚡ Continuous Connection & Auto-Reconnect**: Implemented 15-second background keepalive heartbeat ping loop and automatic socket reconnect on network drops.

*(For detailed changelogs of v2.8.0, v2.7.0, v2.6.0, and earlier releases, visit [GitHub Releases](https://github.com/dr-liki/cleanyourphone/releases)).*

---

## 🛠️ Technical Architecture

CleanYourPhone is built using modern Android architecture components:

- **Language & Runtime**: 100% Kotlin with Kotlin Coroutines and StateFlow.
- **UI Toolkit**: Jetpack Compose with Material 3 (Material You Dynamic Color, adaptive monochrome launcher icon).
- **Navigation**: Jetpack Navigation Compose with bottom navigation bar.
- **Privilege Engines**:
  - Local: Shizuku API (UID 2000 shell binder access).
  - Remote: DADb (Kotlin ADB client) over TCP/TLS 1.3.
- **Cryptographic Primitives**: BouncyCastle implementation of SPAKE2 key exchange and TLS 1.3 mutual certificate authentication for Android 11+ Wireless Debugging.
- **QR Code Scanning**: ZXing barcode scanner integration for camera-based wireless pairing.
- **Persistence**: Jetpack DataStore / SharedPreferences with JSON snapshot serialization.

---

## 🏗️ Building from Source & Signing

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or newer
- JDK 17
- Android SDK Platform API 34 (UpsideDownCake)

### Build Commands
Clone the repository and build the release APK using Gradle:

```bash
git clone https://github.com/dr-liki/cleanyourphone.git
cd cleanyourphone
./gradlew assembleRelease
```

### Official Release Signing Certificate
All official CleanYourPhone APK releases are signed with a permanent release keystore:
- **Keystore Alias:** `cleanyourphone`
- **Certificate SHA-256 Fingerprint:**
  ```text
  7C:7B:09:B5:2D:57:BF:82:56:BE:DD:68:71:9C:D6:F0:9C:88:B5:AA:C4:07:19:3E:52:1A:21:F3:A4:D3:12:76
  ```
- **Verify APK Signature:**
  ```bash
  apksigner verify --print-certs CleanYourPhone-v2.9.0.apk
  ```

---

## 🤝 Open Source Credits & Acknowledgements

CleanYourPhone is built on the shoulders of incredible open-source projects:

- **[Universal Android Debloater Next Generation (UAD-ng)](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation)**: Curated package classifications, safety tiers, and debloat definitions for Android phones and tablets.
- **[CleanYourTV by gathin23](https://github.com/gathin23/cleanyourtv)**: Dedicated debloat list repository and definitions for Android TV and Google TV devices.
- **[Shizuku by Rikka](https://shizuku.rikka.app/)**: On-device privileged Android system service binder access.
- **[DADb by mobile.dev](https://github.com/mobile-dev-inc/DADb)**: Lightweight, pure Kotlin ADB protocol implementation.
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