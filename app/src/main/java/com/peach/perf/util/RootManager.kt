package com.peach.perf.util

import com.topjohnwu.superuser.Shell

object RootManager {
    
    init {
        Shell.enableVerboseLogging = true
    }
    
    fun init() {
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(10)
        )
    }
    
    fun isRootAvailable(): Boolean {
        return try {
            Shell.getShell().isRoot
        } catch (e: Exception) {
            false
        }
    }
}
