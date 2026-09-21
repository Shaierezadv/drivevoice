package com.drivevoice.assistant

import android.app.Application
import com.drivevoice.assistant.service.DrivingNotifications

class DriveVoiceApp : Application() {
    lateinit var runtime: AssistantRuntime
        private set

    override fun onCreate() {
        super.onCreate()
        DrivingNotifications.ensureChannel(this)
        runtime = AssistantRuntime(this)
    }
}
