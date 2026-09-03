package com.aphoneus.modes

import com.aphoneus.model.Cluster
import com.aphoneus.model.CustomProfile
import com.aphoneus.model.GpuInfo
import com.aphoneus.root.ShellExecutor
import com.aphoneus.root.WriteResult

/**
 * Custom Mode: Applies user-selected per-cluster discrete sliders and governor choices.
 */
object CustomModeStrategy {

    suspend fun apply(
        profile: CustomProfile,
        discoveredClusters: List<Cluster>,
        gpu: GpuInfo
    ): Map<String, WriteResult> {
        val writes = mutableListOf<Pair<String, String>>()

        for (cConfig in profile.clusters) {
            val cluster = discoveredClusters.find { it.policy == cConfig.policy } ?: continue
            val seq = RangeOrderHelper.determineWriteSequence(
                policy = cluster.policy,
                minKHz = cConfig.minFreqKHz,
                maxKHz = cConfig.maxFreqKHz,
                curMaxKHz = cluster.maxFreqKHz
            )
            writes.addAll(seq)
            writes.add("/sys/devices/system/cpu/cpufreq/${cluster.policy}/scaling_governor" to cConfig.governor)
        }

        if (profile.uclampMin != null) {
            writes.add("/proc/sys/kernel/sched_util_clamp_min" to "${profile.uclampMin}")
        }
        if (profile.uclampMax != null) {
            writes.add("/proc/sys/kernel/sched_util_clamp_max" to "${profile.uclampMax}")
        }

        if (gpu.path.isNotBlank()) {
            val devfreq = "${gpu.path}/devfreq"
            if (profile.gpuMinFreqKHz != null) {
                writes.add("$devfreq/min_freq" to "${profile.gpuMinFreqKHz}")
            }
            if (profile.gpuMaxFreqKHz != null) {
                writes.add("$devfreq/max_freq" to "${profile.gpuMaxFreqKHz}")
            }
            if (profile.gpuGovernor != null) {
                writes.add("$devfreq/governor" to profile.gpuGovernor)
            }
        }

        return ShellExecutor.writeBatchVerified(writes)
    }
}
