package com.drivevoice.assistant.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import com.drivevoice.assistant.nlu.IntentType
import com.drivevoice.assistant.nlu.ParsedIntent

sealed class ActionResult {
    data class Success(val messageHe: String) : ActionResult()
    data class Failure(val messageHe: String) : ActionResult()
    data class NeedsConfirm(val intent: ParsedIntent, val messageHe: String) : ActionResult()
}

/**
 * Executes CALL / SMS / EMAIL / OPEN_APP using platform intents / SmsManager.
 */
class ActionExecutor(
    private val context: Context,
    private val contacts: ContactResolver = ContactResolver(context)
) {

    fun prepareOrExecute(intent: ParsedIntent, confirmed: Boolean): ActionResult {
        return when (intent.type) {
            IntentType.CALL, IntentType.SMS, IntentType.EMAIL -> {
                if (!confirmed) {
                    ActionResult.NeedsConfirm(intent, "לאשר: ${intent.summaryHe()}?")
                } else {
                    executeSensitive(intent)
                }
            }
            IntentType.OPEN_APP -> openApp(intent.appLabel)
            IntentType.CONFIRM, IntentType.CANCEL, IntentType.UNKNOWN ->
                ActionResult.Failure("אין פעולה לביצוע")
        }
    }

    private fun executeSensitive(intent: ParsedIntent): ActionResult = when (intent.type) {
        IntentType.CALL -> placeCall(intent)
        IntentType.SMS -> sendSms(intent)
        IntentType.EMAIL -> sendEmail(intent)
        else -> ActionResult.Failure("סוג פעולה לא נתמך")
    }

    private fun placeCall(intent: ParsedIntent): ActionResult {
        val phone = contacts.lookupPhone(intent.contactName, intent.phoneNumber)
            ?: return ActionResult.Failure("איש קשר או מספר לא נמצא")
        return try {
            val call = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(call)
            ActionResult.Success("מתקשר אל $phone")
        } catch (e: SecurityException) {
            // Fallback to dialer UI
            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dial)
            ActionResult.Success("פותח חיוג אל $phone")
        } catch (e: Exception) {
            ActionResult.Failure("שגיאה בחיוג: ${e.message}")
        }
    }

    private fun sendSms(intent: ParsedIntent): ActionResult {
        val phone = contacts.lookupPhone(intent.contactName, intent.phoneNumber)
            ?: return ActionResult.Failure("איש קשר או מספר לא נמצא")
        val body = intent.messageBody.orEmpty()
        return try {
            if (body.isNotBlank()) {
                @Suppress("DEPRECATION")
                val sms = SmsManager.getDefault()
                sms.sendTextMessage(phone, null, body, null, null)
                ActionResult.Success("SMS נשלח אל $phone")
            } else {
                val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(i)
                ActionResult.Success("פותח SMS אל $phone")
            }
        } catch (e: SecurityException) {
            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
                putExtra("sms_body", body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(i)
            ActionResult.Success("פותח SMS אל $phone")
        } catch (e: Exception) {
            ActionResult.Failure("שגיאה ב-SMS: ${e.message}")
        }
    }

    private fun sendEmail(intent: ParsedIntent): ActionResult {
        val to = intent.email ?: return ActionResult.Failure("חסרה כתובת מייל")
        return try {
            val mail = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$to")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
                intent.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                intent.messageBody?.let { putExtra(Intent.EXTRA_TEXT, it) }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(mail, "שליחת מייל").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            ActionResult.Success("פותח מייל אל $to")
        } catch (e: Exception) {
            ActionResult.Failure("שגיאה במייל: ${e.message}")
        }
    }

    private fun openApp(label: String?): ActionResult {
        if (label.isNullOrBlank()) return ActionResult.Failure("לא צוינה אפליקציה")
        val pm = context.packageManager
        val known = knownPackageForLabel(label)
        if (known != null) {
            val launch = pm.getLaunchIntentForPackage(known)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                return ActionResult.Success("פותח $label")
            }
        }
        // Resolve by launcher label contains
        val main = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(main, PackageManager.MATCH_ALL)
        val needle = label.trim().lowercase()
        val match = apps.firstOrNull { ri ->
            val appLabel = ri.loadLabel(pm).toString().lowercase()
            appLabel.contains(needle) || needle.contains(appLabel)
        }
        return if (match != null) {
            val launch = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(match.activityInfo.packageName, match.activityInfo.name)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(launch)
            ActionResult.Success("פותח ${match.loadLabel(pm)}")
        } else {
            ActionResult.Failure("אפליקציה לא נמצאה: $label")
        }
    }

    private fun knownPackageForLabel(label: String): String? {
        val l = label.lowercase()
        return when {
            l.contains("waze") || l.contains("ווייז") || l.contains("וויז") -> "com.waze"
            l.contains("whatsapp") || l.contains("וואטסאפ") || l.contains("ווטסאפ") -> "com.whatsapp"
            l.contains("maps") || l.contains("מפות") -> "com.google.android.apps.maps"
            l.contains("spotify") || l.contains("ספוטיפיי") -> "com.spotify.music"
            l.contains("yt music") || l.contains("youtube music") -> "com.google.android.apps.youtube.music"
            l == "music" || l == "מוזיקה" -> listOf(
                "com.google.android.apps.youtube.music",
                "com.spotify.music",
                "com.google.android.music"
            ).firstOrNull { pkg ->
                context.packageManager.getLaunchIntentForPackage(pkg) != null
            }
            else -> null
        }
    }
}
