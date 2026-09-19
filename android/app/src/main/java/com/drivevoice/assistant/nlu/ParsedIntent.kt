package com.drivevoice.assistant.nlu

/**
 * On-device NLU result for DriveVoice MVP.
 */
enum class IntentType {
    CALL,
    SMS,
    EMAIL,
    OPEN_APP,
    CONFIRM,
    CANCEL,
    UNKNOWN
}

data class ParsedIntent(
    val type: IntentType,
    val contactName: String? = null,
    val phoneNumber: String? = null,
    val messageBody: String? = null,
    val email: String? = null,
    val subject: String? = null,
    val appLabel: String? = null,
    val rawText: String = ""
) {
    val needsConfirmation: Boolean
        get() = type == IntentType.CALL || type == IntentType.SMS || type == IntentType.EMAIL

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
        IntentType.OPEN_APP -> "פתיחת ${appLabel ?: "?"}"
        IntentType.CONFIRM -> "אישור"
        IntentType.CANCEL -> "ביטול"
        IntentType.UNKNOWN -> "לא זוהתה פקודה"
    }
}
