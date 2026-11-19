<div align="center">

# 📱 CleanYourPhone v2.9.0

**Debloat Android phones & Android/Google TVs — directly on this device via Shizuku, or wirelessly connect to other phones and TVs over Wi-Fi.**

[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-38BDF8?style=flat-square)](#compatibility)
[![Privilege](https://img.shields.io/badge/Modes-Shizuku%20%7C%20Wireless%20ADB-10B981?style=flat-square)](#operation-modes)
[![Databases](https://img.shields.io/badge/Databases-UAD--ng%20%26%20CleanYourTV-818CF8?style=flat-square)](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation)
[![Pairing](https://img.shields.io/badge/Wireless%20Pairing-SPAKE2%20%26%20TLS%201.3-10B981?style=flat-square)](#wireless-pairing)
[![Backup](https://img.shields.io/badge/Backup%20%26%20Restore-JSON%20Snapshot-8B5CF6?style=flat-square)](#backup--restore)
[![Design](https://img.shields.io/badge/UI-Material%20Design%203-F59E0B?style=flat-square)](#user-interface)
[![License](https://img.shields.io/badge/License-Apache--2.0-64748B?style=flat-square)](LICENSE)

</div>

---

**CleanYourPhone** is a modern, open-source Android debloater built from the ground up with **Material Design 3**. It offers a dual-engine architecture that lets you optimize your current device or connect wirelessly across your local network to debloat other phones and Android/Google TVs.

---

## 🌟 What's New in v2.9.0

- **🔍 Top Header Search Action**:
  - Moved search boxes from individual tab contents to the top header as an intuitive search button beside the menu button.
  - Tapping Search seamlessly transforms the top bar into a clean Material 3 search field with instant filtering across packages and action logs.
- **⭐ Tailored Brand & Model Recommendations**:
  - Automatically identifies connected phone and TV brand/model (Samsung, Xiaomi, POCO, Google Pixel, OnePlus, OPPO, Vivo, Motorola, Sony, TCL, etc.).
  - Curates and highlights debloat recommendations tailored specifically to that device's OEM software and models.
  - Added "Tailored" filter chip and badge pills on package cards.
- **🏷️ Unlisted Tier Filter**:
  - Full support for filtering unlisted packages in the Removal Tiers row (`All Tiers`, `Tailored`, `Recommended`, `Advanced`, `Expert`, `Unsafe`, `Unlisted`).
- **📱 Comprehensive Device & OS Info in Shizuku Mode**:
  - Tab 1 ("Status") now features full hardware and OS specifications: Manufacturer, Brand, Model, Codename, Android Version, SDK API Level, Security Patch date, Display Resolution/Density, CPU ABI, Binder Privileged Access (UID 2000), and full package metrics.
- **🗑️ Saved Profile Delete with Confirmation**:
  - Repositioned the "Delete" button right beside the "Connect Device" button with an explicit Material 3 confirmation dialog.
- **📌 Persistent Filter Selections**:
  - Search queries, app type filters, safety tiers, sorting modes, and device filter selections remain intact across tab switches, screen rotations, and sessions until manually changed.
- **⚡ Continuous Connection & Auto-Reconnect**:
  - Implemented background keepalive heartbeat loop (every 15s) and automatic socket reconnection on network drops.
  - Asynchronous stream reading and process timeout prevents Shizuku shell deadlocks, ensuring continuous connection until manual disconnect.

---

## 🌟 What's New in v2.8.0

- **💾 Comprehensive Backup & Restore**:
  - Full snapshot backup of remembered device profiles, action log history, and custom repository configurations.
  - One-tap **Copy JSON to Clipboard** and native Android share sheet for exporting or saving backup files.
  - Safe import and restore with schema validation, non-destructive profile & log merging, and instant state refresh.
- **📱 Deep Device Specifications & Analysis**:
  - Tab 1 (**Device**) displays detailed hardware and OS specs for connected remote devices: Manufacturer, Brand, Model, Device Codename, Android Version, API Level, Security Patch level, Screen Resolution, Display Density (dpi), and CPU ABI architecture.
- **📊 Precise Remote Package Categorization**:
  - Direct ADB package queries using Android package manager flags (`pm list packages -s`, `pm list packages -3`, `pm list packages -d`).
  - Enables 100% accurate filtering by **All Apps**, **System Apps**, **User Apps**, and **Disabled Apps** on remote phones and Android/Google TVs.
  - Real-time statistics breakdown displaying counts for Total, System, User, Disabled, and Recommended debloat packages.
- **📜 Persistent Cross-Device Action Log**:
  - Main **All Activities Log** now stores all actions persistently in internal storage, preserving history even after disconnection and app restarts.
  - Interactive **Device Filter Chips** allow isolating actions per connected phone, TV, or local Shizuku session with a single tap.
  - Prominent device connection badges on each action log entry card.
- **🧭 Streamlined Wireless Debloater UX**:
  - 3-tab Bottom Navigation Bar appears **only after device connection is established**.
  - Clean disconnected view with 2 intuitive top tabs: **Saved Profiles** and **Scan Network**.
  - Redundant separate manual connect page removed (fully integrated inside the multi-mode Pair & Connect dialog).
  - Saved device profile card **Connect Device** button repositioned ergonomically below the metadata line, eliminating text overflow.
  - Top bar refresh buttons removed in both Shizuku and Wireless ADB modes for a clean, minimalist header.
  - Top bar and menu Pair Device shortcuts removed in favor of contextual action buttons.
  - Wireless ADB description updated to **Other Phone/TV** across the app.

---

## 🌟 What's New in v2.7.0

- **🧭 Material Design 3 Bottom Navigation Bar**:
  - **In Shizuku Mode**:
    - **Tab 1 (Status)**: Dedicated Shizuku service connection status view with status badge, binder access details, device app metrics overview, and an elevated **Refresh Service Status** button.
    - **Tab 2 (Packages)**: Complete app packages debloater with search, dual-row filters, sorting, app icons, and selection controls.
    - **Tab 3 (Action Log)**: Full-featured embedded action log displaying on-device activities with search, multi-select, and batch restore.
  - **In Wireless Debloater Mode**:
    - **Tab 1 (Device)**: Shows connected device info (device name, IP, port, TLS 1.3 / TCP status, Android TV badge), device stats, and a prominent **Disconnect Device** button. When disconnected, displays the 3-tab connection manager (Saved Profiles, Scan Network, Manual Connect).
    - **Tab 2 (Packages)**: Shows packages on the remote phone or Android/Google TV with curated recommendations.
    - **Tab 3 (Action Log)**: Full-featured embedded action log for remote ADB activities.
- **📺 Android & Google TV Debloat Repository (CleanYourTV)**:
  - Integrated debloat list repository from [gathin23/cleanyourtv](https://github.com/gathin23/cleanyourtv) specifically for wireless debug mode.
  - Automatically detects Android TV and Google TV devices over ADB (via `android.software.leanback`, `android.hardware.type.television`, and build characteristics).
  - Uses CleanYourTV's curated catalog with **2,427+ TV debloat definitions** across 492 TV models (Xiaomi, Chromecast, Sony, TCL, Hisense, Fire OS, NVIDIA Shield, and more).
- **🔄 Auto & Manual Repository Updates**:
  - Added manual and auto-update options in the top menu and Settings for both **Phone (UAD-ng)** and **TV (CleanYourTV)** debloat lists.
  - Customizable repository URLs, database timestamp tracking, and definition counters.
- **🎯 Precise Pairing Banner Placement**:
  - The "Pairing request detected!" banner is now displayed **only on the Scan Network page**, cleanly removed from Saved Profiles and Manual Connect tabs.
- **🏷️ Clean Saved Profiles Heading**:
  - Removed bracketed number counts from the Saved Profiles tab and heading for a clean, consistent Material 3 aesthetic.

---

## 🌟 What's New in v2.6.0

- **🔐 Android 11+ Native SPAKE2 Wireless Pairing over TLS**: Full implementation of Android 11+ AOSP wireless debugging pairing handshake using SPAKE2 password-authenticated key exchange and TLS 1.3.
- **🎯 Accurate Pairing Port & Code Auto-Population**: Automatically populates the temporary pairing port from `_adb-tls-pairing._tcp` broadcasts (e.g. `38411`), separating it from the main screen's dynamic connect port (e.g. `41255`).
- **🔢 6-Digit Wi-Fi Pairing Code Input**: Restored and prominently featured the 6-digit Wi-Fi pairing code input field with numeric keypad and monospace styling across the pairing dialog and manual connect screens.
- **⚡ Dynamic TLS 1.3 + Plain TCP Port 5555 Multi-Engine**: Seamlessly connects to Android 11+ dynamic ports via TLS 1.3 using authenticated client certificates, while retaining full support for classic unencrypted ADB on port 5555.
- **💡 Actionable Dynamic Port Guidance**: Replaced cryptic socket resets with clear, helpful instructions when an unpaired device attempts to connect directly to dynamic ports.
- **📱 Three-Tab Pairing & Connection Dialog**: Seamlessly switch between "Pair Code" (6-digit PIN + auto-discovered ports), "Direct (5555)" (one-tap connect without pairing), and "QR Code" (visual code for target phone camera scanning).

---

## 🌟 What's New in v2.5

- **📐 Proportionate Card Spacing**: App icon anchored top-left, selection/lock/tick button anchored bottom-left. Card height scales dynamically with long package descriptions while maintaining proportional vertical separation.
- **🔄 Action Log Multi-Select & Batch Restore**: Action log screen now supports multi-select checkboxes below the app icon, a persistent bottom "Restore Selected" action bar, and "Select All Active" toggle.
- **📂 Mode-Specific Action Logs**: Action log opened from Shizuku mode displays only on-device actions; Wireless mode displays only remote ADB actions; Mode Selection screen displays combined activity.
- **⬆️ Fast Scroll-to-Top Overlay**: Quick-return floating action button appears on the bottom right on both apps and action log screens for fast one-tap navigation to top.
- **⚡ Wireless ADB Authorization Polling**: 15-second authorization retry loop replaces instant handshake timeouts, giving users ample time to tap 'Always allow' on the target phone screen.
- **🔌 Connect Port vs Pairing Port Differentiation**: Clearly separates Connect Port from temporary pairing dialog ports, preventing `ECONNRESET` / `ECONNREFUSED` socket confusion.
- **🔤 Optimized 64dp Text Field Clearances**: Expanded text fields across Wireless ADB and Pairing screens so all digits, port numbers, and floating labels remain 100% visible.
- **📲 Ergonomic Full-Width Connect Button**: Positioned directly below the device IP line in discovered devices list for effortless tapping.

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
