package com.drliki.cleanyourphone.ui.components

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.drliki.cleanyourphone.data.AppSettings
import com.drliki.cleanyourphone.data.UadRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    repository: UadRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    val scope = rememberCoroutineScope()

    var repoUrl by remember { mutableStateOf(settings.uadRepoUrl) }
    var autoUpdate by remember { mutableStateOf(settings.autoUpdateOnLaunch) }
    var isUpdating by remember { mutableStateOf(false) }
    var updateResultMsg by remember { mutableStateOf<String?>(null) }
    var lastUpdateText by remember {
        mutableStateOf(
            if (settings.lastUpdateTime > 0) {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(settings.lastUpdateTime))
            } else "Never"
        )
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider()

                // Section 1: Bloat List Repository Link
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Bloat List Repository",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Specify the raw JSON source for community debloat definitions (Universal Android Debloater format).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = repoUrl,
                        onValueChange = {
                            repoUrl = it
                            settings.uadRepoUrl = it
                        },
                        label = { Text("Repository URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                settings.resetRepoUrl()
                                repoUrl = settings.uadRepoUrl
                            }
                        ) {
                            Icon(Icons.Default.RestartAlt, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset to Default", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                HorizontalDivider()

                // Section 2: Automatic Updates Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Auto-Update on Launch",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Periodically check and refresh debloat definitions in background on startup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoUpdate,
                        onCheckedChange = {
                            autoUpdate = it
                            settings.autoUpdateOnLaunch = it
                        }
                    )
                }

                HorizontalDivider()

                // Section 3: Manual Update
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Database Status",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Last updated: $lastUpdateText\nCurrent catalog: ${repository.getCatalogSize()} definitions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    updateResultMsg?.let { msg ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                msg,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isUpdating = true
                                updateResultMsg = null
                                repository.updateFromGitHub(repoUrl)
                                    .onSuccess { count ->
                                        lastUpdateText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                        updateResultMsg = "Successfully loaded $count definitions!"
                                    }
                                    .onFailure {
                                        updateResultMsg = "Update failed: ${it.message}"
                                    }
                                isUpdating = false
                            }
                        },
                        enabled = !isUpdating,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Updating Catalog...")
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Update Bloat List Now")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}
