package com.example.localfind.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Canvas
import com.example.localfind.util.QrCodeUtil
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localfind.server.NsdStatus
import com.example.localfind.server.ServerStatus
import com.example.localfind.server.DiscoveryStatus
import com.example.localfind.server.DiscoveredDevice
import com.example.localfind.server.RemoteControlClient
import com.example.localfind.server.ControlResult
import com.example.localfind.auth.RemoteDeviceTokenStore
import com.example.localfind.model.PairingRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.max

enum class RemoteConnectionStatus {
    IDLE,          // Idle
    CONNECTING,    // Connecting
    ONLINE,        // Online
    OFFLINE,       // Offline / Service down
    TIMEOUT,       // Timeout
    UNAUTHORIZED,  // Token error
    SEARCHING,     // Searching
    PARTIAL_SUCCESS, // Partial success
    STOPPED        // Stopped
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isServiceRunning: Boolean,
    serverStatus: ServerStatus,
    lastServerError: String?,
    wakeLockHeld: Boolean,
    wifiLockHeld: Boolean,
    localIp: String?,
    port: Int,
    ringActive: Boolean,
    flashMode: String,
    nsdStatus: NsdStatus,
    nsdServiceType: String,
    discoveryStatus: DiscoveryStatus,
    discoveredDevices: List<DiscoveredDevice>,
    pairingToken: String,
    localDeviceId: String,
    localDeviceName: String,
    pairingModeActive: Boolean,
    pairingModeExpiresAt: Long,
    pendingPairingRequests: List<PairingRequest>,
    remoteTokenStore: RemoteDeviceTokenStore,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRestartServer: () -> Unit,
    onRegenerateToken: () -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onOpenDevice: (DiscoveredDevice) -> Unit,
    onScanQrCode: () -> Unit,
    onTestRingToggle: () -> Unit,
    onTestFlashSteady: () -> Unit,
    onTestFlashStrobe: () -> Unit,
    onTestFlashStop: () -> Unit,
    onStopAll: () -> Unit,
    onEnablePairingMode: () -> Unit,
    onDisablePairingMode: () -> Unit,
    onAcceptPairingRequest: (String) -> Unit,
    onRejectPairingRequest: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onAuthenticate: (reason: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit
) {
    LFS.setLanguage(language)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(LFS.str("find_me"), LFS.str("controller"))

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "Local Find",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTabIndex == 0) {
                FinderModeScreen(
                    isServiceRunning = isServiceRunning,
                    serverStatus = serverStatus,
                    lastServerError = lastServerError,
                    wakeLockHeld = wakeLockHeld,
                    wifiLockHeld = wifiLockHeld,
                    localIp = localIp,
                    port = port,
                    ringActive = ringActive,
                    flashMode = flashMode,
                    nsdStatus = nsdStatus,
                    nsdServiceType = nsdServiceType,
                    pairingToken = pairingToken,
                    localDeviceId = localDeviceId,
                    localDeviceName = localDeviceName,
                    pairingModeActive = pairingModeActive,
                    pairingModeExpiresAt = pairingModeExpiresAt,
                    pendingPairingRequests = pendingPairingRequests,
                    onStartService = onStartService,
                    onStopService = onStopService,
                    onRestartServer = onRestartServer,
                    onRegenerateToken = onRegenerateToken,
                    onTestRingToggle = onTestRingToggle,
                    onTestFlashSteady = onTestFlashSteady,
                    onTestFlashStrobe = onTestFlashStrobe,
                    onTestFlashStop = onTestFlashStop,
                    onStopAll = onStopAll,
                    onEnablePairingMode = onEnablePairingMode,
                    onDisablePairingMode = onDisablePairingMode,
                    onAcceptPairingRequest = onAcceptPairingRequest,
                    onRejectPairingRequest = onRejectPairingRequest,
                    onRequestPermission = onRequestPermission,
                    onOpenBatterySettings = onOpenBatterySettings,
                    onAuthenticate = onAuthenticate
                )
            } else {
                ControllerModeScreen(
                    discoveryStatus = discoveryStatus,
                    discoveredDevices = discoveredDevices,
                    tokenStore = remoteTokenStore,
                    localDeviceId = localDeviceId,
                    localIp = localIp,
                    localPort = port,
                    onStartDiscovery = onStartDiscovery,
                    onStopDiscovery = onStopDiscovery,
                    onOpenBrowser = onOpenDevice,
                    onScanQrCode = onScanQrCode,
                    onAuthenticate = onAuthenticate
                )
            }
        }
    }
}

