package com.aphoneus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aphoneus.ui.theme.CriticalRust
import com.aphoneus.ui.theme.NominalCyan
import com.aphoneus.ui.theme.SurfaceBorder
import com.aphoneus.ui.theme.SurfaceContainer
import com.aphoneus.ui.theme.SurfaceContainerElevated
import com.aphoneus.ui.theme.TextPrimary
import com.aphoneus.ui.theme.TextSecondary
import com.aphoneus.ui.theme.WarningAmber

@Composable
fun ThermalGauge(
    currentTempCelsius: Double,
    ceilingTempCelsius: Double = 85.0,
    modifier: Modifier = Modifier
) {
    val progress = (currentTempCelsius / 100.0).coerceIn(0.0, 1.0).toFloat()
    val statusColor = when {
        currentTempCelsius >= ceilingTempCelsius -> CriticalRust
        currentTempCelsius >= 65.0 -> WarningAmber
        else -> NominalCyan
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .semantics {
                    contentDescription = "SoC Thermal Status"
                    progressBarRangeInfo = ProgressBarRangeInfo(currentTempCelsius.toFloat(), 20f..100f)
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Silicon Thermal Headroom",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "%.1f °C".format(currentTempCelsius),
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Labeled Progress Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceContainerElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Nominal: <65°C", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(text = "Safety Ceiling: ${ceilingTempCelsius.toInt()}°C", style = MaterialTheme.typography.labelSmall, color = CriticalRust)
            }
        }
    }
}
