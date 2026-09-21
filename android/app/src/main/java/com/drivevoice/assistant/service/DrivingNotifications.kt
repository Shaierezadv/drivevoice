package com.drivevoice.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.drivevoice.assistant.MainActivity

object DrivingNotifications {
    const val CHANNEL_ID = "drivevoice_mic"
    const val NOTIF_ID = 42

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(
            CHANNEL_ID,
            "מיקרופון נהיגה",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "האזנה למילת הפעלה ברקע"
            setShowBadge(false)
        }
        nm.createNotificationChannel(ch)
    }

    fun build(context: Context, wakePhrase: String): Notification {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            context,
            1,
            Intent(context, DrivingForegroundService::class.java)
                .setAction(DrivingForegroundService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("עוזר נהיגה")
            .setContentText("מאזין ל«$wakePhrase»")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, "עצור האזנה", stop)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
