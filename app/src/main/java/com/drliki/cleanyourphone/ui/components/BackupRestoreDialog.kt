package com.drliki.cleanyourphone.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.drliki.cleanyourphone.data.AppSettings
import com.drliki.cleanyourphone.ui.theme.TierRecommended

@Composable
fun BackupRestoreDialog(
    settings: AppSettings,
    onBackupRestored: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // Export state
    val backupData = remember { settings.createBackup() }
    val backupJson = remember { settings.exportBackupJson() }
    var copiedToClipboard by remember { mutableStateOf(false) }

    // Import state
    var importJsonText by remember { mutableStateOf("") }
    var restoreSuccessMsg by remember { mutableStateOf<String?>(null) }
    var restoreErrorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Backup,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Backup & Restore",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Two Tabs: Export vs Import
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Export Backup") },
                        icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Import Restore") },
                        icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, Modifier.size(18.dp)) }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // ── Tab 0: Export Backup ──────────────────────────────
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Create a complete snapshot of your debloat states, custom debloat URLs, remembered device profiles, and action log history.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Snapshot Summary Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        "Backup Snapshot Contents:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Remembered Devices", style = MaterialTheme.typography.bodySmall)
                                        Text("${backupData.savedProfiles.size} profile(s)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Action History Log", style = MaterialTheme.typography.bodySmall)
                                        Text("${backupData.actionHistory.size} record(s)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Catalog URLs & Flags", style = MaterialTheme.typography.bodySmall)
                                        Text("UAD & CleanYourTV", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            // Copy to Clipboard Button
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(backupJson))
                                    copiedToClipboard = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    if (copiedToClipboard) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = if (copiedToClipboard) TierRecommended else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (copiedToClipboard) "Copied JSON to Clipboard!" else "Copy JSON to Clipboard",
                                    color = if (copiedToClipboard) TierRecommended else MaterialTheme.colorScheme.primary
                                )
                            }

                            // Share / Export File Button
                            Button(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, backupJson)
                                        putExtra(Intent.EXTRA_SUBJECT, "CleanYourPhone-Backup-${System.currentTimeMillis()}.json")
                                        type = "application/json"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Export CleanYourPhone Backup"))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Share or Save Backup File")
                            }
                        }
                    }

                    1 -> {
                        // ── Tab 1: Import / Restore ───────────────────────────
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Paste a previously exported CleanYourPhone JSON snapshot below to restore saved device profiles, action history, and configuration.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = importJsonText,
                                onValueChange = {
                                    importJsonText = it
                                    restoreErrorMsg = null
                                    restoreSuccessMsg = null
                                },
                                label = { Text("Backup JSON Content") },
                                placeholder = { Text("Paste {\"version\": \"2.8.0\", ...} here") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Quick paste button
                            TextButton(
                                onClick = {
                                    val text = clipboardManager.getText()?.text ?: ""
                                    if (text.isNotBlank()) {
                                        importJsonText = text
                                        restoreErrorMsg = null
                                        restoreSuccessMsg = null
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Paste from Clipboard", style = MaterialTheme.typography.labelSmall)
                            }

                            // Success Message Box
                            AnimatedVisibility(visible = restoreSuccessMsg != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = TierRecommended.copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, tint = TierRecommended, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            restoreSuccessMsg ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TierRecommended,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Error Message Box
                            AnimatedVisibility(visible = restoreErrorMsg != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            restoreErrorMsg ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            // Apply Restore Button
                            Button(
                                onClick = {
                                    if (importJsonText.isBlank()) {
                                        restoreErrorMsg = "Please enter or paste JSON backup data first."
                                        return@Button
                                    }
                                    val result = settings.restoreBackupFromJson(importJsonText)
                                    result.onSuccess { msg ->
                                        restoreSuccessMsg = msg
                                        restoreErrorMsg = null
                                        onBackupRestored()
                                    }.onFailure { err ->
                                        restoreErrorMsg = "Invalid backup format: ${err.message}"
                                        restoreSuccessMsg = null
                                    }
                                },
                                enabled = importJsonText.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Validate & Apply Restore")
                            }
                        }
                    }
                }
            }
        }
    }
}
