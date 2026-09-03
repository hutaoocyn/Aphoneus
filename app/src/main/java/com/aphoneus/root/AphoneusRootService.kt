package com.aphoneus.root

import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager

/**
 * libsu RootService: Runs with UID 0 in a dedicated root daemon process.
 * Exposes FileSystemManager over IPC Binder for high-frequency remote sysfs reads
 * without the overhead of fork/exec on every single read.
 */
class AphoneusRootService : RootService() {

    override fun onBind(intent: Intent): IBinder {
        return FileSystemManager.getService()
    }
}
