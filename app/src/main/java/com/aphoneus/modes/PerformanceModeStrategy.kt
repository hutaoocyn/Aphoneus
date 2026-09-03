package com.aphoneus.modes

import com.aphoneus.model.Cluster
import com.aphoneus.model.GpuInfo
import com.aphoneus.root.ShellExecutor
import com.aphoneus.root.WriteResult

/**
 * Mode A: PERFORMANCE ("Frequency Lock to Maximum")
 * Semantics: Pin every cluster to its highest available frequency.
 * Widens cpusets, raises uclamp, sets performance governor, neutralizes overriders.
 */
object PerformanceModeStrategy {

    suspend fun apply(
        clusters: List<Cluster>,
        gpu: GpuInfo
    ): Map<String, WriteResult> {
        val writes = mutableListOf<Pair<String, String>>()

        // 1. CPU Clusters: Pin to highest available frequency
        for (c in clusters) {
            val highest = c.freqsKHz.lastOrNull() ?: c.maxFreqKHz
            val base = "/sys/devices/system/cpu/cpufreq/${c.policy}"

            // Range ordering: Set max first, then min = max
            writes.add("$base/scaling_max_freq" to "$highest")
            writes.add("$base/scaling_min_freq" to "$highest")

            // Governor
            val targetGov = if (c.governors.contains("performance")) "performance" else "schedutil"
            writes.add("$base/scaling_governor" to targetGov)

            if (targetGov == "schedutil") {
                writes.add("$base/schedutil/up_rate_limit_us" to "0")
                writes.add("$base/schedutil/down_rate_limit_us" to "2000")
            }
        }

        // 2. Bring all CPU cores online
        for (i in 0..15) {
            writes.add("/sys/devices/system/cpu/cpu$i/online" to "1")
        }

        // 3. Neutralize overriders
        writes.add("/sys/devices/system/cpu/cpu0/core_ctl/enable" to "0")
        writes.add("/sys/module/msm_performance/parameters/cpu_max_freq" to "4294967295")

        // 4. Widen cpusets for top-app
        writes.add("/dev/cpuset/top-app/cpus" to "0-15")

        // 5. uclamp boost
        writes.add("/proc/sys/kernel/sched_util_clamp_min" to "512")
        writes.add("/proc/sys/kernel/sched_util_clamp_max" to "1024")

        // 6. GPU Maximum Lock
        if (gpu.path.isNotBlank() && gpu.availableFreqsKHz.isNotEmpty()) {
            val gpuMax = gpu.availableFreqsKHz.last()
            val devfreq = "${gpu.path}/devfreq"
            writes.add("$devfreq/max_freq" to "$gpuMax")
            writes.add("$devfreq/min_freq" to "$gpuMax")
            val gpuGov = if (gpu.availableGovernors.contains("performance")) "performance" else "msm-adreno-tz"
            writes.add("$devfreq/governor" to gpuGov)
            writes.add("${gpu.path}/force_clk_on" to "1")
            writes.add("${gpu.path}/idle_timer" to "250")
        }

        // 7. Memory & VM
        writes.add("/proc/sys/vm/swappiness" to "30")

        // 8. Execute batch write
        val results = ShellExecutor.writeBatchVerified(writes)

        // 9. Framework-level fixed performance mode
        ShellExecutor.readLine("cmd power set-fixed-performance-mode-enabled true 2>/dev/null")
        ShellExecutor.readLine("settings put system peak_refresh_rate 120 2>/dev/null")

        return results
    }
}
