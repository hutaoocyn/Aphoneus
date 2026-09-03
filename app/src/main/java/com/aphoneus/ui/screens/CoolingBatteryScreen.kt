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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aphoneus.model.BatteryState
import com.aphoneus.model.ThermalZone
import com.aphoneus.ui.components.TelemetryCard
import com.aphoneus.ui.theme.CriticalRust
import com.aphoneus.ui.theme.NominalCyan
import com.aphoneus.ui.theme.SuccessGreen
import com.aphoneus.ui.theme.SurfaceBorder
import com.aphoneus.ui.theme.SurfaceCanvas
import com.aphoneus.ui.theme.SurfaceContainer
import com.aphoneus.ui.theme.TextPrimary
import com.aphoneus.ui.theme.TextSecondary
import com.aphoneus.ui.theme.WarningAmber

@Composable
fun CoolingBatteryScreen(
    battery: BatteryState,
    thermalZones: List<ThermalZone>,
    onCooldownNow: () -> Unit,
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
        Text(
            text = "Cooling & Battery Subsystem",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Text(
            text = "Real-time thermal zone breakdown and hardware power telemetry",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // "Cooldown Now" One-Shot Action
        Button(
            onClick = onCooldownNow,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, NominalCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NominalCyan.copy(alpha = 0.2f),
                contentColor = TextPrimary
            )
        ) {
            Text(
                text = "❄️ Cooldown Now: Hard-Clamp to Minimum",
                style = MaterialTheme.typography.titleMedium,
                color = NominalCyan
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Battery Telemetry Cards
        Text(
            text = "Battery Power & Health",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryCard(
                title = "Discharge / Charge",
                primaryValue = "%.1f mA".format(battery.currentMa),
                secondaryValue = "%.2f V".format(battery.voltageMv / 1000.0),
                accentColor = if (battery.currentMa < 0) WarningAmber else SuccessGreen,
                modifier = Modifier.weight(1f)
            )
            TelemetryCard(
                title = "Total Power",
                primaryValue = "%.0f mW".format(battery.powerMw),
                secondaryValue = "${battery.capacityPercent}% (${battery.status})",
                accentColor = NominalCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        TelemetryCard(
            title = "Battery Temperature & Cycles",
            primaryValue = "%.1f °C".format(battery.tempCelsius),
            secondaryValue = "Charge Cycles: ${if (battery.cycleCount > 0) "${battery.cycleCount}" else "N/A"}",
            accentColor = if (battery.tempCelsius > 42.0) CriticalRust else NominalCyan
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Thermal Zones Breakdown Table
        Text(
            text = "Thermal Zones (${thermalZones.size} Detected)",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (thermalZones.isEmpty()) {
                    Text(
                        text = "No standard Linux thermal zones discovered.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    for (z in thermalZones.take(8)) {
                        val tempC = z.tempCelsius
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "zone${z.id}: ${z.type}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "%.1f °C".format(tempC),
                                style = MaterialTheme.typography.labelLarge,
                                color = when {
                                    tempC >= 80.0 -> CriticalRust
                                    tempC >= 55.0 -> WarningAmber
                                    else -> NominalCyan
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}
