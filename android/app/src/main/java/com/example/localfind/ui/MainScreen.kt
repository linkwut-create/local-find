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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.launch
import org.json.JSONObject

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
    remoteTokenStore: RemoteDeviceTokenStore,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRestartServer: () -> Unit,
    onRegenerateToken: () -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onOpenDevice: (DiscoveredDevice) -> Unit,
    onTestRingToggle: () -> Unit,
    onTestFlashSteady: () -> Unit,
    onTestFlashStrobe: () -> Unit,
    onTestFlashStop: () -> Unit,
    onStopAll: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit,
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
                    onStartService = onStartService,
                    onStopService = onStopService,
                    onRestartServer = onRestartServer,
                    onRegenerateToken = onRegenerateToken,
                    onTestRingToggle = onTestRingToggle,
                    onTestFlashSteady = onTestFlashSteady,
                    onTestFlashStrobe = onTestFlashStrobe,
                    onTestFlashStop = onTestFlashStop,
                    onStopAll = onStopAll,
                    onRequestPermission = onRequestPermission,
                    onOpenBatterySettings = onOpenBatterySettings
                )
            } else {
                ControllerModeScreen(
                    discoveryStatus = discoveryStatus,
                    discoveredDevices = discoveredDevices,
                    tokenStore = remoteTokenStore,
                    onStartDiscovery = onStartDiscovery,
                    onStopDiscovery = onStopDiscovery,
                    onOpenBrowser = onOpenDevice
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
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRestartServer: () -> Unit,
    onRegenerateToken: () -> Unit,
    onTestRingToggle: () -> Unit,
    onTestFlashSteady: () -> Unit,
    onTestFlashStrobe: () -> Unit,
    onTestFlashStop: () -> Unit,
    onStopAll: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit,
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
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text("控制页地址:", style = MaterialTheme.typography.labelSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = controlUrl, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { clipboardManager.setText(AnnotatedString(controlUrl)) }) {
                                Text("复制", fontSize = 12.sp)
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
                    "服务正在以前台模式运行，但在某些设备上，系统仍可能为了省电而杀掉后台连接。",
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
    onOpenBrowser: (DiscoveredDevice) -> Unit
) {
    var selectedDevice by remember { mutableStateOf<DiscoveredDevice?>(null) }
    var recentDevice by remember { mutableStateOf(tokenStore.getRecentDevice()) }

    // Manual Connection States
    var manualHost by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("8888") }
    var manualName by remember { mutableStateOf("") }

    if (selectedDevice != null) {
        RemoteControlPanel(
            device = selectedDevice!!,
            tokenStore = tokenStore,
            onBack = { 
                selectedDevice = null 
                // Refresh recent device when coming back
                recentDevice = tokenStore.getRecentDevice()
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                }
            }

            // 3. Manual Connection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("手动连接 fallback", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualHost,
                            onValueChange = { manualHost = it },
                            label = { Text("IP 地址") },
                            modifier = Modifier.weight(2f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = manualPort,
                            onValueChange = { manualPort = it },
                            label = { Text("端口") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
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

                    Button(
                        onClick = {
                            val hostTrimmed = manualHost.trim()
                            val portInt = manualPort.toIntOrNull() ?: 8888
                            if (hostTrimmed.isNotBlank() && portInt in 1..65535) {
                                val name = manualName.ifBlank { "Manual Device" }
                                val device = DiscoveredDevice(
                                    name = name,
                                    host = hostTrimmed,
                                    port = portInt,
                                    controlUrl = "http://$hostTrimmed:$portInt"
                                )
                                tokenStore.saveRecentDevice(name, device.host, portInt)
                                selectedDevice = device
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        enabled = manualHost.isNotBlank() && (manualPort.toIntOrNull() ?: 0) in 1..65535
                    ) {
                        Text("连接到设备")
                    }
                }
            }

            // 4. Discovered Devices List
            if (discoveredDevices.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("暂未发现设备", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
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

@Composable
fun RemoteControlPanel(
    device: DiscoveredDevice,
    tokenStore: RemoteDeviceTokenStore,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val client = remember { RemoteControlClient() }
    val snackbarHostState = remember { SnackbarHostState() }
    
    var token by remember { mutableStateOf(tokenStore.getToken(device.host, device.port) ?: "") }
    var ringActive by remember { mutableStateOf(false) }
    var flashMode by remember { mutableStateOf("off") }
    var isLoading by remember { mutableStateOf(false) }

    fun refreshStatus() {
        scope.launch {
            isLoading = true
            when (val result = client.getStatus(device.host, device.port)) {
                is ControlResult.Success -> {
                    val json = result.statusJson
                    if (json != null) {
                        ringActive = json.optBoolean("ring_active", false)
                        flashMode = json.optString("flash_mode", "off")
                    }
                }
                is ControlResult.Timeout -> {
                    snackbarHostState.showSnackbar("刷新超时：设备无响应")
                }
                is ControlResult.Error -> {
                    snackbarHostState.showSnackbar("刷新失败: ${result.message}")
                }
                else -> {}
            }
            isLoading = false
        }
    }

    fun sendCommand(endpoint: String) {
        if (token.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("请输入 Token") }
            return
        }
        scope.launch {
            isLoading = true
            when (val result = client.sendCommand(device.host, device.port, token, endpoint)) {
                is ControlResult.Success -> {
                    tokenStore.saveToken(device.host, device.port, token)
                    refreshStatus()
                }
                is ControlResult.Unauthorized -> {
                    snackbarHostState.showSnackbar("Token 错误或未授权 (401)")
                }
                is ControlResult.Timeout -> {
                    snackbarHostState.showSnackbar("控制超时：硬件可能卡住或离线")
                }
                is ControlResult.Error -> {
                    snackbarHostState.showSnackbar("控制失败: ${result.message}")
                }
            }
            isLoading = false
        }
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
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("输入配对 Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Text("提示：Token 由被寻找端生成的 8 位代码。首次控制成功后将自动保存。", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
