package com.aphoneus.model

import kotlinx.serialization.Serializable

@Serializable
data class TripPoint(
    val id: Int,
    val tempMilliC: Int,
    val type: String
)

@Serializable
data class ThermalZone(
    val id: Int,
    val type: String,
    val tempMilliC: Int,
    val tripPoints: List<TripPoint> = emptyList()
) {
    val tempCelsius: Double
        get() = tempMilliC / 1000.0
}

@Serializable
data class CoolingDevice(
    val id: Int,
    val type: String,
    val curState: Int,
    val maxState: Int
)
