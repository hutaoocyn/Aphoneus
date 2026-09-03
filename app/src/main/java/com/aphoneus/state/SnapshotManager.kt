package com.aphoneus.state

import android.content.Context
import com.aphoneus.model.Cluster
import com.aphoneus.model.GpuInfo
import com.aphoneus.model.NodeSnapshot
import com.aphoneus.model.SnapshotBundle
import com.aphoneus.root.ShellExecutor
import com.aphoneus.root.WriteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Captures, stores, and restores pristine boot snapshots of kernel sysfs nodes.
 * Safety guarantee: Mode Balanced restores the pristine boot snapshot bit-for-bit.
 */
object SnapshotManager {

    private const val PRISTINE_SNAPSHOT_FILE = "pristine_boot_snapshot.json"
    private var cachedPristineSnapshot: SnapshotBundle? = null

    suspend fun capturePristineIfNeeded(
        context: Context,
        clusters: List<Cluster>,
        gpu: GpuInfo
    ): SnapshotBundle = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, PRISTINE_SNAPSHOT_FILE)
        if (file.exists()) {
            runCatching {
                val jsonStr = file.readText()
                val bundle = Json.decodeFromString<SnapshotBundle>(jsonStr)
                cachedPristineSnapshot = bundle
                return@withContext bundle
            }
        }

        // Generate fresh snapshot
        val paths = compileTargetPaths(clusters, gpu)
        val readMap = ShellExecutor.readBatch(paths)

        val snapshots = readMap.mapNotNull { (path, value) ->
            if (value.isNotBlank()) NodeSnapshot(path, value) else null
        }

        val bundle = SnapshotBundle(
            timestamp = System.currentTimeMillis(),
            description = "Pristine Boot Snapshot",
            snapshots = snapshots
        )

        runCatching {
            file.writeText(Json.encodeToString(bundle))
        }
        cachedPristineSnapshot = bundle
        bundle
    }

    fun compileTargetPaths(clusters: List<Cluster>, gpu: GpuInfo): List<String> {
        val paths = mutableListOf<String>()

        for (c in clusters) {
            val base = "/sys/devices/system/cpu/cpufreq/${c.policy}"
            paths.add("$base/scaling_max_freq")
            paths.add("$base/scaling_min_freq")
            paths.add("$base/scaling_governor")
            paths.add("$base/schedutil/rate_limit_us")
            paths.add("$base/schedutil/up_rate_limit_us")
            paths.add("$base/schedutil/down_rate_limit_us")
        }

        if (gpu.path.isNotBlank()) {
            val devfreq = "${gpu.path}/devfreq"
            paths.add("$devfreq/min_freq")
            paths.add("$devfreq/max_freq")
            paths.add("$devfreq/governor")
            paths.add("${gpu.path}/force_clk_on")
            paths.add("${gpu.path}/idle_timer")
        }

        paths.add("/proc/sys/kernel/sched_util_clamp_min")
        paths.add("/proc/sys/kernel/sched_util_clamp_max")
        paths.add("/dev/cpuset/top-app/cpus")
        paths.add("/proc/sys/vm/swappiness")

        return paths
    }

    suspend fun restoreSnapshot(bundle: SnapshotBundle): Map<String, WriteResult> = withContext(Dispatchers.IO) {
        // Restore in reverse order to unwind state safely
        val reversed = bundle.snapshots.reversed()
        val pairs = reversed.map { it.path to it.value }
        ShellExecutor.writeBatchVerified(pairs)
    }

    fun getPristineSnapshot(): SnapshotBundle? = cachedPristineSnapshot
}
