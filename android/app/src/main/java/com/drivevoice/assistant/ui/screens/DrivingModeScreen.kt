package com.drivevoice.assistant.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivevoice.assistant.AssistantState
import com.drivevoice.assistant.UiState
import com.drivevoice.assistant.ui.theme.CancelRed
import com.drivevoice.assistant.ui.theme.ConfirmGreen
import com.drivevoice.assistant.ui.theme.MicActive
import com.drivevoice.assistant.ui.theme.MicIdle

@Composable
fun DrivingModeScreen(
    state: UiState,
    onMicClick: () -> Unit,
    onConfirmYes: () -> Unit,
    onConfirmNo: () -> Unit
) {
    val listening = state.assistantState == AssistantState.LISTENING
    val scale by animateFloatAsState(
        targetValue = if (listening) 1.12f else 1f,
        animationSpec = tween(400),
        label = "micScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "מצב נהיגה",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stateLabel(state.assistantState),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        StatusCard(title = "מה שנשמע", body = state.transcript.ifBlank { "—" })
        Spacer(modifier = Modifier.height(12.dp))
        StatusCard(title = "פעולה אחרונה", body = state.lastAction.ifBlank { "—" })

        if (state.assistantState == AssistantState.AWAITING_CONFIRM ||
            state.pendingIntent != null
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = state.confirmPrompt.ifBlank { "ממתין לאישור" },
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = onConfirmYes,
                    colors = ButtonDefaults.buttonColors(containerColor = ConfirmGreen),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                ) {
                    Text("כן", fontSize = 22.sp)
                }
                Button(
                    onClick = onConfirmNo,
                    colors = ButtonDefaults.buttonColors(containerColor = CancelRed),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                ) {
                    Text("לא", fontSize = 22.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(contentAlignment = Alignment.Center) {
            FloatingActionButton(
                onClick = onMicClick,
                shape = CircleShape,
                containerColor = if (listening) MicActive else MicIdle,
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
            ) {
                Icon(
                    imageVector = if (listening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "מיקרופון",
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Text(
            text = if (listening) "מאזין… הקש לעצירה" else "הקש לדבר",
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun StatusCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp
            )
        }
    }
}

private fun stateLabel(s: AssistantState): String = when (s) {
    AssistantState.IDLE -> "מוכן"
    AssistantState.LISTENING -> "מאזין…"
    AssistantState.PROCESSING -> "מעבד…"
    AssistantState.SPEAKING -> "מדבר…"
    AssistantState.AWAITING_CONFIRM -> "ממתין לאישור"
}
