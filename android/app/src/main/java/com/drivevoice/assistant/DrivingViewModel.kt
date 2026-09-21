package com.drivevoice.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class DrivingViewModel(app: Application) : AndroidViewModel(app) {

    private val runtime = (app as DriveVoiceApp).runtime

    val ui: UiState
        get() = runtime.ui

    fun toggleListen() = runtime.toggleListen()
    fun confirmPending() = runtime.confirmPending()
    fun cancelPending() = runtime.cancelPending()
    fun setConfirmBeforeSensitive(value: Boolean) = runtime.setConfirmBeforeSensitive(value)
    fun setBackgroundListening(value: Boolean) = runtime.setBackgroundListening(value)
    fun setWakePhrase(value: String) = runtime.setWakePhrase(value)
    fun setWhatsAppHidden(value: Boolean) = runtime.setWhatsAppHidden(value)
    fun ensureBackground() = runtime.ensureBackgroundService()
}
