package com.drliki.cleanyourphone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drliki.cleanyourphone.ui.components.AboutDialog
import com.drliki.cleanyourphone.ui.components.SettingsDialog
import com.drliki.cleanyourphone.ui.screens.ActionLogScreen
import com.drliki.cleanyourphone.ui.screens.ModeSelectScreen
import com.drliki.cleanyourphone.ui.screens.ShizukuScreen
import com.drliki.cleanyourphone.ui.screens.WirelessScreen
import com.drliki.cleanyourphone.ui.theme.CleanYourPhoneTheme
import com.drliki.cleanyourphone.viewmodel.ShizukuViewModel
import com.drliki.cleanyourphone.viewmodel.WirelessViewModel

enum class AppScreen {
    MODE_SELECT,
    SHIZUKU,
    WIRELESS,
    ACTION_LOG
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure user.home is consistently set to writable internal filesDir across the entire process
        System.setProperty("user.home", filesDir.absolutePath)
        enableEdgeToEdge()

        setContent {
            CleanYourPhoneTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CleanYourPhoneAppRoot()
                }
            }
        }
    }
}

@Composable
fun CleanYourPhoneAppRoot(
    shizukuVm: ShizukuViewModel = viewModel(),
    wirelessVm: WirelessViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(AppScreen.MODE_SELECT) }
    var previousScreen by remember { mutableStateOf(AppScreen.MODE_SELECT) }
    var targetDeviceFilter by remember { mutableStateOf<String?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val shizukuStatus by shizukuVm.status.collectAsState()
    val shizukuLog by shizukuVm.actionLog.collectAsState()
    val wirelessLog by wirelessVm.actionLog.collectAsState()

    // Persistent all-activities history across entire app
    val allHistory = remember(currentScreen, shizukuLog, wirelessLog) {
        shizukuVm.settings.getActionHistory()
    }

    BackHandler(enabled = currentScreen != AppScreen.MODE_SELECT) {
        currentScreen = if (currentScreen == AppScreen.ACTION_LOG) previousScreen else AppScreen.MODE_SELECT
    }

    fun navigateTo(screen: AppScreen) {
        previousScreen = currentScreen
        currentScreen = screen
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            AppScreen.MODE_SELECT -> {
                ModeSelectScreen(
                    shizukuStatus = shizukuStatus,
                    onSelectShizuku = {
                        shizukuVm.refreshStatus()
                        navigateTo(AppScreen.SHIZUKU)
                    },
                    onSelectWireless = {
                        navigateTo(AppScreen.WIRELESS)
                    },
                    onOpenActionLog = {
                        targetDeviceFilter = null
                        navigateTo(AppScreen.ACTION_LOG)
                    },
                    onOpenSettings = {
                        showSettingsDialog = true
                    },
                    onOpenAbout = {
                        showAboutDialog = true
                    }
                )
            }
            AppScreen.SHIZUKU -> {
                ShizukuScreen(
                    viewModel = shizukuVm,
                    onNavigateBack = { currentScreen = AppScreen.MODE_SELECT },
                    onNavigateToActionLog = { navigateTo(AppScreen.ACTION_LOG) },
                    onOpenSettings = { showSettingsDialog = true }
                )
            }
            AppScreen.WIRELESS -> {
                WirelessScreen(
                    viewModel = wirelessVm,
                    onNavigateBack = { currentScreen = AppScreen.MODE_SELECT },
                    onNavigateToActionLog = { devFilter ->
                        targetDeviceFilter = devFilter
                        navigateTo(AppScreen.ACTION_LOG)
                    },
                    onOpenSettings = { showSettingsDialog = true }
                )
            }
            AppScreen.ACTION_LOG -> {
                val (logToDisplay, logTitle) = when (previousScreen) {
                    AppScreen.SHIZUKU -> Pair(
                        allHistory.filter { it.mode == "SHIZUKU" || it.deviceName.contains("Local", ignoreCase = true) },
                        "Shizuku Action Log"
                    )
                    AppScreen.WIRELESS -> Pair(
                        allHistory.filter { it.mode == "WIRELESS" },
                        "Wireless ADB Action Log"
                    )
                    else -> Pair(allHistory, "All Activities Log")
                }
                val isWirelessMode = previousScreen == AppScreen.WIRELESS
                val isConnected = if (isWirelessMode) {
                    wirelessVm.isConnected.value && (targetDeviceFilter == null || targetDeviceFilter == wirelessVm.connectedName.value)
                } else if (previousScreen == AppScreen.SHIZUKU) {
                    shizukuVm.isShizukuAvailable.value
                } else {
                    true
                }
                ActionLogScreen(
                    actionLog = logToDisplay,
                    sourceTitle = logTitle,
                    initialDeviceFilter = targetDeviceFilter,
                    isDeviceConnected = isConnected,
                    checkDeviceConnected = { dev ->
                        if (isWirelessMode) {
                            wirelessVm.isConnected.value && (dev == null || dev == wirelessVm.connectedName.value)
                        } else if (previousScreen == AppScreen.SHIZUKU) {
                            shizukuVm.isShizukuAvailable.value
                        } else {
                            true
                        }
                    },
                    onUndoAction = { entry ->
                        if (entry.mode == "SHIZUKU" || entry.deviceName.contains("Local", ignoreCase = true)) {
                            shizukuVm.undoAction(entry)
                        } else {
                            wirelessVm.undoAction(entry)
                        }
                    },
                    onUndoMultipleActions = { entries ->
                        entries.forEach { entry ->
                            if (entry.mode == "SHIZUKU" || entry.deviceName.contains("Local", ignoreCase = true)) {
                                shizukuVm.undoAction(entry)
                            } else {
                                wirelessVm.undoAction(entry)
                            }
                        }
                    },
                    onNavigateBack = {
                        targetDeviceFilter = null
                        currentScreen = previousScreen
                    }
                )
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showSettingsDialog) {
        SettingsDialog(
            repository = shizukuVm.repo,
            onDismiss = { showSettingsDialog = false }
        )
    }
}
