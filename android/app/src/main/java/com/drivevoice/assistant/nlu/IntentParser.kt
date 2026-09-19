package com.drivevoice.assistant.nlu

/**
 * Rule / regex Hebrew intent parser (no cloud NLU).
 * Patterns aligned with COMMANDS-HE.md.
 */
object IntentParser {

    private val phonePattern = Regex("""(?:0\d[\d\- ]{7,12}|\+?\d[\d\- ]{8,15})""")
    private val emailPattern = Regex(
        """[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""",
        RegexOption.IGNORE_CASE
    )

    private val confirmWords = listOf("כן", "אשר", "בצע", "מאשר", "בסדר", "אישור")
    private val cancelWords = listOf("לא", "בטל", "עצור", "ביטול", "תעזוב")

    fun parse(raw: String): ParsedIntent {
        val text = normalize(raw)
        if (text.isBlank()) {
            return ParsedIntent(IntentType.UNKNOWN, rawText = raw)
        }

        // Confirm / cancel (short utterances)
        if (matchesExactWord(text, confirmWords)) {
            return ParsedIntent(IntentType.CONFIRM, rawText = raw)
        }
        if (matchesExactWord(text, cancelWords)) {
            return ParsedIntent(IntentType.CANCEL, rawText = raw)
        }

        parseCall(text, raw)?.let { return it }
        parseEmail(text, raw)?.let { return it }
        parseSms(text, raw)?.let { return it }
        parseOpenApp(text, raw)?.let { return it }

        return ParsedIntent(IntentType.UNKNOWN, rawText = raw)
    }

    private fun normalize(s: String): String =
        s.trim()
            .replace('\u200f', ' ')
            .replace('\u200e', ' ')
            .replace(Regex("\\s+"), " ")

    private fun matchesExactWord(text: String, words: List<String>): Boolean {
        val t = text.trim()
        return words.any { w -> t.equals(w, ignoreCase = true) || t == w }
    }

    private fun parseCall(text: String, raw: String): ParsedIntent? {
        // חייג 050... / התקשר למספר ...
        val dialNumber = Regex(
            """^(?:חייג|חיוג|התקשר\s+למספר|תתקשר\s+למספר|התקשר\s+אל\s+מספר)\s+(.+)$"""
        ).find(text)
        if (dialNumber != null) {
            val rest = dialNumber.groupValues[1].trim()
            val phone = extractPhone(rest) ?: rest.replace(Regex("[^\\d+]"), "")
            if (phone.length >= 7) {
                return ParsedIntent(IntentType.CALL, phoneNumber = phone, rawText = raw)
            }
        }

        // התקשר ליוסי / תתקשר למיכל / התקשר אל ...
        val callContact = Regex(
            """^(?:התקשר|תתקשר|תתקשרי|חייג(?:י)?)\s+(?:אל\s+|ל)?(.+)$"""
        ).find(text)
        if (callContact != null) {
            val rest = callContact.groupValues[1].trim()
                .removePrefix("ל")
                .removePrefix("אל ")
                .trim()
            if (rest.startsWith("מספר")) return null
            val phone = extractPhone(rest)
            return if (phone != null) {
                ParsedIntent(IntentType.CALL, phoneNumber = phone, rawText = raw)
            } else {
                ParsedIntent(IntentType.CALL, contactName = cleanName(rest), rawText = raw)
            }
        }
        return null
    }

    private fun parseSms(text: String, raw: String): ParsedIntent? {
        // שלח הודעה לדני תגיע בעוד...
        // שלח SMS ליוסי אני בדרך
        // הודעה למיכל מחכה בחניה
        val patterns = listOf(
            Regex("""^(?:שלח(?:י)?\s+)?(?:הודעה|sms|SMS|מסרון)\s+(?:אל\s+|ל)(.+)$""", RegexOption.IGNORE_CASE),
            Regex("""^שלח(?:י)?\s+(?:אל\s+|ל)(.+)$""")
        )
        for (p in patterns) {
            val m = p.find(text) ?: continue
            val rest = m.groupValues[1].trim().removePrefix("ל").trim()
            val phone = extractPhone(rest)
            if (phone != null) {
                val afterPhone = rest.replaceFirst(phonePattern, "").trim()
                return ParsedIntent(
                    IntentType.SMS,
                    phoneNumber = phone,
                    messageBody = afterPhone.ifBlank { null },
                    rawText = raw
                )
            }
            val (name, body) = splitNameAndBody(rest)
            if (name.isNotBlank()) {
                return ParsedIntent(
                    IntentType.SMS,
                    contactName = cleanName(name),
                    messageBody = body.ifBlank { null },
                    rawText = raw
                )
            }
        }
        return null
    }