@Composable
fun FinderModeScreen(
    isServiceRunning: Boolean,
    serverStatus: ServerStatus,
    lastServerError: String?,
    wakeLockHeld: Boolean,
    wifiLockHeld: Boolean,
    localIp: String?,
    port: Int,
    ringActive: Boolean,
    flashMode: String,
    nsdStatus: NsdStatus,
    nsdServiceType: String,
    pairingToken: String,
    localDeviceId: String,
    localDeviceName: String,
    pairingModeActive: Boolean,
    pairingModeExpiresAt: Long,
    pendingPairingRequests: List<PairingRequest>,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRestartServer: () -> Unit,
    onRegenerateToken: () -> Unit,
    onTestRingToggle: () -> Unit,
    onTestFlashSteady: () -> Unit,
    onTestFlashStrobe: () -> Unit,
    onTestFlashStop: () -> Unit,
    onStopAll: () -> Unit,
    onEnablePairingMode: () -> Unit,
    onDisablePairingMode: () -> Unit,
    onAcceptPairingRequest: (String) -> Unit,
    onRejectPairingRequest: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onAuthenticate: (reason: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var isTokenVisible by remember { mutableStateOf(false) }
    var showRegenerateDialog by remember { mutableStateOf(false) }

    if (showRegenerateDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateDialog = false },
            title = { Text(LFS.str("reset_token_title")) },
            text = { Text(LFS.str("reset_token_body")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRegenerateToken()
                        showRegenerateDialog = false
                    }
                ) {
                    Text(LFS.str("reset"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateDialog = false }) {
                    Text(LFS.str("cancel"))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. Usage Steps Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(LFS.str("how_to_use"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                val steps = listOf(
                    LFS.str("how_step1"),
                    LFS.str("how_step2"),
                    LFS.str("how_step3"),
                    LFS.str("how_step4")
                )
                
                steps.forEach { step ->
                    Text(step, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // 1. Service Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val statusColor by animateColorAsState(
                    targetValue = if (isServiceRunning) Color(0xFF4CAF50) else Color(0xFFF44336),
                    label = "serviceControlStatusColor"
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = androidx.compose.foundation.shape.CircleShape, color = statusColor, modifier = Modifier.size(12.dp)) {}
                    Text(
                        text = if (isServiceRunning) LFS.str("running") else LFS.str("stopped"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onRequestPermission(); onStartService() },
                        modifier = Modifier.weight(1f),
                        enabled = !isServiceRunning
                    ) {
                        Text(LFS.str("start_service"), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onStopService,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        enabled = isServiceRunning
                    ) {
                        Text(LFS.str("stop_service"), fontWeight = FontWeight.Bold)
                    }
                }

                if (isServiceRunning) {
                    OutlinedButton(
                        onClick = onRestartServer,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(LFS.str("restart_http"), fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. Service Status Detail Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statusColor by animateColorAsState(
                    targetValue = if (isServiceRunning) Color(0xFF4CAF50) else Color(0xFFF44336),
                    label = "statusColor"
                )
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = androidx.compose.foundation.shape.CircleShape, color = statusColor, modifier = Modifier.size(12.dp)) {}
                    Text(
                        text = if (isServiceRunning) LFS.str("running") else LFS.str("stopped"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                StatusRow(LFS.str("service_status"), when(serverStatus) {
                    ServerStatus.STOPPED -> "Stopped"
                    ServerStatus.STARTING -> "Starting..."
                    ServerStatus.LISTENING -> "Listening"
                    ServerStatus.FAILED -> "Failed"
                })
                if (lastServerError != null) {
                    StatusRow(LFS.str("last_error"), lastServerError)
                }
                StatusRow(LFS.str("lan_address"), localIp ?: LFS.str("offline"))
                StatusRow(LFS.str("port_label"), port.toString())
                StatusRow(LFS.str("nsd"), when(nsdStatus) {
                    NsdStatus.IDLE -> "Idle"
                    NsdStatus.ADVERTISING -> "Advertising"
                    NsdStatus.ADVERTISED -> "Advertised"
                    NsdStatus.FAILED -> "Failed"
                })
                StatusRow(LFS.str("wake_lock"), if (wakeLockHeld) LFS.str("held_cpu") else LFS.str("not_held"))
                StatusRow(LFS.str("wifi_lock"), if (wifiLockHeld) LFS.str("held_wifi") else LFS.str("not_held"))
                StatusRow(LFS.str("service_type"), nsdServiceType)

                if (isServiceRunning && localIp != null) {
                    val controlUrl = "http://$localIp:$port"
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(LFS.str("browser_remote"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(LFS.str("browser_hint"), style = MaterialTheme.typography.bodySmall)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = controlUrl, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            TextButton(
                                onClick = { clipboardManager.setText(AnnotatedString(controlUrl)) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(LFS.str("copy"), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2. Pairing Token Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(LFS.str("pairing_auth"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(LFS.str("pairing_token"), style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (isTokenVisible) pairingToken.ifEmpty { LFS.str("na") } else "********",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row {
                        TextButton(onClick = { isTokenVisible = !isTokenVisible }) {
                            Text(if (isTokenVisible) LFS.str("hide") else LFS.str("show"), fontSize = 12.sp)
                        }
                        TextButton(onClick = { clipboardManager.setText(AnnotatedString(pairingToken)) }) {
                            Text(LFS.str("copy"), fontSize = 12.sp)
                        }
                    }
                }
                
                Button(
                    onClick = { showRegenerateDialog = true },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(LFS.str("reset_token"), fontSize = 12.sp)
                }
            }
        }

        // 2.1 Formal Pairing Mode Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(LFS.str("controller_pairing"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StatusRow(LFS.str("device_label"), localDeviceName.ifBlank { android.os.Build.MODEL })
                StatusRow(LFS.str("device_id"), localDeviceId.ifBlank { "Generated after service start" })
                StatusRow(LFS.str("pairing_mode"), if (pairingModeActive) LFS.str("enabled") else LFS.str("disabled"))
                
                if (pairingModeActive) {
                    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(pairingModeActive) {
                        while (pairingModeActive) {
                            delay(1000)
                            currentTime = System.currentTimeMillis()
                        }
                    }
                    val remainingSeconds = max(0L, (pairingModeExpiresAt - currentTime) / 1000L)
                    StatusRow(LFS.str("remaining"), "${remainingSeconds / 60}m${remainingSeconds % 60}s")
                }

                Text("Pairing mode is active for a short window. Requests must be confirmed on this phone.", style = MaterialTheme.typography.bodySmall)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onEnablePairingMode,
                        modifier = Modifier.weight(1f),
                        enabled = isServiceRunning
                    ) {
                        Text(if (pairingModeActive) LFS.str("renew_5min") else LFS.str("enable_pairing"), fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onDisablePairingMode,
                        modifier = Modifier.weight(1f),
                        enabled = isServiceRunning && pairingModeActive
                    ) {
                        Text(LFS.str("disable_pairing"), fontSize = 12.sp)
                    }
                }

                if (pendingPairingRequests.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(LFS.str("pending_requests"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    pendingPairingRequests.forEach { request ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(request.controllerName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("Type: ${request.controllerType}", style = MaterialTheme.typography.bodySmall)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onAcceptPairingRequest(request.requestId) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Text(LFS.str("accept"), fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { onRejectPairingRequest(request.requestId) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(LFS.str("reject"), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    Text(LFS.str("no_pending"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 2.1 QR Code Pairing Card
        if (isServiceRunning && localIp != null && pairingToken.isNotEmpty()) {
            val qrContent = buildJsonObject {
                put("type", "local_find_pairing")
                put("name", "LocalFind-${android.os.Build.MODEL}")
                put("host", localIp)
                put("port", port)
                put("token", pairingToken)
            }.toString()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val qrBitmap = remember(qrContent) {
                        QrCodeUtil.generateQrCode(qrContent, 400)
                    }

                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        qrBitmap?.let {
                            androidx.compose.foundation.Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = LFS.str("pairing_qr"),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(LFS.str("pairing_qr"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("• Scan from controller to pair", style = MaterialTheme.typography.labelSmall)
                        Text("• QR code is LAN-only", style = MaterialTheme.typography.labelSmall)
                        Text("• Reset token if compromised", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 2.5 Security Tips Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(LFS.str("security"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("• Tokens are LAN-only. Nothing is uploaded to the cloud.", style = MaterialTheme.typography.labelSmall)
                Text("• Do not share tokens with untrusted parties.", style = MaterialTheme.typography.labelSmall)
                Text("• Reset the token if you suspect it has leaked.", style = MaterialTheme.typography.labelSmall)
            }
        }

        // 3. Background Running Support Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(LFS.str("background"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "The service runs as a foreground service. Some devices (Xiaomi, Huawei, Oppo) may restrict background network after screen lock.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Set this app to \"Don't optimize\" or \"Allow background activity\" in system settings.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Some devices disable Wi-Fi on screen lock. Enable \"Keep Wi-Fi on during sleep\" in system settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedButton(
                    onClick = onOpenBatterySettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Battery Optimization Settings", fontSize = 12.sp)
                }
            }
        }

        // 5. Hardware Test Card
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(LFS.str("device_test"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IndicatorBox(LFS.str("ring_label"), if (ringActive) LFS.str("ringing") else LFS.str("silent"), ringActive, Modifier.weight(1f))
                    IndicatorBox(LFS.str("flash_label"), when(flashMode) { "steady" -> LFS.str("steady"); "strobe" -> LFS.str("strobe"); else -> LFS.str("off") }, flashMode != "off", Modifier.weight(1f))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onTestRingToggle, modifier = Modifier.weight(1f)) { Text(if (ringActive) LFS.str("stop_ring") else LFS.str("test_ring"), fontSize = 12.sp) }
                    OutlinedButton(onClick = onTestFlashSteady, modifier = Modifier.weight(1f)) { Text(LFS.str("test_flash"), fontSize = 12.sp) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onTestFlashStrobe, modifier = Modifier.weight(1f)) { Text(LFS.str("test_strobe"), fontSize = 12.sp) }
                    OutlinedButton(onClick = onTestFlashStop, modifier = Modifier.weight(1f)) { Text(LFS.str("turn_flash_off"), fontSize = 12.sp) }
                }
                Button(onClick = onStopAll, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                    Text("Stop All Alerts", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ControllerModeScreen(
    discoveryStatus: DiscoveryStatus,
    discoveredDevices: List<DiscoveredDevice>,
    tokenStore: RemoteDeviceTokenStore,
    localDeviceId: String,
    localIp: String?,
    localPort: Int,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onOpenBrowser: (DiscoveredDevice) -> Unit,
    onScanQrCode: () -> Unit,
    onAuthenticate: (reason: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit
) {
    var selectedDevice by remember { mutableStateOf<DiscoveredDevice?>(null) }
    var recentDevice by remember { mutableStateOf(tokenStore.getRecentDevice()) }
    var savedDevices by remember { mutableStateOf(tokenStore.getSavedDevices()) }
    var isScanning by remember { mutableStateOf(false) }
    var initialScannedToken by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filteredDevices = remember(discoveredDevices, localDeviceId, localIp, localPort) {
        discoveredDevices.filter { device ->
            val isSameIpAndPort = localIp != null && device.host == localIp && device.port == localPort
            !isSameIpAndPort
        }
    }

    // Manual Connection States
    var manualHost by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("8888") }
    var manualName by remember { mutableStateOf("") }
    var manualError by remember { mutableStateOf<String?>(null) }

    if (selectedDevice != null) {
        RemoteControlPanel(
            device = selectedDevice!!,
            tokenStore = tokenStore,
            initialToken = initialScannedToken,
            onBack = {
                selectedDevice = null
                initialScannedToken = ""
                recentDevice = tokenStore.getRecentDevice()
                savedDevices = tokenStore.getSavedDevices()
            },
            onAuthenticate = onAuthenticate
        )
    } else if (isScanning) {
        QrScannerScreen(
            onResult = { result: String ->
                try {
                    val json = JSONObject(result)
                    if (json.optString("type") == "local_find_pairing") {
                        val host = json.getString("host")
                        val port = json.getInt("port")
                        val name = json.optString("name", "Scanned Device")
                        val token = json.getString("token")
                        
                        val device = DiscoveredDevice(
                            name = name,
                            host = host,
                            port = port,
                            controlUrl = "http://$host:$port"
                        )
                        
                        selectedDevice = device
                        initialScannedToken = token
                        isScanning = false
                    } else {
                        scope.launch { snackbarHostState.showSnackbar(LFS.str("not_valid_code")) }
                        isScanning = false
                    }
                } catch (_: Exception) {
                    scope.launch { snackbarHostState.showSnackbar(LFS.str("qr_parse_failed")) }
                    isScanning = false
                }
            },
            onClose = { isScanning = false }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Saved Devices List
                if (savedDevices.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(LFS.str("saved_devices"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)

                            savedDevices.forEach { saved ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(saved.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${saved.host}:${saved.port}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Button(
                                        onClick = {
                                            val device = DiscoveredDevice(
                                                name = saved.name,
                                                host = saved.host,
                                                port = saved.port,
                                                controlUrl = "http://${saved.host}:${saved.port}"
                                            )
                                            tokenStore.saveRecentDevice(saved.name, saved.host, saved.port)
                                            selectedDevice = device
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(LFS.str("connect"), fontSize = 11.sp)
                                    }
                                    TextButton(
                                        onClick = {
                                            tokenStore.removeSavedDevice(saved.host, saved.port)
                                            tokenStore.clearToken(saved.host, saved.port)
                                            recentDevice = tokenStore.getRecentDevice()
                                            savedDevices = tokenStore.getSavedDevices()
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(LFS.str("delete"), fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                } else if (recentDevice != null) {
                    val device = recentDevice!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(LFS.str("recent"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(device.name, fontWeight = FontWeight.Bold)
                                    Text("${device.host}:${device.port}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Button(
                                    onClick = {
                                        tokenStore.saveRecentDevice(device.name, device.host, device.port)
                                        selectedDevice = device
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(LFS.str("connect"), fontSize = 11.sp)
                                }
                                TextButton(
                                    onClick = {
                                        tokenStore.clearRecentDevice()
                                        recentDevice = null
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(LFS.str("delete"), fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                // 2. Discovery Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(LFS.str("device_discovery"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = LFS.str("status_label") + when(discoveryStatus) {
                                    DiscoveryStatus.IDLE -> "Idle"
                                    DiscoveryStatus.SCANNING -> LFS.str("scanning")
                                    DiscoveryStatus.FAILED -> LFS.str("scan_failed")
                                    DiscoveryStatus.STOPPED -> "Stopped"
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (discoveryStatus == DiscoveryStatus.SCANNING) Color(0xFF1976D2) else Color.Gray,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = onStartDiscovery,
                                enabled = discoveryStatus != DiscoveryStatus.SCANNING,
                                modifier = Modifier.height(40.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) { Text(LFS.str("scan"), maxLines = 1, softWrap = false, fontSize = 12.sp) }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = onStopDiscovery,
                                enabled = discoveryStatus == DiscoveryStatus.SCANNING,
                                modifier = Modifier.height(40.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) { Text(LFS.str("stop"), maxLines = 1, softWrap = false, fontSize = 12.sp) }
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )

                        Button(
                            onClick = {
                                onScanQrCode()
                                isScanning = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text(LFS.str("scan_qr_connect"), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 3. Manual Connection Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(LFS.str("manual_connection"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        
                        Text(LFS.str("manual_hint"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualHost,
                                onValueChange = { manualHost = it; manualError = null },
                                label = { Text(LFS.str("ip_address")) },
                                modifier = Modifier.weight(2f),
                                singleLine = true,
                                isError = manualError != null && manualHost.isBlank(),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = manualPort,
                                onValueChange = { manualPort = it; manualError = null },
                                label = { Text(LFS.str("port")) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                isError = manualError != null && manualPort.toIntOrNull() == null,
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        OutlinedTextField(
                            value = manualName,
                            onValueChange = { manualName = it },
                            label = { Text(LFS.str("custom_name_opt")) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("e.g. My old phone") },
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        if (manualError != null) {
                            Text(manualError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                val hostTrimmed = manualHost.trim()
                                val portInt = manualPort.toIntOrNull()
                                
                                if (hostTrimmed.isBlank()) {
                                    manualError = LFS.str("enter_ip")
                                    return@Button
                                }
                                if (portInt == null || portInt !in 1..65535) {
                                    manualError = LFS.str("invalid_port")
                                    return@Button
                                }

                                val name = manualName.ifBlank { "Manual Device" }
                                val device = DiscoveredDevice(
                                    name = name,
                                    host = hostTrimmed,
                                    port = portInt,
                                    controlUrl = "http://$hostTrimmed:$portInt"
                                )
                                tokenStore.saveRecentDevice(name, device.host, portInt)
                                selectedDevice = device
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(LFS.str("connect"))
                        }
                    }
                }

                // 4. Discovered Devices List
                if (filteredDevices.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(LFS.str("no_other_devices"), color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Make sure both phones are on the same Wi-Fi. This device is hidden from the list. Try Manual Connection below if discovery fails.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    filteredDevices.forEach { device ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(device.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("${device.host}:${device.port}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = { 
                                            tokenStore.saveRecentDevice(device.name, device.host, device.port)
                                            selectedDevice = device 
                                        }, 
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(LFS.str("open_in_app"), fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { 
                                            tokenStore.saveRecentDevice(device.name, device.host, device.port)
                                            onOpenBrowser(device) 
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(LFS.str("open_in_browser"), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RemoteControlPanel(
    device: DiscoveredDevice,
    tokenStore: RemoteDeviceTokenStore,
    initialToken: String = "",
    onBack: () -> Unit,
    onAuthenticate: (reason: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()
    val client = remember { RemoteControlClient() }
    val snackbarHostState = remember { SnackbarHostState() }
    
    var inputToken by remember { mutableStateOf(initialToken) }
    var hasSavedToken by remember { mutableStateOf(tokenStore.getToken(device.host, device.port) != null) }
    var connectionStatus by remember { mutableStateOf(RemoteConnectionStatus.IDLE) }
    var ringActive by remember { mutableStateOf(false) }
    var flashMode by remember { mutableStateOf("off") }
    var isLoading by remember { mutableStateOf(false) }

    fun getEffectiveToken(): String? {
        if (inputToken.isNotEmpty()) return inputToken
        return tokenStore.getToken(device.host, device.port)
    }

    fun handleSuccessfulCommand(usedToken: String) {
        tokenStore.saveDevice(device.name, device.host, device.port)
        if (usedToken == inputToken) {
            tokenStore.saveToken(device.host, device.port, inputToken)
            inputToken = ""
            hasSavedToken = true
        }
    }

    fun refreshStatus() {
        scope.launch {
            isLoading = true
            connectionStatus = RemoteConnectionStatus.CONNECTING
            when (val result = client.getStatus(device.host, device.port)) {
                is ControlResult.Success -> {
                    val json = result.statusJson
                    if (json != null) {
                        ringActive = json.optBoolean("ring_active", false)
                        flashMode = json.optString("flash_mode", "off")
                    }
                    connectionStatus = RemoteConnectionStatus.ONLINE
                }
                is ControlResult.Timeout -> {
                    connectionStatus = RemoteConnectionStatus.TIMEOUT
                    snackbarHostState.showSnackbar(LFS.str("timeout"))
                }
                is ControlResult.Unauthorized -> {
                    connectionStatus = RemoteConnectionStatus.UNAUTHORIZED
                }
                is ControlResult.Error -> {
                    connectionStatus = RemoteConnectionStatus.OFFLINE
                    val msg = if (result.message == "connection_failed") LFS.str("offline_svc") else "Connection failed"
                    snackbarHostState.showSnackbar(msg)
                }
            }
            isLoading = false
        }
    }

    fun sendCommand(endpoint: String) {
        val effectiveToken = getEffectiveToken()
        if (effectiveToken == null) {
            scope.launch { snackbarHostState.showSnackbar(LFS.str("enter_token_first")) }
            return
        }
        onAuthenticate(LFS.str("verify_send"), {
            scope.launch {
                isLoading = true
                when (val result = client.sendCommand(device.host, device.port, effectiveToken, endpoint)) {
                    is ControlResult.Success -> {
                        handleSuccessfulCommand(effectiveToken)
                        snackbarHostState.showSnackbar(LFS.str("command_sent"))
                        refreshStatus()
                    }
                    is ControlResult.Unauthorized -> {
                        connectionStatus = RemoteConnectionStatus.UNAUTHORIZED
                        snackbarHostState.showSnackbar(LFS.str("token_error"))
                    }
                    is ControlResult.Timeout -> {
                        connectionStatus = RemoteConnectionStatus.TIMEOUT
                        snackbarHostState.showSnackbar(LFS.str("timeout"))
                    }
                    is ControlResult.Error -> {
                        connectionStatus = RemoteConnectionStatus.OFFLINE
                        val msg = if (result.message == "connection_failed") LFS.str("device_offline") else LFS.str("command_failed")
                        snackbarHostState.showSnackbar(msg)
                    }
                }
                isLoading = false
            }
        }, { error ->
            scope.launch { snackbarHostState.showSnackbar("Local auth failed: $error") }
        })
    }

    fun startFinding() {
        val effectiveToken = getEffectiveToken()
        if (effectiveToken == null) {
            scope.launch { snackbarHostState.showSnackbar(LFS.str("enter_token_first")) }
            return
        }
        onAuthenticate(LFS.str("verify_find"), {
            scope.launch {
                isLoading = true
                // 1. Start ring
                val ringResult = client.sendCommand(device.host, device.port, effectiveToken, "/command/ring/start")
                
                if (ringResult is ControlResult.Unauthorized) {
                    connectionStatus = RemoteConnectionStatus.UNAUTHORIZED
                    snackbarHostState.showSnackbar(LFS.str("token_error"))
                    isLoading = false
                    return@launch
                } else if (ringResult is ControlResult.Timeout) {
                    connectionStatus = RemoteConnectionStatus.TIMEOUT
                    snackbarHostState.showSnackbar(LFS.str("timeout"))
                    isLoading = false
                    return@launch
                } else if (ringResult is ControlResult.Error) {
                    connectionStatus = RemoteConnectionStatus.OFFLINE
                    val msg = if (ringResult.message == "connection_failed") LFS.str("device_offline") else LFS.str("command_failed")
                    snackbarHostState.showSnackbar(msg)
                    isLoading = false
                    return@launch
                }
                
                // 2. Start strobe
                val flashResult = client.sendCommand(device.host, device.port, effectiveToken, "/command/flash/strobe/start")
                
                if (flashResult is ControlResult.Success) {
                    handleSuccessfulCommand(effectiveToken)
                    connectionStatus = RemoteConnectionStatus.SEARCHING
                    snackbarHostState.showSnackbar(LFS.str("searching"))
                } else {
                    // Ring succeeded, flash failed
                    connectionStatus = RemoteConnectionStatus.PARTIAL_SUCCESS
                    snackbarHostState.showSnackbar("Partial success: ring started, flash failed")
                }
                
                refreshStatus()
                isLoading = false
            }
        }, { error ->
            scope.launch { snackbarHostState.showSnackbar("Local auth failed: $error") }
        })
    }

    fun stopFinding() {
        val effectiveToken = getEffectiveToken()
        if (effectiveToken == null) {
            scope.launch { snackbarHostState.showSnackbar(LFS.str("enter_token_first")) }
            return
        }
        onAuthenticate(LFS.str("verify_stop"), {
            scope.launch {
                isLoading = true
                when (val result = client.sendCommand(device.host, device.port, effectiveToken, "/command/stop-all")) {
                    is ControlResult.Success -> {
                        handleSuccessfulCommand(effectiveToken)
                        connectionStatus = RemoteConnectionStatus.STOPPED
                        snackbarHostState.showSnackbar("Stopped")
                        refreshStatus()
                    }
                    is ControlResult.Unauthorized -> {
                        connectionStatus = RemoteConnectionStatus.UNAUTHORIZED
                        snackbarHostState.showSnackbar(LFS.str("token_error"))
                    }
                    is ControlResult.Timeout -> {
                        connectionStatus = RemoteConnectionStatus.TIMEOUT
                        snackbarHostState.showSnackbar(LFS.str("timeout"))
                    }
                    is ControlResult.Error -> {
                        connectionStatus = RemoteConnectionStatus.OFFLINE
                        val msg = if (result.message == "connection_failed") LFS.str("device_offline") else LFS.str("command_failed")
                        snackbarHostState.showSnackbar(msg)
                    }
                }
                isLoading = false
            }
        }, { error ->
            scope.launch { snackbarHostState.showSnackbar("Local auth failed: $error") }
        })
    }

    LaunchedEffect(device) {
        refreshStatus()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(LFS.str("back")) }
                Spacer(modifier = Modifier.weight(1f))
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            // Connection Status Row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = when (connectionStatus) {
                    RemoteConnectionStatus.ONLINE -> Color(0xFFE8F5E9)
                    RemoteConnectionStatus.CONNECTING -> Color(0xFFE3F2FD)
                    RemoteConnectionStatus.UNAUTHORIZED -> Color(0xFFFFF3E0)
                    RemoteConnectionStatus.OFFLINE, RemoteConnectionStatus.TIMEOUT -> Color(0xFFFFEBEE)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusColor = when (connectionStatus) {
                        RemoteConnectionStatus.ONLINE, RemoteConnectionStatus.SEARCHING -> Color(0xFF4CAF50)
                        RemoteConnectionStatus.CONNECTING -> Color(0xFF2196F3)
                        RemoteConnectionStatus.UNAUTHORIZED, RemoteConnectionStatus.PARTIAL_SUCCESS -> Color(0xFFFF9800)
                        RemoteConnectionStatus.OFFLINE, RemoteConnectionStatus.TIMEOUT -> Color(0xFFF44336)
                        else -> Color.Gray
                    }
                    Surface(shape = androidx.compose.foundation.shape.CircleShape, color = statusColor, modifier = Modifier.size(10.dp)) {}
                    Text(
                        text = when (connectionStatus) {
                            RemoteConnectionStatus.IDLE -> "Idle"
                            RemoteConnectionStatus.CONNECTING -> LFS.str("connecting")
                            RemoteConnectionStatus.ONLINE -> LFS.str("online")
                            RemoteConnectionStatus.OFFLINE -> LFS.str("offline_svc")
                            RemoteConnectionStatus.TIMEOUT -> LFS.str("timeout")
                            RemoteConnectionStatus.UNAUTHORIZED -> LFS.str("token_error")
                            RemoteConnectionStatus.SEARCHING -> LFS.str("searching")
                            RemoteConnectionStatus.PARTIAL_SUCCESS -> LFS.str("partial_ringing")
                            RemoteConnectionStatus.STOPPED -> "Stopped"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            // 0. One-tap Find Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { startFinding() },
                        modifier = Modifier.weight(1.3f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(LFS.str("find_phone"), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, textAlign = TextAlign.Center)
                    }
                    Button(
                        onClick = { stopFinding() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(LFS.str("stop"), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(device.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Address: ${device.controlUrl}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IndicatorBox("Remote Ring", if (ringActive) LFS.str("ringing") else LFS.str("silent"), ringActive, Modifier.weight(1f))
                        IndicatorBox("Remote Flash", when(flashMode) { "steady" -> LFS.str("steady"); "strobe" -> LFS.str("strobe"); else -> LFS.str("off") }, flashMode != "off", Modifier.weight(1f))
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(LFS.str("authorization"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    if (hasSavedToken) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                    Text(LFS.str("token_saved"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = { 
                                        tokenStore.clearToken(device.host, device.port)
                                        hasSavedToken = false
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(LFS.str("clear"), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputToken,
                        onValueChange = { inputToken = it },
                        label = { Text(if (hasSavedToken) LFS.str("new_token_opt") else LFS.str("enter_token_target")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    
                    if (inputToken.isNotEmpty()) {
                        TextButton(
                            onClick = { inputToken = "" },
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(LFS.str("clear"), fontSize = 12.sp)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(LFS.str("hardware_ctrl"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { sendCommand("/command/flash/steady/start") }, modifier = Modifier.weight(1f)) { Text(LFS.str("steady"), fontSize = 12.sp) }
                    Button(onClick = { sendCommand("/command/flash/strobe/start") }, modifier = Modifier.weight(1f)) { Text(LFS.str("strobe"), fontSize = 12.sp) }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { sendCommand("/command/flash/stop") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text(LFS.str("turn_flash_off"), fontSize = 12.sp) }
                    Button(onClick = { refreshStatus() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) { Text(LFS.str("refresh"), fontSize = 12.sp) }
                }

                Button(
                    onClick = { sendCommand("/command/stop-all") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Stop All Alerts", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { sendCommand("/command/ring/start") }, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text(LFS.str("alarm_loud"), fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = { sendCommand("/command/ring/stop") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(LFS.str("stop_ring"))
                }
            }
        }
    }
}

@Composable
fun QrScannerScreen(
    onResult: (String) -> Unit,
    onClose: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    
                    val scanner = BarcodeScanning.getClient(
                        BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build()
                    )
                    
                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        barcode.rawValue?.let { 
                                            onResult(it)
                                            // Stop further processing
                                            cameraProvider.unbindAll()
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                    
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Scan frame overlay — dimmed background with clear cutout and corner brackets
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val frameW = canvasW * 0.60f
            val frameH = canvasH * 0.40f
            val left = (canvasW - frameW) / 2f
            val top = (canvasH - frameH) / 2f
            val right = left + frameW
            val bottom = top + frameH
            val dimColor = Color.Black.copy(alpha = 0.45f)
            val frameColor = Color.White
            val lineWidth = 3.dp.toPx()
            val cornerPx = 28.dp.toPx()

            // Four dimmed rectangles around the frame
            drawRect(dimColor, Offset(0f, 0f), Size(canvasW, top))                       // top
            drawRect(dimColor, Offset(0f, bottom), Size(canvasW, canvasH - bottom))      // bottom
            drawRect(dimColor, Offset(0f, top), Size(left, frameH))                      // left
            drawRect(dimColor, Offset(right, top), Size(canvasW - right, frameH))        // right

            // Corner brackets — app icon style (L-shaped)
            // Top-left
            drawLine(frameColor, Offset(left, top + cornerPx), Offset(left, top), lineWidth)
            drawLine(frameColor, Offset(left, top), Offset(left + cornerPx, top), lineWidth)
            // Top-right
            drawLine(frameColor, Offset(right - cornerPx, top), Offset(right, top), lineWidth)
            drawLine(frameColor, Offset(right, top), Offset(right, top + cornerPx), lineWidth)
            // Bottom-left
            drawLine(frameColor, Offset(left, bottom - cornerPx), Offset(left, bottom), lineWidth)
            drawLine(frameColor, Offset(left, bottom), Offset(left + cornerPx, bottom), lineWidth)
            // Bottom-right
            drawLine(frameColor, Offset(right - cornerPx, bottom), Offset(right, bottom), lineWidth)
            drawLine(frameColor, Offset(right, bottom), Offset(right, bottom - cornerPx), lineWidth)
        }

        // UI Overlays (Back button, hint text)
        TextButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
        ) {
            Text(LFS.str("off"), color = Color.White, fontWeight = FontWeight.Bold)
        }
        
        Text(
            LFS.str("align_qr"),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun StatusRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun IndicatorBox(label: String, value: String, isActive: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) Color(0xFFFFEB3B).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, fontWeight = FontWeight.Bold, color = if (isActive) Color(0xFFE65100) else Color.Gray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
