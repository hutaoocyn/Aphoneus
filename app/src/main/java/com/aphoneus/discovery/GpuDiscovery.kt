package com.aphoneus.discovery

import com.aphoneus.model.GpuInfo
import com.aphoneus.model.GpuType
import com.aphoneus.root.ShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GpuDiscovery {

    suspend fun discoverGpu(): GpuInfo = withContext(Dispatchers.IO) {
        // 1. Qualcomm KGSL
        val kgslDevfreq = "/sys/class/kgsl/kgsl-3d0/devfreq"
        val kgslCheck = ShellExecutor.readLine("[ -d $kgslDevfreq ] && echo 'exists'")
        if (kgslCheck == "exists") {
            val batch = ShellExecutor.readBatch(listOf(
                "$kgslDevfreq/available_frequencies",
                "$kgslDevfreq/available_governors",
                "$kgslDevfreq/min_freq",
                "$kgslDevfreq/max_freq",
                "$kgslDevfreq/cur_freq",
                "$kgslDevfreq/governor",
                "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"
            ))

            val freqs = batch["$kgslDevfreq/available_frequencies"].orEmpty()
                .split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }.sorted()
            val govs = batch["$kgslDevfreq/available_governors"].orEmpty()
                .split(Regex("\\s+")).filter { it.isNotBlank() }

            return@withContext GpuInfo(
                type = GpuType.QUALCOMM_KGSL,
                path = "/sys/class/kgsl/kgsl-3d0",
                availableFreqsKHz = freqs,
                availableGovernors = govs,
                minFreqKHz = batch["$kgslDevfreq/min_freq"]?.toIntOrNull() ?: (freqs.firstOrNull() ?: 0),
                maxFreqKHz = batch["$kgslDevfreq/max_freq"]?.toIntOrNull() ?: (freqs.lastOrNull() ?: 0),
                curFreqKHz = batch["$kgslDevfreq/cur_freq"]?.toIntOrNull() ?: 0,
                currentGovernor = batch["$kgslDevfreq/governor"] ?: "msm-adreno-tz",
                busyPercent = batch["/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"]?.toIntOrNull() ?: 0
            )
        }

        // 2. ARM Mali Devfreq
        val maliDev = ShellExecutor.readLine("ls -d /sys/class/devfreq/*.mali 2>/dev/null").split(Regex("\\s+")).firstOrNull()
        if (!maliDev.isNullOrBlank()) {
            val batch = ShellExecutor.readBatch(listOf(
                "$maliDev/available_frequencies",
                "$maliDev/available_governors",
                "$maliDev/min_freq",
                "$maliDev/max_freq",
                "$maliDev/cur_freq",
                "$maliDev/governor"
            ))
            val freqs = batch["$maliDev/available_frequencies"].orEmpty()
                .split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }.sorted()
            val govs = batch["$maliDev/available_governors"].orEmpty()
                .split(Regex("\\s+")).filter { it.isNotBlank() }

            return@withContext GpuInfo(
                type = GpuType.ARM_MALI,
                path = maliDev,
                availableFreqsKHz = freqs,
                availableGovernors = govs,
                minFreqKHz = batch["$maliDev/min_freq"]?.toIntOrNull() ?: (freqs.firstOrNull() ?: 0),
                maxFreqKHz = batch["$maliDev/max_freq"]?.toIntOrNull() ?: (freqs.lastOrNull() ?: 0),
                curFreqKHz = batch["$maliDev/cur_freq"]?.toIntOrNull() ?: 0,
                currentGovernor = batch["$maliDev/governor"] ?: "simple_ondemand"
            )
        }

        // 3. Fallback: No dedicated GPU sysfs node found
        GpuInfo(
            type = GpuType.NONE_DETECTED,
            path = "",
            availableFreqsKHz = emptyList(),
            availableGovernors = emptyList(),
            minFreqKHz = 0,
            maxFreqKHz = 0,
            curFreqKHz = 0,
            currentGovernor = "unknown"
        )
    }
}
