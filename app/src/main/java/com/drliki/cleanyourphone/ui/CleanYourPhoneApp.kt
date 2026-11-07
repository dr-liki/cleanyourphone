package com.drliki.cleanyourphone.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drliki.cleanyourphone.data.UadRepository
import com.drliki.cleanyourphone.engine.PrivilegeEngine
import com.drliki.cleanyourphone.model.*
import com.drliki.cleanyourphone.ui.theme.*
import kotlinx.coroutines.launch

enum class TvSlimTab(val title: String) {
    PHONE("Phone"),
    PACKAGES("Packages"),
    APPS("Apps"),
    MEMORY("Memory"),
    LOG("Log")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanYourPhoneApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { UadRepository(context) }
    val engine = remember { PrivilegeEngine() }

    var activeTab by remember { mutableStateOf(TvSlimTab.PACKAGES) }
    var packages by remember { mutableStateOf<List<PhonePackageItem>>(emptyList()) }
    var selectedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTier by remember { mutableStateOf<RemovalTier?>(RemovalTier.RECOMMENDED) }
    var selectedCategory by remember { mutableStateOf(PackageListCategory.ALL) }
    var actionLog by remember { mutableStateOf<List<ActionLogEntry>>(emptyList()) }
    var privilegeMode by remember { mutableStateOf(engine.detectPrivilegeMode()) }
    var showConfirmModal by remember { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val catalog = repository.loadCatalogFromAssets()
        packages = repository.scanDevicePackages(catalog)
    }

    val filteredPackages = remember(packages, searchQuery, selectedTier, selectedCategory) {
        packages.filter { pkg ->
            val matchesQuery = searchQuery.isBlank() ||
                pkg.packageName.contains(searchQuery, ignoreCase = true) ||
                pkg.appLabel.contains(searchQuery, ignoreCase = true) ||
                pkg.description.contains(searchQuery, ignoreCase = true)
            val matchesTier = selectedTier == null || pkg.removalTier == selectedTier
            val matchesCat = selectedCategory == PackageListCategory.ALL || pkg.listCategory == selectedCategory
            matchesQuery && matchesTier && matchesCat
        }
    }

