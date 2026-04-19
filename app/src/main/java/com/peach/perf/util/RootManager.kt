package com.peach.perf.util

import com.topjohnwu.superuser.Shell

object RootManager {
    
    init {
        // 静态初始化块中配置 Shell
        Shell.enableVerboseLogging = true
    }
    
    fun init() {
        try {
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(10)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun isRootAvailable(): Boolean {
        return try {
            // 先尝试获取 Shell
            val shell = Shell.getShell()
            val result = shell.isRoot
            // 如果还没 root，尝试请求
            if (!result) {
                Shell.cmd("echo test").exec()
                return Shell.getShell().isRoot
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun hasRoot(): Boolean = isRootAvailable()
}
