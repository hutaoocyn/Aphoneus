package com.aphoneus.model

import kotlinx.serialization.Serializable

@Serializable
data class HardwareTelemetry(
    val clusterLiveFreqsKHz: Map<String, Int> = emptyMap(),
    val coreOnlineStates: Map<Int, Boolean> = emptyMap(),
    val gpuFreqKHz: Int = 0,
    val gpuBusyPercent: Int = 0,
    val battery: BatteryState = BatteryState(0.0, 0.0, 0.0, 0.0, 0, "Unknown", 0),
    val highestThermalCelsius: Double = 0.0,
    val thermalZones: List<ThermalZone> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
