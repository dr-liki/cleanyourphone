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
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val shizukuStatus by shizukuVm.status.collectAsState()
    val shizukuLog by shizukuVm.actionLog.collectAsState()
    val wirelessLog by wirelessVm.actionLog.collectAsState()

    // Combined action log preserving timeline order
    val combinedActionLog = remember(shizukuLog, wirelessLog) {
        (shizukuLog + wirelessLog).sortedByDescending { it.timestamp }
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
                    onNavigateToActionLog = { navigateTo(AppScreen.ACTION_LOG) },
                    onOpenSettings = { showSettingsDialog = true }
                )
            }
            AppScreen.ACTION_LOG -> {
                val (logToDisplay, logTitle) = when (previousScreen) {
                    AppScreen.SHIZUKU -> Pair(shizukuLog, "Shizuku Action Log")
                    AppScreen.WIRELESS -> Pair(wirelessLog, "Wireless ADB Action Log")
                    else -> Pair(combinedActionLog, "All Activities Log")
                }
                ActionLogScreen(
                    actionLog = logToDisplay,
                    sourceTitle = logTitle,
                    onUndoAction = { entry ->
                        if (entry.deviceName == "Local Device" || shizukuLog.any { it.id == entry.id }) {
                            shizukuVm.undoAction(entry)
                        } else {
                            wirelessVm.undoAction(entry)
                        }
                    },
                    onUndoMultipleActions = { entries ->
                        entries.forEach { entry ->
                            if (entry.deviceName == "Local Device" || shizukuLog.any { it.id == entry.id }) {
                                shizukuVm.undoAction(entry)
                            } else {
                                wirelessVm.undoAction(entry)
                            }
                        }
                    },
                    onNavigateBack = { currentScreen = previousScreen }
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
