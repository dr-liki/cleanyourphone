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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
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
                    val drawable = pm.getApplicationIcon(packageName)
                    val bmp = drawableToBitmap(drawable)
                    iconCache[packageName] = bmp
                    bmp
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
        // MD3 styled fallback avatar with app initial
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = modifier.size(size)
        ) {
            Box(contentAlignment = Alignment.Center) {
                val initial = appLabel.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: ""
                if (initial.isNotBlank()) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = (size.value * 0.42f).sp
                    )
                } else {
                    Icon(
                        Icons.Default.Android,
                        contentDescription = null,
                        modifier = Modifier.size(size * 0.6f),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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
