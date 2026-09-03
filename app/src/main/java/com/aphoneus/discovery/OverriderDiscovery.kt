package com.aphoneus.discovery

import com.aphoneus.root.ShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Probes vendor overriders that fight user-applied frequency and governor locks.
 */
object OverriderDiscovery {

    suspend fun detectOverriders(): List<String> = withContext(Dispatchers.IO) {
        val detected = mutableListOf<String>()

        val checkPaths = listOf(
            "/sys/module/msm_performance/parameters/cpu_max_freq" to "Qualcomm msm_performance",
            "/sys/module/cpu_boost/parameters/input_boost_freq" to "Qualcomm cpu_boost",
            "/proc/ppm/policy_status" to "MediaTek PPM (Processor Power Management)",
            "/sys/class/thermal/thermal_message/sconfig" to "Xiaomi thermal_message"
        )

        for ((path, desc) in checkPaths) {
            val exists = ShellExecutor.readLine("[ -e $path ] && echo 'yes'") == "yes"
            if (exists) detected.add(desc)
        }

        // Check running vendor background daemons
        val psOutput = ShellExecutor.readLine("ps -A -o CMDLINE 2>/dev/null | grep -E 'perfd|mpdecision|thermal-engine|mi_thermald|perfservice' || echo ''")
        if (psOutput.isNotBlank()) {
            val daemons = psOutput.split(Regex("\\s+")).filter { it.isNotBlank() }.distinct()
            for (d in daemons) {
                detected.add("Daemon: $d")
            }
        }

        detected
    }
}
