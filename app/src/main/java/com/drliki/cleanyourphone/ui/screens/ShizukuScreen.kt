package com.drliki.cleanyourphone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drliki.cleanyourphone.engine.ShizukuStatus
import com.drliki.cleanyourphone.model.RemovalTier
import com.drliki.cleanyourphone.ui.components.AboutDialog
import com.drliki.cleanyourphone.ui.components.BadgePill
import com.drliki.cleanyourphone.ui.components.PackageManagementContent
import com.drliki.cleanyourphone.ui.theme.TierRecommended
import com.drliki.cleanyourphone.viewmodel.ShizukuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuScreen(
    viewModel: ShizukuViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToActionLog: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val packages by viewModel.packages.collectAsState()
    val status by viewModel.status.collectAsState()
    val actionLog by viewModel.actionLog.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarMsg by viewModel.snackbar.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedNavTab by remember { mutableIntStateOf(if (status is ShizukuStatus.Ready) 1 else 0) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var pendingDisablePackages by remember { mutableStateOf<Set<String>?>(null) }

    val activeUndoCount = actionLog.count { !it.isUndone }

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
                    IconButton(onClick = {
                        viewModel.refreshStatus()
                        viewModel.loadPackages()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Update Debloat Lists") },
                                onClick = {
                                    showMenu = false
                                    viewModel.updateAllDebloatLists()
                                },
                                leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    showMenu = false
                                    onOpenSettings()
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("About") },
                                onClick = {
                                    showMenu = false
                                    showAboutDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedNavTab == 0,
                    onClick = { selectedNavTab = 0 },
                    icon = { Icon(Icons.Default.Dns, contentDescription = null) },
                    label = { Text("Status") }
                )
                NavigationBarItem(
                    selected = selectedNavTab == 1,
                    onClick = { selectedNavTab = 1 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                    label = { Text("Packages") }
                )
                NavigationBarItem(
                    selected = selectedNavTab == 2,
                    onClick = { selectedNavTab = 2 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeUndoCount > 0) {
                                    Badge { Text("$activeUndoCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.History, contentDescription = null)
                        }
                    },
                    label = { Text("Action Log") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedNavTab) {
                0 -> {
                    // ── Tab 1: Shizuku Service Connection Status & Refresh ──────
                    ShizukuStatusContent(
                        status = status,
                        packagesCount = packages.size,
                        disabledCount = packages.count { !it.isEnabled },
                        recommendedCount = packages.count { it.isEnabled && it.tier == RemovalTier.RECOMMENDED },
                        onRefreshStatus = {
                            viewModel.refreshStatus()
                            viewModel.loadPackages()
                        },
                        onRequestPermission = { viewModel.requestPermission() },
                        onGoToPackages = { selectedNavTab = 1 }
                    )
                }
                1 -> {
                    // ── Tab 2: App Packages ──────────────────────────────────────
                    PackageManagementContent(
                        packages = packages,
                        isLoading = isLoading,
                        onDisablePackages = { selected ->
                            pendingDisablePackages = selected
                        },
                        onEnablePackage = { pkgName ->
                            viewModel.enablePackage(pkgName)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                2 -> {
                    // ── Tab 3: Action Log ────────────────────────────────────────
                    ActionLogContent(
                        actionLog = actionLog,
                        onUndoAction = { viewModel.undoAction(it) },
                        onUndoMultipleActions = { list ->
                            list.forEach { viewModel.undoAction(it) }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
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

    // About Dialog
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun ShizukuStatusContent(
    status: ShizukuStatus,
    packagesCount: Int,
    disabledCount: Int,
    recommendedCount: Int,
    onRefreshStatus: () -> Unit,
    onRequestPermission: () -> Unit,
    onGoToPackages: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Service Status Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = when (status) {
                    is ShizukuStatus.Ready -> TierRecommended.copy(alpha = 0.08f)
                    is ShizukuStatus.PermissionNeeded -> Color(0xFFF59E0B).copy(alpha = 0.08f)
                    is ShizukuStatus.NotRunning -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                }
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (status) {
                            is ShizukuStatus.Ready -> TierRecommended.copy(alpha = 0.2f)
                            is ShizukuStatus.PermissionNeeded -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                            is ShizukuStatus.NotRunning -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when (status) {
                                    is ShizukuStatus.Ready -> Icons.Default.VerifiedUser
                                    is ShizukuStatus.PermissionNeeded -> Icons.Default.Security
                                    is ShizukuStatus.NotRunning -> Icons.Default.Warning
                                },
                                contentDescription = null,
                                tint = when (status) {
                                    is ShizukuStatus.Ready -> TierRecommended
                                    is ShizukuStatus.PermissionNeeded -> Color(0xFFF59E0B)
                                    is ShizukuStatus.NotRunning -> MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    BadgePill(
                        label = status.label,
                        color = when (status) {
                            is ShizukuStatus.Ready -> TierRecommended
                            is ShizukuStatus.PermissionNeeded -> Color(0xFFF59E0B)
                            is ShizukuStatus.NotRunning -> MaterialTheme.colorScheme.error
                        },
                        bgColor = when (status) {
                            is ShizukuStatus.Ready -> TierRecommended.copy(alpha = 0.15f)
                            is ShizukuStatus.PermissionNeeded -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            is ShizukuStatus.NotRunning -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = when (status) {
                        is ShizukuStatus.Ready -> "Shizuku Service Running"
                        is ShizukuStatus.PermissionNeeded -> "Permission Required"
                        is ShizukuStatus.NotRunning -> "Shizuku Service Not Running"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = when (status) {
                        is ShizukuStatus.Ready -> "Privileged binder connection is active and authorized. You can safely freeze and disable packages on this device without root."
                        is ShizukuStatus.PermissionNeeded -> "Shizuku is running in the background, but CleanYourPhone has not been granted debloater permission yet."
                        is ShizukuStatus.NotRunning -> "Shizuku is not running on this phone. Start the Shizuku app using Wireless Debugging or a PC command."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                when (status) {
                    is ShizukuStatus.PermissionNeeded -> {
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Grant Shizuku Permission")
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    is ShizukuStatus.NotRunning -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "How to start Shizuku:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "1. Open the Shizuku app\n2. Tap 'Start via Wireless Debugging' (or connect via PC)\n3. Tap 'Refresh Service Status' below once started",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    is ShizukuStatus.Ready -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Binder Access", style = MaterialTheme.typography.bodySmall)
                                Text("Granted (UID 2000)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TierRecommended)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // Primary Refresh Button
                FilledTonalButton(
                    onClick = onRefreshStatus,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh Service Status")
                }
            }
        }

        // Summary Statistics Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Device App Overview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBox(label = "Total Apps", value = "$packagesCount", color = MaterialTheme.colorScheme.primary)
                    StatBox(label = "Disabled", value = "$disabledCount", color = MaterialTheme.colorScheme.secondary)
                    StatBox(label = "Recommended", value = "$recommendedCount", color = TierRecommended)
                }
            }
        }

        // Action: Jump to Packages
        Button(
            onClick = onGoToPackages,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("View & Manage Packages", fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null, Modifier.size(18.dp))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RowScope.StatBox(label: String, value: String, color: Color) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
