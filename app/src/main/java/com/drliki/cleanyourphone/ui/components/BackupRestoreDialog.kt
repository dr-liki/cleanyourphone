package com.drliki.cleanyourphone.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun getFileNameFromUri(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "backup.json"
}

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
    var exportSuccessMsg by remember { mutableStateOf<String?>(null) }
    var exportErrorMsg by remember { mutableStateOf<String?>(null) }
    var copiedToClipboard by remember { mutableStateOf(false) }

    // Import state
    var selectedRestoreFileName by remember { mutableStateOf<String?>(null) }
    var importJsonText by remember { mutableStateOf("") }
    var restoreSuccessMsg by remember { mutableStateOf<String?>(null) }
    var restoreErrorMsg by remember { mutableStateOf<String?>(null) }
    var showManualPaste by remember { mutableStateOf(false) }

    // File-based Export: SAF CreateDocument launcher (.json)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(backupJson.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("Cannot open output stream for selected file")
            }.onSuccess {
                val fileName = getFileNameFromUri(context, uri)
                exportSuccessMsg = "Backup saved to: $fileName"
                exportErrorMsg = null
            }.onFailure { err ->
                exportErrorMsg = "Failed to save file: ${err.message}"
                exportSuccessMsg = null
            }
        }
    }

    // File-based Import: SAF OpenDocument launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri)
            selectedRestoreFileName = fileName
            runCatching {
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: throw IllegalStateException("Cannot read selected file")
                val res = settings.restoreBackupFromJson(content)
                res.getOrThrow()
            }.onSuccess { msg ->
                restoreSuccessMsg = "$msg (from $fileName)"
                restoreErrorMsg = null
                onBackupRestored()
            }.onFailure { err ->
                restoreErrorMsg = "Failed to restore $fileName: ${err.message}"
                restoreSuccessMsg = null
            }
        }
    }

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
                        text = { Text("Export File") },
                        icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Import File") },
                        icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, Modifier.size(18.dp)) }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // ── Tab 0: Export Backup File ──────────────────────────────
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Create a complete snapshot file of your remembered devices, disabled package states, catalog URLs, and action log history.",
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

                            // Success Message Box
                            AnimatedVisibility(visible = exportSuccessMsg != null) {
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
                                            exportSuccessMsg ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TierRecommended,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Error Message Box
                            AnimatedVisibility(visible = exportErrorMsg != null) {
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
                                            exportErrorMsg ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            // Primary: Save Backup File (.json) via SAF
                            Button(
                                onClick = {
                                    val timeTag = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                                    val defaultName = "CleanYourPhone-Backup-$timeTag.json"
                                    createDocumentLauncher.launch(defaultName)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Save Backup File (.json)")
                            }

                            // Secondary: Share File Intent
                            OutlinedButton(
                                onClick = {
                                    val timeTag = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, backupJson)
                                        putExtra(Intent.EXTRA_SUBJECT, "CleanYourPhone-Backup-$timeTag.json")
                                        type = "application/json"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Backup"))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Share Backup via Apps")
                            }

                            // Copy JSON to Clipboard as minor option
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(backupJson))
                                    copiedToClipboard = true
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(
                                    if (copiedToClipboard) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = if (copiedToClipboard) TierRecommended else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (copiedToClipboard) "Copied JSON to Clipboard!" else "Copy JSON to Clipboard",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (copiedToClipboard) TierRecommended else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    1 -> {
                        // ── Tab 1: Import / Restore File ───────────────────────────
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Pick a previously saved CleanYourPhone backup file (.json) from your storage, downloads, or cloud drive to restore.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Primary: Select File via SAF
                            Button(
                                onClick = {
                                    openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Select Backup File (.json)")
                            }

                            // Selected file badge if loaded
                            selectedRestoreFileName?.let { fname ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Description, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            fname,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
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

                            // Optional Manual Paste Toggle
                            TextButton(
                                onClick = { showManualPaste = !showManualPaste },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(
                                    if (showManualPaste) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (showManualPaste) "Hide Manual JSON Input" else "Or Paste JSON Manually",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            AnimatedVisibility(visible = showManualPaste) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = importJsonText,
                                        onValueChange = {
                                            importJsonText = it
                                            restoreErrorMsg = null
                                            restoreSuccessMsg = null
                                        },
                                        label = { Text("Backup JSON Content") },
                                        placeholder = { Text("Paste {\"version\": \"3.2.0\", ...} here") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                val text = clipboardManager.getText()?.text ?: ""
                                                if (text.isNotBlank()) {
                                                    importJsonText = text
                                                    restoreErrorMsg = null
                                                    restoreSuccessMsg = null
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = null, Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Paste from Clipboard", style = MaterialTheme.typography.labelSmall)
                                        }

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
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = null, Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Apply", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
