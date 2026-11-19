<div align="center">

# 📱 CleanYourPhone

**Modern, rootless Android debloater for phones, tablets, and Android/Google TVs.**  
*Debloat directly on-device with Shizuku, or wirelessly across your local network via Wireless ADB.*

[![Release](https://img.shields.io/badge/Release-v3.2.0-0284C7?style=flat-square&logo=github)](https://github.com/dr-liki/cleanyourphone/releases/tag/v3.2.0)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-059669?style=flat-square&logo=android)](#-quick-start-guide)
[![Modes](https://img.shields.io/badge/Engines-Shizuku%20%7C%20Wireless%20ADB-4F46E5?style=flat-square)](#-two-operating-modes)
[![Catalogs](https://img.shields.io/badge/Catalogs-UAD--ng%20%26%20CleanYourTV-D97706?style=flat-square)](#-curated-debloat-catalogs--brand-intelligence)
[![UI](https://img.shields.io/badge/UI-Material%20Design%203-DB2777?style=flat-square)](#-core-features)
[![License](https://img.shields.io/badge/License-Apache--2.0-475569?style=flat-square)](LICENSE)

</div>

---

## ⚡ Overview

**CleanYourPhone** is a standalone, rootless Android debloater built with Jetpack Compose and Material Design 3. It safely disables unwanted OEM bloatware, carrier analytics, and background services without modifying system partitions (`/system`) and without requiring a computer.

Every action is completely reversible with one-tap restoration.

---

## 🔌 Two Operating Modes

| Mode | Target | Connection Method | Best For |
| :--- | :--- | :--- | :--- |
| **This Device** | Local phone or tablet | **Shizuku API** (`UID 2000`) | Instant on-device debloating without PC or cables. |
| **Other Phone / TV** | Remote phones & Android/Google TVs | **Wireless ADB** (Wi-Fi) | Debloating other devices over Wi-Fi via QR/pairing code or Port 5555. |

---

## ✨ Core Features

- **Safe, Rootless Debloating**: Disables packages non-destructively for user 0 (`pm disable-user --user 0`). Core system packages (System UI, Settings, Phone, Permission Controller) are hard-locked against removal.
- **Dual Privilege Engines**: Supports on-device management via Shizuku binder access (`UID 2000`) and wireless remote debloating via Android 11+ Wireless Debugging or classic Port 5555.
- **Curated Catalogs & Brand Intelligence**: Integrated with [UAD-ng](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation) (phones) and [CleanYourTV](https://github.com/gathin23/cleanyourtv) (TVs). Automatically detects connected brand (Samsung, Xiaomi, Pixel, OnePlus, OPPO, Vivo, Motorola, Sony, TCL) and flags manufacturer-specific bloatware.
- **Accurate App Names & Branded Icons**: Resolves clean human-readable names and vector glyphs for 500+ system and popular user-installed apps (VLC, MX Player, Kodi, Plex, browsers, file explorers, dev tools, emulators), with compound word splitting and acronym formatting.
- **Fast Search & Persistent Filtering**: Full-width header search with instant keyboard focus. Filter by app type (`All`, `System`, `User`, `Disabled`), safety tier, and sorting with persistent state across tabs.
- **Hardware & OS Diagnostics**: Inspects device hardware details, Android version, security patch level, display metrics, CPU architecture, and live package distribution stats.
- **Action Log & 1-Tap Restoration**: Scoped ledger of disabled packages with single undo, batch restoration, and context-aware action controls.
- **File-Based Backup & Restore**: Export and import complete `.json` backup snapshots directly via Android Storage Access Framework (SAF) without manual copy-pasting.
- **Continuous Connection & Saved Profiles**: 15-second heartbeat keepalive loop prevents wireless disconnects. Saved profiles show real-time network availability for instant reconnection.

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
1. Connect both devices to the same Wi-Fi network.
2. On target phone: Enable **Developer Options** → **Wireless debugging** → **Pair device with pairing code**.
3. In CleanYourPhone: Open **Other Phone / TV (Wireless ADB)**.
4. Tap pairing notification banner, enter 6-digit code, and tap **Complete Pairing**.
5. Tap **Connect Device** from saved profiles.

### Option C: Android TV / Google TV (Port 5555)
1. Connect TV and phone to the same Wi-Fi network.
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

Licensed under the [Apache License, Version 2.0](LICENSE).
