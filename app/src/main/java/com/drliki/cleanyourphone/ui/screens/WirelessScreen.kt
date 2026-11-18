package com.drliki.cleanyourphone.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.drliki.cleanyourphone.ui.components.AboutDialog
import com.drliki.cleanyourphone.ui.components.ActionLogContent
import com.drliki.cleanyourphone.ui.components.PackageManagementContent
import com.drliki.cleanyourphone.ui.components.PairingDialog
import com.drliki.cleanyourphone.ui.theme.TierRecommended
import com.drliki.cleanyourphone.ui.theme.TierUnsafe
import com.drliki.cleanyourphone.viewmodel.WirelessViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WirelessScreen(
    viewModel: WirelessViewModel,
    onNavigateBack: () -> Unit
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val connectedName by viewModel.connectedName.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val customScanPort by viewModel.customScanPort.collectAsState()
    val packages by viewModel.packages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val actionLog by viewModel.actionLog.collectAsState()
    val snackbarMsg by viewModel.snackbar.collectAsState()
    val pairingQrPayload by viewModel.pairingQrPayload.collectAsState()
    val pairingCode by viewModel.pairingCode.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showActionLogSheet by remember { mutableStateOf(false) }
    var showPairingDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var pendingDisablePackages by remember { mutableStateOf<Set<String>?>(null) }

    // Manual connection inputs
    var manualIp by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("5555") }

    // Dynamic scan port input
    var portInput by remember { mutableStateOf(customScanPort.toString()) }

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
                    Column {
                        Text(
                            if (isConnected) connectedName.ifBlank { "Remote Phone" } else "Wireless ADB",
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
                                if (isConnected) "Connected over Wi-Fi" else "mDNS Active · Not Connected",
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
                    if (isConnected) {
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
                        IconButton(onClick = { viewModel.disconnect() }) {
                            Icon(Icons.Default.LinkOff, contentDescription = "Disconnect", tint = TierUnsafe)
                        }
                    } else {
                        IconButton(onClick = { showPairingDialog = true }) {
                            Icon(Icons.Default.QrCode2, contentDescription = "Pair Device")
                        }
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
                                text = { Text("Pair Device (QR/PIN)") },
                                onClick = {
                                    showMenu = false
                                    showPairingDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Update UAD Lists") },
                                onClick = {
                                    showMenu = false
                                    viewModel.updateUadLists()
                                },
                                leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null) }
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isConnected) {
                // Connection Tabs: Scan vs Manual
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Scan Network") },
                        icon = { Icon(Icons.Default.WifiFind, contentDescription = null, Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Manual Connect") },
                        icon = { Icon(Icons.Default.SettingsEthernet, contentDescription = null, Modifier.size(18.dp)) }
                    )
                }

                if (selectedTab == 0) {
                    // ── Network Scanner Tab ──────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Scan trigger banner
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
                                    OutlinedTextField(
                                        value = portInput,
                                        onValueChange = {
                                            portInput = it
                                            it.toIntOrNull()?.let { p -> viewModel.setCustomScanPort(p) }
                                        },
                                        label = { Text("Port") },
                                        placeholder = { Text("5555") },
                                        modifier = Modifier.width(110.dp),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Button(
                                        onClick = {
                                            val p = portInput.toIntOrNull() ?: 5555
                                            viewModel.startScan(p)
                                        },
                                        enabled = !isScanning,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        if (isScanning) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Icon(Icons.Default.Search, contentDescription = null, Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Scan Subnet")
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { showPairingDialog = true },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.QrCode2, contentDescription = null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Pair")
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

                        // Results or Setup Guide
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
                                TextButton(onClick = { viewModel.startScan() }) {
                                    Text("Re-scan")
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
                } else {
                    // ── Manual Connect Tab ───────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Connect by IP & Dynamic Port",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Enter the IP address and dynamic port shown under Settings > Developer Options > Wireless debugging on the target phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            label = { Text("Target Phone IP Address") },
                            placeholder = { Text("e.g. 192.168.1.105") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = manualPort,
                            onValueChange = { manualPort = it },
                            label = { Text("Port (shown on Wireless debugging screen)") },
                            placeholder = { Text("e.g. 41255 or 5555") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                val p = manualPort.toIntOrNull() ?: 5555
                                viewModel.connectManual(manualIp.trim(), p)
                            },
                            enabled = manualIp.isNotBlank() && !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Connecting...")
                            } else {
                                Icon(Icons.Default.Link, contentDescription = null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Connect to Remote Phone")
                            }
                        }

                        OutlinedButton(
                            onClick = { showPairingDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Pin, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pair with QR or Pairing Code First")
                        }

                        Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                // ── CONNECTED: Remote Package Manager ────────────────────────
                Surface(
                    color = TierRecommended.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = TierRecommended,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Connected to $connectedName",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Managing remote packages over Wireless ADB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.disconnect() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Disconnect", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

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
    }

    // Pairing Dialog (QR and PIN)
    if (showPairingDialog) {
        PairingDialog(
            qrPayload = pairingQrPayload,
            pairingCode = pairingCode,
            onRegenerateQr = { viewModel.generateNewPairingCredentials() },
            onPairWithCode = { h, pPort, code, cPort ->
                viewModel.pairWithCode(h, pPort, code, cPort)
            },
            onDismiss = { showPairingDialog = false }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    // Confirmation Dialog before disabling packages on remote phone
    pendingDisablePackages?.let { selected ->
        val targetList = packages.filter { it.packageName in selected && !it.isSafeguardLocked }
        AlertDialog(
            onDismissRequest = { pendingDisablePackages = null },
            icon = { Icon(Icons.Default.WifiTethering, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text("Disable ${targetList.size} package(s) on $connectedName?")
            },
            text = {
                Column {
                    Text(
                        "Commands will be sent over Wireless ADB to the connected phone using 'pm disable-user --user 0'. Safe, reversible, and logged.",
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
                    Text("Apply to Remote Phone")
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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

            Button(
                onClick = onConnect,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Connect")
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
                    "Setup Guide: Other Phone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            val steps = listOf(
                "1. Connect both phones to the same Wi-Fi network.",
                "2. On the other phone, go to Settings > Developer Options.",
                "3. Turn ON 'Wireless debugging'. Note the IP address and dynamic port shown.",
                "4. If not paired yet, tap 'Pair device with QR code' or 'Pair device with pairing code' (use the 'Pair' button above).",
                "5. Tap 'Scan Subnet' to auto-discover it, or use the 'Manual Connect' tab to connect directly."
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
