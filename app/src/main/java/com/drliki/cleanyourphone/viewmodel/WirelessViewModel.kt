package com.drliki.cleanyourphone.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drliki.cleanyourphone.data.UadRepository
import com.drliki.cleanyourphone.engine.AdbEngine
import com.drliki.cleanyourphone.model.ActionLogEntry
import com.drliki.cleanyourphone.model.DevicePackage
import com.drliki.cleanyourphone.model.DiscoveredDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WirelessViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = UadRepository(app)
    private val engine = AdbEngine()

    // ── Scan state ───────────────────────────────────────────────────────────
    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    // ── Connection state ─────────────────────────────────────────────────────
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _connectedName = MutableStateFlow("")
    val connectedName = _connectedName.asStateFlow()

    // ── Package state ────────────────────────────────────────────────────────
    private val _packages = MutableStateFlow<List<DevicePackage>>(emptyList())
    val packages = _packages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _actionLog = MutableStateFlow<List<ActionLogEntry>>(emptyList())
    val actionLog = _actionLog.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar = _snackbar.asStateFlow()

    // ── Actions ──────────────────────────────────────────────────────────────
    fun startScan() {
        viewModelScope.launch {
            _isScanning.value = true
            _devices.value = emptyList()
            _devices.value = engine.scanNetwork()
            _isScanning.value = false
            if (_devices.value.isEmpty()) _snackbar.value = "No devices found on network"
        }
    }

    fun connectTo(device: DiscoveredDevice) {
        viewModelScope.launch {
            _isLoading.value = true
            engine.connect(device)
                .onSuccess { msg ->
                    _isConnected.value = true
                    _connectedName.value = engine.connectedDevice?.name ?: device.host
                    _snackbar.value = msg
                    loadRemotePackages()
                }
                .onFailure { _snackbar.value = "Connection failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun connectManual(host: String, port: Int) =
        connectTo(DiscoveredDevice(host, port))

    fun disconnect() {
        engine.disconnect()
        _isConnected.value = false
        _connectedName.value = ""
        _packages.value = emptyList()
        _snackbar.value = "Disconnected"
    }

    private fun loadRemotePackages() {
        viewModelScope.launch {
            _isLoading.value = true
            engine.listPackages()
                .onSuccess { _packages.value = repo.resolveRemotePackages(it) }
                .onFailure { _snackbar.value = "Failed to list packages: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun disablePackages(selected: Set<String>) {
        viewModelScope.launch {
            val targets = _packages.value.filter {
                it.packageName in selected && !it.isSafeguardLocked && it.isEnabled
            }
            var ok = 0
            targets.forEach { pkg ->
                engine.disablePackage(pkg.packageName).onSuccess {
                    ok++
                    _actionLog.value = listOf(
                        ActionLogEntry(
                            packageName = pkg.packageName,
                            appLabel = pkg.appLabel,
                            action = "pm disable-user --user 0 ${pkg.packageName}",
                            undoCommand = "pm enable --user 0 ${pkg.packageName}",
                            deviceName = _connectedName.value
                        )
                    ) + _actionLog.value
                    _packages.value = _packages.value.map {
                        if (it.packageName == pkg.packageName) it.copy(isEnabled = false) else it
                    }
                }
            }
            _snackbar.value = "Disabled $ok package(s) on ${_connectedName.value}"
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
                _actionLog.value = _actionLog.value.map {
                    if (it.id == entry.id) it.copy(isUndone = true) else it
                }
                _packages.value = _packages.value.map {
                    if (it.packageName == entry.packageName) it.copy(isEnabled = true) else it
                }
                _snackbar.value = "Undone: ${entry.appLabel}"
            }
        }
    }

    fun clearSnackbar() { _snackbar.value = null }

    override fun onCleared() {
        super.onCleared()
        engine.disconnect()
    }
}
