package com.aphoneus.discovery

import com.aphoneus.model.CoolingDevice
import com.aphoneus.model.ThermalZone
import com.aphoneus.model.TripPoint
import com.aphoneus.root.ShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThermalDiscovery {

    suspend fun discoverThermalZones(): List<ThermalZone> = withContext(Dispatchers.IO) {
        val zonesOut = ShellExecutor.readLine("ls -d /sys/class/thermal/thermal_zone* 2>/dev/null")
        if (zonesOut.isBlank()) return@withContext emptyList()

        val zoneDirs = zonesOut.split(Regex("\\s+")).filter { it.isNotBlank() }
            .sortedBy { it.substringAfterLast("thermal_zone").toIntOrNull() ?: 0 }

        val paths = mutableListOf<String>()
        for (d in zoneDirs) {
            paths.add("$d/type")
            paths.add("$d/temp")
            paths.add("$d/trip_point_0_temp")
            paths.add("$d/trip_point_0_type")
        }

        val map = ShellExecutor.readBatch(paths)

        zoneDirs.mapNotNull { d ->
            val id = d.substringAfterLast("thermal_zone").toIntOrNull() ?: return@mapNotNull null
            val type = map["$d/type"].orEmpty().ifBlank { "zone$id" }
            val tempMilliC = map["$d/temp"]?.toIntOrNull() ?: 0

            val trip0Temp = map["$d/trip_point_0_temp"]?.toIntOrNull()
            val trip0Type = map["$d/trip_point_0_type"]
            val trips = if (trip0Temp != null && trip0Type != null) {
                listOf(TripPoint(0, trip0Temp, trip0Type))
            } else emptyList()

            ThermalZone(id = id, type = type, tempMilliC = tempMilliC, tripPoints = trips)
        }
    }

    suspend fun discoverCoolingDevices(): List<CoolingDevice> = withContext(Dispatchers.IO) {
        val cdevsOut = ShellExecutor.readLine("ls -d /sys/class/thermal/cooling_device* 2>/dev/null")
        if (cdevsOut.isBlank()) return@withContext emptyList()

        val cdevDirs = cdevsOut.split(Regex("\\s+")).filter { it.isNotBlank() }
        val paths = mutableListOf<String>()
        for (d in cdevDirs) {
            paths.add("$d/type")
            paths.add("$d/cur_state")
            paths.add("$d/max_state")
        }

        val map = ShellExecutor.readBatch(paths)
        cdevDirs.mapNotNull { d ->
            val id = d.substringAfterLast("cooling_device").toIntOrNull() ?: return@mapNotNull null
            val type = map["$d/type"].orEmpty()
            val cur = map["$d/cur_state"]?.toIntOrNull() ?: 0
            val max = map["$d/max_state"]?.toIntOrNull() ?: 0
            CoolingDevice(id, type, cur, max)
        }
    }
}
