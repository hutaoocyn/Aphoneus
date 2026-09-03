package com.aphoneus.discovery

import com.aphoneus.model.BatteryState
import com.aphoneus.root.ShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

object BatteryDiscovery {

    suspend fun readBatteryState(): BatteryState = withContext(Dispatchers.IO) {
        val bDir = "/sys/class/power_supply/battery"
        val batch = ShellExecutor.readBatch(listOf(
            "$bDir/current_now",
            "$bDir/voltage_now",
            "$bDir/temp",
            "$bDir/capacity",
            "$bDir/status",
            "$bDir/cycle_count"
        ))

        val rawCurrent = batch["$bDir/current_now"]?.toDoubleOrNull() ?: 0.0
        val rawVoltage = batch["$bDir/voltage_now"]?.toDoubleOrNull() ?: 0.0
        val rawTemp = batch["$bDir/temp"]?.toDoubleOrNull() ?: 0.0
        val capacity = batch["$bDir/capacity"]?.toIntOrNull() ?: 0
        val status = batch["$bDir/status"] ?: "Unknown"
        val cycles = batch["$bDir/cycle_count"]?.toIntOrNull() ?: 0

        // Normalize units:
        // current_now is typically in microamperes (uA). Convert to milliamperes (mA).
        var currentMa = if (abs(rawCurrent) > 100000.0) rawCurrent / 1000.0 else rawCurrent
        // voltage_now is typically in microvolts (uV). Convert to millivolts (mV).
        val voltageMv = if (rawVoltage > 100000.0) rawVoltage / 1000.0 else rawVoltage
        // temp is typically in tenths of deg C (e.g. 350 = 35.0 C) or millidegrees (35000 = 35.0 C).
        val tempCelsius = when {
            rawTemp > 1000.0 -> rawTemp / 1000.0
            rawTemp > 100.0 -> rawTemp / 10.0
            else -> rawTemp
        }

        // OEM sign normalization:
        // Linux standard: discharge is negative mA, charge is positive mA.
        // Some OEMs (Samsung/MediaTek) report positive current_now during discharge.
        if (status.equals("Discharging", ignoreCase = true) && currentMa > 0) {
            currentMa = -currentMa
        } else if (status.equals("Charging", ignoreCase = true) && currentMa < 0) {
            currentMa = -currentMa
        }

        val powerMw = (abs(currentMa) * (voltageMv / 1000.0))

        BatteryState(
            currentMa = currentMa,
            voltageMv = voltageMv,
            powerMw = powerMw,
            tempCelsius = tempCelsius,
            capacityPercent = capacity,
            status = status,
            cycleCount = cycles
        )
    }
}
