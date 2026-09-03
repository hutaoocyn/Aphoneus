package com.aphoneus.root

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Manages the connection to the libsu RootService and cached root shell.
 */
object RootSession {

    private var remoteFs: FileSystemManager? = null
    private val _isRootGranted = MutableStateFlow(false)
    val isRootGranted: StateFlow<Boolean> = _isRootGranted.asStateFlow()

    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: StateFlow<Boolean> = _isServiceBound.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            remoteFs = FileSystemManager.getRemote(service)
            _isServiceBound.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            remoteFs = null
            _isServiceBound.value = false
        }
    }

    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        // Configure libsu shell parameters
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(10)
        )

        val rootAvailable = Shell.getShell().isRoot
        _isRootGranted.value = rootAvailable

        if (rootAvailable) {
            runCatching {
                val intent = Intent(context, AphoneusRootService::class.java)
                RootService.bind(intent, connection)
            }
        }
    }

    fun getFileSystemManager(): FileSystemManager? = remoteFs

    suspend fun readSysfs(path: String): String = withContext(Dispatchers.IO) {
        val fs = remoteFs
        if (fs != null) {
            runCatching {
                val file = fs.getFile(path)
                if (file.exists()) {
                    file.newInputStream().bufferedReader().use { it.readText().trim() }
                } else ""
            }.getOrElse { ShellExecutor.readLine(path) }
        } else {
            ShellExecutor.readLine(path)
        }
    }

    suspend fun writeSysfs(path: String, value: String): WriteResult {
        return ShellExecutor.writeVerified(path, value)
    }

    suspend fun writeBatch(pairs: List<Pair<String, String>>): Map<String, WriteResult> {
        return ShellExecutor.writeBatchVerified(pairs)
    }
}
