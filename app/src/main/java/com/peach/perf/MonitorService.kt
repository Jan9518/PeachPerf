package com.peach.perf

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.peach.perf.data.MonitorHolder
import com.peach.perf.data.SystemStats
import com.peach.perf.util.CpuLoadUtils
import com.peach.perf.util.MemoryUtils
import com.peach.perf.util.NetworkUtils
import com.peach.perf.util.ProcessUtils
import com.peach.perf.util.ThermalControlUtils
import kotlinx.coroutines.*

class MonitorService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notificationId = 1
    private val channelId = "peach_perf_monitor"

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("PeachPerf")
                .setContentText("性能监控运行中")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build()
            startForeground(notificationId, notification)

            scope.launch {
                while (isActive) {
                    try {
                        val stats = collectStats()
                        MonitorHolder.stats.value = stats
                        MonitorHolder.chart.value = MonitorHolder.chart.value.toMutableList().apply {
                            add(com.peach.perf.data.ChartDataPoint(
                                stats.cpuUsage,
                                stats.gpuUsage,
                                stats.memoryUsage,
                                stats.temperature
                            ))
                            if (size > 60) removeAt(0)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delay(1000)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private suspend fun collectStats(): SystemStats {
        // 使用 Scene 的工具类读取数据
        val cpu = try { CpuLoadUtils.getCpuUsage() } catch (e: Exception) { 0f }
        val gpu = try { 0f } catch (e: Exception) { 0f } // GPU 暂时用 0
        val memory = try { MemoryUtils.getMemoryUsage() } catch (e: Exception) { 0f }
        val temp = try { ThermalControlUtils.getTemperature() } catch (e: Exception) { 0f }
        val (rx, tx) = try { NetworkUtils.getNetworkSpeed() } catch (e: Exception) { 0L to 0L }
        val processes = try { ProcessUtils.getProcessList() } catch (e: Exception) { emptyList() }
        
        return SystemStats(
            timestamp = System.currentTimeMillis(),
            cpuUsage = cpu,
            gpuUsage = gpu,
            memoryUsage = memory,
            temperature = temp,
            networkRx = rx,
            networkTx = tx,
            topProcesses = processes
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "PeachPerf 监控", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onDestroy() { super.onDestroy(); scope.cancel() }
    override fun onBind(intent: Intent?): IBinder? = null
}
