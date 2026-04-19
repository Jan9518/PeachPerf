package com.peach.perf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peach.perf.data.MonitorHolder
import com.peach.perf.util.RootManager
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 先初始化 Shell，请求 Root 权限
        Shell.getShell { shell ->
            RootManager.init()
        }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val ctx = LocalContext.current
    val stats by MonitorHolder.stats.collectAsState(initial = null)
    var running by remember { mutableStateOf(false) }
    var root by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf(false) }
    var debugInfo by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            root = RootManager.isRootAvailable()
            overlay = Settings.canDrawOverlays(ctx)
            debugInfo = "Root: $root, Overlay: $overlay\nStats: ${stats != null}"
            delay(1000)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("PeachPerf", color = Color(0xFFFFB7C5), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("蜜桃性能监控", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("系统状态", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Root: ${if (root) "已授权" else "未授权"}", color = if (root) Color(0xFF00E676) else Color(0xFFFF6D00))
                        Text("悬浮窗: ${if (overlay) "已授权" else "未授权"}", color = if (overlay) Color(0xFF00E676) else Color(0xFFFF6D00))
                        Text("数据连接: ${if (stats != null) "正常" else "无数据"}", color = if (stats != null) Color(0xFF00E676) else Color(0xFFFF6D00))
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("实时数据", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("CPU: ${stats?.cpuUsage?.toInt() ?: 0}%", color = Color(0xFF00E5FF), fontSize = 20.sp, fontFamily = FontFamily.Monospace)
                        Text("GPU: ${stats?.gpuUsage?.toInt() ?: 0}%", color = Color(0xFFD500F9), fontSize = 20.sp, fontFamily = FontFamily.Monospace)
                        Text("内存: ${stats?.memoryUsage?.toInt() ?: 0}%", color = Color(0xFF00E676), fontSize = 20.sp, fontFamily = FontFamily.Monospace)
                        Text("温度: ${stats?.temperature?.toInt() ?: 0}°C", color = Color(0xFFFF6D00), fontSize = 20.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            item {
                Text(debugInfo, color = Color.Gray, fontSize = 12.sp)
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (overlay && root) {
                                ctx.startForegroundService(Intent(ctx, OverlayService::class.java))
                                ctx.startForegroundService(Intent(ctx, MonitorService::class.java))
                                running = true
                                Toast.makeText(ctx, "服务已启动", Toast.LENGTH_SHORT).show()
                            } else if (!overlay) {
                                ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply { data = Uri.parse("package:${ctx.packageName}") })
                            } else {
                                Toast.makeText(ctx, "请授予Root权限", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = overlay && !running,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB7C5))
                    ) { Text("启动") }
                    Button(
                        onClick = {
                            ctx.stopService(Intent(ctx, OverlayService::class.java))
                            ctx.stopService(Intent(ctx, MonitorService::class.java))
                            running = false
                            Toast.makeText(ctx, "服务已停止", Toast.LENGTH_SHORT).show()
                        },
                        enabled = running,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444))
                    ) { Text("停止") }
                }
            }
        }
    }
}
