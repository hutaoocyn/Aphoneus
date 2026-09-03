package com.aphoneus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aphoneus.model.Cluster
import com.aphoneus.model.GpuInfo
import com.aphoneus.model.HardwareTelemetry
import com.aphoneus.model.PrimaryMode
import com.aphoneus.ui.components.PanicButton
import com.aphoneus.ui.components.TelemetryCard
import com.aphoneus.ui.components.ThermalGauge
import com.aphoneus.ui.theme.CriticalRust
import com.aphoneus.ui.theme.NavPillAccent
import com.aphoneus.ui.theme.NominalCyan
import com.aphoneus.ui.theme.SuccessGreen
import com.aphoneus.ui.theme.SurfaceBorder
import com.aphoneus.ui.theme.SurfaceCanvas
import com.aphoneus.ui.theme.SurfaceContainer
import com.aphoneus.ui.theme.TextPrimary
import com.aphoneus.ui.theme.TextSecondary
import com.aphoneus.ui.theme.WarningAmber

@Composable
fun DashboardScreen(
    activeMode: PrimaryMode,
    telemetry: HardwareTelemetry,
    clusters: List<Cluster>,
    gpu: GpuInfo,
    onPanicReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceCanvas)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // App Title & Active Mode Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Aphoneus",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Kernel Topology & Telemetry",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Mode Badge
            val (badgeColor, badgeText) = when (activeMode) {
                PrimaryMode.PERFORMANCE -> Pair(CriticalRust, "PERFORMANCE LOCK")
                PrimaryMode.BALANCED -> Pair(SuccessGreen, "BALANCED (STOCK)")
                PrimaryMode.BATTERY_SAVER -> Pair(NominalCyan, "BATTERY SAVER")
                PrimaryMode.CUSTOM -> Pair(NavPillAccent, "CUSTOM PROFILE")
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Thermal Gauge (Top-level safety metric)
        ThermalGauge(
            currentTempCelsius = telemetry.highestThermalCelsius.coerceAtLeast(25.0),
            ceilingTempCelsius = 85.0
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Power Consumption Telemetry (mA and mW)
        val powerStr = "%.0f mW".format(telemetry.battery.powerMw)
        val currentStr = "%.1f mA (%s)".format(telemetry.battery.currentMa, telemetry.battery.status)
        TelemetryCard(
            title = "Instantaneous Power Draw",
            primaryValue = powerStr,
            secondaryValue = currentStr,
            accentColor = if (telemetry.battery.currentMa < 0) WarningAmber else SuccessGreen
        )

        Spacer(modifier = Modifier.height(16.dp))

        // GPU Telemetry (if available)
        if (gpu.path.isNotBlank()) {
            val gpuFreqStr = "${telemetry.gpuFreqKHz / 1000} MHz"
            val gpuBusyStr = "Load: ${telemetry.gpuBusyPercent}% | ${gpu.currentGovernor}"
            TelemetryCard(
                title = "GPU Engine (${gpu.type.name})",
                primaryValue = gpuFreqStr,
                secondaryValue = gpuBusyStr,
                accentColor = NominalCyan
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // CPU Cluster Live Frequencies
        Text(
            text = "CPU Cluster Frequencies",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        for (cluster in clusters) {
            val liveKHz = telemetry.clusterLiveFreqsKHz[cluster.policy] ?: cluster.curFreqKHz
            val liveMHz = liveKHz / 1000
            val minMHz = cluster.minFreqKHz / 1000
            val maxMHz = cluster.maxFreqKHz / 1000

            TelemetryCard(
                title = cluster.clusterDisplayName,
                primaryValue = "$liveMHz MHz",
                secondaryValue = "Range: $minMHz - $maxMHz MHz | ${cluster.currentGovernor}",
                accentColor = NavPillAccent
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Global Panic Reset
        PanicButton(onPanicReset = onPanicReset)

        // Extra spacing so floating capsule navbar never occludes bottom content
        Spacer(modifier = Modifier.height(96.dp))
    }
}
