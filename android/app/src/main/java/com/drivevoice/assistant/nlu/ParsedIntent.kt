package com.drivevoice.assistant.nlu

/**
 * On-device NLU result for DriveVoice.
 */
enum class IntentType {
    CALL,
    SMS,
    EMAIL,
    WHATSAPP,
    OPEN_APP,
    NAVIGATE,
    MEDIA,
    CONFIRM,
    CANCEL,
    STOP_LISTEN,
    UNKNOWN
}

enum class MediaAction {
    PLAY,
    PAUSE,
    NEXT,
    PREV
}

data class ParsedIntent(
    val type: IntentType,
    val contactName: String? = null,
    val phoneNumber: String? = null,
    val messageBody: String? = null,
    val email: String? = null,
    val subject: String? = null,
    val appLabel: String? = null,
    val destination: String? = null,
    val mediaAction: MediaAction? = null,
    val rawText: String = ""
) {
    val needsConfirmation: Boolean
        get() = type == IntentType.CALL ||
            type == IntentType.SMS ||
            type == IntentType.EMAIL ||
            type == IntentType.WHATSAPP

    fun summaryHe(): String = when (type) {
        IntentType.CALL -> {
            val target = contactName ?: phoneNumber ?: "?"
            "התקשרות אל $target"
        }
        IntentType.SMS -> {
            val target = contactName ?: phoneNumber ?: "?"
            val body = messageBody?.let { " — $it" } ?: ""
            "SMS אל $target$body"
        }
        IntentType.EMAIL -> {
            val to = email ?: "?"
            val sub = subject?.let { " נושא: $it" } ?: ""
            "מייל אל $to$sub"
        }
        IntentType.WHATSAPP -> {
            val target = contactName ?: phoneNumber ?: "?"
            val body = messageBody?.let { " — $it" } ?: ""
            "וואטסאפ אל $target$body"
        }
        IntentType.OPEN_APP -> "פתיחת ${appLabel ?: "?"}"
        IntentType.NAVIGATE -> "ניווט אל ${destination ?: "?"}"
        IntentType.MEDIA -> when (mediaAction) {
            MediaAction.PLAY -> "ניגון"
            MediaAction.PAUSE -> "השהיה"
            MediaAction.NEXT -> "שיר הבא"
            MediaAction.PREV -> "שיר קודם"
            null -> "מדיה"
        }
        IntentType.CONFIRM -> "אישור"
        IntentType.CANCEL -> "ביטול"
        IntentType.STOP_LISTEN -> "עצירת האזנה"
        IntentType.UNKNOWN -> "לא זוהתה פקודה"
    }
}
