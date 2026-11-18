package com.drliki.cleanyourphone.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drliki.cleanyourphone.model.ActionLogEntry
import com.drliki.cleanyourphone.ui.components.AppIconView
import com.drliki.cleanyourphone.ui.components.BadgePill
import com.drliki.cleanyourphone.ui.theme.TierRecommended

enum class LogFilterType(val label: String) {
    ALL("All Actions"),
    ACTIVE("Active (Can Undo)"),
    RESTORED("Restored")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionLogScreen(
    actionLog: List<ActionLogEntry>,
    onUndoAction: (ActionLogEntry) -> Unit,
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(LogFilterType.ALL) }
    var showRestoreAllDialog by remember { mutableStateOf(false) }

    val activeCount = actionLog.count { !it.isUndone }
    val restoredCount = actionLog.count { it.isUndone }

    val filteredLog = remember(actionLog, searchQuery, selectedFilter) {
        actionLog.filter { entry ->
            val matchesFilter = when (selectedFilter) {
                LogFilterType.ALL -> true
                LogFilterType.ACTIVE -> !entry.isUndone
                LogFilterType.RESTORED -> entry.isUndone
            }
            val matchesSearch = searchQuery.isBlank() ||
                entry.appLabel.contains(searchQuery, true) ||
                entry.packageName.contains(searchQuery, true) ||
                entry.deviceName.contains(searchQuery, true) ||
                entry.action.contains(searchQuery, true)

            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Action Log",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("Search by app, package, device, or command\u2026") },
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

            // Filter Chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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
                                "Whenever you disable a package on this phone or another phone, it is recorded here and can be undone."
                            else "Try adjusting your search terms or filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredLog, key = { it.id }) { entry ->
                        ActionLogItemCard(
                            entry = entry,
                            onUndo = { onUndoAction(entry) }
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog for Restore All
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
private fun ActionLogItemCard(
    entry: ActionLogEntry,
    onUndo: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconView(
                packageName = entry.packageName,
                appLabel = entry.appLabel,
                modifier = Modifier.padding(end = 12.dp),
                size = 44.dp
            )

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
                Text(
                    "${entry.timestamp} · ${entry.deviceName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    entry.action,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

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
