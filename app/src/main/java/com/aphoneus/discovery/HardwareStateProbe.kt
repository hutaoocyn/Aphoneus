package com.aphoneus.discovery

import com.aphoneus.model.CapabilityReport
import com.aphoneus.root.RootManagerDetector
import com.aphoneus.root.ShellExecutor
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HardwareStateProbe {

    suspend fun generateCapabilityReport(fs: FileSystemManager? = null): CapabilityReport = withContext(Dispatchers.IO) {
        val rootEnv = RootManagerDetector.detect()
        val clusters = ClusterDiscovery.discoverClusters(fs)
        val gpu = GpuDiscovery.discoverGpu()
        val thermals = ThermalDiscovery.discoverThermalZones()
        val zramAlgo = MemoryDiscovery.selectBestZramAlgorithm()
        val overriders = OverriderDiscovery.detectOverriders()

        val socPlatform = ShellExecutor.readLine("getprop ro.board.platform").ifBlank {
            ShellExecutor.readLine("getprop ro.hardware")
        }

        val testNodes = listOf(
            "/sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq",
            "/sys/devices/system/cpu/cpufreq/policy0/scaling_governor",
            "/sys/class/kgsl/kgsl-3d0/devfreq/max_freq",
            "/sys/block/zram0/comp_algorithm",
            "/proc/sys/kernel/sched_util_clamp_min",
            "/dev/cpuset/top-app/cpus"
        )

        val probedNodesMap = mutableMapOf<String, Boolean>()
        for (node in testNodes) {
            val exists = ShellExecutor.readLine("[ -e $node ] && echo '1'") == "1"
            probedNodesMap[node] = exists
        }

        CapabilityReport(
            rootEnvironment = "${rootEnv.manager.name} (${rootEnv.version})",
            socPlatform = socPlatform.ifBlank { "Generic ARM64 SoC" },
            cpuClustersCount = clusters.size,
            gpuDetected = "${gpu.type.name} (${gpu.path})",
            thermalZonesCount = thermals.size,
            zramAlgorithm = zramAlgo,
            discoveredOverriders = overriders,
            probedNodes = probedNodesMap
        )
    }
}
