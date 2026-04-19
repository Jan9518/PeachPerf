package com.peach.perf.data

import kotlinx.coroutines.flow.MutableStateFlow

data class ProcessInfo(
    val pid: Int,
    val name: String,
    val uid: Int,
    val cpuPercent: Float,
    val memoryPss: Long,
    val state: String,
    val threads: Int
)

data class SystemStats(
    val timestamp: Long,
    val cpuUsage: Float,
    val gpuUsage: Float,
    val memoryUsage: Float,
    val temperature: Float,
    val networkRx: Long,
    val networkTx: Long,
    val topProcesses: List<ProcessInfo>
)

data class CpuCoreInfo(
    val id: Int,
    val usage: Float,
    val currentFreq: Int,
    val maxFreq: Int
)

data class ChartDataPoint(
    val cpu: Float,
    val gpu: Float,
    val memory: Float,
    val temp: Float,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class AnomalyType {
    data class HighCpu(val usage: Float) : AnomalyType()
    data class HighMemory(val usage: Float) : AnomalyType()
    data class HighTemperature(val temp: Float) : AnomalyType()
    data class AbnormalProcess(val process: ProcessInfo) : AnomalyType()
}

object MonitorHolder {
    val stats = MutableStateFlow<SystemStats?>(null)
    val anomalies = MutableStateFlow<List<AnomalyType>>(emptyList())
    val chart = MutableStateFlow<List<ChartDataPoint>>(emptyList())
    val batteryLevel = MutableStateFlow(0)
    val batteryVoltage = MutableStateFlow(0f)
    val batteryCurrent = MutableStateFlow(0)
    val todayRxBytes = MutableStateFlow(0L)
    val todayTxBytes = MutableStateFlow(0L)
}
