package com.drivevoice.assistant.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivevoice.assistant.AssistantState
import com.drivevoice.assistant.UiState
import com.drivevoice.assistant.ui.theme.CancelRed
import com.drivevoice.assistant.ui.theme.ConfirmGreen
import com.drivevoice.assistant.ui.theme.MicActive
import com.drivevoice.assistant.ui.theme.MicIdle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrivingModeScreen(
    state: UiState,
    onMicClick: () -> Unit,
    onConfirmYes: () -> Unit,
    onConfirmNo: () -> Unit,
    onSubmitText: (String) -> Unit = {}
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val listening = state.assistantState == AssistantState.LISTENING ||
        state.assistantState == AssistantState.LISTENING_WAKE
    val scale by animateFloatAsState(
        targetValue = if (listening) 1.12f else 1f,
        animationSpec = tween(400),
        label = "micScale"
    )
    var typed by remember { mutableStateOf("") }
    val quick = listOf(
        "פתח מפות",
        "פתח כרום",
        "פתח חיוג",
        "פתח וואטסאפ",
        "נווט לבית"
    )

    fun sendTyped() {
        val t = typed.trim()
        if (t.isEmpty()) return
        onSubmitText(t)
        typed = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        StatusCard(title = "מה שנשמע", body = state.transcript.ifBlank { "—" })
        Spacer(modifier = Modifier.height(8.dp))
        StatusCard(title = "פעולה אחרונה", body = state.lastAction.ifBlank { "—" })

        if (state.candidates.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            StatusCard(
                title = "בחר איש קשר",
                body = state.candidates.joinToString(" · ")
            )
        }

        if (!state.error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = state.error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.assistantState == AssistantState.AWAITING_CONFIRM ||
            state.pendingIntent != null
        ) {
            Spacer(modifier = Modifier.height(12.dp))
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

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "פקודות מהירות",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quick.forEach { cmd ->
                FilledTonalButton(onClick = { onSubmitText(cmd) }) {
                    Text(cmd)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = typed,
            onValueChange = { typed = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("הקלדת פקודה") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { sendTyped() }),
            trailingIcon = {
                IconButton(onClick = { sendTyped() }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "שלח")
                }
            }
        )
        Button(
            onClick = { sendTyped() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(48.dp)
        ) {
            Text("שלח פקודה", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(contentAlignment = Alignment.Center) {
            FloatingActionButton(
                onClick = onMicClick,
                shape = CircleShape,
                containerColor = if (listening) MicActive else MicIdle,
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "מיקרופון",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Text(
            text = if (listening) {
                if (state.assistantState == AssistantState.LISTENING_WAKE) {
                    "מחכה ל«${state.wakePhrase}» — הקש לפקודה מיידית"
                } else {
                    "מאזין… הקש לעצירה"
                }
            } else if (state.backgroundListening) {
                "אמור «${state.wakePhrase}» / הקש / הקלד"
            } else {
                "הקש או הקלד"
            },
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
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
    AssistantState.LISTENING_WAKE -> "מחכה למילת הפעלה"
    AssistantState.PROCESSING -> "מעבד…"
    AssistantState.SPEAKING -> "מדבר…"
    AssistantState.AWAITING_CONFIRM -> "ממתין לאישור"
}
