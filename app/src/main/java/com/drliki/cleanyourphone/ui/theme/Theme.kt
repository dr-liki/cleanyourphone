package com.drliki.cleanyourphone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// TVSlim-inspired Obsidian & Sky Slate Dark Palette
val TvSlimBg = Color(0xFF0B0F17)
val TvSlimSurface = Color(0xFF131B2E)
val TvSlimCard = Color(0xFF1A243B)
val TvSlimBorder = Color(0xFF283552)
val TvSlimAccentSky = Color(0xFF38BDF8)
val TvSlimRecommended = Color(0xFF10B981)
val TvSlimAdvanced = Color(0xFFF59E0B)
val TvSlimExpert = Color(0xFFF97316)
val TvSlimUnsafe = Color(0xFFEF4444)
val TvSlimTextPrimary = Color(0xFFF8FAFC)
val TvSlimTextSecondary = Color(0xFF94A3B8)

private val DarkColorScheme = darkColorScheme(
    primary = TvSlimAccentSky,
    onPrimary = Color(0xFF061320),
    secondary = TvSlimRecommended,
    background = TvSlimBg,
    surface = TvSlimSurface,
    surfaceVariant = TvSlimCard,
    onBackground = TvSlimTextPrimary,
    onSurface = TvSlimTextPrimary,
    onSurfaceVariant = TvSlimTextSecondary,
    error = TvSlimUnsafe
)

@Composable
fun CleanYourPhoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
