package com.drliki.cleanyourphone.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drliki.cleanyourphone.data.AppSettings
import com.drliki.cleanyourphone.data.UadRepository
import com.drliki.cleanyourphone.engine.AdbEngine
import com.drliki.cleanyourphone.engine.DiscoveredPairingService
import com.drliki.cleanyourphone.model.ActionLogEntry
import com.drliki.cleanyourphone.model.CategorizedPackages
import com.drliki.cleanyourphone.model.DevicePackage
import com.drliki.cleanyourphone.model.DiscoveredDevice
import com.drliki.cleanyourphone.model.RemoteDeviceInfo
import com.drliki.cleanyourphone.model.SavedDeviceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class WirelessViewModel(app: Application) : AndroidViewModel(app) {

    val repo = UadRepository(app)
    val settings = AppSettings(app)
    private val engine = AdbEngine(app)

    // ── Scan state ───────────────────────────────────────────────────────────
    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _customScanPort = MutableStateFlow(5555)
    val customScanPort = _customScanPort.asStateFlow()

    // ── Saved device profiles (v2.3) ─────────────────────────────────────────
    private val _savedProfiles = MutableStateFlow<List<SavedDeviceProfile>>(emptyList())
    val savedProfiles = _savedProfiles.asStateFlow()

    // ── Connection state ─────────────────────────────────────────────────────
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _connectedName = MutableStateFlow("")
    val connectedName = _connectedName.asStateFlow()

    private val _isAndroidTv = MutableStateFlow(false)
    val isAndroidTv = _isAndroidTv.asStateFlow()

    private val _deviceInfo = MutableStateFlow<RemoteDeviceInfo?>(null)
    val deviceInfo = _deviceInfo.asStateFlow()

    // ── Package state ────────────────────────────────────────────────────────
    private val _packages = MutableStateFlow<List<DevicePackage>>(emptyList())
    val packages = _packages.asStateFlow()

    private val _categorizedPackages = MutableStateFlow<CategorizedPackages?>(null)
    val categorizedPackages = _categorizedPackages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _actionLog = MutableStateFlow<List<ActionLogEntry>>(emptyList())
    val actionLog = _actionLog.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar = _snackbar.asStateFlow()

    // ── Persistent Filter States across tab switches & session ───────────────
    val searchQuery = MutableStateFlow("")
    val isSearchActive = MutableStateFlow(false)
    val selectedTypeFilter = MutableStateFlow(com.drliki.cleanyourphone.model.AppTypeFilter.ALL)
    val selectedTier = MutableStateFlow<com.drliki.cleanyourphone.model.RemovalTier?>(null)
    val selectedSort = MutableStateFlow(com.drliki.cleanyourphone.ui.components.AppSortOption.NAME_ASC)
    val isTailoredFilter = MutableStateFlow(false)

    // ── Pairing state ────────────────────────────────────────────────────────
    private val _pairingQrPayload = MutableStateFlow("")
    val pairingQrPayload = _pairingQrPayload.asStateFlow()

    private val _pairingCode = MutableStateFlow("")
    val pairingCode = _pairingCode.asStateFlow()

    private val _discoveredPairingService = MutableStateFlow<DiscoveredPairingService?>(null)
    val discoveredPairingService = _discoveredPairingService.asStateFlow()

    init {
        refreshSavedProfiles()
        startContinuousDiscovery()
        generateNewPairingCredentials()
        loadActionLog()
    }

    fun loadActionLog() {
        _actionLog.value = settings.getActionHistory().filter { it.mode == "WIRELESS" }
    }

    fun refreshSavedProfiles() {
        _savedProfiles.value = settings.getSavedProfiles()
    }

    private fun startContinuousDiscovery() {
        engine.startNsdDiscovery(
            onDeviceFound = { device ->
                val current = _devices.value.toMutableList()
                if (current.none { it.host == device.host && it.port == device.port }) {
                    current.add(0, device)
                    _devices.value = current
                }
            },
            onPairingServiceFound = { service ->
                _discoveredPairingService.value = service
            }
        )
    }

    fun setCustomScanPort(port: Int) {
        _customScanPort.value = port
    }

    fun generateNewPairingCredentials() {
        val code = (100000 + Random.nextInt(900000)).toString()
        val sessionName = "CleanYourPhone-${Random.nextInt(1000, 9999)}"
        _pairingCode.value = code
        _pairingQrPayload.value = "WIFI:T:ADB;S:$sessionName;P:$code;;"
    }

    fun startScan(port: Int = _customScanPort.value) {
        viewModelScope.launch {
            _isScanning.value = true
            val portsToScan = if (port == 5555) listOf(5555) else listOf(port, 5555)
            val result = engine.scanNetwork(portsToScan)
            val merged = (_devices.value + result).distinctBy { "${it.host}:${it.port}" }
            _devices.value = merged
            _isScanning.value = false
            if (merged.isEmpty()) {
                _snackbar.value = "No devices found. Check Wireless Debugging port on target phone."
            }
        }
    }

    fun connectTo(device: DiscoveredDevice) {
        viewModelScope.launch {
            _isLoading.value = true
            engine.connect(device)
                .onSuccess { msg ->
                    _isConnected.value = true
                    val info = engine.getRemoteDeviceInfo()
                    _deviceInfo.value = info
                    val isTv = info.isTv || engine.detectIsAndroidTv()
                    _isAndroidTv.value = isTv

                    val rawName = if (info.model.isNotBlank()) info.model else (engine.connectedDevice?.name ?: device.name)
                    val deviceName = if (isTv && !rawName.contains("TV", ignoreCase = true)) "$rawName (Android TV)" else rawName
                    _connectedName.value = deviceName
                    _snackbar.value = if (isTv) "$msg — Android/Google TV detected! CleanYourTV debloat list active." else msg

                    // Persist connected device profile
                    settings.saveProfile(
                        SavedDeviceProfile(
                            name = deviceName,
                            host = device.host,
                            port = device.port,
                            lastConnected = System.currentTimeMillis(),
                            isAndroidTv = isTv
                        )
                    )
                    refreshSavedProfiles()
                    loadRemotePackages()
                }
                .onFailure { _snackbar.value = "Connection failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun connectToProfile(profile: SavedDeviceProfile) {
        _isAndroidTv.value = profile.isAndroidTv
        connectTo(DiscoveredDevice(profile.host, profile.port, profile.name, profile.name))
    }

    fun deleteProfile(id: String) {
        settings.deleteProfile(id)
        refreshSavedProfiles()
    }

    fun connectManual(host: String, port: Int) =
        connectTo(DiscoveredDevice(host, port))

    fun getMatchingConnectPort(host: String): Int? =
        _devices.value.firstOrNull { it.host == host }?.port

    fun pairWithCode(host: String, pairingPort: Int, code: String, connectPort: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _snackbar.value = "Pairing with $host:$pairingPort using code $code..."

            val pairRes = engine.pair(host, pairingPort, code)
            pairRes.onSuccess { pairMsg ->
                val targetPort = if (connectPort > 0) connectPort else pairingPort
                _snackbar.value = "$pairMsg Connecting to $host:$targetPort..."

                engine.connect(DiscoveredDevice(host, targetPort, "Remote Phone ($host)", "Android Phone"))
                    .onSuccess {
                        _isConnected.value = true
                        val info = engine.getRemoteDeviceInfo()
                        _deviceInfo.value = info
                        val isTv = info.isTv || engine.detectIsAndroidTv()
                        _isAndroidTv.value = isTv

                        val rawName = if (info.model.isNotBlank()) info.model else (engine.connectedDevice?.name ?: "Remote Device")
                        val deviceName = if (isTv && !rawName.contains("TV", ignoreCase = true)) "$rawName (Android TV)" else rawName
                        _connectedName.value = deviceName
                        _snackbar.value = if (isTv) "Connected to $deviceName ($host:$targetPort)! CleanYourTV debloat list active." else "Connected to $deviceName ($host:$targetPort)!"

                        settings.saveProfile(
                            SavedDeviceProfile(
                                name = deviceName,
                                host = host,
                                port = targetPort,
                                lastConnected = System.currentTimeMillis(),
                                isAndroidTv = isTv
                            )
                        )
                        refreshSavedProfiles()
                        loadRemotePackages()
                    }
                    .onFailure { connErr ->
                        _snackbar.value = "Paired! But connection to port $targetPort failed: ${connErr.message}. Ensure Connect Port under 'IP address & Port' on other phone matches."
                    }
            }.onFailure { pairErr ->
                _snackbar.value = pairErr.message ?: "Pairing failed. Keep the pairing dialog open on the other phone."
            }
            _isLoading.value = false
        }
    }

    fun disconnect() {
        engine.disconnect()
        _isConnected.value = false
        _deviceInfo.value = null
        _categorizedPackages.value = null
        _connectedName.value = ""
        _isAndroidTv.value = false
        _packages.value = emptyList()
        _snackbar.value = "Disconnected"
    }

    private fun loadRemotePackages() {
        viewModelScope.launch {
            _isLoading.value = true
            engine.listCategorizedPackages()
                .onSuccess { catPkgs ->
                    _categorizedPackages.value = catPkgs
                    val isTv = _isAndroidTv.value
                    val info = _deviceInfo.value
                    val resolved = repo.resolveRemotePackages(
                        catPkgs,
                        isTv = isTv,
                        brand = info?.brand,
                        manufacturer = info?.manufacturer,
                        model = info?.model
                    )
                    _packages.value = resolved

                    // Update total count in saved profile
                    engine.connectedDevice?.let { dev ->
                        settings.recordDisabledPackages(dev.host, dev.port, emptyList(), catPkgs.allPackages.size)
                        refreshSavedProfiles()
                    }
                }
                .onFailure { _snackbar.value = "Failed to list packages: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun updateUadLists() {
        viewModelScope.launch {
            _isLoading.value = true
            _snackbar.value = "Updating UAD-ng definitions from GitHub..."
            repo.updateFromGitHub()
                .onSuccess { count ->
                    _snackbar.value = "Updated $count package definitions from UAD-ng!"
                    if (_isConnected.value) {
                        loadRemotePackages()
                    }
                }
                .onFailure { _snackbar.value = "Failed to update catalog: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun updateTvLists() {
        viewModelScope.launch {
            _isLoading.value = true
            _snackbar.value = "Updating CleanYourTV definitions from GitHub..."
            repo.updateTvFromGitHub()
                .onSuccess { count ->
                    _snackbar.value = "Updated $count TV package definitions from CleanYourTV!"
                    if (_isConnected.value) {
                        loadRemotePackages()
                    }
                }
                .onFailure { _snackbar.value = "Failed to update TV catalog: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun updateAllDebloatLists() {
        viewModelScope.launch {
            _isLoading.value = true
            _snackbar.value = "Updating debloat lists from GitHub..."
            var uadCount = 0
            var tvCount = 0
            val uadRes = repo.updateFromGitHub()
            uadRes.onSuccess { uadCount = it }
            val tvRes = repo.updateTvFromGitHub()
            tvRes.onSuccess { tvCount = it }

            if (uadRes.isSuccess || tvRes.isSuccess) {
                _snackbar.value = "Updated $uadCount UAD definitions & $tvCount CleanYourTV definitions!"
                if (_isConnected.value) {
                    loadRemotePackages()
                }
            } else {
                _snackbar.value = "Update failed: ${uadRes.exceptionOrNull()?.message ?: tvRes.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    fun disablePackages(selected: Set<String>) {
        viewModelScope.launch {
            val targets = _packages.value.filter {
                it.packageName in selected && !it.isSafeguardLocked && it.isEnabled
            }
            var ok = 0
            var lastError: String? = null
            val disabledNames = mutableListOf<String>()
            val newEntries = mutableListOf<ActionLogEntry>()
            val devName = _connectedName.value.ifBlank { "Remote Device" }
            targets.forEach { pkg ->
                engine.disablePackage(pkg.packageName).onSuccess {
                    ok++
                    disabledNames.add(pkg.packageName)
                    val entry = ActionLogEntry(
                        packageName = pkg.packageName,
                        appLabel = pkg.appLabel,
                        action = "pm disable-user --user 0 ${pkg.packageName}",
                        undoCommand = "pm enable --user 0 ${pkg.packageName}",
                        deviceName = devName,
                        mode = "WIRELESS"
                    )
                    newEntries.add(entry)
                    _packages.value = _packages.value.map {
                        if (it.packageName == pkg.packageName) it.copy(isEnabled = false) else it
                    }
                }.onFailure { err ->
                    lastError = err.message
                }
            }
            if (newEntries.isNotEmpty()) {
                settings.recordActions(newEntries)
                _actionLog.value = newEntries + _actionLog.value
            }
            // Update remembered device profile disabled packages
            engine.connectedDevice?.let { dev ->
                settings.recordDisabledPackages(dev.host, dev.port, disabledNames, _packages.value.size)
                refreshSavedProfiles()
            }
            if (ok > 0) {
                _snackbar.value = "Disabled $ok package(s) on $devName"
            } else {
                _snackbar.value = "Failed to disable packages: ${lastError ?: "Command failed"}"
            }
        }
    }

    fun enablePackage(packageName: String) {
        viewModelScope.launch {
            engine.enablePackage(packageName).onSuccess {
                _packages.value = _packages.value.map {
                    if (it.packageName == packageName) it.copy(isEnabled = true) else it
                }
                _snackbar.value = "Re-enabled $packageName"
            }.onFailure { _snackbar.value = "Failed: ${it.message}" }
        }
    }

    fun undoAction(entry: ActionLogEntry) {
        viewModelScope.launch {
            engine.enablePackage(entry.packageName).onSuccess {
                val updated = entry.copy(isUndone = true)
                settings.updateAction(updated)
                _actionLog.value = _actionLog.value.map {
                    if (it.id == entry.id) updated else it
                }
                _packages.value = _packages.value.map {
                    if (it.packageName == entry.packageName) it.copy(isEnabled = true) else it
                }
                _snackbar.value = "Undone: ${entry.appLabel}"
            }.onFailure {
                _snackbar.value = "Failed to undo ${entry.appLabel}: ${it.message}"
            }
        }
    }

    fun clearSnackbar() { _snackbar.value = null }

    override fun onCleared() {
        super.onCleared()
        engine.stopNsdDiscovery()
        engine.disconnect()
    }
}
