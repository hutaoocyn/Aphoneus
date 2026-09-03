package com.aphoneus.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aphoneus.discovery.BatteryDiscovery
import com.aphoneus.discovery.ClusterDiscovery
import com.aphoneus.discovery.GpuDiscovery
import com.aphoneus.discovery.HardwareStateProbe
import com.aphoneus.discovery.ThermalDiscovery
import com.aphoneus.model.CapabilityReport
import com.aphoneus.model.Cluster
import com.aphoneus.model.GpuInfo
import com.aphoneus.model.GpuType
import com.aphoneus.model.HardwareTelemetry
import com.aphoneus.model.PrimaryMode
import com.aphoneus.modes.ModeManager
import com.aphoneus.service.BootPersistenceManager
import com.aphoneus.service.ModeForegroundService
import com.aphoneus.state.ProfileRepository
import com.aphoneus.state.SnapshotManager
import com.aphoneus.ui.components.FloatingSegmentedNavBar
import com.aphoneus.ui.components.NavDestination
import com.aphoneus.ui.screens.CoolingBatteryScreen
import com.aphoneus.ui.screens.CustomClusterScreen
import com.aphoneus.ui.screens.DashboardScreen
import com.aphoneus.ui.screens.DiagnosticsScreen
import com.aphoneus.ui.screens.ModesScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun MainNavHost(
    context: Context,
    modeManager: ModeManager,
    profileRepo: ProfileRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var currentDestination by remember { mutableStateOf(NavDestination.DASHBOARD) }

    val activeMode by modeManager.activeMode.collectAsState()

    var clusters by remember { mutableStateOf<List<Cluster>>(emptyList()) }
    var gpu by remember {
        mutableStateOf(
            GpuInfo(GpuType.NONE_DETECTED, "", emptyList(), emptyList(), 0, 0, 0, "unknown")
        )
    }
    var telemetry by remember { mutableStateOf(HardwareTelemetry()) }
    var capabilityReport by remember {
        mutableStateOf(
            CapabilityReport("Detecting...", "Detecting...", 0, "Probing...", 0, "lz4", emptyList(), emptyMap())
        )
    }

    // Initial Hardware Discovery & Pristine Boot Snapshot
    LaunchedEffect(Unit) {
        val discoveredClusters = ClusterDiscovery.discoverClusters()
        val discoveredGpu = GpuDiscovery.discoverGpu()
        clusters = discoveredClusters
        gpu = discoveredGpu

        // Capture Pristine Boot Snapshot before any mutations
        SnapshotManager.capturePristineIfNeeded(context, discoveredClusters, discoveredGpu)

        // Generate Capability Report
        capabilityReport = HardwareStateProbe.generateCapabilityReport()
    }

    // Adaptive Telemetry Polling (1s interval in foreground)
    LaunchedEffect(Unit) {
        while (isActive) {
            val liveClusters = ClusterDiscovery.discoverClusters()
            clusters = liveClusters
            val battery = BatteryDiscovery.readBatteryState()
            val thermals = ThermalDiscovery.discoverThermalZones()
            val maxThermalC = thermals.maxOfOrNull { it.tempCelsius } ?: 30.0

            val freqsMap = liveClusters.associate { it.policy to it.curFreqKHz }

            telemetry = HardwareTelemetry(
                clusterLiveFreqsKHz = freqsMap,
                coreOnlineStates = emptyMap(),
                gpuFreqKHz = gpu.curFreqKHz,
                gpuBusyPercent = gpu.busyPercent,
                battery = battery,
                highestThermalCelsius = maxThermalC,
                thermalZones = thermals
            )

            delay(1000L)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Active Screen Content
        when (currentDestination) {
            NavDestination.DASHBOARD -> {
                DashboardScreen(
                    activeMode = activeMode,
                    telemetry = telemetry,
                    clusters = clusters,
                    gpu = gpu,
                    onPanicReset = {
                        coroutineScope.launch {
                            modeManager.panicReset(clusters, gpu)
                            ModeForegroundService.start(context, PrimaryMode.BALANCED)
                        }
                    }
                )
            }
            NavDestination.MODES -> {
                ModesScreen(
                    activeMode = activeMode,
                    onSelectMode = { selectedMode ->
                        coroutineScope.launch {
                            modeManager.applyMode(selectedMode, null, clusters, gpu)
                            ModeForegroundService.start(context, selectedMode)
                        }
                    }
                )
            }
            NavDestination.QUICK_CYCLE -> {
                // Handled via onQuickCycle action; fallback renders Modes
                ModesScreen(activeMode = activeMode, onSelectMode = { m ->
                    coroutineScope.launch { modeManager.applyMode(m, null, clusters, gpu) }
                })
            }
            NavDestination.CUSTOM -> {
                CustomClusterScreen(
                    clusters = clusters,
                    onApplyCustomProfile = { profile ->
                        coroutineScope.launch {
                            modeManager.applyMode(PrimaryMode.CUSTOM, profile, clusters, gpu)
                            ModeForegroundService.start(context, PrimaryMode.CUSTOM)
                        }
                    },
                    onSaveProfile = { profile ->
                        coroutineScope.launch {
                            profileRepo.saveCustomProfile(profile)
                        }
                    }
                )
            }
            NavDestination.DIAGNOSTICS -> {
                DiagnosticsScreen(
                    report = capabilityReport,
                    onInstallBootScript = {
                        coroutineScope.launch {
                            BootPersistenceManager.installBootScript(activeMode, clusters)
                        }
                    },
                    onRemoveBootScript = {
                        coroutineScope.launch {
                            BootPersistenceManager.removeBootScript()
                        }
                    },
                    onExportReportJson = {
                        kotlinx.serialization.json.Json.encodeToString(capabilityReport)
                    }
                )
            }
        }

        // Floating Capsule Segmented Navigation Bar (Matching reference image 2986.jpg)
        FloatingSegmentedNavBar(
            currentDestination = currentDestination,
            onNavigate = { dest -> currentDestination = dest },
            onQuickCycle = {
                // Rapid cycle: Balanced -> Performance -> Battery Saver -> Balanced
                val nextMode = when (activeMode) {
                    PrimaryMode.BALANCED -> PrimaryMode.PERFORMANCE
                    PrimaryMode.PERFORMANCE -> PrimaryMode.BATTERY_SAVER
                    PrimaryMode.BATTERY_SAVER, PrimaryMode.CUSTOM -> PrimaryMode.BALANCED
                }
                coroutineScope.launch {
                    modeManager.applyMode(nextMode, null, clusters, gpu)
                    ModeForegroundService.start(context, nextMode)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
