package com.aphoneus.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aphoneus.model.Cluster
import com.aphoneus.model.ClusterConfig
import com.aphoneus.model.CustomProfile
import com.aphoneus.ui.components.ClusterFrequencySlider
import com.aphoneus.ui.theme.NavPillAccent
import com.aphoneus.ui.theme.SurfaceCanvas
import com.aphoneus.ui.theme.TextPrimary
import com.aphoneus.ui.theme.TextSecondary
import java.util.UUID

@Composable
fun CustomClusterScreen(
    clusters: List<Cluster>,
    onApplyCustomProfile: (CustomProfile) -> Unit,
    onSaveProfile: (CustomProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // State holding user-selected min and max per cluster policy
    val rangesMap = remember(clusters) {
        mutableStateMapOf<String, Pair<Int, Int>>().apply {
            for (c in clusters) {
                put(c.policy, Pair(c.minFreqKHz, c.maxFreqKHz))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceCanvas)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Custom Per-Cluster Tuning",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Text(
            text = "Independent discrete frequency controls snapped to kernel OPP tables",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Sliders for EVERY discovered cluster
        for (cluster in clusters) {
            val range = rangesMap[cluster.policy] ?: Pair(cluster.minFreqKHz, cluster.maxFreqKHz)
            ClusterFrequencySlider(
                cluster = cluster,
                selectedMinKHz = range.first,
                selectedMaxKHz = range.second,
                onRangeChanged = { newMin, newMax ->
                    rangesMap[cluster.policy] = Pair(newMin, newMax)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons: Apply & Save
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    val configs = clusters.map { c ->
                        val r = rangesMap[c.policy] ?: Pair(c.minFreqKHz, c.maxFreqKHz)
                        ClusterConfig(c.policy, r.first, r.second, c.currentGovernor)
                    }
                    val profile = CustomProfile(
                        id = UUID.randomUUID().toString(),
                        name = "Custom Tuning",
                        clusters = configs
                    )
                    onApplyCustomProfile(profile)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavPillAccent)
            ) {
                Text("Apply Profile", color = TextPrimary)
            }

            Spacer(modifier = Modifier.padding(horizontal = 8.dp))

            OutlinedButton(
                onClick = {
                    val configs = clusters.map { c ->
                        val r = rangesMap[c.policy] ?: Pair(c.minFreqKHz, c.maxFreqKHz)
                        ClusterConfig(c.policy, r.first, r.second, c.currentGovernor)
                    }
                    val profile = CustomProfile(
                        id = UUID.randomUUID().toString(),
                        name = "Saved Profile",
                        clusters = configs
                    )
                    onSaveProfile(profile)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Profile", color = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}
