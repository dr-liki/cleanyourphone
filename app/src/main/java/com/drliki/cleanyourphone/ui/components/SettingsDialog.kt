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

    // UAD Settings State
    var repoUrl by remember { mutableStateOf(settings.uadRepoUrl) }
    var autoUpdate by remember { mutableStateOf(settings.autoUpdateOnLaunch) }
    var isUpdatingUad by remember { mutableStateOf(false) }
    var updateResultMsg by remember { mutableStateOf<String?>(null) }
    var lastUpdateText by remember {
        mutableStateOf(
            if (settings.lastUpdateTime > 0) {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(settings.lastUpdateTime))
            } else "Never"
        )
    }

    // CleanYourTV Settings State
    var tvRepoUrl by remember { mutableStateOf(settings.tvRepoUrl) }
    var autoUpdateTv by remember { mutableStateOf(settings.autoUpdateTvOnLaunch) }
    var isUpdatingTv by remember { mutableStateOf(false) }
    var updateTvResultMsg by remember { mutableStateOf<String?>(null) }
    var lastTvUpdateText by remember {
        mutableStateOf(
            if (settings.lastTvUpdateTime > 0) {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(settings.lastTvUpdateTime))
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
                        "Settings & Repositories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider()

                // Section 1: Phone Debloat List (Universal Android Debloater)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Smartphone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Phone Debloat List (UAD-ng)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Community debloat catalog for phones & tablets from Universal Android Debloater.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = repoUrl,
                        onValueChange = {
                            repoUrl = it
                            settings.uadRepoUrl = it
                        },
                        label = { Text("UAD Repository URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                settings.resetRepoUrl()
                                repoUrl = settings.uadRepoUrl
                            }
                        ) {
                            Icon(Icons.Default.RestartAlt, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Auto-update", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(6.dp))
                            Switch(
                                checked = autoUpdate,
                                onCheckedChange = {
                                    autoUpdate = it
                                    settings.autoUpdateOnLaunch = it
                                }
                            )
                        }
                    }

                    Text(
                        "Last updated: $lastUpdateText · ${repository.getCatalogSize()} definitions",
                        style = MaterialTheme.typography.labelSmall,
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
                                isUpdatingUad = true
                                updateResultMsg = null
                                repository.updateFromGitHub(repoUrl)
                                    .onSuccess { count ->
                                        lastUpdateText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                        updateResultMsg = "Successfully loaded $count UAD definitions!"
                                    }
                                    .onFailure {
                                        updateResultMsg = "Update failed: ${it.message}"
                                    }
                                isUpdatingUad = false
                            }
                        },
                        enabled = !isUpdatingUad,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isUpdatingUad) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Updating UAD List...")
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Update Phone List Now")
                        }
                    }
                }

                HorizontalDivider()

                // Section 2: Android TV / Google TV Debloat List (CleanYourTV)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tv, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "TV Debloat List (CleanYourTV)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Debloat definitions specifically tailored for Android TV & Google TV from github.com/gathin23/cleanyourtv.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = tvRepoUrl,
                        onValueChange = {
                            tvRepoUrl = it
                            settings.tvRepoUrl = it
                        },
                        label = { Text("CleanYourTV Repository URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                settings.resetTvRepoUrl()
                                tvRepoUrl = settings.tvRepoUrl
                            }
                        ) {
                            Icon(Icons.Default.RestartAlt, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Auto-update", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(6.dp))
                            Switch(
                                checked = autoUpdateTv,
                                onCheckedChange = {
                                    autoUpdateTv = it
                                    settings.autoUpdateTvOnLaunch = it
                                }
                            )
                        }
                    }

                    Text(
                        "Last updated: $lastTvUpdateText · ${repository.getTvCatalogSize()} TV definitions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    updateTvResultMsg?.let { msg ->
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
                                isUpdatingTv = true
                                updateTvResultMsg = null
                                repository.updateTvFromGitHub(tvRepoUrl)
                                    .onSuccess { count ->
                                        lastTvUpdateText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                        updateTvResultMsg = "Successfully loaded $count CleanYourTV definitions!"
                                    }
                                    .onFailure {
                                        updateTvResultMsg = "Update failed: ${it.message}"
                                    }
                                isUpdatingTv = false
                            }
                        },
                        enabled = !isUpdatingTv,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isUpdatingTv) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Updating TV List...")
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Update TV Debloat List Now")
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
