package com.drliki.cleanyourphone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.drliki.cleanyourphone.ui.CleanYourPhoneApp
import com.drliki.cleanyourphone.ui.theme.CleanYourPhoneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CleanYourPhoneTheme {
                CleanYourPhoneApp()
            }
        }
    }
}
