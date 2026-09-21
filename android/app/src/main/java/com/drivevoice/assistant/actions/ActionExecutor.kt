package com.drivevoice.assistant.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.view.KeyEvent
import com.drivevoice.assistant.nlu.IntentType
import com.drivevoice.assistant.nlu.MediaAction
import com.drivevoice.assistant.nlu.ParsedIntent

sealed class ActionResult {
    data class Success(val messageHe: String) : ActionResult()
    data class Failure(val messageHe: String) : ActionResult()
    data class NeedsConfirm(val intent: ParsedIntent, val messageHe: String) : ActionResult()
    data class NeedsDisambiguation(
        val intent: ParsedIntent,
        val candidates: List<ResolvedContact>,
        val messageHe: String
    ) : ActionResult()
}

/**
 * Executes CALL / SMS / EMAIL / OPEN_APP / NAVIGATE / MEDIA using platform APIs.
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
            IntentType.NAVIGATE -> navigateTo(intent.destination)
            IntentType.MEDIA -> dispatchMedia(intent.mediaAction)
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
        if (!intent.phoneNumber.isNullOrBlank()) {
            val phone = intent.phoneNumber.replace(Regex("[^\\d+]"), "")
            if (phone.length < 7) return ActionResult.Failure("איש קשר או מספר לא נמצא")
            return dial(phone)
        }
        val query = intent.contactName.orEmpty()
        val matches = contacts.findAllByDisplayNameContains(query)
        if (matches.isEmpty()) return ActionResult.Failure("איש קשר או מספר לא נמצא")
        val unique = contacts.pickUnique(matches, query)
            ?: return ActionResult.NeedsDisambiguation(
                intent,
                matches,
                "מצאתי כמה אנשי קשר: ${names(matches)}. אמור את השם המלא"
            )
        return dial(unique.phoneNumber, unique.displayName)
    }

    private fun dial(phone: String, label: String = phone): ActionResult {
        return try {
            val call = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(call)
            ActionResult.Success("מתקשר אל $label")
        } catch (e: SecurityException) {
            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dial)
            ActionResult.Success("פותח חיוג אל $label")
        } catch (e: Exception) {
            ActionResult.Failure("שגיאה בחיוג: ${e.message}")
        }
    }

    private fun sendSms(intent: ParsedIntent): ActionResult {
        val phone: String
        val body: String
        val label: String
        if (!intent.phoneNumber.isNullOrBlank()) {
            phone = intent.phoneNumber.replace(Regex("[^\\d+]"), "")
            if (phone.length < 7) return ActionResult.Failure("איש קשר או מספר לא נמצא")
            body = intent.messageBody.orEmpty()
            label = phone
        } else {
            val rest = listOfNotNull(intent.contactName, intent.messageBody).joinToString(" ")
            val match = contacts.resolveNameAndBody(rest)
                ?: return ActionResult.Failure("איש קשר או מספר לא נמצא")
            val query = intent.contactName.orEmpty()
            val unique = contacts.pickUnique(match.candidates, query)
                ?: return ActionResult.NeedsDisambiguation(
                    intent.copy(messageBody = match.body.ifBlank { intent.messageBody }),
                    match.candidates,
                    "מצאתי כמה אנשי קשר: ${names(match.candidates)}. אמור את השם המלא"
                )
            phone = unique.phoneNumber
            body = match.body.ifBlank { intent.messageBody.orEmpty() }
            label = unique.displayName
        }
        return try {
            if (body.isNotBlank()) {
                val sms = smsManager()
                val parts = sms.divideMessage(body)
                if (parts.size <= 1) {
                    sms.sendTextMessage(phone, null, body, null, null)
                } else {
                    sms.sendMultipartTextMessage(phone, null, parts, null, null)
                }
                ActionResult.Success("SMS נשלח אל $label")
            } else {
                openSmsComposer(phone, body)
                ActionResult.Success("פותח SMS אל $label")
            }
        } catch (e: SecurityException) {
            openSmsComposer(phone, body)
            ActionResult.Success("פותח SMS אל $label")
        } catch (e: Exception) {
            ActionResult.Failure("שגיאה ב-SMS: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    private fun smsManager(): SmsManager {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)?.let { return it }
        }
        return SmsManager.getDefault()
    }

    private fun openSmsComposer(phone: String, body: String) {
        val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
            if (body.isNotBlank()) putExtra("sms_body", body)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(i)
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

    private fun navigateTo(dest: String?): ActionResult {
        if (dest.isNullOrBlank()) return ActionResult.Failure("לא צוין יעד")
        val encoded = Uri.encode(dest)
        val pm = context.packageManager
        val waze = Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?q=$encoded&navigate=yes")).apply {
            setPackage("com.waze")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (waze.resolveActivity(pm) != null) {
            context.startActivity(waze)
            return ActionResult.Success("מנווט אל $dest ב-Waze")
        }
        val maps = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$encoded")).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (maps.resolveActivity(pm) != null) {
            context.startActivity(maps)
            return ActionResult.Success("מנווט אל $dest במפות")
        }
        val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(geo)
            ActionResult.Success("מנווט אל $dest")
        } catch (e: Exception) {
            ActionResult.Failure("לא נמצאה אפליקציית ניווט: ${e.message}")
        }
    }

    private fun dispatchMedia(action: MediaAction?): ActionResult {
        if (action == null) return ActionResult.Failure("לא צוינה פעולת מדיה")
        val key = when (action) {
            MediaAction.PLAY -> KeyEvent.KEYCODE_MEDIA_PLAY
            MediaAction.PAUSE -> KeyEvent.KEYCODE_MEDIA_PAUSE
            MediaAction.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            MediaAction.PREV -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
        }
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
            ActionResult.Success(
                when (action) {
                    MediaAction.PLAY -> "מנגן"
                    MediaAction.PAUSE -> "מושהה"
                    MediaAction.NEXT -> "שיר הבא"
                    MediaAction.PREV -> "שיר קודם"
                }
            )
        } catch (e: Exception) {
            ActionResult.Failure("שגיאה במדיה: ${e.message}")
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

    private fun names(matches: List<ResolvedContact>): String =
        matches.take(3).joinToString(", ") { it.displayName }
}
