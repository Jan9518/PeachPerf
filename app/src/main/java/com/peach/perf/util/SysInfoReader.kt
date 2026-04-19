package com.peach.perf.util

import com.peach.perf.data.CpuCoreInfo
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

object SysInfoReader {
    private var lastCpuTotal = 0L
    private var lastCpuIdle = 0L
    private var lastRx = 0L
    private var lastTx = 0L
    private var lastNetTime = 0L

    suspend fun readCpuUsage(): Float = withContext(Dispatchers.IO) {
        try {
            val result = Shell.cmd("cat /proc/stat").exec()
            if (!result.isSuccess) return@withContext 0f
            val stat = result.out.firstOrNull { it.startsWith("cpu ") } ?: return@withContext 0f
            val parts = stat.split("\\s+".toRegex()).drop(1).mapNotNull { it.toLongOrNull() }
            if (parts.size < 4) return@withContext 0f
            val user = parts[0]; val nice = parts[1]; val system = parts[2]; val idle = parts[3]
            val iowait = parts.getOrNull(4) ?: 0L; val irq = parts.getOrNull(5) ?: 0L; val softirq = parts.getOrNull(6) ?: 0L
            val total = user + nice + system + idle + iowait + irq + softirq
            val totalDiff = total - lastCpuTotal; val idleDiff = idle - lastCpuIdle
            lastCpuTotal = total; lastCpuIdle = idle
            if (totalDiff <= 0) return@withContext 0f
            ((totalDiff - idleDiff).toFloat() / totalDiff * 100f).coerceIn(0f, 100f)
        } catch (e: Exception) { 0f }
    }

    suspend fun readGpuLoad(): Float = withContext(Dispatchers.IO) {
        try {
            val paths = listOf("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage", "/sys/class/kgsl/kgsl-3d0/devfreq/gpu_load")
            for (p in paths) {
                val result = Shell.cmd("cat $p 2>/dev/null").exec()
                if (result.isSuccess) {
                    val v = result.out.firstOrNull()?.trim()?.toIntOrNull() ?: 0
                    return@withContext v.coerceIn(0, 100).toFloat()
                }
            }
            0f
        } catch (e: Exception) { 0f }
    }

    suspend fun readMemoryUsage(): Float = withContext(Dispatchers.IO) {
        try {
            val result = Shell.cmd("cat /proc/meminfo").exec()
            if (!result.isSuccess) return@withContext 0f
            var total = 0L; var avail = 0L
            for (line in result.out) {
                when {
                    line.startsWith("MemTotal:") -> total = line.split("\\s+".toRegex()).getOrNull(1)?.toLongOrNull() ?: 0L
                    line.startsWith("MemAvailable:") -> avail = line.split("\\s+".toRegex()).getOrNull(1)?.toLongOrNull() ?: 0L
                }
                if (total > 0 && avail > 0) break
            }
            if (total <= 0) return@withContext 0f
            ((total - avail).toFloat() / total * 100f).coerceIn(0f, 100f)
        } catch (e: Exception) { 0f }
    }

    suspend fun readMaxTemperature(): Float = withContext(Dispatchers.IO) {
        try {
            var max = 0f
            val result = Shell.cmd("ls /sys/class/thermal").exec()
            if (!result.isSuccess) return@withContext 0f
            for (tz in result.out) {
                if (tz.startsWith("thermal_zone")) {
                    val res = Shell.cmd("cat /sys/class/thermal/$tz/temp 2>/dev/null").exec()
                    if (res.isSuccess) {
                        val raw = res.out.firstOrNull()?.trim()?.toIntOrNull() ?: 0
                        if (raw in 1..100000) { val c = raw / 1000f; if (c > max) max = c }
                    }
                }
            }
            max
        } catch (e: Exception) { 0f }
    }

    suspend fun readNetworkSpeed(): Pair<Long, Long> = withContext(Dispatchers.IO) {
        try {
            var rx = 0L; var tx = 0L
            val result = Shell.cmd("cat /proc/net/dev").exec()
            if (!result.isSuccess) return@withContext Pair(0L, 0L)
            for (line in result.out) {
                if (line.contains("wlan") || line.contains("rmnet") || line.contains("eth")) {
                    val parts = line.split(":").getOrNull(1)?.trim()?.split("\\s+".toRegex()) ?: continue
                    if (parts.size >= 10) { rx += parts[0].toLongOrNull() ?: 0; tx += parts[8].toLongOrNull() ?: 0 }
                }
            }
            val now = System.currentTimeMillis(); val dt = max(1L, now - lastNetTime)
            var rxDiff = rx - lastRx; var txDiff = tx - lastTx
            if (rxDiff < 0) rxDiff = 0; if (txDiff < 0) txDiff = 0
            lastRx = rx; lastTx = tx; lastNetTime = now
            Pair(rxDiff * 1000L / dt, txDiff * 1000L / dt)
        } catch (e: Exception) { Pair(0L, 0L) }
    }

    suspend fun readBatteryVoltage(): Float = withContext(Dispatchers.IO) {
        try {
            val result = Shell.cmd("cat /sys/class/power_supply/battery/voltage_now 2>/dev/null").exec()
            if (result.isSuccess) return@withContext (result.out.firstOrNull()?.trim()?.toIntOrNull() ?: 0) / 1000000f
            0f
        } catch (e: Exception) { 0f }
    }

    suspend fun readBatteryCurrent(): Int = withContext(Dispatchers.IO) {
        try {
            val result = Shell.cmd("cat /sys/class/power_supply/battery/current_now 2>/dev/null").exec()
            if (result.isSuccess) return@withContext (result.out.firstOrNull()?.trim()?.toIntOrNull() ?: 0) / 1000
            0
        } catch (e: Exception) { 0 }
    }

    suspend fun readBatteryLevel(): Int = withContext(Dispatchers.IO) {
        try {
            val result = Shell.cmd("cat /sys/class/power_supply/battery/capacity 2>/dev/null").exec()
            if (result.isSuccess) return@withContext result.out.firstOrNull()?.trim()?.toIntOrNull() ?: 0
            0
        } catch (e: Exception) { 0 }
    }
}
