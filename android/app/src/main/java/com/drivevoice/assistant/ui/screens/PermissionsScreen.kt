package com.drivevoice.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drivevoice.assistant.permissions.AppPermissions

@Composable
fun PermissionsScreen(
    missing: List<String>,
    onRequest: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "הרשאות לנהיגה בטוחה",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "DriveVoice צריך הרשאות אלה כדי לאפשר פקודות קול בזמן נהיגה — בלי להסיט מבט מהכביש.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        AppPermissions.required.forEach { perm ->
            val granted = perm !in missing
            Text(
                text = "${if (granted) "✓" else "○"} ${AppPermissions.labelHe(perm)}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (granted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (missing.isNotEmpty()) {
            Button(
                onClick = onRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("אשר הרשאות")
            }
        }
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = missing.isEmpty()
        ) {
            Text("המשך")
        }
    }
}
