package com.drivevoice.assistant.nlu

/**
 * Rule / regex Hebrew intent parser (no cloud NLU).
 * Patterns aligned with COMMANDS.md.
 */
object IntentParser {

    private val phonePattern = Regex("""(?:0\d[\d\- ]{7,12}|\+?\d[\d\- ]{8,15})""")
    private val emailPattern = Regex(
        """[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""",
        RegexOption.IGNORE_CASE
    )

    private val confirmWords = listOf("כן", "אשר", "בצע", "מאשר", "בסדר", "אישור")
    private val cancelWords = listOf("לא", "בטל", "עצור", "ביטול", "תעזוב")
    private val trailingFillers = setOf("בבקשה", "תודה", "טוב")

    fun parse(raw: String, awaitingConfirm: Boolean = false): ParsedIntent {
        val text = normalize(raw)
        if (text.isBlank()) {
            return ParsedIntent(IntentType.UNKNOWN, rawText = raw)
        }

        if (matchesCommandWord(text, confirmWords)) {
            return ParsedIntent(IntentType.CONFIRM, rawText = raw)
        }
        if (matchesCommandWord(text, cancelWords, allowFillersOnly = !awaitingConfirm)) {
            return ParsedIntent(IntentType.CANCEL, rawText = raw)
        }

        parseNavigate(text, raw)?.let { return it }
        parseMedia(text, raw)?.let { return it }
        parseCall(text, raw)?.let { return it }
        parseEmail(text, raw)?.let { return it }
        parseSms(text, raw)?.let { return it }
        parseOpenApp(text, raw)?.let { return it }

        return ParsedIntent(IntentType.UNKNOWN, rawText = raw)
    }

    private fun normalize(s: String): String {
        var t = s.trim()
            .replace('\u200f', ' ')
            .replace('\u200e', ' ')
            .replace(Regex("[.,;:!?()\\[\\]\"'`״׳]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        t = t.replace(Regex("^(?:אפשר|אנא|תוכל|תוכלי)\\s+"), "")
        t = t.replace(Regex("\\s+בבקשה$"), "")
        t = HebrewNumbers.replaceSpokenDigits(t)
        return t
    }

    /**
     * Exact word, or short phrase whose first token is the command and the rest are fillers
     * ("כן בבקשה"). Does not treat "בצע ניווט" as confirm.
     */
    private fun matchesCommandWord(
        text: String,
        words: List<String>,
        allowFillersOnly: Boolean = true
    ): Boolean {
        val t = text.trim()
        if (words.any { t.equals(it, ignoreCase = true) }) return true
        val tokens = t.split(Regex("\\s+"))
        if (tokens.isEmpty() || tokens.size > 3) return false
        if (tokens.first() !in words) return false
        val rest = tokens.drop(1)
        return if (allowFillersOnly) rest.all { it in trailingFillers } else rest.isEmpty() || rest.all { it in trailingFillers }
    }

    private fun parseCall(text: String, raw: String): ParsedIntent? {
        val dialNumber = Regex(
            """^(?:חייג|חיוג|התקשר\s+למספר|תתקשר\s+למספר|התקשר\s+אל\s+מספר|להתקשר\s+למספר)\s+(.+)$"""
        ).find(text)
        if (dialNumber != null) {
            val rest = dialNumber.groupValues[1].trim()
            val phone = extractPhone(rest) ?: rest.replace(Regex("[^\\d+]"), "")
            if (phone.length >= 7) {
                return ParsedIntent(IntentType.CALL, phoneNumber = phone, rawText = raw)
            }
        }

        val callContact = Regex(
            """^(?:התקשר|תתקשר|תתקשרי|להתקשר|חייג(?:י)?)\s+(?:אל\s+|ל)?(.+)$"""
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
        val emailMatch = emailPattern.find(text) ?: return null
        if (!Regex("""מייל|אימייל|email|דוא.?ל|דואר""", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            return null
        }

        val email = emailMatch.value
        val after = text.substring(emailMatch.range.last + 1).trim()
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

    private fun parseNavigate(text: String, raw: String): ParsedIntent? {
        val m = Regex(
            """^(?:נווט|נווטי|ניווט|קח אותי|קחי אותי|קח אותנו)\s+(?:אל |ל|עד |כיוון )?(.+)$"""
        ).find(text) ?: return null
        val dest = m.groupValues[1].trim()
            .removePrefix("ל")
            .trim()
        if (dest.isBlank()) return null
        return ParsedIntent(IntentType.NAVIGATE, destination = dest, rawText = raw)
    }

    private fun parseMedia(text: String, raw: String): ParsedIntent? {
        val t = text.trim()
        val play = setOf("נגן", "נגני", "המשך", "המשיכי")
        val pause = setOf("השהה", "השהי", "pause")
        when {
            t in play || Regex("""^נגן(?:י)?\s+מוזיקה$""").matches(t) ->
                return ParsedIntent(IntentType.MEDIA, mediaAction = MediaAction.PLAY, rawText = raw)
            t in pause ->
                return ParsedIntent(IntentType.MEDIA, mediaAction = MediaAction.PAUSE, rawText = raw)
            Regex("""^(?:שיר\s+הבא|השיר\s+הבא|הבא\s+שיר)$""").matches(t) ->
                return ParsedIntent(IntentType.MEDIA, mediaAction = MediaAction.NEXT, rawText = raw)
            Regex("""^(?:שיר\s+קודם|השיר\s+הקודם)$""").matches(t) ->
                return ParsedIntent(IntentType.MEDIA, mediaAction = MediaAction.PREV, rawText = raw)
        }
        return null
    }

    private fun extractPhone(s: String): String? {
        val m = phonePattern.find(s)
        if (m != null) {
            val digits = m.value.replace(Regex("[^\\d+]"), "")
            if (digits.length >= 7) return digits
        }
        return HebrewNumbers.spokenRunToDigits(s)
    }

    private fun cleanName(s: String): String =
        s.replace(Regex("""^(את|ל|אל)\s+"""), "").trim()

    /**
     * First whitespace-separated token = name (MVP default for tests).
     * [ContactResolver] re-joins name+body and matches the longest contact prefix
     * so "דוד כהן אני בדרך" still resolves correctly at execution time.
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
