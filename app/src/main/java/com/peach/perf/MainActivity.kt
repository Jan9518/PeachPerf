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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peach.perf.data.MonitorHolder
import com.peach.perf.util.RootManager
import kotlinx.coroutines.delay
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        RootManager.initialize()
        RootManager.requestRootPermission { granted ->
            runOnUiThread {
                Toast.makeText(this, if (granted) "✅ Root 权限已获取" else "❌ 未获取 Root 权限", Toast.LENGTH_SHORT).show()
            }
        }
        
        setContent {
            PeachPerfTheme {
                MainScreen()
            }
        }
    }
}
@Composable
fun PeachPerfTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFFB7C5),
            secondary = Color(0xFF00E5FF),
            tertiary = Color(0xFF00E676),
            background = Color(0xFF0D0D0D),
            surface = Color(0xFF1A1A2E),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}
@Composable
fun MainScreen() {
    val ctx = LocalContext.current
    val stats by MonitorHolder.stats.collectAsState(initial = null)
    var running by remember { mutableStateOf(false) }
    var root by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            root = try { RootManager.isRootAvailable() } catch (e: Exception) { false }
            overlay = Settings.canDrawOverlays(ctx)
            delay(1000)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D0D), Color(0xFF1A1A2E))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🍑 PeachPerf", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB7C5))
                    Text("蜜桃性能监控", fontSize = 14.sp, color = Color.Gray)
                }
                Spacer(Modifier.height(8.dp))
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatusItem("Root", root, Color(0xFF00E676), Color(0xFFFF6D00))
                        StatusItem("悬浮窗", overlay, Color(0xFF00E5FF), Color(0xFFFF6D00))
                        StatusItem("监控", stats != null, Color(0xFF00E676), Color.Gray)
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("实时数据", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        DataRow("CPU", stats?.cpuUsage ?: 0f, "%", Color(0xFF00E5FF))
                        DataRow("GPU", stats?.gpuUsage ?: 0f, "%", Color(0xFFD500F9))
                        DataRow("内存", stats?.memoryUsage ?: 0f, "%", Color(0xFF00E676))
                        DataRow("温度", stats?.temperature ?: 0f, "°C", Color(0xFFFF6D00))
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (overlay) {
                                ctx.startForegroundService(Intent(ctx, OverlayService::class.java))
                                ctx.startForegroundService(Intent(ctx, MonitorService::class.java))
                                running = true
                                Toast.makeText(ctx, "🍑 监控已启动", Toast.LENGTH_SHORT).show()
                            } else {
                                ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                    data = Uri.parse("package:${ctx.packageName}")
                                })
                            }
                        },
                        enabled = overlay && !running,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB7C5))
                    ) {
                        Text("▶ 启动", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            ctx.stopService(Intent(ctx, OverlayService::class.java))
                            ctx.stopService(Intent(ctx, MonitorService::class.java))
                            running = false
                            Toast.makeText(ctx, "⏹ 监控已停止", Toast.LENGTH_SHORT).show()
                        },
                        enabled = running,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444))
                    ) {
                        Text("■ 停止", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
@Composable
fun StatusItem(label: String, enabled: Boolean, trueColor: Color, falseColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (enabled) trueColor else falseColor)
        )
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(
            if (enabled) "已授权" else "未授权",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) trueColor else falseColor
        )
    }
}
@Composable
fun DataRow(label: String, value: Float, unit: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, color = Color.Gray)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value.toInt().toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                unit,
                fontSize = 16.sp,
                color = color.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}