    Scaffold(
        containerColor = TvSlimBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvSlimSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TvSlimAccentSky.copy(alpha = 0.16f))
                                .border(1.dp, TvSlimAccentSky.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = TvSlimAccentSky)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "CleanYourPhone",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TvSlimTextPrimary
                            )
                            Text(
                                "${Build.MANUFACTURER.uppercase()} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}",
                                fontSize = 12.sp,
                                color = TvSlimTextSecondary
                            )
                        }
                    }

                    // Privilege status chip (Shizuku / Wireless ADB)
                    Surface(
                        color = TvSlimCard,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable {
                            engine.requestShizukuPermissionIfNeeded()
                            privilegeMode = engine.detectPrivilegeMode()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (privilegeMode == PrivilegeMode.SHIZUKU_READY) TvSlimRecommended
                                        else TvSlimAccentSky
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                privilegeMode.title,
                                fontSize = 11.sp,
                                color = TvSlimTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // TVSlim-style 5-tab top bar
                ScrollableTabRow(
                    selectedTabIndex = activeTab.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = TvSlimAccentSky,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    TvSlimTab.entries.forEach { tab ->
                        Tab(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            text = {
                                Text(
                                    text = when (tab) {
                                        TvSlimTab.PACKAGES -> "Packages (${packages.size})"
                                        TvSlimTab.LOG -> "Log (${actionLog.count { !it.undone }})"
                                        else -> tab.title
                                    },
                                    fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (activeTab == TvSlimTab.PACKAGES && selectedPackages.isNotEmpty()) {
                Surface(
                    color = TvSlimSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${selectedPackages.size} packages selected",
                                fontWeight = FontWeight.Bold,
                                color = TvSlimTextPrimary
                            )
                            Text(
                                "Reversible via pm disable-user --user 0",
                                fontSize = 12.sp,
                                color = TvSlimTextSecondary
                            )
                        }
                        Button(
                            onClick = { showConfirmModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TvSlimAccentSky)
                        ) {
                            Text("Review & Disable", color = Color(0xFF061320), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            statusBanner?.let { msg ->
                Surface(
                    color = TvSlimRecommended.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, TvSlimRecommended.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(msg, color = TvSlimTextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { statusBanner = null }) {
                            Text("Dismiss", color = TvSlimRecommended)
                        }
                    }
                }
            }

            when (activeTab) {
                TvSlimTab.PHONE -> PhoneOverviewTab(
                    packages = packages,
                    actionLog = actionLog,
                    onSyncUadFromGitHub = {
                        scope.launch {
                            repository.syncLatestUadListsFromGitHub()
                                .onSuccess { remoteCatalog ->
                                    packages = repository.scanDevicePackages(remoteCatalog)
                                    statusBanner = "Synced ${remoteCatalog.size} UAD-ng definitions from GitHub."
                                }
                                .onFailure {
                                    statusBanner = "Using bundled UAD-ng offline database (${packages.size} matched packages)."
                                }
                        }
                    },
                    onRestoreAll = {
                        scope.launch {
                            actionLog.filter { !it.undone }.forEach { entry ->
                                engine.enablePackageUser0(entry.packageName)
                            }
                            packages = packages.map { it.copy(isEnabled = true) }
                            actionLog = actionLog.map { it.copy(undone = true) }
                            statusBanner = "Restored all disabled packages to enabled state."
                        }
                    }
                )
                TvSlimTab.PACKAGES -> PackagesCatalogTab(
                    packages = filteredPackages,
                    selectedPackages = selectedPackages,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    selectedTier = selectedTier,
                    onSelectTier = { selectedTier = it },
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                    onTogglePackage = { pkgName ->
                        selectedPackages = if (pkgName in selectedPackages) {
                            selectedPackages - pkgName
                        } else {
                            selectedPackages + pkgName
                        }
                    },
                    onApplyProfile = { profile ->
                        val matching = packages.filter(profile.selector).map { it.packageName }.toSet()
                        selectedPackages = matching
                        statusBanner = "Profile '${profile.title}' selected ${matching.size} safe packages."
                    }
                )
                TvSlimTab.APPS -> AppsManagerTab(
                    packages = packages,
                    onToggleDisable = { pkg ->
                        scope.launch {
                            if (pkg.isEnabled && !pkg.isSafeguardLocked) {
                                engine.disablePackageUser0(pkg.packageName, pkg.appLabel, pkg.removalTier.label)
                                    .onSuccess { entry ->
                                        actionLog = listOf(entry) + actionLog
                                        packages = packages.map {
                                            if (it.packageName == pkg.packageName) it.copy(isEnabled = false) else it
                                        }
                                    }
                            } else if (!pkg.isEnabled) {
                                engine.enablePackageUser0(pkg.packageName).onSuccess {
                                    packages = packages.map {
                                        if (it.packageName == pkg.packageName) it.copy(isEnabled = true) else it
                                    }
                                }
                            }
                        }
                    }
                )
                TvSlimTab.MEMORY -> MemoryComparisonTab(packages = packages)
                TvSlimTab.LOG -> ActionLogTab(
                    entries = actionLog,
                    onUndoSingle = { entry ->
                        scope.launch {
                            engine.enablePackageUser0(entry.packageName).onSuccess {
                                actionLog = actionLog.map {
                                    if (it.id == entry.id) it.copy(undone = true) else it
                                }
                                packages = packages.map {
                                    if (it.packageName == entry.packageName) it.copy(isEnabled = true) else it
                                }
                                statusBanner = "Re-enabled ${entry.packageName} via '${entry.undoCommand}'."
                            }
                        }
                    },
                    onRestoreEverything = {
                        scope.launch {
                            actionLog.filter { !it.undone }.forEach { entry ->
                                engine.enablePackageUser0(entry.packageName)
                            }
                            actionLog = actionLog.map { it.copy(undone = true) }
                            packages = packages.map { it.copy(isEnabled = true) }
                            statusBanner = "All logged changes have been undone."
                        }
                    }
                )
            }
        }

        if (showConfirmModal) {
            val targetItems = packages.filter { it.packageName in selectedPackages && !it.isSafeguardLocked }
            AlertDialog(
                onDismissRequest = { showConfirmModal = false },
                containerColor = TvSlimSurface,
                title = {
                    Text(
                        "Disable ${targetItems.size} packages (User 0)?",
                        color = TvSlimTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        item {
                            Text(
                                "CleanYourPhone will run 'pm disable-user --user 0' for each package below. Nothing is uninstalled, and every line can be undone in the Log tab:",
                                color = TvSlimTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        items(targetItems) { pkg ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(
                                    "${pkg.appLabel} (${pkg.packageName})",
                                    color = TvSlimTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    pkg.description,
                                    color = TvSlimTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val newEntries = mutableListOf<ActionLogEntry>()
                                targetItems.forEach { pkg ->
                                    engine.disablePackageUser0(pkg.packageName, pkg.appLabel, pkg.removalTier.label)
                                        .onSuccess { newEntries.add(it) }
                                }
                                val disabledSet = targetItems.map { it.packageName }.toSet()
                                packages = packages.map {
                                    if (it.packageName in disabledSet) it.copy(isEnabled = false) else it
                                }
                                actionLog = newEntries + actionLog
                                selectedPackages = emptySet()
                                showConfirmModal = false
                                statusBanner = "Disabled ${newEntries.size} packages with undo records written to Log."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TvSlimRecommended)
                    ) {
                        Text("Apply (${targetItems.size})", color = Color(0xFF051812), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmModal = false }) {
                        Text("Cancel", color = TvSlimTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun PhoneOverviewTab(
    packages: List<PhonePackageItem>,
    actionLog: List<ActionLogEntry>,
    onSyncUadFromGitHub: () -> Unit,
    onRestoreAll: () -> Unit
) {
    val enabledCount = packages.count { it.isEnabled }
    val disabledCount = packages.count { !it.isEnabled }
    val recommendedRemaining = packages.count { it.isEnabled && it.removalTier == RemovalTier.RECOMMENDED }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Surface(
                color = TvSlimCard,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Device & Privilege Summary", fontWeight = FontWeight.Bold, color = TvSlimTextPrimary, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Enabled: $enabledCount packages · Disabled: $disabledCount packages · Recommended bloatware remaining: $recommendedRemaining",
                        color = TvSlimTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onSyncUadFromGitHub,
                            colors = ButtonDefaults.buttonColors(containerColor = TvSlimAccentSky)
                        ) {
                            Text("Sync UAD-ng GitHub List", color = Color(0xFF061320), fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = onRestoreAll) {
                            Text("Restore Everything (${actionLog.count { !it.undone }})", color = TvSlimTextPrimary)
                        }
                    }
                }
            }
        }

        item {
            Surface(
                color = TvSlimCard,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🧱 Active TVSlim-Style Safeguards", fontWeight = FontWeight.Bold, color = TvSlimTextPrimary)
                    Spacer(Modifier.height(6.dp))
                   Text(
                        text = """
                            • System packages are never uninstalled — only disabled with 'pm disable-user --user 0'.
                            • UAD-ng 'Unsafe' packages and core telephony/SystemUI services are hard-locked.
                            • Active Home launcher and default keyboard cannot be disabled unless a replacement is enabled.
                        """.trimIndent(),
                        color = TvSlimTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackagesCatalogTab(
    packages: List<PhonePackageItem>,
    selectedPackages: Set<String>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedTier: RemovalTier?,
    onSelectTier: (RemovalTier?) -> Unit,
    selectedCategory: PackageListCategory,
    onSelectCategory: (PackageListCategory) -> Unit,
    onTogglePackage: (String) -> Unit,
    onApplyProfile: (DebloatProfile) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Ready-made profiles row (like TVSlim)
        item {
            Text("Ready-Made Profiles", fontWeight = FontWeight.Bold, color = TvSlimTextPrimary, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(UadRepository.READY_MADE_PROFILES) { profile ->
                    Surface(
                        color = TvSlimCard,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(260.dp)
                            .clickable { onApplyProfile(profile) }
                            .border(1.dp, TvSlimBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(profile.title, fontWeight = FontWeight.Bold, color = TvSlimTextPrimary, fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(profile.subtitle, color = TvSlimTextSecondary, fontSize = 11.sp, maxLines = 2)
                        }
                    }
                }
            }
        }

        // Search & UAD-ng Tier / Category filters
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search package ID, name, or UAD-ng description…") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedTier == null,
                        onClick = { onSelectTier(null) },
                        label = { Text("All Tiers") }
                    )
                }
                items(
                    listOf(
                        RemovalTier.RECOMMENDED,
                        RemovalTier.ADVANCED,
                        RemovalTier.EXPERT,
                        RemovalTier.UNSAFE
                    )
                ) { tier ->
                    FilterChip(
                        selected = selectedTier == tier,
                        onClick = { onSelectTier(tier) },
                        label = { Text(tier.label) }
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PackageListCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onSelectCategory(cat) },
                        label = { Text(cat.label) }
                    )
                }
            }
        }

        // Package cards
        items(packages, key = { it.packageName }) { pkg ->
            val isSelected = pkg.packageName in selectedPackages
            val badgeColor = when (pkg.removalTier) {
                RemovalTier.RECOMMENDED -> TvSlimRecommended
                RemovalTier.ADVANCED -> TvSlimAdvanced
                RemovalTier.EXPERT -> TvSlimExpert
                RemovalTier.UNSAFE -> TvSlimUnsafe
                RemovalTier.UNLISTED -> TvSlimTextSecondary
            }

            Surface(
                color = TvSlimCard,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isSelected) TvSlimAccentSky else TvSlimBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = !pkg.isSafeguardLocked && pkg.isEnabled) {
                        onTogglePackage(pkg.packageName)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = {
                            if (!pkg.isSafeguardLocked && pkg.isEnabled) onTogglePackage(pkg.packageName)
                        },
                        enabled = !pkg.isSafeguardLocked && pkg.isEnabled
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pkg.appLabel, fontWeight = FontWeight.Bold, color = TvSlimTextPrimary, fontSize = 15.sp)
                            Surface(
                                color = badgeColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (pkg.isSafeguardLocked) "Never disable" else pkg.removalTier.label,
                                    color = badgeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            "${pkg.packageName} · ${pkg.listCategory.label} · ${if (pkg.isEnabled) "Enabled" else "Disabled (User 0)"}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TvSlimAccentSky
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(pkg.description, color = TvSlimTextSecondary, fontSize = 12.sp)
                        pkg.safeguardReason?.let { reason ->
                            Spacer(Modifier.height(4.dp))
                            Text("🧱 $reason", color = TvSlimUnsafe, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppsManagerTab(
    packages: List<PhonePackageItem>,
    onToggleDisable: (PhonePackageItem) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(packages, key = { it.packageName }) { pkg ->
            Surface(color = TvSlimCard, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pkg.appLabel, fontWeight = FontWeight.Bold, color = TvSlimTextPrimary)
                        Text(pkg.packageName, fontSize = 11.sp, color = TvSlimTextSecondary, fontFamily = FontFamily.Monospace)
                    }
                    OutlinedButton(
                        onClick = { onToggleDisable(pkg) },
                        enabled = !pkg.isSafeguardLocked
                    ) {
                        Text(if (pkg.isEnabled) "Disable" else "Enable")
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryComparisonTab(packages: List<PhonePackageItem>) {
    val activeRamMb = packages.filter { it.isEnabled }.sumOf { it.estimatedRamMb }
    val savedRamMb = packages.filter { !it.isEnabled }.sumOf { it.estimatedRamMb }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = TvSlimCard, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("RAM & Background Process Savings", fontWeight = FontWeight.Bold, color = TvSlimTextPrimary, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Reclaimed background RAM: $savedRamMb MB · Active monitored package footprint: $activeRamMb MB",
                    color = TvSlimRecommended,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ActionLogTab(
    entries: List<ActionLogEntry>,
    onUndoSingle: (ActionLogEntry) -> Unit,
    onRestoreEverything: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reversible Action Log", fontWeight = FontWeight.Bold, color = TvSlimTextPrimary, fontSize = 16.sp)
            if (entries.any { !it.undone }) {
                Button(
                    onClick = onRestoreEverything,
                    colors = ButtonDefaults.buttonColors(containerColor = TvSlimRecommended)
                ) {
                    Text("Restore Everything", color = Color(0xFF051812), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (entries.isEmpty()) {
            Text("No packages disabled yet. Every 'pm disable-user --user 0' action will be logged here with its undo command.", color = TvSlimTextSecondary)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries, key = { it.id }) { entry ->
                    Surface(color = TvSlimCard, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${entry.appLabel} (${entry.packageName})", fontWeight = FontWeight.Bold, color = TvSlimTextPrimary, fontSize = 13.sp)
                                Text("Executed: ${entry.commandExecuted}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TvSlimTextSecondary)
                                Text("Undo cmd: ${entry.undoCommand}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TvSlimAccentSky)
                            }
                            if (!entry.undone) {
                                OutlinedButton(onClick = { onUndoSingle(entry) }) {
                                    Text("Undo")
                                }
                            } else {
                                Text("Restored", color = TvSlimRecommended, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
