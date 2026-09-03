package com.aphoneus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aphoneus.model.PrimaryMode
import com.aphoneus.ui.components.DestructiveConfirmationDialog
import com.aphoneus.ui.theme.CriticalRust
import com.aphoneus.ui.theme.NominalCyan
import com.aphoneus.ui.theme.SuccessGreen
import com.aphoneus.ui.theme.SurfaceBorder
import com.aphoneus.ui.theme.SurfaceCanvas
import com.aphoneus.ui.theme.SurfaceContainer
import com.aphoneus.ui.theme.SurfaceContainerElevated
import com.aphoneus.ui.theme.TextPrimary
import com.aphoneus.ui.theme.TextSecondary

@Composable
fun ModesScreen(
    activeMode: PrimaryMode,
    onSelectMode: (PrimaryMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPerfConfirmDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceCanvas)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Operating Modes",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Text(
            text = "Mutually exclusive, atomically applied, instantly revertible",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Mode A: Performance
        ModeSelectionCard(
            title = "Performance / Frequency Lock",
            subtitle = "Pins all clusters to max available OPP frequency. Widens cpusets, raises uclamp, unlocks GPU max clock. Non-disableable thermal guard active at 85°C.",
            icon = Icons.Default.Speed,
            accentColor = CriticalRust,
            isActive = activeMode == PrimaryMode.PERFORMANCE,
            onClick = { showPerfConfirmDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mode B: Balanced (Stock)
        ModeSelectionCard(
            title = "Balanced (Pristine Stock)",
            subtitle = "Bit-for-bit restoration of pristine boot snapshot. Resets schedutil governor, framework settings, and thermal mitigation. Automatic panic target.",
            icon = Icons.Default.Restore,
            accentColor = SuccessGreen,
            isActive = activeMode == PrimaryMode.BALANCED,
            onClick = { onSelectMode(PrimaryMode.BALANCED) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mode C: Battery Saver / Max Cool
        ModeSelectionCard(
            title = "Battery Saver / Max Cool",
            subtitle = "Pins all clusters to lowest available OPP frequency. Restricts top-app cpuset to little cluster. GPU minimum clock lock. Deep Doze enabled.",
            icon = Icons.Default.BatterySaver,
            accentColor = NominalCyan,
            isActive = activeMode == PrimaryMode.BATTERY_SAVER,
            onClick = { onSelectMode(PrimaryMode.BATTERY_SAVER) }
        )

        Spacer(modifier = Modifier.height(96.dp))
    }

    if (showPerfConfirmDialog) {
        DestructiveConfirmationDialog(
            title = "Engage Performance Lock?",
            message = "This locks all CPU clusters and the GPU to maximum stock frequencies. Increased power draw and thermal generation will occur.",
            confirmButtonText = "Engage Lock",
            onConfirm = {
                showPerfConfirmDialog = false
                onSelectMode(PrimaryMode.PERFORMANCE)
            },
            onDismiss = { showPerfConfirmDialog = false }
        )
    }
}

@Composable
private fun ModeSelectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) accentColor else SurfaceBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) SurfaceContainerElevated else SurfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(10.dp)
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = accentColor)
                    }
                    Spacer(modifier = Modifier.padding(start = 12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
