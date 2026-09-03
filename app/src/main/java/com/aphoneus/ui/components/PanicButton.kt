package com.aphoneus.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aphoneus.ui.theme.CriticalRust
import com.aphoneus.ui.theme.TextPrimary

/**
 * Panic Button: Reachable in <= 2 taps from anywhere in the app.
 * Reverts all mutated sysfs nodes to pristine boot stock state bit-for-bit.
 */
@Composable
fun PanicButton(
    onPanicReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onPanicReset,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, CriticalRust.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .semantics {
                contentDescription = "Panic Button: Immediate 1-tap restore to pristine stock boot state"
            },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CriticalRust.copy(alpha = 0.2f),
            contentColor = TextPrimary
        )
    ) {
        Text(
            text = "Panic Reset: Revert to Stock Snapshot",
            style = MaterialTheme.typography.titleMedium,
            color = CriticalRust
        )
    }
}
