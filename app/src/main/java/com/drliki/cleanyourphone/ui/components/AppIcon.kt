package com.drliki.cleanyourphone.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

// In-memory icon bitmap cache to ensure 60fps scrolling
private val iconCache = ConcurrentHashMap<String, Bitmap>()

// Remote to local package aliases for fetching authentic icons from local device
private val REMOTE_PACKAGE_ALIASES = mapOf(
    // Google & TV
    "com.google.android.youtube.tv" to "com.google.android.youtube",
    "com.google.android.youtube.tvmusic" to "com.google.android.apps.youtube.music",
    "com.google.android.tvlauncher" to "com.google.android.apps.nexuslauncher",
    "com.google.android.tv.launcher" to "com.google.android.apps.nexuslauncher",
    "com.google.android.apps.tv.launcherx" to "com.google.android.apps.nexuslauncher",
    "com.google.android.katniss" to "com.google.android.googlequicksearchbox",
    "com.google.android.tungsten.setupwraith" to "com.google.android.setupwizard",
    "com.google.android.tvrecommendations" to "com.google.android.gms",

    // Streaming & Media
    "com.netflix.ninja" to "com.netflix.mediaclient",
    "com.spotify.tv.android" to "com.spotify.music",
    "com.amazon.amazonvideo.livingroom" to "com.amazon.avod.thirdpartyclient",
    "com.hotstar.androidtv" to "in.startv.hotstar",
    "com.jio.media.ondemand" to "com.jio.media.ondemand",

    // Samsung & OEM
    "com.sec.android.app.sbrowser.beta" to "com.sec.android.app.sbrowser",
    "com.sec.android.app.popupcalculator" to "com.google.android.calculator",
    "com.sec.android.app.clockpackage" to "com.google.android.deskclock",
    "com.sec.android.gallery3d" to "com.google.android.apps.photos",
    "com.sec.android.app.myfiles" to "com.google.android.apps.nbu.files",
    "com.samsung.android.calendar" to "com.google.android.calendar",
    "com.samsung.android.messaging" to "com.google.android.apps.messaging",
    "com.samsung.android.dialer" to "com.google.android.dialer",
    "com.samsung.android.app.contacts" to "com.google.android.contacts"
)

private val AVATAR_PALETTE = listOf(
    Color(0xFF0284C7), Color(0xFF059669), Color(0xFFD97706),
    Color(0xFF7C3AED), Color(0xFFDC2626), Color(0xFF2563EB),
    Color(0xFF0D9488), Color(0xFF9333EA), Color(0xFFEA580C),
    Color(0xFF4F46E5), Color(0xFF0891B2), Color(0xFFC026D3)
)

data class AppGlyph(
    val icon: ImageVector,
    val containerColor: Color,
    val iconColor: Color = Color.White
)

