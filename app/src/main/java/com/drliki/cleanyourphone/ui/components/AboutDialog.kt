package com.drliki.cleanyourphone.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.drliki.cleanyourphone.R
import com.drliki.cleanyourphone.ui.theme.TierRecommended

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Logo
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "CleanYourPhone",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        "v2.9.0 · Material Design 3",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    "A modern, rootless Android debloater engineered with Material Design 3. Safely optimize this device via Shizuku or wirelessly debloat other phones and Android/Google TVs over Wi-Fi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // Feature Highlights
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AboutFeatureRow(
                        icon = Icons.Default.PhoneAndroid,
                        title = "Dual Operation Architecture",
                        subtitle = "On-device privileged debloating via Shizuku (UID 2000) or wireless ADB over local Wi-Fi with Android 11+ SPAKE2 / TLS 1.3 pairing."
                    )
                    AboutFeatureRow(
                        icon = Icons.Default.Tv,
                        title = "Phone & Android TV Databases",
                        subtitle = "Integrates Universal Android Debloater (UAD-ng) for phones/tablets and CleanYourTV with 2,400+ definitions for Android/Google TVs."
                    )
                    AboutFeatureRow(
                        icon = Icons.Default.Star,
                        title = "Brand-Tailored Recommendations",
                        subtitle = "Automatically identifies connected device brand & model (Samsung, Xiaomi, Pixel, OnePlus, Sony, TCL, etc.) to curate recommendations."
                    )
                    AboutFeatureRow(
                        icon = Icons.Default.PermDeviceInformation,
                        title = "Deep Device & OS Analytics",
                        subtitle = "Comprehensive hardware specs, Android version, display resolution/density, and CPU architecture in both Shizuku and Wireless modes."
                    )
                    AboutFeatureRow(
                        icon = Icons.Default.Search,
                        title = "Top Header Search & Persistent Filters",
                        subtitle = "Seamless header search action button, Unlisted tier filter, and persistent filter state across tab navigation."
                    )
                    AboutFeatureRow(
                        icon = Icons.Default.WifiTethering,
                        title = "Continuous Connection & Auto-Reconnect",
                        subtitle = "15-second background keepalive heartbeat ping and automatic socket reconnection prevent idle connection timeouts."
                    )
                    AboutFeatureRow(
                        icon = Icons.Default.Backup,
                        title = "JSON Snapshot Backup & Restore",
                        subtitle = "Export and import device profiles, persistent action logs, and repository settings via clipboard or native share sheet."
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Safety Callout Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Safe",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Non-Destructive & Reversible: Packages are disabled for the current user (pm disable-user --user 0). System partition files are never deleted and can be re-enabled at any time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Open Source Credits
                Text(
                    "Open Source Credits & Dependencies",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "• Universal-Debloater-Alliance (UAD-ng) — Debloat catalog\n" +
                    "• CleanYourTV by gathin23 — Android & Google TV database\n" +
                    "• Shizuku by Rikka — On-device ADB privileged binder API\n" +
                    "• DADb by mobile.dev — Lightweight Kotlin ADB client\n" +
                    "• BouncyCastle — SPAKE2 & TLS cryptographic primitives\n" +
                    "• ZXing — Wireless pairing camera QR scanner",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                // Actions: GitHub and Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/dr-liki/cleanyourphone")
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("GitHub")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = TierRecommended,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}