package com.drliki.cleanyourphone.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.drliki.cleanyourphone.model.DiscoveredDevice
import com.drliki.cleanyourphone.model.RemovalTier
import com.drliki.cleanyourphone.model.RemoteDeviceInfo
import com.drliki.cleanyourphone.model.SavedDeviceProfile
import com.drliki.cleanyourphone.ui.components.AboutDialog
import com.drliki.cleanyourphone.ui.components.BadgePill
import com.drliki.cleanyourphone.ui.components.PackageManagementContent
import com.drliki.cleanyourphone.ui.components.PairingDialog
import com.drliki.cleanyourphone.ui.components.TopHeaderSearchBar
import com.drliki.cleanyourphone.ui.theme.TierRecommended
import com.drliki.cleanyourphone.ui.theme.TierUnsafe
import com.drliki.cleanyourphone.viewmodel.WirelessViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WirelessScreen(
    viewModel: WirelessViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToActionLog: (String?) -> Unit = {},
    onOpenSettings: () -> Unit
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val isAndroidTv by viewModel.isAndroidTv.collectAsState()
    val connectedName by viewModel.connectedName.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val savedProfiles by viewModel.savedProfiles.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val customScanPort by viewModel.customScanPort.collectAsState()
    val packages by viewModel.packages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val actionLog by viewModel.actionLog.collectAsState()
    val snackbarMsg by viewModel.snackbar.collectAsState()
    val pairingQrPayload by viewModel.pairingQrPayload.collectAsState()
    val pairingCode by viewModel.pairingCode.collectAsState()
    val discoveredPairingService by viewModel.discoveredPairingService.collectAsState()

    // Persistent filter states
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val selectedTier by viewModel.selectedTier.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()
    val isTailoredFilter by viewModel.isTailoredFilter.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedNavTab by remember { mutableIntStateOf(if (isConnected) 1 else 0) }
    var connectionSubTab by remember { mutableIntStateOf(0) }
    var showPairingDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var pendingDisablePackages by remember { mutableStateOf<Set<String>?>(null) }

    // Dynamic scan port input
    var portInput by remember { mutableStateOf(customScanPort.toString()) }

    val activeUndoCount = actionLog.count { !it.isUndone }

    // If connection status changes to connected, switch to Packages tab automatically
    LaunchedEffect(isConnected) {
        if (isConnected && selectedNavTab == 0) {
            selectedNavTab = 1
        }
    }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TopHeaderSearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.searchQuery.value = it },
                            onCloseSearch = {
                                viewModel.isSearchActive.value = false
                                viewModel.searchQuery.value = ""
                            },
                            placeholder = "Search apps by name or ID…"
                        )
                    } else {
                        Column {
                            Text(
                                if (isConnected) connectedName.ifBlank { "Remote Device" } else "Wireless ADB",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isConnected) TierRecommended else MaterialTheme.colorScheme.outline)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (isConnected) {
                                        if (isAndroidTv) "Connected · Android/Google TV" else "Connected over Wi-Fi"
                                    } else "mDNS Active · Ready",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (!isSearchActive) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        // Search button beside menu
                        IconButton(onClick = {
                            viewModel.isSearchActive.value = true
                            if (isConnected && selectedNavTab == 0) {
                                selectedNavTab = 1
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
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
                }
            },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (isConnected) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedNavTab == 0,
                        onClick = { selectedNavTab = 0 },
                        icon = {
                            Icon(
                                if (isAndroidTv) Icons.Default.Tv else Icons.Default.Phonelink,
                                contentDescription = null
                            )
                        },
                        label = { Text("Device") }
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedNavTab) {
                0 -> {
                    // ── Tab 0: Device Connection Info / Connection Management ──
                    if (isConnected) {
                        ConnectedDeviceInfoContent(
                            connectedName = connectedName,
                            isAndroidTv = isAndroidTv,
                            deviceInfo = deviceInfo,
                            packagesCount = packages.size,
                            systemCount = packages.count { it.isSystemApp },
                            userCount = packages.count { !it.isSystemApp },
                            disabledCount = packages.count { !it.isEnabled },
                            recommendedCount = packages.count { it.isEnabled && it.tier == RemovalTier.RECOMMENDED },
                            tailoredCount = packages.count { it.isEnabled && it.isTailored },
                            tvCatalogSize = viewModel.repo.getTvCatalogSize(),
                            onDisconnect = {
                                viewModel.disconnect()
                                selectedNavTab = 0
                            },
                            onManagePackages = { selectedNavTab = 1 }
                        )
                    } else {
                        // Disconnected: Show Sub-tabs (Saved Profiles & Scan Network)
                        Column(modifier = Modifier.fillMaxSize()) {
                            TabRow(
                                selectedTabIndex = connectionSubTab,
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                Tab(
                                    selected = connectionSubTab == 0,
                                    onClick = { connectionSubTab = 0 },
                                    text = { Text("Saved Profiles") },
                                    icon = { Icon(Icons.Default.Devices, contentDescription = null, Modifier.size(18.dp)) }
                                )
                                Tab(
                                    selected = connectionSubTab == 1,
                                    onClick = { connectionSubTab = 1 },
                                    text = { Text("Scan Network") },
                                    icon = { Icon(Icons.Default.WifiFind, contentDescription = null, Modifier.size(18.dp)) }
                                )
                            }

                            when (connectionSubTab) {
                                0 -> {
                                    // ── Sub-Tab 0: Saved Profiles (No pairing banner here!) ──
                                    SavedProfilesContent(
                                        profiles = savedProfiles,
                                        availableDevices = devices,
                                        isLoading = isLoading,
                                        onConnect = { viewModel.connectToProfile(it) },
                                        onDelete = { viewModel.deleteProfile(it.id) },
                                        onOpenActionLog = { devName ->
                                            onNavigateToActionLog(devName)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                1 -> {
                                    // ── Sub-Tab 1: Scan Network (PAIRING BANNER ONLY HERE) ──
                                    Column(modifier = Modifier.weight(1f)) {
                                        // Discovered pairing notification banner ONLY in Scan Network page
                                        discoveredPairingService?.let { pairing ->
                                            Surface(
                                                color = TierRecommended.copy(alpha = 0.12f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Icon(Icons.Default.CheckCircle, null, tint = TierRecommended, modifier = Modifier.size(18.dp))
                                                        Spacer(Modifier.width(8.dp))
                                                        Column {
                                                            Text(
                                                                pairing.serviceName.ifBlank { "Wireless Debugging Device" },
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                "${pairing.host}:${pairing.port}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = TierRecommended
                                                            )
                                                        }
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Button(
                                                            onClick = { showPairingDialog = true },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Text("Pair Now", style = MaterialTheme.typography.labelSmall)
                                                        }
                                                        Spacer(Modifier.width(4.dp))
                                                        IconButton(
                                                            onClick = { viewModel.dismissPairingService() },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp)
                                        ) {
                                            ElevatedCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text(
                                                        "Dynamic Network Discovery",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        "mDNS continuously listens for Wireless Debugging broadcasts. You can also probe a specific dynamic port across your local Wi-Fi subnet.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )

                                                    Spacer(Modifier.height(12.dp))

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        // Box 1: Port Input (Uniform 64dp Size)
                                                        OutlinedTextField(
                                                            value = portInput,
                                                            onValueChange = {
                                                                portInput = it
                                                                it.toIntOrNull()?.let { p -> viewModel.setCustomScanPort(p) }
                                                            },
                                                            label = { Text("Port", maxLines = 1) },
                                                            placeholder = { Text("5555") },
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(64.dp),
                                                            singleLine = true,
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            shape = RoundedCornerShape(12.dp)
                                                        )

                                                        // Box 2: Scan Subnet (Uniform 64dp Size)
                                                        Button(
                                                            onClick = {
                                                                val p = portInput.toIntOrNull() ?: 5555
                                                                viewModel.startScan(p)
                                                            },
                                                            enabled = !isScanning,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(64.dp),
                                                            shape = RoundedCornerShape(12.dp),
                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            if (isScanning) {
                                                                CircularProgressIndicator(
                                                                    modifier = Modifier.size(18.dp),
                                                                    strokeWidth = 2.dp,
                                                                    color = MaterialTheme.colorScheme.onPrimary
                                                                )
                                                            } else {
                                                                Column(
                                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                                    verticalArrangement = Arrangement.Center
                                                                ) {
                                                                    Icon(Icons.Default.WifiFind, contentDescription = null, Modifier.size(18.dp))
                                                                    Spacer(Modifier.height(2.dp))
                                                                    Text("Scan Subnet", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                                                }
                                                            }
                                                        }

                                                        // Box 3: Pair (Uniform 64dp Size)
                                                        OutlinedButton(
                                                            onClick = { showPairingDialog = true },
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(64.dp),
                                                            shape = RoundedCornerShape(12.dp),
                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Column(
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                verticalArrangement = Arrangement.Center
                                                            ) {
                                                                Icon(Icons.Default.QrCode2, contentDescription = null, Modifier.size(18.dp))
                                                                Spacer(Modifier.height(2.dp))
                                                                Text("Pair Device", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                                            }
                                                        }
                                                    }

                                                    AnimatedVisibility(visible = isScanning) {
                                                        Column(Modifier.padding(top = 12.dp)) {
                                                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                                            Spacer(Modifier.height(6.dp))
                                                            Text(
                                                                "Probing 254 subnet hosts on port ${portInput.ifBlank { "5555" }}...",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.height(16.dp))

                                            // Results or Guide
                                            if (devices.isNotEmpty()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "Discovered Devices (${devices.size}):",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        TextButton(onClick = { viewModel.clearDiscoveredDevices() }) {
                                                            Text("Clear")
                                                        }
                                                        TextButton(onClick = { viewModel.startScan() }) {
                                                            Text("Re-scan")
                                                        }
                                                    }
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                LazyColumn(
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    items(devices, key = { "${it.host}:${it.port}" }) { device ->
                                                        DiscoveredDeviceCard(
                                                            device = device,
                                                            isLoading = isLoading,
                                                            onConnect = { viewModel.connectTo(device) }
                                                        )
                                                    }
                                                }
                                            } else if (!isScanning) {
                                                GuideCard(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // ── Tab 1: Packages ──────────────────────────────────────────
                    if (isConnected) {
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
                    } else {
                        // Empty state prompting connection
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Phonelink,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No Device Connected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Connect to a remote phone or Android/Google TV in the Device tab to scan, inspect, and disable packages.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(Modifier.height(20.dp))
                                Button(
                                    onClick = { selectedNavTab = 0 },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.WifiTethering, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Go to Device Connection")
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // ── Tab 2: Action Log (Only Connected Device) ───────────────
                    ActionLogContent(
                        actionLog = actionLog,
                        searchQuery = searchQuery,
                        fixedDevice = connectedName,
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
                        "Each package will be disabled on the remote device using 'pm disable-user --user 0'. This preserves user data and can be undone at any time.",
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

    // Pairing Dialog
    if (showPairingDialog) {
        val detectedPairing = discoveredPairingService
        val matchingPort = detectedPairing?.host?.let { viewModel.getMatchingConnectPort(it) } ?: 0
        PairingDialog(
            qrPayload = pairingQrPayload,
            pairingCode = pairingCode,
            discoveredHost = detectedPairing?.host ?: "",
            discoveredPairingPort = detectedPairing?.port ?: 0,
            discoveredConnectPort = matchingPort,
            discoveredDeviceName = detectedPairing?.serviceName ?: "",
            onRegenerateQr = { viewModel.generateNewPairingCredentials() },
            onPairWithCode = { host, pairingPort, code, connectPort ->
                viewModel.pairWithCode(host, pairingPort, code, connectPort)
                showPairingDialog = false
            },
            onDirectConnect = { host, port ->
                viewModel.connectManual(host, port)
                showPairingDialog = false
            },
            onDismiss = { showPairingDialog = false }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun ConnectedDeviceInfoContent(
    connectedName: String,
    isAndroidTv: Boolean,
    deviceInfo: RemoteDeviceInfo?,
    packagesCount: Int,
    systemCount: Int,
    userCount: Int,
    disabledCount: Int,
    recommendedCount: Int,
    tailoredCount: Int = 0,
    tvCatalogSize: Int,
    onDisconnect: () -> Unit,
    onManagePackages: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Device Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = TierRecommended.copy(alpha = 0.08f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TierRecommended.copy(alpha = 0.2f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isAndroidTv) Icons.Default.Tv else Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = TierRecommended,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BadgePill(
                            label = if (isAndroidTv) "Android TV" else "Android Phone",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            bgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        )
                        BadgePill(
                            label = "Connected",
                            color = TierRecommended,
                            bgColor = TierRecommended.copy(alpha = 0.15f)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = connectedName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (deviceInfo != null && deviceInfo.host.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Wireless ADB · ${deviceInfo.host}:${deviceInfo.port}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isAndroidTv) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "CleanYourTV Debloat Repository Active",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "Using curated debloat definitions from cleanyourtv ($tvCatalogSize packages across 492 TV models).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Disconnect Button (Prominent & Clear)
                FilledTonalButton(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.LinkOff, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Disconnect Device", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Detailed Hardware & OS Specifications Card
        if (deviceInfo != null) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PermDeviceInformation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Device Specifications",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    SpecRow(label = "Manufacturer / Brand", value = "${deviceInfo.manufacturer} · ${deviceInfo.brand}")
                    SpecRow(label = "Model Name", value = deviceInfo.model)
                    if (deviceInfo.deviceName.isNotBlank()) {
                        SpecRow(label = "Device Codename", value = deviceInfo.deviceName)
                    }
                    SpecRow(
                        label = "Android Version",
                        value = if (deviceInfo.sdkVersion.isNotBlank()) "Android ${deviceInfo.androidVersion} (API ${deviceInfo.sdkVersion})" else deviceInfo.androidVersion
                    )
                    if (deviceInfo.securityPatch.isNotBlank()) {
                        SpecRow(label = "Security Patch Level", value = deviceInfo.securityPatch)
                    }
                    if (deviceInfo.displayResolution.isNotBlank()) {
                        val densityStr = if (deviceInfo.displayDensity.isNotBlank()) " · ${deviceInfo.displayDensity} dpi" else ""
                        SpecRow(label = "Display Resolution", value = "${deviceInfo.displayResolution}$densityStr")
                    }
                    if (deviceInfo.cpuAbi.isNotBlank()) {
                        SpecRow(label = "CPU Architecture (ABI)", value = deviceInfo.cpuAbi)
                    }
                }
            }
        }

        // Installed Packages Categorization Analytics Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Installed Packages Categorization",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    "Queried directly over ADB via Android pm flags (-s, -3, -d)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                "$tailoredCount brand specific package(s) for ${deviceInfo?.brand ?: "this device"}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
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
            fontFamily = FontFamily.Monospace
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

@Composable
private fun SavedProfilesContent(
    profiles: List<SavedDeviceProfile>,
    availableDevices: List<DiscoveredDevice> = emptyList(),
    isLoading: Boolean,
    onConnect: (SavedDeviceProfile) -> Unit,
    onDelete: (SavedDeviceProfile) -> Unit,
    onOpenActionLog: (String) -> Unit = {},
    onNewPairing: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (profiles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Smartphone,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "No Remembered Devices Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Once you connect to or pair with another phone or TV over Wireless Debugging, it will be remembered here for instant one-tap reconnection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onNewPairing, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pair New Device")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(profiles, key = { it.id }) { profile ->
                val isAvailable = availableDevices.any { it.host == profile.host }
                SavedDeviceProfileCard(
                    profile = profile,
                    isAvailable = isAvailable,
                    isLoading = isLoading,
                    onConnect = { onConnect(profile) },
                    onDelete = { onDelete(profile) },
                    onOpenActionLog = { onOpenActionLog(profile.name) }
                )
            }
        }
    }
}

@Composable
private fun SavedDeviceProfileCard(
    profile: SavedDeviceProfile,
    isAvailable: Boolean = false,
    isLoading: Boolean,
    onConnect: () -> Unit,
    onDelete: () -> Unit,
    onOpenActionLog: () -> Unit
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = onOpenActionLog,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (profile.isAndroidTv) Icons.Default.Tv else Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${profile.host}:${profile.port}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (isAvailable) {
                    BadgePill(
                        label = "Available",
                        color = TierRecommended,
                        bgColor = TierRecommended.copy(alpha = 0.15f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Profile metadata
            val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(profile.lastConnected))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (profile.isAndroidTv) {
                    BadgePill(
                        label = "Android TV",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        bgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    )
                }
                BadgePill(
                    label = "${profile.disabledPackages.size} disabled",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    bgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                )
                BadgePill(
                    label = "Seen $dateStr",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    bgColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            // Connect & Delete Buttons side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onConnect,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Connect Device", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Saved Profile?") },
            text = {
                Text(
                    "Are you sure you want to delete profile \"${profile.name}\" (${profile.host}:${profile.port})? This will remove the remembered connection.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DiscoveredDeviceCard(
    device: DiscoveredDevice,
    isLoading: Boolean,
    onConnect: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        device.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${device.host}:${device.port}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onConnect,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Link, contentDescription = null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Connect to Device")
            }
        }
    }
}

@Composable
private fun GuideCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Setup Guide: Other Phone / TV",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            val steps = listOf(
                "1. Connect both devices to the same Wi-Fi network.",
                "2. On target device, go to Settings > Developer Options.",
                "3. Turn ON 'Wireless debugging'. Note the IP address and dynamic port shown.",
                "4. If not paired yet, tap 'Pair device with pairing code' (the app will auto-discover the IP & port!).",
                "5. Tap 'Scan Subnet' or use 'Saved Profiles' to reconnect instantly."
            )

            steps.forEach { step ->
                Text(
                    step,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
