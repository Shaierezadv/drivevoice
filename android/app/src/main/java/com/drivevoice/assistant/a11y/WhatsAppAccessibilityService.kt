package com.drivevoice.assistant.a11y

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.drivevoice.assistant.MainActivity
import com.drivevoice.assistant.overlay.SendCoverOverlay

/**
 * Clicks WhatsApp's send button when a hidden send was armed.
 * Enabled only if the user turns on the accessibility service in system settings.
 */
class WhatsAppAccessibilityService : AccessibilityService() {

    private val main = Handler(Looper.getMainLooper())
    private var clicking = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pending = WhatsAppSendBroker.pending ?: return
        if (WhatsAppSendBroker.isStale()) {
            WhatsAppSendBroker.clear()
            SendCoverOverlay.hide(this)
            return
        }
        val pkg = event?.packageName?.toString().orEmpty()
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") return
        if (clicking) return
        val root = rootInActiveWindow ?: return
        val send = findSendButton(root) ?: return
        clicking = true
        send.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        WhatsAppSendBroker.clear()
        if (pending.hideUi) {
            main.postDelayed({
                performGlobalAction(GLOBAL_ACTION_BACK)
                val i = Intent(this, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
                startActivity(i)
                SendCoverOverlay.hide(this)
                clicking = false
            }, 450)
        } else {
            SendCoverOverlay.hide(this)
            clicking = false
        }
    }

    override fun onInterrupt() {
        clicking = false
    }

    private fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val byId = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
            ?: emptyList()
        byId.firstOrNull { it.isClickable }?.let { return it }
        val byId2 = root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/send")
            ?: emptyList()
        byId2.firstOrNull { it.isClickable }?.let { return it }
        for (label in listOf("שלח", "Send")) {
            val nodes = root.findAccessibilityNodeInfosByText(label) ?: continue
            nodes.firstOrNull { it.isClickable }?.let { return it }
            nodes.firstOrNull { parentClickable(it) != null }?.let { return parentClickable(it) }
        }
        return findByDesc(root)
    }

    private fun parentClickable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var p = node.parent
        var hops = 0
        while (p != null && hops < 4) {
            if (p.isClickable) return p
            p = p.parent
            hops++
        }
        return null
    }

    private fun findByDesc(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        val id = node.viewIdResourceName.orEmpty()
        if (node.isClickable && (
                desc == "send" || desc == "שלח" || desc.contains("שלח") ||
                    id.endsWith(":id/send")
                )
        ) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findByDesc(child)?.let { return it }
        }
        return null
    }
}
