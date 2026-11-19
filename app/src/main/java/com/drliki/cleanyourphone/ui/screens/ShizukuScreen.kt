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
import com.drliki.cleanyourphone.model.RemoteDeviceInfo
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
    onNavigateToActionLog: () -> Unit = {},
    onOpenSettings: () -> Unit
) {
    val status by viewModel.status.collectAsState()
    val packages by viewModel.packages.collectAsState()
    val actionLog by viewModel.actionLog.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarMsg by viewModel.snackbar.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()

    // Persistent filter states
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val selectedTier by viewModel.selectedTier.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()
    val isTailoredFilter by viewModel.isTailoredFilter.collectAsState()

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
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            placeholder = { Text("Search apps by name or ID…") },
                            leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                        Icon(Icons.Default.Clear, "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
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
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            viewModel.isSearchActive.value = false
                            viewModel.searchQuery.value = ""
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            if (isSearchActive) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = if (isSearchActive) "Close search" else "Back"
                        )
                    }
                },
                actions = {
                    // Search Button beside menu
                    IconButton(onClick = {
                        val willBeActive = !isSearchActive
                        viewModel.isSearchActive.value = willBeActive
                        if (!willBeActive) viewModel.searchQuery.value = ""
                        if (willBeActive && selectedNavTab == 0) {
                            selectedNavTab = 1 // Switch to Packages when starting search
                        }
                    }) {
                        Icon(
                            if (isSearchActive) Icons.Default.SearchOff else Icons.Default.Search,
                            contentDescription = "Search"
                        )
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
                    // ── Tab 1: Shizuku Service Status & Device Information ──────
                    ShizukuStatusContent(
                        status = status,
                        deviceInfo = deviceInfo,
                        packagesCount = packages.size,
                        systemCount = packages.count { it.isSystemApp },
                        userCount = packages.count { !it.isSystemApp },
                        disabledCount = packages.count { !it.isEnabled },
                        recommendedCount = packages.count { it.isEnabled && it.tier == RemovalTier.RECOMMENDED },
                        tailoredCount = packages.count { it.isEnabled && it.isTailored },
                        onRefreshStatus = {
                            viewModel.refreshStatus()
                            viewModel.loadPackages()
                        },
                        onRequestPermission = { viewModel.requestPermission() },
                        onGoToPackages = { selectedNavTab = 1 }
                    )
                }
                1 -> {
                    // ── Tab 2: App Packages (with hoisted persistent filters) ────
                    PackageManagementContent(
                        packages = packages,
                        isLoading = isLoading,
                        onDisablePackages = { selected ->
                            pendingDisablePackages = selected
                        },
                        onEnablePackage = { pkgName ->
                            viewModel.enablePackage(pkgName)
                        },
                        searchQuery = searchQuery,
                        selectedTypeFilter = selectedTypeFilter,
                        onTypeFilterChange = { viewModel.selectedTypeFilter.value = it },
                        selectedTier = selectedTier,
                        onTierChange = { viewModel.selectedTier.value = it },
                        selectedSort = selectedSort,
                        onSortChange = { viewModel.selectedSort.value = it },
                        isTailoredFilter = isTailoredFilter,
                        onTailoredFilterChange = { viewModel.isTailoredFilter.value = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                2 -> {
                    // ── Tab 3: Action Log ────────────────────────────────────────
                    ActionLogContent(
                        actionLog = actionLog,
                        searchQuery = searchQuery,
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
                    Text("Disable Selected (${targetList.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDisablePackages = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun ShizukuStatusContent(
    status: ShizukuStatus,
    deviceInfo: RemoteDeviceInfo,
    packagesCount: Int,
    systemCount: Int,
    userCount: Int,
    disabledCount: Int,
    recommendedCount: Int,
    tailoredCount: Int,
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

        // ── Device & OS Specifications Card ─────────────────────────────────
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Device Specifications & OS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                ShizukuSpecRow("Brand / OEM", deviceInfo.brand.ifBlank { deviceInfo.manufacturer })
                ShizukuSpecRow("Model", deviceInfo.model)
                ShizukuSpecRow("Device Codename", deviceInfo.deviceName)
                ShizukuSpecRow("Android Version", "Android ${deviceInfo.androidVersion} (API ${deviceInfo.sdkVersion})")
                if (deviceInfo.securityPatch.isNotBlank() && deviceInfo.securityPatch != "Unknown") {
                    ShizukuSpecRow("Security Patch", deviceInfo.securityPatch)
                }
                ShizukuSpecRow("Display", "${deviceInfo.displayResolution} (${deviceInfo.displayDensity})")
                ShizukuSpecRow("CPU Architecture", deviceInfo.cpuAbi)
            }
        }

        // ── Summary Statistics Card ─────────────────────────────────────────
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Device App Breakdown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(label = "Total Apps", value = "$packagesCount", color = MaterialTheme.colorScheme.primary)
                    StatBox(label = "System Apps", value = "$systemCount", color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(label = "User Apps", value = "$userCount", color = MaterialTheme.colorScheme.tertiary)
                    StatBox(label = "Disabled", value = "$disabledCount", color = Color(0xFFF59E0B))
                }

                if (recommendedCount > 0) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TierRecommended.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = TierRecommended, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "$recommendedCount bloat package(s) safe & recommended to disable.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TierRecommended,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (tailoredCount > 0) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "$tailoredCount package(s) specifically tailored for ${deviceInfo.brand}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
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
private fun ShizukuSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (label.contains("Codename") || label.contains("API")) FontFamily.Monospace else FontFamily.Default
        )
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
