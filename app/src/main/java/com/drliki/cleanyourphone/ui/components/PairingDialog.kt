package com.drliki.cleanyourphone.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.drliki.cleanyourphone.ui.theme.TierRecommended

@Composable
fun PairingDialog(
    qrPayload: String,
    pairingCode: String,
    discoveredHost: String = "",
    discoveredPairingPort: Int = 0,
    discoveredConnectPort: Int = 0,
    discoveredDeviceName: String = "",
    onRegenerateQr: () -> Unit,
    onPairWithCode: (host: String, pairingPort: Int, code: String, connectPort: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Pairing inputs initialized with auto-discovered values if present
    var host by remember(discoveredHost) { mutableStateOf(discoveredHost) }
    var pairingPort by remember(discoveredPairingPort) {
        mutableStateOf(if (discoveredPairingPort > 0) discoveredPairingPort.toString() else "")
    }
    var connectPort by remember(discoveredConnectPort) {
        mutableStateOf(if (discoveredConnectPort > 0) discoveredConnectPort.toString() else "")
    }
    var codeInput by remember { mutableStateOf("") }

    // Generate QR bitmap when qrPayload changes
    val qrBitmap = remember(qrPayload) {
        if (qrPayload.isNotBlank()) {
            runCatching {
                QrCodeGenerator.generateQrBitmap(
                    content = qrPayload,
                    size = 512,
                    darkColor = Color(0xFF0F172A),
                    lightColor = Color.White
                )
            }.getOrNull()
        } else null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Pair Device over Wi-Fi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "Required once per device for Android 11+ Wireless Debugging",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                // Tabs: Pairing Code vs QR
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Pairing Code") },
                        icon = { Icon(Icons.Default.Pin, contentDescription = null, Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Pair with QR") },
                        icon = { Icon(Icons.Default.QrCode2, contentDescription = null, Modifier.size(18.dp)) }
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // ── Tab 1: Pairing Code (Auto-Discovery Supported) ───────
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Auto-discovery notification banner
                        if (discoveredHost.isNotBlank() && discoveredPairingPort > 0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TierRecommended.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = TierRecommended,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "Auto-Discovered Pairing Service!",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TierRecommended
                                        )
                                        Text(
                                            "${discoveredDeviceName.ifBlank { "Remote Phone" }} ($discoveredHost:$discoveredPairingPort)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "On the other phone:\nSettings > Developer options > Wireless debugging > 'Pair device with pairing code'.\nIP and port will be auto-filled once discovered.",
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Host Input (64dp height for full visibility)
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("Target Phone IP") },
                            placeholder = { Text("e.g. 192.168.1.105") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Pairing Port and Connect Port (Uniform Equal-Sized 64dp Boxes)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = pairingPort,
                                onValueChange = { pairingPort = it },
                                label = { Text("Pairing Port", maxLines = 1) },
                                placeholder = { Text("e.g. 38411") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = connectPort,
                                onValueChange = { connectPort = it },
                                label = { Text("Connect Port", maxLines = 1) },
                                placeholder = { Text("e.g. 41255") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Text(
                            "Pairing Port is temporary (shown in pairing popup). Connect Port is the port under 'IP address & Port' on the target phone.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 6-Digit Code Input (64dp height for full visibility)
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it },
                            label = { Text("6-Digit Pairing Code") },
                            placeholder = { Text("Shown on other phone dialog") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val pPort = pairingPort.toIntOrNull() ?: 0
                                val cPort = connectPort.toIntOrNull() ?: 5555
                                if (pPort > 0) {
                                    onPairWithCode(host.trim(), pPort, codeInput.trim(), cPort)
                                    onDismiss()
                                }
                            },
                            enabled = host.isNotBlank() && pairingPort.isNotBlank() && codeInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pair & Connect")
                        }
                    }
                } else {
                    // ── Tab 2: Pair with QR ──────────────────────────────────
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "On the other phone:\nSettings > Developer options > Wireless debugging > 'Pair device with QR code' to scan.\n\nTip: For phone-to-phone pairing, the 'Pairing Code' tab with the 6-digit PIN is the recommended and standard method.",
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        if (qrBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Pairing QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "PIN: $pairingCode",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onRegenerateQr,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("New QR Code")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}
