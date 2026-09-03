package com.aphoneus.model

import kotlinx.serialization.Serializable

@Serializable
data class CapabilityReport(
    val rootEnvironment: String,
    val socPlatform: String,
    val cpuClustersCount: Int,
    val gpuDetected: String,
    val thermalZonesCount: Int,
    val zramAlgorithm: String,
    val discoveredOverriders: List<String>,
    val probedNodes: Map<String, Boolean>,
    val timestamp: Long = System.currentTimeMillis()
)
