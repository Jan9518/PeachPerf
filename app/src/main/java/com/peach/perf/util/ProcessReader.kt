package com.peach.perf.util

import com.peach.perf.data.ProcessInfo
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

object ProcessReader {
    private val processStats = mutableMapOf<Int, ProcessStat>()
    private var lastTotalCpuTime = 0L
    private data class ProcessStat(val utime: Long, val stime: Long)

    suspend fun readTopProcesses(limit: Int = 10): List<ProcessInfo> = withContext(Dispatchers.IO) {
        val processes = mutableListOf<ProcessInfo>()
        val totalCpuTime = readTotalCpuTime()
        val totalCpuDiff = max(1L, totalCpuTime - lastTotalCpuTime)
        val dirList = Shell.cmd("ls /proc | grep '^[0-9]'").exec().out
        for (dir in dirList) {
            val pid = dir.toIntOrNull() ?: continue
            try {
                val statResult = Shell.cmd("cat /proc/$pid/stat 2>/dev/null").exec()
                if (!statResult.isSuccess) continue
                val stat = statResult.out.firstOrNull() ?: continue
                val parts = stat.split(" ")
                if (parts.size < 20) continue
                val name = parts[1].trim('(', ')')
                val utime = parts[13].toLongOrNull() ?: 0L
                val stime = parts[14].toLongOrNull() ?: 0L
                val state = parts[2]
                val threads = parts[19].toIntOrNull() ?: 1
                val totalTime = utime + stime
                val prev = processStats[pid]
                val cpuPercent = if (prev != null) {
                    val diff = totalTime - (prev.utime + prev.stime)
                    if (diff > 0 && totalCpuDiff > 0) (diff.toFloat() / totalCpuDiff * 100f).coerceIn(0f, 100f) else 0f
                } else 0f
                processStats[pid] = ProcessStat(utime, stime)
                val pss = try {
                    val s = Shell.cmd("cat /proc/$pid/statm 2>/dev/null").exec().out.firstOrNull()?.split("\\s+".toRegex())
                    if (s != null && s.size >= 2) (s[1].toLongOrNull() ?: 0L) * 4096L else 0L
                } catch (e: Exception) { 0L }
                processes.add(ProcessInfo(pid, name, 0, cpuPercent, pss, state, threads))
            } catch (e: Exception) {}
        }
        lastTotalCpuTime = totalCpuTime
        processes.sortedByDescending { it.cpuPercent }.take(limit)
    }

    private fun readTotalCpuTime(): Long = try {
        val result = Shell.cmd("cat /proc/stat").exec()
        if (!result.isSuccess) return 0L
        val stat = result.out.firstOrNull { it.startsWith("cpu ") } ?: return 0L
        stat.split("\\s+".toRegex()).drop(1).mapNotNull { it.toLongOrNull() }.sum()
    } catch (e: Exception) { 0L }
}
