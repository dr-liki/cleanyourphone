package com.drliki.cleanyourphone.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drliki.cleanyourphone.model.ActionLogEntry
import com.drliki.cleanyourphone.model.DevicePackage
import com.drliki.cleanyourphone.model.RemovalTier
import com.drliki.cleanyourphone.ui.theme.TierAdvanced
import com.drliki.cleanyourphone.ui.theme.TierExpert
import com.drliki.cleanyourphone.ui.theme.TierRecommended
import com.drliki.cleanyourphone.ui.theme.TierUnsafe

// ═════════════════════════════════════════════════════════════════════════════
//  PackageManagementContent — full package list with search, filters, actions
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
    var selectedTier by remember { mutableStateOf<RemovalTier?>(null) }
    var selectedPkgs by remember { mutableStateOf<Set<String>>(emptySet()) }

    val filtered = remember(packages, searchQuery, selectedTier) {
        packages.filter { pkg ->
            val q = searchQuery.isBlank() ||
                pkg.packageName.contains(searchQuery, true) ||
                pkg.appLabel.contains(searchQuery, true) ||
                pkg.description.contains(searchQuery, true)
            val t = selectedTier == null || pkg.tier == selectedTier
            q && t
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading && packages.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(Modifier.fillMaxSize()) {
                // ── Search ───────────────────────────────────────────────
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search packages\u2026") },
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

                // ── Tier filters ─────────────────────────────────────────
                LazyRow(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTier == null,
                            onClick = { selectedTier = null },
                            label = { Text("All (${packages.size})") }
                        )
                    }
                    items(RemovalTier.entries.filter { it != RemovalTier.UNLISTED }) { tier ->
                        FilterChip(
                            selected = selectedTier == tier,
                            onClick = { selectedTier = if (selectedTier == tier) null else tier },
                            label = { Text(tier.label) }
                        )
                    }
                }

                Text(
                    "${filtered.size} packages",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ── Package list ─────────────────────────────────────────
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.packageName }) { pkg ->
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
                    if (filtered.isEmpty()) {
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

                // ── Bottom action bar ────────────────────────────────────
                AnimatedVisibility(visible = selectedPkgs.isNotEmpty()) {
                    Surface(tonalElevation = 3.dp) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "${selectedPkgs.size} selected",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "pm disable-user --user 0",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Button(onClick = {
                                onDisablePackages(selectedPkgs.toSet())
                                selectedPkgs = emptySet()
                            }) {
                                Icon(Icons.Default.Block, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Disable")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  PackageCard
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun PackageCard(
    pkg: DevicePackage,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onEnable: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            if (pkg.isSafeguardLocked) {
                Icon(
                    Icons.Default.Lock,
                    "Locked",
                    modifier = Modifier.padding(12.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (pkg.isEnabled) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() }
                )
            } else {
                Icon(
                    Icons.Default.CheckCircleOutline,
                    "Disabled",
                    modifier = Modifier.padding(12.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        pkg.appLabel,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    TierBadge(pkg)
                }

                Text(
                    pkg.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    pkg.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                pkg.safeguardReason?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "\uD83E\uDDF1 $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = TierUnsafe,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!pkg.isEnabled && !pkg.isSafeguardLocked) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = onEnable, Modifier.height(32.dp)) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Enable", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  TierBadge
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun TierBadge(pkg: DevicePackage) {
    val (label, color) = when {
        pkg.isSafeguardLocked -> "Locked" to TierUnsafe
        !pkg.isEnabled -> "Disabled" to MaterialTheme.colorScheme.outline
        else -> pkg.tier.label to when (pkg.tier) {
            RemovalTier.RECOMMENDED -> TierRecommended
            RemovalTier.ADVANCED -> TierAdvanced
            RemovalTier.EXPERT -> TierExpert
            RemovalTier.UNSAFE -> TierUnsafe
            RemovalTier.UNLISTED -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  ActionLogContent — for ModalBottomSheet
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun ActionLogContent(
    entries: List<ActionLogEntry>,
    onUndo: (ActionLogEntry) -> Unit
) {
    Column(Modifier.padding(16.dp)) {
        Text("Action Log", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        if (entries.isEmpty()) {
            Text(
                "No actions recorded yet. Disabled packages will appear here with undo capability.",
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
                                    style = MaterialTheme.typography.titleSmall
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
                                IconButton(onClick = { onUndo(entry) }) {
                                    Icon(Icons.Default.Undo, "Undo")
                                }
                            } else {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TierRecommended.copy(alpha = 0.14f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "Restored",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TierRecommended,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
