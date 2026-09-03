package com.aphoneus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.aphoneus.model.Cluster
import com.aphoneus.ui.theme.NavPillAccent
import com.aphoneus.ui.theme.NominalCyan
import com.aphoneus.ui.theme.SurfaceBorder
import com.aphoneus.ui.theme.SurfaceContainer
import com.aphoneus.ui.theme.SurfaceContainerElevated
import com.aphoneus.ui.theme.TextPrimary
import com.aphoneus.ui.theme.TextSecondary

/**
 * Discrete Frequency Control for CPU clusters:
 * Snaps strictly to the kernel's reported OPP table (never continuous float).
 * Features dual 48x48dp stepper buttons (+ / -) for motor tremor accessibility (WCAG 2.2 AA).
 */
@Composable
fun ClusterFrequencySlider(
    cluster: Cluster,
    selectedMinKHz: Int,
    selectedMaxKHz: Int,
    onRangeChanged: (minKHz: Int, maxKHz: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val table = cluster.freqsKHz.ifEmpty { listOf(cluster.minFreqKHz, cluster.maxFreqKHz) }
    val minIndex = table.indexOf(selectedMinKHz).coerceAtLeast(0)
    val maxIndex = table.indexOf(selectedMaxKHz).let { if (it < 0) table.lastIndex else it }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Cluster Name + Live Frequency Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = cluster.clusterDisplayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Governor: ${cluster.currentGovernor}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                // Tabular readout of selected max freq
                Text(
                    text = "${selectedMaxKHz / 1000} MHz",
                    style = MaterialTheme.typography.labelLarge,
                    color = NominalCyan
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stepper and Slider Controls for Max Frequency
            Text(
                text = "Maximum Scaling Frequency",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Discrete Stepper (-)
                IconButton(
                    onClick = {
                        val newIndex = (maxIndex - 1).coerceAtLeast(minIndex)
                        onRangeChanged(selectedMinKHz, table[newIndex])
                    },
                    enabled = maxIndex > minIndex,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerElevated)
                        .semantics { contentDescription = "Step down ${cluster.policy} max frequency" }
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, tint = TextPrimary)
                }

                // Discrete Snapped Slider
                Slider(
                    value = maxIndex.toFloat(),
                    onValueChange = { floatVal ->
                        val idx = floatVal.toInt().coerceIn(minIndex, table.lastIndex)
                        onRangeChanged(selectedMinKHz, table[idx])
                    },
                    valueRange = 0f..table.lastIndex.toFloat(),
                    steps = (table.size - 2).coerceAtLeast(0),
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = "${cluster.policy} maximum frequency slider"
                            stateDescription = "${selectedMaxKHz / 1000} Megahertz"
                        },
                    colors = SliderDefaults.colors(
                        thumbColor = NavPillAccent,
                        activeTrackColor = NavPillAccent,
                        inactiveTrackColor = SurfaceContainerElevated
                    )
                )

                // Discrete Stepper (+)
                IconButton(
                    onClick = {
                        val newIndex = (maxIndex + 1).coerceAtMost(table.lastIndex)
                        onRangeChanged(selectedMinKHz, table[newIndex])
                    },
                    enabled = maxIndex < table.lastIndex,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerElevated)
                        .semantics { contentDescription = "Step up ${cluster.policy} max frequency" }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Range Bounds Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Min: ${table.first() / 1000} MHz",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = "Max: ${table.last() / 1000} MHz",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}
