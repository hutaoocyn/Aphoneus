package com.aphoneus.state

import android.content.Context
import com.aphoneus.model.Cluster
import com.aphoneus.model.GpuInfo
import com.aphoneus.root.ShellExecutor
import com.aphoneus.root.WriteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages full revert engine to stock state, drift detection, and global panic reset.
 */
object RevertEngine {

    suspend fun revertToPristine(
        context: Context,
        clusters: List<Cluster>,
        gpu: GpuInfo
    ): Map<String, WriteResult> = withContext(Dispatchers.IO) {
        val pristine = SnapshotManager.capturePristineIfNeeded(context, clusters, gpu)

        // 1. Reset framework services
        ShellExecutor.readLine("cmd thermalservice reset 2>/dev/null")
        ShellExecutor.readLine("cmd power set-fixed-performance-mode-enabled false 2>/dev/null")

        // 2. Restore all nodes from snapshot
        val results = SnapshotManager.restoreSnapshot(pristine)

        // 3. Remove boot persistence script if present
        ShellExecutor.readLine("rm -f /data/adb/service.d/aphoneus.sh 2>/dev/null")

        results
    }

    suspend fun detectDrift(
        context: Context,
        clusters: List<Cluster>,
        gpu: GpuInfo
    ): Map<String, Pair<String, String>> = withContext(Dispatchers.IO) {
        val pristine = SnapshotManager.capturePristineIfNeeded(context, clusters, gpu)
        val paths = pristine.snapshots.map { it.path }
        val currentLive = ShellExecutor.readBatch(paths)

        val driftMap = mutableMapOf<String, Pair<String, String>>()
        for (snap in pristine.snapshots) {
            val live = currentLive[snap.path]
            if (live != null && live != snap.value) {
                driftMap[snap.path] = Pair(snap.value, live)
            }
        }
        driftMap
    }
}
