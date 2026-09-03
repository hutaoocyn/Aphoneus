package com.aphoneus.model

import kotlinx.serialization.Serializable

enum class PrimaryMode {
    PERFORMANCE,
    BALANCED,
    BATTERY_SAVER,
    CUSTOM
}

@Serializable
data class ClusterConfig(
    val policy: String,
    val minFreqKHz: Int,
    val maxFreqKHz: Int,
    val governor: String
)

@Serializable
data class CustomProfile(
    val id: String,
    val name: String,
    val clusters: List<ClusterConfig>,
    val gpuMinFreqKHz: Int? = null,
    val gpuMaxFreqKHz: Int? = null,
    val gpuGovernor: String? = null,
    val uclampMin: Int? = null,
    val uclampMax: Int? = null,
    val coreOnlineMask: Map<Int, Boolean> = emptyMap()
)
