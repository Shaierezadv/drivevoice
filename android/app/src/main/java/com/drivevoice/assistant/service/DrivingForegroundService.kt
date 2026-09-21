package com.drivevoice.assistant.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.drivevoice.assistant.DriveVoiceApp

class DrivingForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            runtime().setBackgroundListening(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val phrase = runtime().ui.wakePhrase
        val notification = DrivingNotifications.build(this, phrase)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                DrivingNotifications.NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(DrivingNotifications.NOTIF_ID, notification)
        }
        runtime().onServiceStarted()
        return START_STICKY
    }

    override fun onDestroy() {
        runtime().onServiceDestroyed()
        super.onDestroy()
    }

    private fun runtime() = (application as DriveVoiceApp).runtime

    companion object {
        const val ACTION_STOP = "com.drivevoice.assistant.STOP_BACKGROUND"

        fun start(context: Context) {
            val i = Intent(context, DrivingForegroundService::class.java)
            ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DrivingForegroundService::class.java))
        }
    }
}
