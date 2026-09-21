package com.drivevoice.assistant.a11y

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager

data class PendingWhatsAppSend(
    val phoneIntl: String,
    val body: String,
    val hideUi: Boolean,
    val armedAtMs: Long = System.currentTimeMillis()
)

object WhatsAppSendBroker {
    @Volatile
    var pending: PendingWhatsAppSend? = null

    fun arm(phoneIntl: String, body: String, hideUi: Boolean) {
        pending = PendingWhatsAppSend(phoneIntl, body, hideUi)
    }

    fun clear() {
        pending = null
    }

    fun isStale(timeoutMs: Long = 15_000): Boolean {
        val p = pending ?: return true
        return System.currentTimeMillis() - p.armedAtMs > timeoutMs
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        if (!am.isEnabled) return false
        val list = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        val needle = context.packageName + "/"
        return list.any { info ->
            val id = info.resolveInfo?.serviceInfo?.let { "${it.packageName}/${it.name}" }.orEmpty()
            id.startsWith(needle) && id.contains("WhatsAppAccessibilityService")
        }
    }
}
