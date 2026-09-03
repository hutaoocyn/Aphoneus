package com.aphoneus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aphoneus.ui.theme.CriticalRust
import com.aphoneus.ui.theme.SurfaceContainerElevated
import com.aphoneus.ui.theme.TextPrimary
import com.aphoneus.ui.theme.TextSecondary

@Composable
fun DestructiveConfirmationDialog(
    title: String,
    message: String,
    confirmButtonText: String = "Confirm",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerElevated,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = CriticalRust)
        },
        text = {
            Column {
                Text(text = message, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "A non-disableable hardware thermal watchdog remains active.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CriticalRust)
            ) {
                Text(text = confirmButtonText, color = TextPrimary)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextSecondary)
            }
        }
    )
}
