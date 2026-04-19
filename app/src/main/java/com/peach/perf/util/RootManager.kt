package com.peach.perf.util

import com.topjohnwu.superuser.Shell

object RootManager {
    fun init() {
        Shell.setDefaultBuilder(Shell.Builder.create().setTimeout(10))
    }
    fun isRootAvailable(): Boolean = try { Shell.getShell().isRoot } catch (e: Exception) { false }
}
