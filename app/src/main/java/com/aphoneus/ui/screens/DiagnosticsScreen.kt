package com.aphoneus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aphoneus.model.CapabilityReport
import com.aphoneus.ui.theme.CriticalRust
import com.aphoneus.ui.theme.NavPillAccent
import com.aphoneus.ui.theme.NominalCyan
import com.aphoneus.ui.theme.SuccessGreen
import com.aphoneus.ui.theme.SurfaceBorder
import com.aphoneus.ui.theme.SurfaceCanvas
import com.aphoneus.ui.theme.SurfaceContainer
import com.aphoneus.ui.theme.TextPrimary
import com.aphoneus.ui.theme.TextSecondary

@Composable
fun DiagnosticsScreen(
    report: CapabilityReport,
    onInstallBootScript: () -> Unit,
    onRemoveBootScript: () -> Unit,
    onExportReportJson: () -> String,
    modifier: Modifier = Modifier
) {
    var bootScriptStatus by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceCanvas)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Hardware Diagnostics",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Text(
            text = "Verified capability probe & reboot persistence watchdog",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // System Capabilities Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DiagnosticRow("Root Manager", report.rootEnvironment)
                DiagnosticRow("SoC Platform", report.socPlatform)
                DiagnosticRow("CPU Clusters", "${report.cpuClustersCount} policy nodes discovered")
                DiagnosticRow("GPU Architecture", report.gpuDetected)
                DiagnosticRow("Thermal Sensors", "${report.thermalZonesCount} zones accessible")
                DiagnosticRow("ZRAM Algorithm", report.zramAlgorithm)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Boot Persistence Control
        Text(
            text = "Reboot Persistence Service",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Text(
            text = "Installs /data/adb/service.d/aphoneus.sh with crash watchdog",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    onInstallBootScript()
                    bootScriptStatus = "Installed to /data/adb/service.d/aphoneus.sh"
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavPillAccent)
            ) {
                Text("Enable on Boot", color = TextPrimary)
            }

            Spacer(modifier = Modifier.padding(horizontal = 6.dp))

            OutlinedButton(
                onClick = {
                    onRemoveBootScript()
                    bootScriptStatus = "Persistence script removed"
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Disable Boot", color = TextSecondary)
            }
        }

        if (bootScriptStatus != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = bootScriptStatus.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = NominalCyan
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Discovered Overriders
        Text(
            text = "Detected Vendor Overriders",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (report.discoveredOverriders.isEmpty()) {
                    Text(
                        text = "No active Qualcomm/MediaTek/Xiaomi overriders detected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SuccessGreen
                    )
                } else {
                    for (overrider in report.discoveredOverriders) {
                        Text(
                            text = "• $overrider",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CriticalRust
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
    }
}
