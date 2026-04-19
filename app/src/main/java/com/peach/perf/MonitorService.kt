package com.peach.perf

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.peach.perf.data.ChartDataPoint
import com.peach.perf.data.MonitorHolder
import com.peach.perf.data.SystemStats
import com.peach.perf.util.ProcessReader
import com.peach.perf.util.SysInfoReader
import kotlinx.coroutines.*

class MonitorService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notificationId = 1
    private val channelId = "peach_perf_monitor"

    override fun onCreate() {
        super.onCreate()
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
                val stats = collectStats()
                MonitorHolder.stats.value = stats
                MonitorHolder.batteryLevel.value = SysInfoReader.readBatteryLevel()
                MonitorHolder.batteryVoltage.value = SysInfoReader.readBatteryVoltage()
                MonitorHolder.batteryCurrent.value = SysInfoReader.readBatteryCurrent()
                MonitorHolder.chart.value = MonitorHolder.chart.value.toMutableList().apply {
                    add(ChartDataPoint(stats.cpuUsage, stats.gpuUsage, stats.memoryUsage, stats.temperature))
                    if (size > 60) removeAt(0)
                }
                val (rx, tx) = stats.networkRx to stats.networkTx
                MonitorHolder.todayRxBytes.value = MonitorHolder.todayRxBytes.value + rx
                MonitorHolder.todayTxBytes.value = MonitorHolder.todayTxBytes.value + tx
                delay(1000)
            }
        }
    }

    private suspend fun collectStats(): SystemStats {
        val cpu = SysInfoReader.readCpuUsage()
        val gpu = SysInfoReader.readGpuLoad()
        val memory = SysInfoReader.readMemoryUsage()
        val temp = SysInfoReader.readMaxTemperature()
        val (rx, tx) = SysInfoReader.readNetworkSpeed()
        val processes = ProcessReader.readTopProcesses(10)
        return SystemStats(System.currentTimeMillis(), cpu, gpu, memory, temp, rx, tx, processes)
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