fun resolveAppGlyph(pkgName: String, label: String): AppGlyph? {
    val pkg = pkgName.lowercase()

    return when {
        // YouTube / TV Video
        pkg.contains("youtube.tv") || pkg.contains("youtube") ->
            if (pkg.contains("music")) AppGlyph(Icons.Default.MusicNote, Color(0xFFCC0000))
            else AppGlyph(Icons.Default.PlayArrow, Color(0xFFCC0000))

        // Netflix
        pkg.contains("netflix") -> AppGlyph(Icons.Default.Movie, Color(0xFFE50914))

        // Spotify
        pkg.contains("spotify") -> AppGlyph(Icons.Default.MusicNote, Color(0xFF1DB954))

        // Prime Video / Amazon
        pkg.contains("amazonvideo") || (pkg.contains("amazon") && pkg.contains("video")) ->
            AppGlyph(Icons.Default.PlayArrow, Color(0xFF00A8E1))
        pkg.contains("amazon") && pkg.contains("mp3") ->
            AppGlyph(Icons.Default.MusicNote, Color(0xFF00A8E1))
        pkg.contains("amazon") ->
            AppGlyph(Icons.Default.ShoppingCart, Color(0xFFFF9900))

        // Disney+ / Hulu / Max / Streaming
        pkg.contains("disney") || pkg.contains("hulu") || pkg.contains("hbo") || pkg.contains("peacock") || pkg.contains("paramount") || pkg.contains("crunchyroll") ->
            AppGlyph(Icons.Default.Tv, Color(0xFF113CCF))

        // Chrome & Browsers
        pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("sbrowser") ->
            AppGlyph(Icons.Default.Language, Color(0xFF1A73E8))

        // Play Store / App Stores
        pkg.contains("vending") || pkg.contains("samsungapps") || pkg.contains("appmarket") || pkg.contains("mipicks") || pkg.contains("appstore") ->
            AppGlyph(Icons.Default.ShoppingBag, Color(0xFF01875F))

        // Gmail & Email
        pkg.contains(".gm") || pkg.contains("email") || pkg.contains("mail") || pkg.contains("outlook") ->
            AppGlyph(Icons.Default.Email, Color(0xFFEA4335))

        // Maps & Navigation
        pkg.contains("maps") || pkg.contains("navigation") || pkg.contains("location") ->
            AppGlyph(Icons.Default.LocationOn, Color(0xFF34A853))

        // Photos & Gallery
        pkg.contains("photos") || pkg.contains("gallery") ->
            AppGlyph(Icons.Default.PhotoLibrary, Color(0xFF9333EA))

        // Camera
        pkg.contains("camera") -> AppGlyph(Icons.Default.CameraAlt, Color(0xFF0D9488))

        // TV Launchers / Android TV Home
        pkg.contains("tvlauncher") || pkg.contains("tv.launcher") || pkg.contains("launcherx") ->
            AppGlyph(Icons.Default.Tv, Color(0xFF2E7D32))

        // Phone & Dialer
        pkg.contains("dialer") || pkg.contains("telephony") || pkg.contains(".phone") ->
            AppGlyph(Icons.Default.Phone, Color(0xFF43A047))

        // Messages & Chat
        pkg.contains("whatsapp") -> AppGlyph(Icons.Default.Chat, Color(0xFF25D366))
        pkg.contains("telegram") -> AppGlyph(Icons.Default.Send, Color(0xFF2AABEE))
        pkg.contains("messaging") || pkg.contains(".mms") || pkg.contains("chat") ->
            AppGlyph(Icons.Default.Chat, Color(0xFF1E88E5))
        pkg.contains("facebook") || pkg.contains("messenger") || pkg.contains(".orca") ->
            AppGlyph(Icons.Default.Message, Color(0xFF1877F2))
        pkg.contains("instagram") -> AppGlyph(Icons.Default.PhotoCamera, Color(0xFFE1306C))
        pkg.contains("twitter") -> AppGlyph(Icons.Default.Tag, Color(0xFF111827))

        // Clock & Alarm
        pkg.contains("clock") -> AppGlyph(Icons.Default.Schedule, Color(0xFF3949AB))

        // Calculator
        pkg.contains("calculator") -> AppGlyph(Icons.Default.Calculate, Color(0xFF00838F))

        // Calendar
        pkg.contains("calendar") -> AppGlyph(Icons.Default.CalendarToday, Color(0xFF1976D2))

        // Weather
        pkg.contains("weather") -> AppGlyph(Icons.Default.WbSunny, Color(0xFFFFA000))

        // Files & Storage
        pkg.contains("myfiles") || pkg.contains("filemanager") || pkg.contains("files") || pkg.contains("documentsui") ->
            AppGlyph(Icons.Default.Folder, Color(0xFFFF6F00))

        // Music & Audio
        pkg.contains("music") || pkg.contains("audio") || pkg.contains(".player") || pkg.contains("sound") ->
            AppGlyph(Icons.Default.MusicNote, Color(0xFFD81B60))

        // Video & Media Players
        pkg.contains("video") || pkg.contains("movie") || pkg.contains("videoplayer") || pkg.contains("plex") || pkg.contains("kodi") || pkg.contains("vlc") ->
            AppGlyph(Icons.Default.SmartDisplay, Color(0xFF8E24AA))

        // Games
        pkg.contains("game") || pkg.contains("gaming") || pkg.contains("gamespace") ->
            AppGlyph(Icons.Default.SportsEsports, Color(0xFF6A1B9A))

        // Notes & Docs
        pkg.contains("notes") || pkg.contains("keep") || pkg.contains("memo") ->
            AppGlyph(Icons.Default.EditNote, Color(0xFFF9A825))

        // Settings & System
        pkg.contains("settings") || pkg.contains("setup") || pkg.contains("config") ->
            AppGlyph(Icons.Default.Settings, Color(0xFF546E7A))

        // Security, Knox, Antivirus
        pkg.contains("security") || pkg.contains("knox") || pkg.contains("protect") || pkg.contains("cleanmaster") ->
            AppGlyph(Icons.Default.Shield, Color(0xFF37474F))

        // Voice Assistant & Bixby
        pkg.contains("bixby") || pkg.contains("assistant") || pkg.contains("voiceassist") ->
            AppGlyph(Icons.Default.Mic, Color(0xFF6200EE))

        // Wallet & Payments
        pkg.contains("wallet") || pkg.contains("spay") || pkg.contains("pay") || pkg.contains("billing") ->
            AppGlyph(Icons.Default.Payment, Color(0xFF0D47A1))

        // Cloud & Sync
        pkg.contains("cloud") || pkg.contains("sync") || pkg.contains("backup") ->
            AppGlyph(Icons.Default.Cloud, Color(0xFF0288D1))

        // Remote & Cast & Connectivity
        pkg.contains("remote") || pkg.contains("cast") || pkg.contains("bluetooth") || pkg.contains("wifi") || pkg.contains("share") ->
            AppGlyph(Icons.Default.Cast, Color(0xFF455A64))

        // Health & Fitness
        pkg.contains("health") || pkg.contains("fitness") || pkg.contains("wellbeing") ->
            AppGlyph(Icons.Default.Favorite, Color(0xFFE91E63))

        // Keyboard & Input
        pkg.contains("keyboard") || pkg.contains("honeyboard") || pkg.contains("inputmethod") ->
            AppGlyph(Icons.Default.Keyboard, Color(0xFF5C6BC0))

        // Telemetry, Ads & Analytics
        pkg.contains("analytics") || pkg.contains("telemetry") || pkg.contains("msa") || pkg.contains("tracking") ->
            AppGlyph(Icons.Default.BarChart, Color(0xFF78909C))

        else -> null
    }
}

