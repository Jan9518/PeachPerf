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
        // 简化版：先返回测试数据，确保编译通过
        return SystemStats(
            timestamp = System.currentTimeMillis(),
            cpuUsage = 0f,
            gpuUsage = 0f,
            memoryUsage = 0f,
            temperature = 0f,
            networkRx = 0L,
            networkTx = 0L,
            topProcesses = emptyList()
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
