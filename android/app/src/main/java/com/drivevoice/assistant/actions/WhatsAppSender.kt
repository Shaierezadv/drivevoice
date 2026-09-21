package com.drivevoice.assistant.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.drivevoice.assistant.a11y.WhatsAppSendBroker
import com.drivevoice.assistant.overlay.SendCoverOverlay

enum class WhatsAppSendStyle {
    /** Open the WhatsApp chat (user taps send). */
    OPEN_CHAT,
    /** Auto-press send via accessibility, then return to DriveVoice. */
    HIDDEN
}

object PhoneNumbers {
    fun toWhatsAppIntl(phone: String): String {
        val d = phone.filter { it.isDigit() }
        return when {
            d.startsWith("972") -> d
            d.startsWith("0") && d.length >= 9 -> "972${d.drop(1)}"
            else -> d
        }
    }
}

class WhatsAppSender(private val context: Context) {

    fun send(phone: String, body: String, style: WhatsAppSendStyle): ActionResult {
        val intl = PhoneNumbers.toWhatsAppIntl(phone)
        if (intl.length < 8) return ActionResult.Failure("מספר לא תקין לוואטסאפ")
        val pkg = installedPackage() ?: return ActionResult.Failure("וואטסאפ לא מותקן")
        val url = "https://api.whatsapp.com/send?phone=$intl" +
            if (body.isNotBlank()) "&text=${Uri.encode(body)}" else ""
        val launch = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(pkg)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            if (style == WhatsAppSendStyle.HIDDEN) {
                if (!WhatsAppSendBroker.isAccessibilityEnabled(context)) {
                    context.startActivity(launch)
                    return ActionResult.Success(
                        "פותח וואטסאפ. לשליחה מוסתרת הפעל נגישות בהגדרות"
                    )
                }
                WhatsAppSendBroker.arm(intl, body, hideUi = true)
                SendCoverOverlay.show(context, "שולח וואטסאפ…")
                context.startActivity(launch)
                ActionResult.Success("שולח וואטסאפ")
            } else {
                context.startActivity(launch)
                ActionResult.Success("פותח וואטסאפ")
            }
        } catch (e: Exception) {
            SendCoverOverlay.hide(context)
            WhatsAppSendBroker.clear()
            ActionResult.Failure("שגיאה בוואטסאפ: ${e.message}")
        }
    }

    private fun installedPackage(): String? {
        val pm = context.packageManager
        return listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
            pm.getLaunchIntentForPackage(pkg) != null
        }
    }
}
