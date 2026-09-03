package com.aphoneus.model

import kotlinx.serialization.Serializable

enum class GpuType {
    QUALCOMM_KGSL,
    ARM_MALI,
    GENERIC_DEVFREQ,
    NONE_DETECTED
}

@Serializable
data class GpuInfo(
    val type: GpuType,
    val path: String,
    val availableFreqsKHz: List<Int>,
    val availableGovernors: List<String>,
    val minFreqKHz: Int,
    val maxFreqKHz: Int,
    val curFreqKHz: Int,
    val currentGovernor: String,
    val busyPercent: Int = 0
)
