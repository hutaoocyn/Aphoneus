package com.aphoneus.model

import kotlinx.serialization.Serializable

/**
 * Normalized battery telemetry.
 * Discharging is always represented as negative mA and mW.
 * Charging is always represented as positive mA and mW.
 */
@Serializable
data class BatteryState(
    val currentMa: Double,
    val voltageMv: Double,
    val powerMw: Double,
    val tempCelsius: Double,
    val capacityPercent: Int,
    val status: String,
    val cycleCount: Int
)
