package com.drivevoice.assistant.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.drivevoice.assistant.a11y.WhatsAppSendBroker
import com.drivevoice.assistant.overlay.SendCoverOverlay

@Composable
fun SettingsScreen(
    confirmBeforeSensitive: Boolean,
    backgroundListening: Boolean,
    wakePhrase: String,
    whatsAppHidden: Boolean,
    onConfirmChanged: (Boolean) -> Unit,
    onBackgroundChanged: (Boolean) -> Unit,
    onWakePhraseChanged: (String) -> Unit,
    onWhatsAppHiddenChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumeTick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    val a11yOn = remember(resumeTick) { WhatsAppSendBroker.isAccessibilityEnabled(context) }
    val overlayOn = remember(resumeTick) { SendCoverOverlay.canDraw(context) }
    var phrase by remember(wakePhrase) { mutableStateOf(wakePhrase) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("הגדרות", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Text("שפת זיהוי", style = MaterialTheme.typography.titleLarge)
        Text("עברית (he-IL)", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(24.dp))
        SettingSwitch(
            title = "השמע אישור לפני שיחה / SMS / מייל / וואטסאפ",
            hint = "מומלץ להשאיר דלוק בנהיגה.",
            checked = confirmBeforeSensitive,
            onChecked = onConfirmChanged
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingSwitch(
            title = "מיקרופון ברקע + מילת הפעלה",
            hint = "מאזין ל«${wakePhrase}» גם כשהמסך כבוי, עם התראת נהיגה.",
            checked = backgroundListening,
            onChecked = onBackgroundChanged
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("מילת הפעלה", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = phrase,
            onValueChange = {
                phrase = it
                onWakePhraseChanged(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            label = { Text("למשל: היי דרייב") }
        )
        Text(
            text = "אמור את המילה ואז את הפקודה, או רק את המילה ואז חכה ל«כן?».",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("שליחת וואטסאפ", style = MaterialTheme.typography.titleLarge)
        SettingSwitch(
            title = "הסתרת מסך (שליחה אוטומטית)",
            hint = if (whatsAppHidden) {
                "לוחץ שלח לבד וחוזר לעוזר. דורש שירות נגישות."
            } else {
                "כבוי: עובר למסך וואטסאפ עם ההודעה מוכנה."
            },
            checked = whatsAppHidden,
            onChecked = onWhatsAppHiddenChanged
        )
        Text(
            text = "נגישות: ${if (a11yOn) "פעילה" else "כבויה"}  ·  כיסוי מסך: ${if (overlayOn) "מורשה" else "לא מורשה"}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("הפעל נגישות לשליחה מוסתרת")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("הפעל כיסוי מסך (הסתרת וואטסאפ)")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("אל תכבה מיקרופון לחיסכון בסוללה")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("אפליקציות מועדפות", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Waze, מפות, WhatsApp, מוזיקה — מזוהות לפי שם או חבילה. ניווט נפתח ב-Waze אם מותקן.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    hint: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChecked)
    }
    Text(
        text = hint,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        modifier = Modifier.padding(top = 4.dp)
    )
}