@Composable
fun AppIconView(
    packageName: String,
    appLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val context = LocalContext.current
    var bitmap by remember(packageName) { mutableStateOf(iconCache[packageName]) }

    LaunchedEffect(packageName) {
        if (bitmap == null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val pm = context.packageManager
                    // 1. Direct package lookup
                    val directDrawable = runCatching { pm.getApplicationIcon(packageName) }.getOrNull()
                    if (directDrawable != null) {
                        val bmp = drawableToBitmap(directDrawable)
                        iconCache[packageName] = bmp
                        return@runCatching bmp
                    }

                    // 2. Remote alias fallback lookup from host installed packages
                    val alias = REMOTE_PACKAGE_ALIASES[packageName]
                    if (alias != null) {
                        val aliasDrawable = runCatching { pm.getApplicationIcon(alias) }.getOrNull()
                        if (aliasDrawable != null) {
                            val bmp = drawableToBitmap(aliasDrawable)
                            iconCache[packageName] = bmp
                            return@runCatching bmp
                        }
                    }

                    null
                }.getOrNull()?.let { loaded ->
                    bitmap = loaded
                }
            }
        }
    }

    if (bitmap != null) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            modifier = modifier.size(size)
        ) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = appLabel,
                modifier = Modifier.size(size)
            )
        }
    } else {
        val glyph = remember(packageName, appLabel) { resolveAppGlyph(packageName, appLabel) }
        if (glyph != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = glyph.containerColor,
                modifier = modifier.size(size)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = glyph.icon,
                        contentDescription = appLabel,
                        modifier = Modifier.size(size * 0.58f),
                        tint = glyph.iconColor
                    )
                }
            }
        } else {
            // Vibrant Material You styled fallback avatar with app initial
            val colorIndex = remember(packageName) {
                (packageName.hashCode() and 0x7FFFFFFF) % AVATAR_PALETTE.size
            }
            val bgColor = AVATAR_PALETTE[colorIndex]

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = bgColor,
                modifier = modifier.size(size)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val initial = appLabel.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: ""
                    if (initial.isNotBlank()) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = (size.value * 0.44f).sp
                        )
                    } else {
                        Icon(
                            Icons.Default.Android,
                            contentDescription = null,
                            modifier = Modifier.size(size * 0.6f),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
