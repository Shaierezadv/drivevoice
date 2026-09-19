package com.drivevoice.assistant.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    confirmBeforeSensitive: Boolean,
    onConfirmChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("הגדרות", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Text("שפת זיהוי", style = MaterialTheme.typography.titleLarge)
        Text("עברית (he-IL)", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "השמע אישור לפני שיחה / SMS / מייל",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = confirmBeforeSensitive,
                onCheckedChange = onConfirmChanged
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("אפליקציות מועדפות", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Waze, מפות, WhatsApp, מוזיקה — מזוהות לפי שם בתצוגה או חבילה ידועה.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
