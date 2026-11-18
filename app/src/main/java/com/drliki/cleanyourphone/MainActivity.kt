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
import com.drliki.cleanyourphone.ui.screens.ModeSelectScreen
import com.drliki.cleanyourphone.ui.screens.ShizukuScreen
import com.drliki.cleanyourphone.ui.screens.WirelessScreen
import com.drliki.cleanyourphone.ui.theme.CleanYourPhoneTheme
import com.drliki.cleanyourphone.viewmodel.ShizukuViewModel
import com.drliki.cleanyourphone.viewmodel.WirelessViewModel

enum class AppScreen {
    MODE_SELECT,
    SHIZUKU,
    WIRELESS
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    val shizukuStatus by shizukuVm.status.collectAsState()

    BackHandler(enabled = currentScreen != AppScreen.MODE_SELECT) {
        currentScreen = AppScreen.MODE_SELECT
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
                        currentScreen = AppScreen.SHIZUKU
                    },
                    onSelectWireless = {
                        currentScreen = AppScreen.WIRELESS
                    }
                )
            }
            AppScreen.SHIZUKU -> {
                ShizukuScreen(
                    viewModel = shizukuVm,
                    onNavigateBack = { currentScreen = AppScreen.MODE_SELECT }
                )
            }
            AppScreen.WIRELESS -> {
                WirelessScreen(
                    viewModel = wirelessVm,
                    onNavigateBack = { currentScreen = AppScreen.MODE_SELECT }
                )
            }
        }
    }
}
