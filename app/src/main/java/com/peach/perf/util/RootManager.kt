package com.peach.perf.util

object RootManager {
    
    fun init() {
        // Scene 的 Root 初始化
        KeepShell.init()
    }
    
    fun isRootAvailable(): Boolean {
        return CheckRootStatus.isRootAvailable()
    }
}
