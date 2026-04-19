package com.peach.perf.util
import com.topjohnwu.superuser.Shell
object RootManager {
    fun initialize() {
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER or Shell.FLAG_NONROOT_SHELL)
                .setTimeout(10_000)
        )
        Shell.enableVerboseLogging = true
    }
    fun requestRootPermission(onResult: (Boolean) -> Unit) {
        Shell.getShell { shell ->
            onResult(shell.isRoot)
        }
    }
    fun isRootAvailable(): Boolean {
        return try {
            Shell.getShell().isRoot
        } catch (e: Exception) {
            false
        }
    }
}
