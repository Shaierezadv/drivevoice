package com.drivevoice.assistant.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("עזרה ופקודות", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Section("שיחה", listOf(
            "התקשר ליוסי",
            "תתקשר למיכל",
            "חייג 0501234567",
            "התקשר למספר 03-7520432"
        ))
        Section("SMS", listOf(
            "שלח הודעה לדני תגיע בעוד עשר דקות",
            "שלח SMS ליוסי אני בדרך",
            "הודעה למיכל מחכה בחניה"
        ))
        Section("מייל", listOf(
            "שלח מייל ל name@example.com נושא פגישה תוכן נתראה מחר",
            "מייל ל name@example.com נושא שלום תוכן היי"
        ))
        Section("פתיחת אפליקציה", listOf(
            "פתח ווייז",
            "תפתח מוזיקה",
            "פתח וואטסאפ",
            "פתח מפות"
        ))
        Section("אישור / ביטול", listOf(
            "כן / אשר / בצע",
            "לא / בטל / עצור"
        ))
    }
}

@Composable
private fun Section(title: String, lines: List<String>) {
    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(8.dp))
    lines.forEach {
        Text("• $it", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 2.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))
}
