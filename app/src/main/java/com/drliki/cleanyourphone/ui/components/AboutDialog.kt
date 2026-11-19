package com.drliki.cleanyourphone.ui.components

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.drliki.cleanyourphone.R
import com.drliki.cleanyourphone.ui.theme.TierRecommended

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
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
                        "v2.8.0 · Material 3",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Professional rootless Android debloater with dual operation modes. Clean this phone on-device via Shizuku, or connect wirelessly over Wi-Fi to debloat other phones and Android/Google TVs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                // Highlights
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AboutFeatureRow(
                        icon = Icons.Default.Backup,
                        title = "Comprehensive Backup & Restore",
                        subtitle = "Export and restore debloat states, custom URLs, device profiles, and action history via JSON or clipboard."
                    )
                    AboutFeatureRow(
                        icon = Icons.Default.PermDeviceInformation,
                        title = "Deep Device Specs & Analytics",
                        subtitle = "Inspects Android OS, API level, security patch, screen resolution/density, and CPU architecture."
                    )
                    AboutFeatureRow(
                        icon = Icons.Default.Category,
                        title = "Package Categorization over ADB",
                        subtitle = "Categorizes remote packages into System, User 3rd-party, and Disabled apps with accurate filtering."
                    )
                    AboutFeatureRow(
                        icon = Icons.Default.History,
                        title = "Persistent Cross-Device Action Log",
                        subtitle = "Retains activities even after disconnection, with interactive device connection filtering."
                    )
                    AboutFeatureRow(
                        icon = Icons.Default.Tv,
                        title = "Android & Google TV Support",
                        subtitle = "Auto-detects TVs and uses CleanYourTV debloat catalog (492+ models)."
                    )
                    AboutFeatureRow(
                        icon = Icons.Default.Shield,
                        title = "100% Reversible Non-Destructive",
                        subtitle = "Uses 'pm disable-user --user 0'. Zero data loss and instant one-tap undo."
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Open Source Credits",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "• Universal-Debloater-Alliance (UAD-ng)\n• CleanYourTV by gathin23\n• Shizuku by Rikka\n• DADb by mobile.dev\n• ZXing QR Project",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = TierRecommended,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column {
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
