package com.drliki.cleanyourphone.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.drliki.cleanyourphone.model.ActionLogEntry
import com.drliki.cleanyourphone.model.AppTypeFilter
import com.drliki.cleanyourphone.model.DevicePackage
import com.drliki.cleanyourphone.model.RemovalTier
import com.drliki.cleanyourphone.ui.theme.TierAdvanced
import com.drliki.cleanyourphone.ui.theme.TierExpert
import com.drliki.cleanyourphone.ui.theme.TierRecommended
import com.drliki.cleanyourphone.ui.theme.TierUnsafe

// ═════════════════════════════════════════════════════════════════════════════
//  AppSortOption — user selectable sorting modes
// ═════════════════════════════════════════════════════════════════════════════

enum class AppSortOption(val label: String, val shortLabel: String) {
    NAME_ASC("App Name (A → Z)", "Name A-Z"),
    NAME_DESC("App Name (Z → A)", "Name Z-A"),
    PACKAGE_ASC("Package ID (A → Z)", "Pkg A-Z"),
    TIER_SAFETY("Safety Tier (Safest first)", "Safety"),
    STATUS_DISABLED("Disabled first", "Disabled"),
    STATUS_ENABLED("Enabled first", "Enabled")
}

// ═════════════════════════════════════════════════════════════════════════════
//  PackageManagementContent — full package list with dual-row filters & actions
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageManagementContent(
    packages: List<DevicePackage>,
    isLoading: Boolean,
    onDisablePackages: (Set<String>) -> Unit,
    onEnablePackage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf(AppTypeFilter.ALL) }
    var selectedTier by remember { mutableStateOf<RemovalTier?>(null) }
    var selectedSort by remember { mutableStateOf(AppSortOption.NAME_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedPkgs by remember { mutableStateOf<Set<String>>(emptySet()) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

    val filtered = remember(packages, searchQuery, selectedTypeFilter, selectedTier) {
        packages.filter { pkg ->
            val matchesQuery = searchQuery.isBlank() ||
                pkg.packageName.contains(searchQuery, true) ||
                pkg.appLabel.contains(searchQuery, true) ||
                pkg.description.contains(searchQuery, true)

            val matchesType = when (selectedTypeFilter) {
                AppTypeFilter.ALL -> true
                AppTypeFilter.SYSTEM -> pkg.isSystemApp
                AppTypeFilter.USER -> !pkg.isSystemApp
                AppTypeFilter.DISABLED -> !pkg.isEnabled
            }

            val matchesTier = selectedTier == null || pkg.tier == selectedTier
            matchesQuery && matchesType && matchesTier
        }
    }

    val sortedPackages = remember(filtered, selectedSort) {
        when (selectedSort) {
            AppSortOption.NAME_ASC -> filtered.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.appLabel.ifBlank { it.packageName } }
            )
            AppSortOption.NAME_DESC -> filtered.sortedWith(
                compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.appLabel.ifBlank { it.packageName } }
            )
            AppSortOption.PACKAGE_ASC -> filtered.sortedBy { it.packageName.lowercase() }
            AppSortOption.TIER_SAFETY -> filtered.sortedWith(
                compareBy<DevicePackage> {
                    when (it.tier) {
                        RemovalTier.RECOMMENDED -> 0
                        RemovalTier.ADVANCED -> 1
                        RemovalTier.EXPERT -> 2
                        RemovalTier.UNSAFE -> 3
                        RemovalTier.UNLISTED -> 4
                    }
                }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.appLabel.ifBlank { it.packageName } }
            )
            AppSortOption.STATUS_DISABLED -> filtered.sortedWith(
                compareBy<DevicePackage> { if (!it.isEnabled) 0 else 1 }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.appLabel.ifBlank { it.packageName } }
            )
            AppSortOption.STATUS_ENABLED -> filtered.sortedWith(
                compareBy<DevicePackage> { if (it.isEnabled) 0 else 1 }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.appLabel.ifBlank { it.packageName } }
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading && packages.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(Modifier.fillMaxSize()) {
                // ── Search Bar ───────────────────────────────────────────
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    placeholder = { Text("Search packages by name or ID\u2026") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty())
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // ── Filter Row 1: App Type (All, System, User, Disabled) ──
                LazyRow(
                    Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AppTypeFilter.entries) { filter ->
                        val count = when (filter) {
                            AppTypeFilter.ALL -> packages.size
                            AppTypeFilter.SYSTEM -> packages.count { it.isSystemApp }
                            AppTypeFilter.USER -> packages.count { !it.isSystemApp }
                            AppTypeFilter.DISABLED -> packages.count { !it.isEnabled }
                        }
                        FilterChip(
                            selected = selectedTypeFilter == filter,
                            onClick = { selectedTypeFilter = filter },
                            label = { Text("${filter.label} ($count)") }
                        )
                    }
                }

                // ── Filter Row 2: Removal Tiers (Recommended, Advanced, etc.) ─
                LazyRow(
                    Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTier == null,
                            onClick = { selectedTier = null },
                            label = { Text("All Tiers") }
                        )
                    }
                    items(RemovalTier.entries.filter { it != RemovalTier.UNLISTED }) { tier ->
                        val count = packages.count { it.tier == tier }
                        FilterChip(
                            selected = selectedTier == tier,
                            onClick = { selectedTier = if (selectedTier == tier) null else tier },
                            label = { Text("${tier.label} ($count)") }
                        )
                    }
                }

                // ── Result header & quick selection ──────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${sortedPackages.size} packages shown",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Sort Options Dropdown Menu Button
                        Box {
                            OutlinedButton(
                                onClick = { showSortMenu = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Sort,
                                    contentDescription = "Sort Options",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    selectedSort.shortLabel,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                Text(
                                    "Sort Packages",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider()
                                AppSortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (selectedSort == option) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                } else {
                                                    Spacer(Modifier.width(24.dp))
                                                }
                                                Text(
                                                    option.label,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (selectedSort == option) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedSort = option
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        val selectableFiltered = sortedPackages.filter { it.isEnabled && !it.isSafeguardLocked }
                        if (selectableFiltered.isNotEmpty()) {
                            val allFilteredSelected = selectableFiltered.all { it.packageName in selectedPkgs }
                            Spacer(Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    selectedPkgs = if (allFilteredSelected) {
                                        selectedPkgs - selectableFiltered.map { it.packageName }.toSet()
                                    } else {
                                        selectedPkgs + selectableFiltered.map { it.packageName }.toSet()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    if (allFilteredSelected) "Deselect All" else "Select All",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                // ── Package list ─────────────────────────────────────────
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedPackages, key = { it.packageName }) { pkg ->
                        PackageCard(
                            pkg = pkg,
                            isSelected = pkg.packageName in selectedPkgs,
                            onToggle = {
                                selectedPkgs = if (pkg.packageName in selectedPkgs)
                                    selectedPkgs - pkg.packageName
                                else selectedPkgs + pkg.packageName
                            },
                            onEnable = { onEnablePackage(pkg.packageName) }
                        )
                    }
                    if (sortedPackages.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No packages match your filters",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ── Bottom action bar (Optimized to prevent text overflow) ───
                AnimatedVisibility(visible = selectedPkgs.isNotEmpty()) {
                    Surface(
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    "${selectedPkgs.size} selected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    "pm disable-user --user 0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                            Button(
                                onClick = {
                                    onDisablePackages(selectedPkgs.toSet())
                                    selectedPkgs = emptySet()
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Block, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Disable Selected", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // ── Scroll to Top Floating Button Overlay ─────────────────
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = if (selectedPkgs.isNotEmpty()) 84.dp else 16.dp
                    )
            ) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
                }
            }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  PackageCard — Displays app icon, full names, full description, and badges below
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun PackageCard(
    pkg: DevicePackage,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onEnable: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Column: App Icon at top-left, selection/tick/lock button at bottom-left (proportionate spacing)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Real App Icon (from PackageManager or MD3 glyph)
                AppIconView(
                    packageName = pkg.packageName,
                    appLabel = pkg.appLabel,
                    size = 46.dp
                )

                Spacer(Modifier.height(8.dp))

                // Selection checkbox or status icon directly at bottom-left below app icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (pkg.isSafeguardLocked) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Locked Safeguard",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    } else if (pkg.isEnabled) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggle() },
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Disabled",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Package Details Column
            Column(Modifier.weight(1f)) {
                // Full app name (no ellipsis, no maxLines constraint)
                Text(
                    text = pkg.appLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(2.dp))

                // Full package name (no ellipsis, monospaced font)
                Text(
                    text = pkg.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(6.dp))

                // Full description (no ellipsis, no maxLines constraint)
                Text(
                    text = pkg.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ── Package tags/badges moved BELOW the description line ──
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge (Disabled or Active)
                    if (!pkg.isEnabled) {
                        BadgePill(
                            label = "Disabled",
                            color = MaterialTheme.colorScheme.outline,
                            bgColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    // Removal Tier Badge
                    TierBadgePill(pkg.tier)

                    // System / User App Badge
                    if (pkg.isSystemApp) {
                        BadgePill(
                            label = "System",
                            color = MaterialTheme.colorScheme.secondary,
                            bgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    } else {
                        BadgePill(
                            label = "User",
                            color = MaterialTheme.colorScheme.tertiary,
                            bgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        )
                    }

                    // Safeguard Lock Badge
                    if (pkg.isSafeguardLocked) {
                        BadgePill(
                            label = "Locked",
                            color = TierUnsafe,
                            bgColor = TierUnsafe.copy(alpha = 0.12f)
                        )
                    }
                }

                // Safeguard explanation if present
                pkg.safeguardReason?.let { reason ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "🔒 $reason",
                        style = MaterialTheme.typography.labelSmall,
                        color = TierUnsafe,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Quick Enable button if currently disabled
                if (!pkg.isEnabled && !pkg.isSafeguardLocked) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onEnable,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Re-enable", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  BadgePills
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun BadgePill(
    label: String,
    color: Color,
    bgColor: Color
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TierBadgePill(tier: RemovalTier) {
    val (color, bgColor) = when (tier) {
        RemovalTier.RECOMMENDED -> TierRecommended to TierRecommended.copy(alpha = 0.14f)
        RemovalTier.ADVANCED -> TierAdvanced to TierAdvanced.copy(alpha = 0.14f)
        RemovalTier.EXPERT -> TierExpert to TierExpert.copy(alpha = 0.14f)
        RemovalTier.UNSAFE -> TierUnsafe to TierUnsafe.copy(alpha = 0.14f)
        RemovalTier.UNLISTED -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
    }
    BadgePill(label = tier.label, color = color, bgColor = bgColor)
}

// ═════════════════════════════════════════════════════════════════════════════
//  ActionLogContent — compact view for quick review
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun ActionLogContent(
    entries: List<ActionLogEntry>,
    onUndo: (ActionLogEntry) -> Unit
) {
    Column(Modifier.padding(16.dp)) {
        Text("Action History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        if (entries.isEmpty()) {
            Text(
                "No actions recorded yet. Disabled packages will appear here with one-tap undo capability.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.appLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    entry.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "${entry.timestamp} · ${entry.deviceName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!entry.isUndone) {
                                Button(
                                    onClick = { onUndo(entry) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Undo, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Undo", style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                BadgePill(
                                    label = "Restored",
                                    color = TierRecommended,
                                    bgColor = TierRecommended.copy(alpha = 0.14f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
