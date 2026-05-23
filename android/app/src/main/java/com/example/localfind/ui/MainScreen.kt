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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
    IDLE,          // 未检测
    CONNECTING,    // 连接中
    ONLINE,        // 在线
    OFFLINE,       // 离线 / 服务未启动
    TIMEOUT,       // 请求超时
    UNAUTHORIZED,  // Token 错误
    SEARCHING,     // 正在寻找手机
    PARTIAL_SUCCESS, // 部分成功
    STOPPED        // 已停止
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
    onAuthenticate: (reason: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("被寻找端", "控制端")

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
            title = { Text("确认重置 Token？") },
            text = { Text("重置后，所有已连接的浏览器控制页将失效，需要重新输入新 Token 才能继续控制。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRegenerateToken()
                        showRegenerateDialog = false
                    }
                ) {
                    Text("确认重置", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateDialog = false }) {
                    Text("取消")
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
                Text("如何寻找这台手机", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                val steps = listOf(
                    "第一步：点击下方按钮“启动服务”",
                    "第二步：保持本手机与控制端在同一 Wi-Fi / 局域网",
                    "第三步：在控制端扫描设备，或手动输入 IP:8888",
                    "第四步：在控制端输入本页面显示的 8 位 Token",
                    "第五步：在控制端点击“开始寻找手机”触发响铃和闪烁"
                )
                
                steps.forEach { step ->
                    Text(step, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // 1. Service Status Card
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
                        text = if (isServiceRunning) "正在监听 (Running)" else "未运行 (Stopped)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                StatusRow("服务状态:", when(serverStatus) {
                    ServerStatus.STOPPED -> "已停止"
                    ServerStatus.STARTING -> "正在启动..."
                    ServerStatus.LISTENING -> "正在监听"
                    ServerStatus.FAILED -> "启动失败"
                })
                if (lastServerError != null) {
                    StatusRow("最近服务错误:", lastServerError)
                }
                StatusRow("手机局域网 IP:", localIp ?: "未连接")
                StatusRow("监听端口:", port.toString())
                StatusRow("NSD 状态:", when(nsdStatus) {
                    NsdStatus.IDLE -> "未广播"
                    NsdStatus.ADVERTISING -> "广播中"
                    NsdStatus.ADVERTISED -> "已广播"
                    NsdStatus.FAILED -> "广播失败"
                })
                StatusRow("WakeLock:", if (wakeLockHeld) "已持有 (CPU 唤醒)" else "未持有")
                StatusRow("WifiLock:", if (wifiLockHeld) "已持有 (网络活跃)" else "未持有")
                StatusRow("服务类型:", nsdServiceType)

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
                        Text("电脑浏览器远程控制 (无需安装):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("在电脑浏览器打开下方地址，即可在同 Wi-Fi 下寻找手机。", style = MaterialTheme.typography.bodySmall)
                        
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
                                Text("复制地址", fontSize = 12.sp)
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
                Text("本地配对与鉴权", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("当前配对 Token:", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (isTokenVisible) pairingToken.ifEmpty { "未生成" } else "********",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row {
                        TextButton(onClick = { isTokenVisible = !isTokenVisible }) {
                            Text(if (isTokenVisible) "隐藏" else "显示", fontSize = 12.sp)
                        }
                        TextButton(onClick = { clipboardManager.setText(AnnotatedString(pairingToken)) }) {
                            Text("复制", fontSize = 12.sp)
                        }
                    }
                }
                
                Button(
                    onClick = { showRegenerateDialog = true },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("重置 Token", fontSize = 12.sp)
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
                Text("电脑插件配对模式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StatusRow("设备名称:", localDeviceName.ifBlank { android.os.Build.MODEL })
                StatusRow("设备 ID:", localDeviceId.ifBlank { "服务启动后生成" })
                StatusRow("配对模式:", if (pairingModeActive) "已开启" else "已关闭")
                
                if (pairingModeActive) {
                    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(pairingModeActive) {
                        while (pairingModeActive) {
                            delay(1000)
                            currentTime = System.currentTimeMillis()
                        }
                    }
                    val remainingSeconds = max(0L, (pairingModeExpiresAt - currentTime) / 1000L)
                    StatusRow("剩余时间:", "${remainingSeconds / 60}分${remainingSeconds % 60}秒")
                }

                Text("配对模式只在用户开启后短时间有效；电脑发起配对后，必须在手机端确认。当前不使用二维码，也不需要云账号。", style = MaterialTheme.typography.bodySmall)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onEnablePairingMode,
                        modifier = Modifier.weight(1f),
                        enabled = isServiceRunning
                    ) {
                        Text(if (pairingModeActive) "重新开启 5 分钟" else "开启配对模式", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onDisablePairingMode,
                        modifier = Modifier.weight(1f),
                        enabled = isServiceRunning && pairingModeActive
                    ) {
                        Text("关闭配对模式", fontSize = 12.sp)
                    }
                }

                if (pendingPairingRequests.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("待确认的配对请求", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    pendingPairingRequests.forEach { request ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(request.controllerName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("类型: ${request.controllerType}", style = MaterialTheme.typography.bodySmall)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onAcceptPairingRequest(request.requestId) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Text("接受", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { onRejectPairingRequest(request.requestId) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("拒绝", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    Text("暂无待确认请求。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                contentDescription = "Pairing QR Code",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("配对二维码", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("• 控制端扫码后可快速连接", style = MaterialTheme.typography.labelSmall)
                        Text("• 二维码只在局域网内使用", style = MaterialTheme.typography.labelSmall)
                        Text("• 如果泄露，请重置 Token", style = MaterialTheme.typography.labelSmall)
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
                Text("安全与隐私提示", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("• Token 仅用于局域网内授权控制，不会上传云端。", style = MaterialTheme.typography.labelSmall)
                Text("• 请勿将 Token 提供给不可信的人员。", style = MaterialTheme.typography.labelSmall)
                Text("• 如果怀疑 Token 泄露，请重置 Token。", style = MaterialTheme.typography.labelSmall)
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
                Text("保持后台运行", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "服务正在以前台模式运行，但在某些系统（如小米、华为、OPPO）锁屏后可能限制后台网络或冻结服务，需要允许后台运行/忽略电池优化。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "建议：请在系统中将本应用设置为“不优化电池使用”或“允许后台活动”。",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "注意：部分设备锁屏后会切断 Wi-Fi，请在系统设置中允许“锁屏后保持 Wi-Fi 连接”。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedButton(
                    onClick = onOpenBatterySettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("打开系统电池优化设置", fontSize = 12.sp)
                }
            }
        }

        // 4. Service Controls
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onRequestPermission(); onStartService() },
                modifier = Modifier.weight(1f),
                enabled = !isServiceRunning
            ) {
                Text("启动服务", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onStopService,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                enabled = isServiceRunning
            ) {
                Text("停止服务", fontWeight = FontWeight.Bold)
            }
        }
        
        if (isServiceRunning) {
            OutlinedButton(
                onClick = onRestartServer,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("重启 HTTP 服务 (手动故障恢复)", fontSize = 12.sp)
            }
        }

        // 4. Hardware Test Card
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("硬件寻机外设测试 (本地)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IndicatorBox("音频响铃", if (ringActive) "鸣叫中" else "静音", ringActive, Modifier.weight(1f))
                    IndicatorBox("手电筒", when(flashMode) { "steady" -> "常亮"; "strobe" -> "爆闪"; else -> "关闭" }, flashMode != "off", Modifier.weight(1f))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onTestRingToggle, modifier = Modifier.weight(1f)) { Text(if (ringActive) "停止响铃" else "测试响铃", fontSize = 12.sp) }
                    OutlinedButton(onClick = onTestFlashSteady, modifier = Modifier.weight(1f)) { Text("测试常亮", fontSize = 12.sp) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onTestFlashStrobe, modifier = Modifier.weight(1f)) { Text("测试闪烁", fontSize = 12.sp) }
                    OutlinedButton(onClick = onTestFlashStop, modifier = Modifier.weight(1f)) { Text("关闭手电", fontSize = 12.sp) }
                }
                Button(onClick = onStopAll, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                    Text("一键停止全部动作", fontWeight = FontWeight.Bold)
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
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onOpenBrowser: (DiscoveredDevice) -> Unit,
    onScanQrCode: () -> Unit,
    onAuthenticate: (reason: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) -> Unit
) {
    var selectedDevice by remember { mutableStateOf<DiscoveredDevice?>(null) }
    var recentDevice by remember { mutableStateOf(tokenStore.getRecentDevice()) }
    var isScanning by remember { mutableStateOf(false) }
    var initialScannedToken by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                // Refresh recent device when coming back
                recentDevice = tokenStore.getRecentDevice()
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
                        scope.launch { snackbarHostState.showSnackbar("不是有效的 Local Find 配对码") }
                        isScanning = false
                    }
                } catch (_: Exception) {
                    scope.launch { snackbarHostState.showSnackbar("扫码解析失败") }
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
                // 1. Recent Device Card
                recentDevice?.let { device ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("最近连接", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                                    Text("快速进入", fontSize = 11.sp)
                                }
                            }
                            
                            Text(
                                "提示：如果连接失败，请检查两台手机是否在同一 Wi-Fi，且被寻找端的寻机服务已启动。IP 地址可能会因 Wi-Fi变动而失效。此外，请确保被寻找端没有被系统冻结后台连接。",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
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
                        Text("发现局域网设备 (NSD Scanner)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "状态: " + when(discoveryStatus) {
                                    DiscoveryStatus.IDLE -> "未扫描"
                                    DiscoveryStatus.SCANNING -> "扫描中..."
                                    DiscoveryStatus.FAILED -> "扫描失败"
                                    DiscoveryStatus.STOPPED -> "已停止"
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (discoveryStatus == DiscoveryStatus.SCANNING) Color(0xFF1976D2) else Color.Gray
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = onStartDiscovery, enabled = discoveryStatus != DiscoveryStatus.SCANNING) { Text("开始扫描", fontSize = 12.sp) }
                                OutlinedButton(onClick = onStopDiscovery, enabled = discoveryStatus == DiscoveryStatus.SCANNING) { Text("停止", fontSize = 12.sp) }
                            }
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
                            Text("扫码连接设备", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 3. Manual Connection Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("手动连接 fallback", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        
                        Text("提示：Host 填写被寻找端显示的 IP 地址，端口默认 8888。", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualHost,
                                onValueChange = { manualHost = it; manualError = null },
                                label = { Text("IP 地址") },
                                modifier = Modifier.weight(2f),
                                singleLine = true,
                                isError = manualError != null && manualHost.isBlank(),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = manualPort,
                                onValueChange = { manualPort = it; manualError = null },
                                label = { Text("端口") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                isError = manualError != null && manualPort.toIntOrNull() == null,
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        OutlinedTextField(
                            value = manualName,
                            onValueChange = { manualName = it },
                            label = { Text("自定义名称 (可选)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("例如：我的旧手机") },
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
                                    manualError = "请输入 IP 地址"
                                    return@Button
                                }
                                if (portInt == null || portInt !in 1..65535) {
                                    manualError = "端口无效 (1-65535)"
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
                            Text("连接到设备")
                        }
                    }
                }

                // 4. Discovered Devices List
                if (discoveredDevices.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("未发现局域网设备", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "请确认两台手机在同一 Wi-Fi。如果仍无法发现，请尝试下方的“手动连接”。", 
                            color = Color.Gray, 
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    discoveredDevices.forEach { device ->
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
                                        Text("App 内控制", fontSize = 11.sp)
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
                                        Text("浏览器打开", fontSize = 11.sp)
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
                    snackbarHostState.showSnackbar("请求超时")
                }
                is ControlResult.Unauthorized -> {
                    connectionStatus = RemoteConnectionStatus.UNAUTHORIZED
                }
                is ControlResult.Error -> {
                    connectionStatus = RemoteConnectionStatus.OFFLINE
                    val msg = if (result.message == "connection_failed") "离线 / 服务未启动" else "连接失败"
                    snackbarHostState.showSnackbar(msg)
                }
            }
            isLoading = false
        }
    }

    fun sendCommand(endpoint: String) {
        val effectiveToken = getEffectiveToken()
        if (effectiveToken == null) {
            scope.launch { snackbarHostState.showSnackbar("请先输入 Token") }
            return
        }
        onAuthenticate("验证身份以发送控制命令", {
            scope.launch {
                isLoading = true
                when (val result = client.sendCommand(device.host, device.port, effectiveToken, endpoint)) {
                    is ControlResult.Success -> {
                        handleSuccessfulCommand(effectiveToken)
                        snackbarHostState.showSnackbar("命令已发送")
                        refreshStatus()
                    }
                    is ControlResult.Unauthorized -> {
                        connectionStatus = RemoteConnectionStatus.UNAUTHORIZED
                        snackbarHostState.showSnackbar("Token 错误")
                    }
                    is ControlResult.Timeout -> {
                        connectionStatus = RemoteConnectionStatus.TIMEOUT
                        snackbarHostState.showSnackbar("请求超时")
                    }
                    is ControlResult.Error -> {
                        connectionStatus = RemoteConnectionStatus.OFFLINE
                        val msg = if (result.message == "connection_failed") "设备离线或服务未启动" else "控制失败"
                        snackbarHostState.showSnackbar(msg)
                    }
                }
                isLoading = false
            }
        }, { error ->
            scope.launch { snackbarHostState.showSnackbar("本机认证失败: $error") }
        })
    }

    fun startFinding() {
        val effectiveToken = getEffectiveToken()
        if (effectiveToken == null) {
            scope.launch { snackbarHostState.showSnackbar("请先输入 Token") }
            return
        }
        onAuthenticate("验证身份以寻找手机", {
            scope.launch {
                isLoading = true
                // 1. Start ring
                val ringResult = client.sendCommand(device.host, device.port, effectiveToken, "/command/ring/start")
                
                if (ringResult is ControlResult.Unauthorized) {
                    connectionStatus = RemoteConnectionStatus.UNAUTHORIZED
                    snackbarHostState.showSnackbar("Token 错误")
                    isLoading = false
                    return@launch
                } else if (ringResult is ControlResult.Timeout) {
                    connectionStatus = RemoteConnectionStatus.TIMEOUT
                    snackbarHostState.showSnackbar("请求超时")
                    isLoading = false
                    return@launch
                } else if (ringResult is ControlResult.Error) {
                    connectionStatus = RemoteConnectionStatus.OFFLINE
                    val msg = if (ringResult.message == "connection_failed") "设备离线或服务未启动" else "控制失败"
                    snackbarHostState.showSnackbar(msg)
                    isLoading = false
                    return@launch
                }
                
                // 2. Start strobe
                val flashResult = client.sendCommand(device.host, device.port, effectiveToken, "/command/flash/strobe/start")
                
                if (flashResult is ControlResult.Success) {
                    handleSuccessfulCommand(effectiveToken)
                    connectionStatus = RemoteConnectionStatus.SEARCHING
                    snackbarHostState.showSnackbar("正在寻找手机")
                } else {
                    // Ring succeeded, flash failed
                    connectionStatus = RemoteConnectionStatus.PARTIAL_SUCCESS
                    snackbarHostState.showSnackbar("部分成功：响铃已启动，但手电控制失败")
                }
                
                refreshStatus()
                isLoading = false
            }
        }, { error ->
            scope.launch { snackbarHostState.showSnackbar("本机认证失败: $error") }
        })
    }

    fun stopFinding() {
        val effectiveToken = getEffectiveToken()
        if (effectiveToken == null) {
            scope.launch { snackbarHostState.showSnackbar("请先输入 Token") }
            return
        }
        onAuthenticate("验证身份以停止寻找", {
            scope.launch {
                isLoading = true
                when (val result = client.sendCommand(device.host, device.port, effectiveToken, "/command/stop-all")) {
                    is ControlResult.Success -> {
                        handleSuccessfulCommand(effectiveToken)
                        connectionStatus = RemoteConnectionStatus.STOPPED
                        snackbarHostState.showSnackbar("已停止寻找")
                        refreshStatus()
                    }
                    is ControlResult.Unauthorized -> {
                        connectionStatus = RemoteConnectionStatus.UNAUTHORIZED
                        snackbarHostState.showSnackbar("Token 错误")
                    }
                    is ControlResult.Timeout -> {
                        connectionStatus = RemoteConnectionStatus.TIMEOUT
                        snackbarHostState.showSnackbar("请求超时")
                    }
                    is ControlResult.Error -> {
                        connectionStatus = RemoteConnectionStatus.OFFLINE
                        val msg = if (result.message == "connection_failed") "设备离线或服务未启动" else "控制失败"
                        snackbarHostState.showSnackbar(msg)
                    }
                }
                isLoading = false
            }
        }, { error ->
            scope.launch { snackbarHostState.showSnackbar("本机认证失败: $error") }
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
                TextButton(onClick = onBack) { Text("← 返回设备列表") }
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
                            RemoteConnectionStatus.IDLE -> "未检测"
                            RemoteConnectionStatus.CONNECTING -> "连接中..."
                            RemoteConnectionStatus.ONLINE -> "在线"
                            RemoteConnectionStatus.OFFLINE -> "离线 / 服务未启动"
                            RemoteConnectionStatus.TIMEOUT -> "请求超时"
                            RemoteConnectionStatus.UNAUTHORIZED -> "Token 错误"
                            RemoteConnectionStatus.SEARCHING -> "正在寻找手机"
                            RemoteConnectionStatus.PARTIAL_SUCCESS -> "部分成功 (响铃中)"
                            RemoteConnectionStatus.STOPPED -> "已停止"
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
                        Text("开始寻找手机", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, textAlign = TextAlign.Center)
                    }
                    Button(
                        onClick = { stopFinding() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("停止寻找", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(device.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("控制地址: ${device.controlUrl}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IndicatorBox("远端响铃", if (ringActive) "鸣叫中" else "静音", ringActive, Modifier.weight(1f))
                        IndicatorBox("远端手电", when(flashMode) { "steady" -> "常亮"; "strobe" -> "爆闪"; else -> "关闭" }, flashMode != "off", Modifier.weight(1f))
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("配对鉴权", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
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
                                    Text("Token 已保存，可直接控制", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = { 
                                        tokenStore.clearToken(device.host, device.port)
                                        hasSavedToken = false
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("清除", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputToken,
                        onValueChange = { inputToken = it },
                        label = { Text(if (hasSavedToken) "输入新 Token 以更换" else "请输入被寻找端显示的 Token") },
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
                            Text("清空输入", fontSize = 12.sp)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("硬件控制", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { sendCommand("/command/flash/steady/start") }, modifier = Modifier.weight(1f)) { Text("常亮", fontSize = 12.sp) }
                    Button(onClick = { sendCommand("/command/flash/strobe/start") }, modifier = Modifier.weight(1f)) { Text("爆闪", fontSize = 12.sp) }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { sendCommand("/command/flash/stop") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("关闭手电", fontSize = 12.sp) }
                    Button(onClick = { refreshStatus() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) { Text("刷新状态", fontSize = 12.sp) }
                }

                Button(
                    onClick = { sendCommand("/command/stop-all") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("一键停止全部动作", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { sendCommand("/command/ring/start") }, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("拉响报警 (会发出声音)", fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = { sendCommand("/command/ring/stop") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("停止响铃")
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
        
        // UI Overlays (Back button, frame, etc.)
        TextButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
        ) {
            Text("关闭", color = Color.White, fontWeight = FontWeight.Bold)
        }
        
        Text(
            "将二维码放入框内即可扫码",
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
