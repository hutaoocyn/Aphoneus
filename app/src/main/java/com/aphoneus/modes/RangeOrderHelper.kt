package com.aphoneus.modes

import com.aphoneus.model.Cluster
import com.aphoneus.root.ShellExecutor
import com.aphoneus.root.WriteResult

/**
 * Enforces strict write-ordering when updating frequency ranges to avoid kernel -EINVAL.
 * In Linux cpufreq, writing scaling_min_freq > current scaling_max_freq immediately fails with EINVAL.
 * Similarly, writing scaling_max_freq < current scaling_min_freq fails with EINVAL.
 *
 * Algorithm:
 * If new max >= current max:
 *   1. Write scaling_max_freq = new max (widens upper bound first)
 *   2. Write scaling_min_freq = new min
 * Else:
 *   1. Write scaling_min_freq = new min (narrows lower bound first)
 *   2. Write scaling_max_freq = new max
 */
object RangeOrderHelper {

    suspend fun applyRange(
        cluster: Cluster,
        minKHz: Int,
        maxKHz: Int,
        curMaxKHz: Int
    ): Pair<WriteResult, WriteResult> {
        val lo = minKHz.coerceAtMost(maxKHz)
        val hi = maxKHz.coerceAtLeast(minKHz)
        val base = "/sys/devices/system/cpu/cpufreq/${cluster.policy}"

        return if (hi >= curMaxKHz) {
            val rMax = ShellExecutor.writeVerified("$base/scaling_max_freq", "$hi")
            val rMin = ShellExecutor.writeVerified("$base/scaling_min_freq", "$lo")
            Pair(rMin, rMax)
        } else {
            val rMin = ShellExecutor.writeVerified("$base/scaling_min_freq", "$lo")
            val rMax = ShellExecutor.writeVerified("$base/scaling_max_freq", "$hi")
            Pair(rMin, rMax)
        }
    }

    /**
     * Determines the ordered list of writes for batch transactions.
     */
    fun determineWriteSequence(
        policy: String,
        minKHz: Int,
        maxKHz: Int,
        curMaxKHz: Int
    ): List<Pair<String, String>> {
        val lo = minKHz.coerceAtMost(maxKHz)
        val hi = maxKHz.coerceAtLeast(minKHz)
        val base = "/sys/devices/system/cpu/cpufreq/$policy"

        return if (hi >= curMaxKHz) {
            listOf(
                "$base/scaling_max_freq" to "$hi",
                "$base/scaling_min_freq" to "$lo"
            )
        } else {
            listOf(
                "$base/scaling_min_freq" to "$lo",
                "$base/scaling_max_freq" to "$hi"
            )
        }
    }
}
