<div align="center">

# 📱 CleanYourPhone

**Debloat your Android phone on-device — without root, without a PC, and with every change undoable.**

Switch off the ads, the OEM telemetry, the carrier junk, and the preinstalled bloatware that slow your Android phone down, straight from your phone using **Shizuku** or **Wireless ADB**. Powered by the community-driven [Universal Android Debloater Next Generation (UAD-ng)](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation) package database and styled after [TV Slim](https://github.com/jolabs40/TVSlim).

[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-38BDF8?style=flat-square)](#compatibility)
[![Privilege](https://img.shields.io/badge/Privilege-Shizuku%20%7C%20Wireless%20ADB-10B981?style=flat-square)](#get-started-in-three-steps)
[![Catalogue](https://img.shields.io/badge/Database-UAD--ng%20uad__lists.json-818CF8?style=flat-square)](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/blob/main/resources/assets/uad_lists.json)
[![No Root](https://img.shields.io/badge/Root-Not%20Needed-22C55E?style=flat-square)](#safeguards)
[![License](https://img.shields.io/badge/License-Apache--2.0-F59E0B?style=flat-square)](LICENSE)

</div>

---

**CleanYourPhone** is a free, open-source, standalone on-device debloater for Android smartphones and tablets (`com.drliki.cleanyourphone`). Inspired by the interface and safety philosophy of [TV Slim](https://github.com/jolabs40/TVSlim) and the on-device privilege model of Canta, it runs directly on your Android device via **Shizuku** or **local Wireless ADB**, cross-references every installed package against [UAD-ng's `uad_lists.json`](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/blob/main/resources/assets/uad_lists.json), and disables unwanted bloatware using `pm disable-user --user 0` — never deleting system partitions, recording the exact undo command in a persistent log, and restoring any or all packages in one tap.

## Why CleanYourPhone

- 🛡️ **Reversible by design (`pm disable-user --user 0`)** — System packages are disabled for User 0, never permanently stripped from `/system`. Undo a single line in the **Log** tab, or tap **Restore everything** to return your phone to factory package state in one click.
- ⚡ **On-device, no PC required** — Works directly on your phone using [Shizuku](https://shizuku.rikka.app/) or Android 11+ **Wireless Debugging** (local ADB loopback).
- 📖 **Powered by UAD-ng's `uad_lists.json`** — Every package is matched against the [Universal Android Debloater Next Generation](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation) database across **OEM**, **Carrier**, **Google**, **AOSP**, **Misc**, and **Pending** lists, complete with [UAD-ng safety tiers](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/wiki/Debloat-Lists) (`Recommended`, `Advanced`, `Expert`, `Unsafe`), detailed descriptions, dependencies (`neededBy`), and known side effects.
- 🎯 **Ready-made one-tap profiles** — Apply curated profiles inspired by [TV Slim](https://github.com/jolabs40/TVSlim): *Ads & Telemetry*, *OEM Bloatware & Store Promos*, *Carrier Junk*, and *Full UAD-ng Recommended*, with a pre-flight confirmation modal listing all side effects.
- 🧱 **Safeguards you cannot click past** — Packages marked `Unsafe` in UAD-ng or critical to booting, telephony (`com.android.phone`), SystemUI (`com.android.systemui`), permission management, or active default launchers/keyboards are hardware-locked against disabling.
- 🔁 **Survives OTA system updates** — When an Android OTA or OEM update re-enables disabled bloatware, CleanYourPhone compares your live package states against your log on launch and displays a **System Update Drift** card with a one-tap **Put them back** action.
- 🔒 **Strictly private** — No accounts, no analytics, no telemetry. CleanYourPhone only makes a single optional `GET` request to GitHub to sync the latest [`uad_lists.json`](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/blob/main/resources/assets/uad_lists.json) from `Universal-Debloater-Alliance/universal-android-debloater-next-generation`, and includes an offline seed database out of the box.

---

## UAD-ng Safety Tiers & Package Lists

CleanYourPhone implements the exact classification schema from [UAD-ng Debloat Lists](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/wiki/Debloat-Lists):

| Safety Tier | Badge | Meaning & CleanYourPhone Behavior |
| :--- | :--- | :--- |
| **Recommended** | 🟢 `Recommended` | Safe to disable. Pre-installed commercial apps, OEM ad/analytics daemons, carrier bloatware, and apps replaceable via Play Store. Included in one-tap profiles. |
| **Advanced** | 🟡 `Advanced` | Useful stock apps (default browser, gallery, clock, weather, OEM cloud sync) or minor features. Requires manual review before disabling. |
| **Expert** | 🟠 `Expert` | Deeper system components that may break secondary OS features without boot-looping the phone. Requires explicit warning acknowledgement. |
| **Unsafe** | 🔴 `Unsafe (Locked)` | Vital OS components, emergency alert services, or boot-critical packages. **Hard-blocked by CleanYourPhone's safeguard engine.** |

Packages are also filterable by their **UAD-ng Origin List**:
- `OEM` — Manufacturer-specific packages (Samsung OneUI, Xiaomi HyperOS/MIUI, OnePlus/OPPO ColorOS, Vivo, Motorola, Realme, Asus, Sony, Nothing)
- `Carrier` — Telecom/ISP preloads, SIM toolkits, and carrier diagnostics
- `Google` — Google Mobile Services, preloaded Google apps, and analytics overlays
- `AOSP` — Core Android Open Source Project packages and Easter eggs
- `Misc` — Third-party preloads (Facebook AppManager/Meta services, partner trials, shop demos)

---

## What You Can Do (TV Slim 5-Tab Layout)

1. **📱 Phone** — Shows your detected manufacturer, model, Android version, active privilege backend (**Shizuku Active** or **Wireless ADB Connected**), RAM & storage summary, active home launcher & keyboard safeguards, **System Update Drift** recovery banner, and JSON configuration backup/re-injection (`Save configuration` / `Reinject configuration`).
2. **📦 Packages** — Lists installed packages matched against `uad_lists.json`, with UAD-ng risk badges, origin icons, dependencies, and full descriptions. Includes ready-made profiles (*Ads & Telemetry*, *OEM Bloatware*, *Carrier Junk*, *UAD Recommended*), search, tier/origin chips, and batch apply with a side-effect confirmation sheet.
3. **🧩 Apps** — Every user and system app with its real label, package name, and status: open it, force-stop it, disable/re-enable it via `pm disable-user --user 0`, or uninstall apps you installed yourself (system packages are strictly blocked from uninstallation).
4. **📊 Memory** — Live RAM consumption and before/after memory savings since your first debloat session, plus storage occupied by top bloatware packages.
5. **📜 Log** — Every action recorded with timestamp, package ID, UAD-ng tier, and the exact undo command (`pm enable --user 0 <package>`). Undo any single line, **Restore everything** in one tap, or **Export as Markdown**.

---

## Get Started in Three Steps

### 1. Install CleanYourPhone & Shizuku
Download the latest `CleanYourPhone-v1.0.0.apk` from [Releases](https://github.com/dr-liki/cleanyourphone/releases) (or build from source below) and install [Shizuku](https://shizuku.rikka.app/).

### 2. Start Shizuku (or Wireless Debugging)
- **Android 11+ (No PC needed):** Enable **Developer options** → **Wireless debugging**, open Shizuku, tap **Pair**, enter the 6-digit pairing code, and tap **Start**.
- **Built-in Wireless ADB fallback:** Alternatively, pair directly inside CleanYourPhone's **Phone** tab using local Wireless ADB (`dadb`).

### 3. Pick a Profile & Slim Down Your Phone
Open CleanYourPhone, grant the Shizuku permission prompt, switch to the **Packages** tab, choose a profile (such as **Ads, Telemetry & OEM Promos** or **UAD-ng Recommended**), review the side-effect confirmation sheet, and tap **Apply (`pm disable-user --user 0`)**.

---

## Safeguards

- **Never uninstalls system apps:** CleanYourPhone only executes `pm disable-user --user 0 <package>`, which is cleanly reversed by `pm enable <package>`.
- **Boot-critical blocklist:** Packages in the `Unsafe` tier or matching `PROTECTED_CORE_PACKAGES` (`android`, `com.android.systemui`, `com.android.phone`, `com.android.settings`, `com.android.permissioncontroller`, `com.google.android.packageinstaller`, `moe.shizuku.privileged.api`) are locked in the UI and rejected by the shell engine.
- **Launcher & Keyboard Guard:** CleanYourPhone queries `cmd package resolve-activity -c android.intent.category.HOME` and `settings get secure default_input_method` before every batch run, refusing to disable the active home screen or keyboard unless an enabled replacement is already installed.

---

## Project Structure

```text
cleanyourphone/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   └── uad_lists_seed.json         # Offline UAD-ng package database seed
│       └── java/com/drliki/cleanyourphone/
│           ├── MainActivity.kt             # App entrypoint & edge-to-edge setup
│           ├── model/
│           │   └── UadModels.kt            # UAD-ng schema, tiers, profiles & log models
│           ├── data/
│           │   └── UadRepository.kt        # Bundled + GitHub live uad_lists.json sync
│           ├── engine/
│           │   ├── PrivilegeEngine.kt      # Shizuku IBinder & Wireless ADB command runner
│           │   └── SafeguardValidator.kt   # Bootloop, launcher, keyboard & Unsafe guards
│           └── ui/
│               ├── CleanYourPhoneApp.kt    # TVSlim-inspired 5-tab Jetpack Compose UI
│               └── theme/
│                   └── Theme.kt            # TVSlim Obsidian/Sky/Emerald Material 3 theme
├── docs/
│   ├── SAFEGUARDS.md
│   └── SHIZUKU_ADB_GUIDE.md
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Build from Source

Requires **JDK 17+** and **Android SDK Platform 34**.

```bash
git clone https://github.com/dr-liki/cleanyourphone.git
cd cleanyourphone
./gradlew assembleDebug
# Output APK: app/build/outputs/apk/debug/app-debug.apk
```

## Credits & Acknowledgements

- **Package Database & Safety Tiers:** [Universal Android Debloater Next Generation (UAD-ng)](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation) (`uad_lists.json`)
- **UI/UX Design & Safety Philosophy:** [TV Slim (`jolabs40/TVSlim`)](https://github.com/jolabs40/TVSlim) and [CleanYourTV (`Gathin23/cleanyourtv`)](https://github.com/Gathin23/cleanyourtv)
- **On-Device Shizuku Architecture Inspiration:** [Canta](https://github.com/samolego/Canta) & [Shizuku (`RikkaApps/Shizuku`)](https://github.com/RikkaApps/Shizuku)

## License

Licensed under the **Apache License 2.0** — see [LICENSE](LICENSE) for details.

---

## Publishing a Signed APK & GitHub Release

This repository includes an automated GitHub Actions pipeline (`.github/workflows/release-apk.yml`) that compiles, shrinks (R8), signs (`V1/V2/V3`), checksums (`SHA256SUMS.txt`), and publishes `CleanYourPhone-vX.Y.Z.apk` directly to **GitHub Releases**:

### Option A — One-Click Release in GitHub Browser (No Local Tools Needed)
1. Upload this repository to `https://github.com/dr-liki/cleanyourphone`.
2. Open the **Actions** tab on GitHub → select **Build & Release Signed APK** on the left → click **Run workflow** → enter `v1.0.0` and click **Run workflow**.
3. Within ~2 minutes, GitHub Actions builds the signed `CleanYourPhone-v1.0.0.apk` and `SHA256SUMS.txt` and publishes them on your repository's **Releases** page.

### Option B — Tag Push or Custom Keystore Signing
1. (Optional) Generate your own persistent release keystore and upload it to GitHub Secrets:
   ```bash
   bash tools/generate-keystore.sh
   ```
2. Push any `v*` tag to trigger an automated signed release:
   ```bash
   git tag -a v1.0.0 -m "CleanYourPhone v1.0.0"
   git push origin v1.0.0
   ```
