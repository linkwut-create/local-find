package com.example.localfind.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localfind.server.NsdStatus
import com.example.localfind.server.DiscoveryStatus
import com.example.localfind.server.DiscoveredDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isServiceRunning: Boolean,
    localIp: String?,
    port: Int,
    ringActive: Boolean,
    flashMode: String,
    nsdStatus: NsdStatus,
    nsdServiceType: String,
    discoveryStatus: DiscoveryStatus,
    discoveredDevices: List<DiscoveredDevice>,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onOpenDevice: (DiscoveredDevice) -> Unit,
    onTestRingToggle: () -> Unit,
    onTestFlashSteady: () -> Unit,
    onTestFlashStrobe: () -> Unit,
    onTestFlashStop: () -> Unit,
    onStopAll: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Local Find 被寻找端",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            val statusColor by animateColorAsState(
                targetValue = if (isServiceRunning) Color(0xFF4CAF50) else Color(0xFFF44336),
                label = "statusColor"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "服务状态",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = statusColor,
                            modifier = Modifier.size(12.dp)
                        ) {}
                        Text(
                            text = if (isServiceRunning) "正在监听 (Running)" else "未运行 (Stopped)",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isServiceRunning) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("当前手机局域网 IP:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = localIp ?: "Wi-Fi 未连接",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (localIp != null) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("监听端口:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = port.toString(),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("NSD 状态:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = when(nsdStatus) {
                                NsdStatus.IDLE -> "未广播"
                                NsdStatus.ADVERTISING -> "广播中"
                                NsdStatus.ADVERTISED -> "已广播"
                                NsdStatus.FAILED -> "广播失败"
                            },
                            fontWeight = FontWeight.Bold,
                            color = when(nsdStatus) {
                                NsdStatus.ADVERTISED -> Color(0xFF2E7D32)
                                NsdStatus.FAILED -> Color(0xFFC62828)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("服务类型:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = nsdServiceType,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isServiceRunning && localIp != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "外部控制基准：http://$localIp:$port",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Controls Section (Service Toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { 
                        onRequestPermission()
                        onStartService() 
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = !isServiceRunning
                ) {
                    Text("启动寻机服务", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onStopService,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = isServiceRunning
                ) {
                    Text("停止寻机服务", fontWeight = FontWeight.Bold)
                }
            }

            // Discovery Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "发现局域网设备 (NSD Scanner)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "扫描状态: " + when(discoveryStatus) {
                                DiscoveryStatus.IDLE -> "未扫描"
                                DiscoveryStatus.SCANNING -> "扫描中..."
                                DiscoveryStatus.FAILED -> "扫描失败"
                                DiscoveryStatus.STOPPED -> "已停止"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (discoveryStatus == DiscoveryStatus.SCANNING) Color(0xFF1976D2) else Color.Gray
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onStartDiscovery,
                                enabled = discoveryStatus != DiscoveryStatus.SCANNING,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("开始扫描", fontSize = 12.sp)
                            }
                            FilledTonalButton(
                                onClick = onStopDiscovery,
                                enabled = discoveryStatus == DiscoveryStatus.SCANNING,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("停止", fontSize = 12.sp)
                            }
                        }
                    }

                    if (discoveredDevices.isEmpty()) {
                        Text(
                            "暂未发现设备",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        discoveredDevices.forEach { device ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(device.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "${device.host}:${device.port}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    Button(
                                        onClick = { onOpenDevice(device) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("打开控制页", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hardware Action Controls (Both for testing locally)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "硬件寻机外设测试 (本地)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Alarm status indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (ringActive) Color(0xFFFFEB3B).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("音频响铃", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = if (ringActive) "鸣叫中" else "静音",
                                    fontWeight = FontWeight.Bold,
                                    color = if (ringActive) Color(0xFFE65100) else Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (flashMode != "off") Color(0xFFFFEB3B).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("手电筒闪光", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = when(flashMode) {
                                        "steady" -> "常亮开启"
                                        "strobe" -> "闪烁爆闪"
                                        else -> "已关闭"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = if (flashMode != "off") Color(0xFFE65100) else Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Test Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTestRingToggle,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (ringActive) "停止响铃" else "测试响铃", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onTestFlashSteady,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("测试常亮", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTestFlashStrobe,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("测试闪烁", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onTestFlashStop,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("关闭手电", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = onStopAll,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("一键停止全部动作", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Client instruction help box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "电脑端局域网调用测试 (curl)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "请在电脑终端直接运行以下 curl 命令来触发寻机声光警示：",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "curl -X POST http://${localIp ?: "192.168.x.x"}:8888/command/ring/start",
                            modifier = Modifier.padding(8.dp),
                            color = Color(0xFF00FF00),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
