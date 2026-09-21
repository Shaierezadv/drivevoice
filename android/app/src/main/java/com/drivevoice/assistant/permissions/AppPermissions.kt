package com.drivevoice.assistant.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object AppPermissions {
    val required: List<String>
        get() = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        )

    fun missing(context: Context): List<String> =
        required.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

    fun allGranted(context: Context): Boolean = missing(context).isEmpty()

    fun labelHe(permission: String): String = when (permission) {
        Manifest.permission.RECORD_AUDIO -> "מיקרופון — זיהוי קולי"
        Manifest.permission.READ_CONTACTS -> "אנשי קשר — חיוג והודעות"
        Manifest.permission.CALL_PHONE -> "שיחות — חיוג ידיים-חופשיות"
        Manifest.permission.SEND_SMS -> "SMS — שליחת הודעות"
        else -> permission
    }
}
