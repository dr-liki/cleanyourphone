package com.drliki.cleanyourphone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drliki.cleanyourphone.engine.ShizukuStatus
import com.drliki.cleanyourphone.model.DevicePackage
import com.drliki.cleanyourphone.ui.components.ActionLogContent
import com.drliki.cleanyourphone.ui.components.PackageManagementContent
import com.drliki.cleanyourphone.ui.theme.TierRecommended
import com.drliki.cleanyourphone.viewmodel.ShizukuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuScreen(
    viewModel: ShizukuViewModel,
    onNavigateBack: () -> Unit
) {
    val packages by viewModel.packages.collectAsState()
    val status by viewModel.status.collectAsState()
    val actionLog by viewModel.actionLog.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarMsg by viewModel.snackbar.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showActionLogSheet by remember { mutableStateOf(false) }
    var pendingDisablePackages by remember { mutableStateOf<Set<String>?>(null) }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "This Device",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (status) {
                                            is ShizukuStatus.Ready -> TierRecommended
                                            is ShizukuStatus.PermissionNeeded -> Color(0xFFF59E0B)
                                            is ShizukuStatus.NotRunning -> MaterialTheme.colorScheme.outline
                                        }
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                status.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPackages() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { showActionLogSheet = true }) {
                        BadgedBox(
                            badge = {
                                val activeUndoCount = actionLog.count { !it.isUndone }
                                if (activeUndoCount > 0) {
                                    Badge { Text("$activeUndoCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.History, contentDescription = "Action Log")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Shizuku Status Banner
            when (status) {
                is ShizukuStatus.NotRunning -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Shizuku service is not running",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Start Shizuku on your phone via Wireless Debugging or PC ADB, then tap Refresh.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { viewModel.refreshStatus() }) {
                                Text("Check")
                            }
                        }
                    }
                }
                is ShizukuStatus.PermissionNeeded -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Permission Required",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Grant CleanYourPhone permission via Shizuku to disable packages.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.requestPermission() },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Grant")
                            }
                        }
                    }
                }
                is ShizukuStatus.Ready -> {
                    // All good — compact stats row
                    val disabledCount = packages.count { !it.isEnabled }
                    val recommendedCount = packages.count { it.isEnabled && it.tier == com.drliki.cleanyourphone.model.RemovalTier.RECOMMENDED }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total: ${packages.size} · Disabled: $disabledCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (recommendedCount > 0) {
                                Text(
                                    "$recommendedCount recommended to clean",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TierRecommended,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Package Management Content
            PackageManagementContent(
                packages = packages,
                isLoading = isLoading,
                onDisablePackages = { selected ->
                    pendingDisablePackages = selected
                },
                onEnablePackage = { pkgName ->
                    viewModel.enablePackage(pkgName)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Confirmation Dialog before disabling packages
    pendingDisablePackages?.let { selected ->
        val targetList = packages.filter { it.packageName in selected && !it.isSafeguardLocked }
        AlertDialog(
            onDismissRequest = { pendingDisablePackages = null },
            icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = TierRecommended) },
            title = {
                Text("Disable ${targetList.size} package(s)?")
            },
            text = {
                Column {
                    Text(
                        "Each package will be disabled using 'pm disable-user --user 0'. This preserves user data and can be undone at any time from the Action Log.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        targetList.take(5).joinToString("\n") { "• ${it.appLabel} (${it.packageName})" } +
                            if (targetList.size > 5) "\n...and ${targetList.size - 5} more" else "",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.disablePackages(selected)
                        pendingDisablePackages = null
                    }
                ) {
                    Text("Disable Selected")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDisablePackages = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Action Log Modal Bottom Sheet
    if (showActionLogSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionLogSheet = false }
        ) {
            ActionLogContent(
                entries = actionLog,
                onUndo = { entry -> viewModel.undoAction(entry) }
            )
        }
    }
}
