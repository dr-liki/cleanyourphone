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
    onDirectConnect: ((host: String, port: Int) -> Unit)? = null,
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
    var directPort by remember { mutableStateOf("5555") }

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
                    "Pair & Connect Device",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "Android 11+ Wireless Debugging Pairing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                // Three-tab navigation: Pairing Code | Direct Connect | Pair with QR
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Pair Code", maxLines = 1) },
                        icon = { Icon(Icons.Default.Pin, contentDescription = null, Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Direct (5555)", maxLines = 1) },
                        icon = { Icon(Icons.Default.Wifi, contentDescription = null, Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("QR Code", maxLines = 1) },
                        icon = { Icon(Icons.Default.QrCode2, contentDescription = null, Modifier.size(18.dp)) }
                    )
                }

                Spacer(Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> {
                        // ── Tab 0: Pair with 6-Digit Code (SPAKE2 TLS Handshake) ──
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Auto-discovery notification banner
                            if (discoveredHost.isNotBlank()) {
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
                                                "Auto-Discovered Pairing Request!",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TierRecommended
                                            )
                                            val displayName = if (discoveredDeviceName.isNotBlank()) discoveredDeviceName else "Remote Phone"
                                            val pairingPortStr = if (discoveredPairingPort > 0) "Pairing Port: $discoveredPairingPort" else ""
                                            val connectPortStr = if (discoveredConnectPort > 0) "Connect Port: $discoveredConnectPort" else ""
                                            val portInfo = listOf(pairingPortStr, connectPortStr).filter { it.isNotBlank() }.joinToString(" | ")
                                            Text(
                                                "$displayName ($discoveredHost)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (portInfo.isNotBlank()) {
                                                Text(
                                                    portInfo,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
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
                                        "On the target phone:\n1. Settings > Developer options > Wireless debugging.\n2. Tap 'Pair device with pairing code'.\n3. Enter the IP, Pairing Port, and 6-digit code shown in that popup.",
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Host Input (64dp height)
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                            // 6-Digit Pairing Code Input (64dp height with monospace bold text)
                            OutlinedTextField(
                                value = codeInput,
                                onValueChange = { codeInput = it },
                                label = { Text("6-Digit Wi-Fi Pairing Code") },
                                placeholder = { Text("e.g. 123456") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = LocalTextStyle.current.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )

                            Text(
                                "Pairing Port & 6-digit Code are inside the 'Pair device with pairing code' popup. Connect Port is under 'IP address & Port' on the main Wireless debugging screen.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    val pPort = pairingPort.toIntOrNull() ?: 0
                                    val cPort = connectPort.toIntOrNull() ?: pPort
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
                                Text("Pair & Connect Device")
                            }
                        }
                    }
                    1 -> {
                        // ── Tab 1: Direct Connect (Port 5555 or Already Paired) ───
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Use Direct Connect for devices already paired, or devices running classic ADB on port 5555.",
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

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

                            OutlinedTextField(
                                value = directPort,
                                onValueChange = { directPort = it },
                                label = { Text("Port (e.g. 5555 or dynamic connect port)") },
                                placeholder = { Text("5555") },
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
                                    val port = directPort.toIntOrNull() ?: 5555
                                    if (onDirectConnect != null) {
                                        onDirectConnect(host.trim(), port)
                                    } else {
                                        onPairWithCode(host.trim(), port, "", port)
                                    }
                                    onDismiss()
                                },
                                enabled = host.isNotBlank() && directPort.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Connect to Device")
                            }
                        }
                    }
                    2 -> {
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
                                    "On the other phone:\nSettings > Developer options > Wireless debugging > 'Pair device with QR code' to scan.\n\nTip: The 'Pair Code' tab with the 6-digit PIN is the recommended method for phone-to-phone pairing.",
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
                }

                Spacer(Modifier.height(16.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}