    private fun parseEmail(text: String, raw: String): ParsedIntent? {
        // שלח מייל ל name@example.com נושא פגישה תוכן נתראה מחר
        // מייל ל name@example.com נושא שלום תוכן היי
        val emailMatch = emailPattern.find(text) ?: return null
        if (!text.contains("מייל") && !text.contains("אימייל") &&
            !text.contains("email", ignoreCase = true) &&
            !text.contains("דוא\"ל") && !text.contains("דואר")
        ) {
            // still allow if clearly mailto-style command
            if (!text.startsWith("שלח") && !text.contains("מייל")) return null
        }
        if (!Regex("""מייל|אימייל|email|דוא.?ל|דואר""", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            return null
        }

        val email = emailMatch.value
        var after = text.substring(emailMatch.range.last + 1).trim()
        var subject: String? = null
        var body: String? = null

        val subjectIdx = after.indexOf("נושא")
        val bodyIdx = after.indexOf("תוכן")
        if (subjectIdx >= 0) {
            val from = subjectIdx + "נושא".length
            val to = if (bodyIdx > subjectIdx) bodyIdx else after.length
            subject = after.substring(from, to).trim().ifBlank { null }
        }
        if (bodyIdx >= 0) {
            body = after.substring(bodyIdx + "תוכן".length).trim().ifBlank { null }
        }
        if (subject == null && body == null && after.isNotBlank()) {
            body = after
        }

        return ParsedIntent(
            IntentType.EMAIL,
            email = email,
            subject = subject,
            messageBody = body,
            rawText = raw
        )
    }

    private fun parseOpenApp(text: String, raw: String): ParsedIntent? {
        val m = Regex("""^(?:פתח|תפתח|תפתחי|הפעל|תפעיל)\s+(?:את\s+)?(.+)$""").find(text)
            ?: return null
        val label = m.groupValues[1].trim()
            .removePrefix("את ")
            .trim()
        if (label.isBlank()) return null
        return ParsedIntent(IntentType.OPEN_APP, appLabel = normalizeAppLabel(label), rawText = raw)
    }

    private fun extractPhone(s: String): String? {
        val m = phonePattern.find(s) ?: return null
        val digits = m.value.replace(Regex("[^\\d+]"), "")
        return if (digits.length >= 7) digits else null
    }

    private fun cleanName(s: String): String =
        s.replace(Regex("""^(את|ל|אל)\s+"""), "").trim()

    /**
     * First token(s) as name; remainder as SMS body.
     * Heuristic: first word is name unless multi-word known pattern.
     * For MVP: first whitespace-separated token = name, rest = body.
     */
    private fun splitNameAndBody(rest: String): Pair<String, String> {
        val parts = rest.trim().split(Regex("\\s+"), limit = 2)
        return if (parts.size == 1) parts[0] to ""
        else parts[0] to parts[1]
    }

    private fun normalizeAppLabel(label: String): String {
        val map = mapOf(
            "ווייז" to "Waze",
            "וויז" to "Waze",
            "waze" to "Waze",
            "וואטסאפ" to "WhatsApp",
            "ווטסאפ" to "WhatsApp",
            "whatsapp" to "WhatsApp",
            "מפות" to "Maps",
            "גוגל מפות" to "Maps",
            "מוזיקה" to "Music",
            "יוטיוב מוזיקה" to "YT Music",
            "ספוטיפיי" to "Spotify"
        )
        val key = label.lowercase()
        return map.entries.firstOrNull { key == it.key.lowercase() || label == it.key }?.value
            ?: label
    }
}
