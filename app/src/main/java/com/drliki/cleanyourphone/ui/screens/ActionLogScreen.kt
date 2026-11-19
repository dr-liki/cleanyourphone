package com.drliki.cleanyourphone.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drliki.cleanyourphone.model.ActionLogEntry
import com.drliki.cleanyourphone.ui.components.AppIconView
import com.drliki.cleanyourphone.ui.components.BadgePill
import com.drliki.cleanyourphone.ui.theme.TierRecommended
import kotlinx.coroutines.launch

enum class LogFilterType(val label: String) {
    ALL("All Actions"),
    ACTIVE("Active (Can Undo)"),
    RESTORED("Restored")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionLogScreen(
    actionLog: List<ActionLogEntry>,
    sourceTitle: String = "Action Log",
    onUndoAction: (ActionLogEntry) -> Unit,
    onUndoMultipleActions: (List<ActionLogEntry>) -> Unit = { list -> list.forEach(onUndoAction) },
    onNavigateBack: () -> Unit
) {
    val activeCount = actionLog.count { !it.isUndone }
    val restoredCount = actionLog.count { it.isUndone }
    var showRestoreAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            sourceTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$activeCount active · $restoredCount restored",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (activeCount > 0) {
                        TextButton(onClick = { showRestoreAllDialog = true }) {
                            Icon(Icons.Default.Restore, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Restore All")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        ActionLogContent(
            actionLog = actionLog,
            onUndoAction = onUndoAction,
            onUndoMultipleActions = onUndoMultipleActions,
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (showRestoreAllDialog) {
        val activeEntries = actionLog.filter { !it.isUndone }
        AlertDialog(
            onDismissRequest = { showRestoreAllDialog = false },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = TierRecommended) },
            title = { Text("Restore ${activeEntries.size} package(s)?") },
            text = {
                Text(
                    "This will send re-enable commands for all previously disabled packages. Data and settings will remain intact.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeEntries.forEach { onUndoAction(it) }
                        showRestoreAllDialog = false
                    }
                ) {
                    Text("Restore All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ActionLogContent(
    actionLog: List<ActionLogEntry>,
    onUndoAction: (ActionLogEntry) -> Unit,
    onUndoMultipleActions: (List<ActionLogEntry>) -> Unit = { list -> list.forEach(onUndoAction) },
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(LogFilterType.ALL) }
    var selectedDevice by remember { mutableStateOf<String?>(null) }
    var selectedEntries by remember { mutableStateOf<Set<String>>(emptySet()) }

    val distinctDevices = remember(actionLog) {
        actionLog.map { it.deviceName }.filter { it.isNotBlank() }.distinct()
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 2 }
    }

    val activeCount = actionLog.count { !it.isUndone }
    val restoredCount = actionLog.count { it.isUndone }

    val filteredLog = remember(actionLog, searchQuery, selectedFilter, selectedDevice) {
        actionLog.filter { entry ->
            val matchesFilter = when (selectedFilter) {
                LogFilterType.ALL -> true
                LogFilterType.ACTIVE -> !entry.isUndone
                LogFilterType.RESTORED -> entry.isUndone
            }
            val matchesDevice = selectedDevice == null || entry.deviceName == selectedDevice
            val matchesSearch = searchQuery.isBlank() ||
                entry.appLabel.contains(searchQuery, true) ||
                entry.packageName.contains(searchQuery, true) ||
                entry.deviceName.contains(searchQuery, true) ||
                entry.action.contains(searchQuery, true)

            matchesFilter && matchesDevice && matchesSearch
        }
    }

    val activeFiltered = remember(filteredLog) { filteredLog.filter { !it.isUndone } }
    val allFilteredSelected = activeFiltered.isNotEmpty() && activeFiltered.all { it.id in selectedEntries }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Input (64dp uniform clearance)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(64.dp),
                placeholder = { Text("Search by app, package, device, or command…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Device Filter Chips (shown when actions from multiple device connections exist)
            if (distinctDevices.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedDevice == null,
                            onClick = { selectedDevice = null },
                            label = { Text("All Devices (${actionLog.size})") },
                            leadingIcon = { Icon(Icons.Default.Devices, null, Modifier.size(16.dp)) }
                        )
                    }
                    items(distinctDevices) { dev ->
                        val devCount = actionLog.count { it.deviceName == dev }
                        FilterChip(
                            selected = selectedDevice == dev,
                            onClick = { selectedDevice = dev },
                            label = { Text("$dev ($devCount)") },
                            leadingIcon = {
                                Icon(
                                    if (dev.contains("TV", ignoreCase = true)) Icons.Default.Tv else Icons.Default.PhoneAndroid,
                                    null,
                                    Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            // Status Filter Chips & Select All Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(LogFilterType.entries) { filter ->
                        val count = when (filter) {
                            LogFilterType.ALL -> actionLog.size
                            LogFilterType.ACTIVE -> activeCount
                            LogFilterType.RESTORED -> restoredCount
                        }
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text("${filter.label} ($count)") }
                        )
                    }
                }

                if (activeFiltered.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            selectedEntries = if (allFilteredSelected) {
                                emptySet()
                            } else {
                                activeFiltered.map { it.id }.toSet()
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
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (allFilteredSelected) "Deselect All" else "Select All",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Action Log Entries List
            if (filteredLog.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (actionLog.isEmpty()) "No actions recorded yet"
                            else "No log entries match your search",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (actionLog.isEmpty())
                                "Whenever you disable a package on this phone or another phone, it is recorded here and can be restored."
                            else "Try adjusting your search terms or filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredLog, key = { it.id }) { entry ->
                        ActionLogItemCard(
                            entry = entry,
                            isSelected = entry.id in selectedEntries,
                            onToggle = {
                                selectedEntries = if (entry.id in selectedEntries) {
                                    selectedEntries - entry.id
                                } else {
                                    selectedEntries + entry.id
                                }
                            },
                            onUndo = { onUndoAction(entry) }
                        )
                    }
                }
            }

            // ── Bottom Action Bar: "Restore Selected" ────────────────────
            AnimatedVisibility(visible = selectedEntries.isNotEmpty()) {
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
                                "${selectedEntries.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                "pm enable --user 0",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                        Button(
                            onClick = {
                                val toRestore = actionLog.filter { it.id in selectedEntries && !it.isUndone }
                                onUndoMultipleActions(toRestore)
                                selectedEntries = emptySet()
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TierRecommended)
                        ) {
                            Icon(Icons.Default.Restore, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restore Selected", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }
            }
        }

        // ── Scroll to Top FAB Overlay ─────────────────────────────────
        if (showScrollToTop) {
            val bottomPadding = if (selectedEntries.isNotEmpty()) 84.dp else 16.dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 16.dp, bottom = bottomPadding),
                contentAlignment = Alignment.BottomEnd
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
                }
            }
        }
    }
}

@Composable
private fun ActionLogItemCard(
    entry: ActionLogEntry,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onUndo: () -> Unit
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pinned Left Column: App icon top-left, selection checkbox / restored indicator bottom-left
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                AppIconView(
                    packageName = entry.packageName,
                    appLabel = entry.appLabel,
                    size = 44.dp
                )
                if (!entry.isUndone) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Restored",
                        tint = TierRecommended,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Middle Column: Metadata
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
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        entry.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BadgePill(
                        label = entry.deviceName,
                        color = if (entry.deviceName.contains("Local", ignoreCase = true) || entry.mode == "SHIZUKU")
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer,
                        bgColor = if (entry.deviceName.contains("Local", ignoreCase = true) || entry.mode == "SHIZUKU")
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.action,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            // Right Column: Individual Action Button or Restored Pill
            if (!entry.isUndone) {
                Button(
                    onClick = onUndo,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
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
