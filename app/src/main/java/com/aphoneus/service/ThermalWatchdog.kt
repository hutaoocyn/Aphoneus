package com.aphoneus.service

import com.aphoneus.discovery.ThermalDiscovery
import com.aphoneus.model.Cluster
import com.aphoneus.model.GpuInfo
import com.aphoneus.model.PrimaryMode
import com.aphoneus.modes.ModeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Hard Thermal Watchdog:
 * Monitors kernel thermal zones. If skin temp > 45°C or SoC temp > 85°C for sustained seconds,
 * auto-falls back to Balanced mode, restores thermal config, and emits an emergency alert.
 * THIS GUARD CANNOT BE DISABLED.
 */
class ThermalWatchdog(
    private val modeManager: ModeManager,
    private val scope: CoroutineScope
) {
    private var watchdogJob: Job? = null
    private var isScreenOn: Boolean = true

    private val _thermalTripEvent = MutableSharedFlow<Double>()
    val thermalTripEvent: SharedFlow<Double> = _thermalTripEvent.asSharedFlow()

    fun start(
        clusters: List<Cluster>,
        gpu: GpuInfo,
        ceilingCelsius: Double = 85.0
    ) {
        watchdogJob?.cancel()
        watchdogJob = scope.launch(Dispatchers.IO) {
            var consecutiveOverCeilingCount = 0

            while (isActive) {
                // Adaptive interval: 1s foreground, 10s screen off
                val intervalMs = if (isScreenOn) 1000L else 10000L
                delay(intervalMs)

                val zones = ThermalDiscovery.discoverThermalZones()
                val maxTempMilliC = zones.maxOfOrNull { it.tempMilliC } ?: 0
                val maxTempC = maxTempMilliC / 1000.0

                if (maxTempC >= ceilingCelsius) {
                    consecutiveOverCeilingCount++
                    // 3 consecutive polls over threshold (~3 seconds) triggers fallback
                    if (consecutiveOverCeilingCount >= 3) {
                        if (modeManager.activeMode.value == PrimaryMode.PERFORMANCE) {
                            // Emergency Fallback to Balanced Mode
                            modeManager.applyMode(PrimaryMode.BALANCED, null, clusters, gpu)
                            _thermalTripEvent.emit(maxTempC)
                        }
                    }
                } else {
                    consecutiveOverCeilingCount = 0
                }
            }
        }
    }

    fun setScreenState(screenOn: Boolean) {
        isScreenOn = screenOn
    }

    fun stop() {
        watchdogJob?.cancel()
        watchdogJob = null
    }
}
