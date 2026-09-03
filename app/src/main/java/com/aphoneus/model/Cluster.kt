package com.aphoneus.model

import kotlinx.serialization.Serializable

/**
 * Discovered CPU cluster from /sys/devices/system/cpu/cpufreq/policy*.
 * Never assumes cluster count or names; discovered dynamically.
 */
@Serializable
data class Cluster(
    val policy: String,               // e.g. "policy0"
    val cpus: List<Int>,              // e.g. [0, 1, 2, 3] from related_cpus
    val freqsKHz: List<Int>,          // sorted distinct frequencies from scaling_available_frequencies
    val governors: List<String>,      // e.g. ["schedutil", "performance", "powersave"]
    val minFreqKHz: Int = freqsKHz.firstOrNull() ?: 0,
    val maxFreqKHz: Int = freqsKHz.lastOrNull() ?: 0,
    val currentGovernor: String = "schedutil",
    val curFreqKHz: Int = minFreqKHz
) {
    val clusterDisplayName: String
        get() {
            val cpusStr = if (cpus.size == 1) "CPU ${cpus.first()}" else "CPUs ${cpus.first()}-${cpus.last()}"
            return "$policy ($cpusStr)"
        }
}
