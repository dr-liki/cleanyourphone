# CleanYourPhone Safeguards & Recovery Architecture

CleanYourPhone combines the package intelligence of [Universal Android Debloater Next Generation (UAD-ng)](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation) with the strict safety guards of [TV Slim (`jolabs40/TVSlim`)](https://github.com/jolabs40/TVSlim).

## 1. Never Uninstalling System Packages (`pm disable-user --user 0`)
Unlike older ADB scripts that run `pm uninstall -k --user 0`, CleanYourPhone strictly uses:
```sh
pm disable-user --user 0 <package_name>
```
This keeps the APK intact in `/system`, `/product`, or `/vendor` and allows instant restoration via:
```sh
pm enable <package_name>
```

## 2. Hard-Locked `Unsafe` Tier & Core OS Blocklist
Any package classified as `Unsafe` in `uad_lists.json` or matching `SafeguardValidator.PROTECTED_CORE_PACKAGES` (`android`, `com.android.systemui`, `com.android.phone`, `com.android.settings`, `com.android.permissioncontroller`, `com.google.android.packageinstaller`, `moe.shizuku.privileged.api`) is marked **Never disable** and cannot be selected in the UI or executed by `PrivilegeEngine`.

## 3. Active Home Launcher & Keyboard Guard
Before disabling any package, CleanYourPhone resolves the current default `android.intent.category.HOME` activity and `default_input_method` and locks them so the phone is never left without a working home screen or keyboard.
