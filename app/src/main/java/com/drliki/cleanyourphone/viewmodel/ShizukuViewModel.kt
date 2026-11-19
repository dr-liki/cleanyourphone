package com.drliki.cleanyourphone.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drliki.cleanyourphone.data.AppSettings
import com.drliki.cleanyourphone.data.UadRepository
import com.drliki.cleanyourphone.engine.ShizukuEngine
import com.drliki.cleanyourphone.engine.ShizukuStatus
import com.drliki.cleanyourphone.model.ActionLogEntry
import com.drliki.cleanyourphone.model.DevicePackage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShizukuViewModel(app: Application) : AndroidViewModel(app) {

    val repo = UadRepository(app)
    val settings = AppSettings(app)
    private val engine = ShizukuEngine()

    private val _packages = MutableStateFlow<List<DevicePackage>>(emptyList())
    val packages = _packages.asStateFlow()

    private val _status = MutableStateFlow<ShizukuStatus>(ShizukuStatus.NotRunning)
    val status = _status.asStateFlow()

    private val _actionLog = MutableStateFlow<List<ActionLogEntry>>(emptyList())
    val actionLog = _actionLog.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar = _snackbar.asStateFlow()

    init {
        refreshStatus()
        loadPackages()
        loadActionLog()
    }

    fun loadActionLog() {
        _actionLog.value = settings.getActionHistory().filter {
            it.mode == "SHIZUKU" || it.deviceName.contains("Local", ignoreCase = true) || it.deviceName.contains("Shizuku", ignoreCase = true)
        }
    }

    fun refreshStatus() { _status.value = engine.checkStatus() }

    fun requestPermission() {
        engine.requestPermission()
        refreshStatus()
    }

    fun loadPackages() {
        viewModelScope.launch {
            _isLoading.value = true
            _packages.value = repo.scanLocalPackages()
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
                    loadPackages()
                }
                .onFailure { _snackbar.value = "Failed to update catalog: ${it.message}" }
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
                loadPackages()
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
            val newEntries = mutableListOf<ActionLogEntry>()
            targets.forEach { pkg ->
                engine.disablePackage(pkg.packageName).onSuccess {
                    ok++
                    val entry = ActionLogEntry(
                        packageName = pkg.packageName,
                        appLabel = pkg.appLabel,
                        action = "pm disable-user --user 0 ${pkg.packageName}",
                        undoCommand = "pm enable --user 0 ${pkg.packageName}",
                        deviceName = "This Device (Local Shizuku)",
                        mode = "SHIZUKU"
                    )
                    newEntries.add(entry)
                    _packages.value = _packages.value.map {
                        if (it.packageName == pkg.packageName) it.copy(isEnabled = false) else it
                    }
                }
            }
            if (newEntries.isNotEmpty()) {
                settings.recordActions(newEntries)
                _actionLog.value = newEntries + _actionLog.value
            }
            _snackbar.value = "Disabled $ok package(s)"
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
            }
        }
    }

    fun clearSnackbar() { _snackbar.value = null }
}
