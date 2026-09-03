package com.aphoneus.modes

import com.aphoneus.model.Cluster
import com.aphoneus.model.GpuInfo
import com.aphoneus.root.ShellExecutor
import com.aphoneus.root.WriteResult

/**
 * Mode C: BATTERY SAVER / "MAX COOL"
 * Semantics: Lock every cluster to its lowest available frequency.
 * Uses cpuset restriction (little cluster for top-app) rather than dangerous hotplugging.
 * Enables Doze, powersave governor, throttles refresh rate, and tunes memory.
 */
object BatterySaverModeStrategy {

    suspend fun apply(
        clusters: List<Cluster>,
        gpu: GpuInfo
    ): Map<String, WriteResult> {
        val writes = mutableListOf<Pair<String, String>>()

        // 1. CPU Clusters: Pin to lowest available frequency
        for (c in clusters) {
            val lowest = c.freqsKHz.firstOrNull() ?: c.minFreqKHz
            val base = "/sys/devices/system/cpu/cpufreq/${c.policy}"

            // Range ordering: Set min first, then max = min
            writes.add("$base/scaling_min_freq" to "$lowest")
            writes.add("$base/scaling_max_freq" to "$lowest")

            val targetGov = if (c.governors.contains("powersave")) "powersave" else "schedutil"
            writes.add("$base/scaling_governor" to targetGov)

            if (targetGov == "schedutil") {
                writes.add("$base/schedutil/up_rate_limit_us" to "4000")
                writes.add("$base/schedutil/down_rate_limit_us" to "1000")
            }
        }

        // 2. Cpuset Restriction: Restrict top-app to policy0 / little cores (safe on Samsung/Exynos)
        val littleCluster = clusters.firstOrNull()
        if (littleCluster != null && littleCluster.cpus.isNotEmpty()) {
            val cpusRange = "${littleCluster.cpus.first()}-${littleCluster.cpus.last()}"
            writes.add("/dev/cpuset/top-app/cpus" to cpusRange)
        }

        // 3. uclamp capping
        writes.add("/proc/sys/kernel/sched_util_clamp_min" to "0")
        writes.add("/proc/sys/kernel/sched_util_clamp_max" to "400")

        // 4. GPU Minimum Lock
        if (gpu.path.isNotBlank() && gpu.availableFreqsKHz.isNotEmpty()) {
            val gpuMin = gpu.availableFreqsKHz.first()
            val devfreq = "${gpu.path}/devfreq"
            writes.add("$devfreq/min_freq" to "$gpuMin")
            writes.add("$devfreq/max_freq" to "$gpuMin")
            val gpuGov = if (gpu.availableGovernors.contains("powersave")) "powersave" else "simple_ondemand"
            writes.add("$devfreq/governor" to gpuGov)
            writes.add("${gpu.path}/idle_timer" to "50")
        }

        // 5. Memory & VM
        writes.add("/proc/sys/vm/swappiness" to "100")

        // 6. Execute batch write
        val results = ShellExecutor.writeBatchVerified(writes)

        // 7. Framework battery saver and Doze
        ShellExecutor.readLine("settings put global low_power 1 2>/dev/null")
        ShellExecutor.readLine("settings put system peak_refresh_rate 60 2>/dev/null")
        ShellExecutor.readLine("cmd deviceidle step deep 2>/dev/null")

        return results
    }
}
