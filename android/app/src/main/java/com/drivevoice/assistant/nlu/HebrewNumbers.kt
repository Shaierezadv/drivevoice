package com.drivevoice.assistant.nlu

/**
 * Converts spoken Hebrew digits (common STT output) into numeric characters.
 * Example: "אפס חמש אפס אחד שתיים שלוש ארבע חמש שש שבע" → "0501234567"
 */
object HebrewNumbers {

    private val digitWords: Map<String, String> = mapOf(
        "אפס" to "0",
        "אחד" to "1",
        "אחת" to "1",
        "שתיים" to "2",
        "שתים" to "2",
        "שניים" to "2",
        "שני" to "2",
        "שלוש" to "3",
        "שלושה" to "3",
        "ארבע" to "4",
        "ארבעה" to "4",
        "חמש" to "5",
        "חמישה" to "5",
        "שש" to "6",
        "שישה" to "6",
        "שבע" to "7",
        "שבעה" to "7",
        "שמונה" to "8",
        "תשע" to "9",
        "תשעה" to "9"
    )

    /**
     * Replace runs of 3+ consecutive spoken digits with the numeric string.
     * Shorter runs are left as words so phrases like "עשר דקות" stay intact
     * (those are not digit-words in this map anyway).
     */
    fun replaceSpokenDigits(text: String): String {
        if (text.isBlank()) return text
        val tokens = text.trim().split(Regex("\\s+"))
        val out = ArrayList<String>(tokens.size)
        var i = 0
        while (i < tokens.size) {
            if (digitWords.containsKey(tokens[i])) {
                val start = i
                val digits = StringBuilder()
                while (i < tokens.size) {
                    val d = digitWords[tokens[i]] ?: break
                    digits.append(d)
                    i++
                }
                val runLen = i - start
                if (runLen >= 3) {
                    out.add(digits.toString())
                } else {
                    for (j in start until i) out.add(tokens[j])
                }
            } else {
                out.add(tokens[i])
                i++
            }
        }
        return out.joinToString(" ")
    }

    fun spokenRunToDigits(text: String): String? {
        val tokens = text.trim().split(Regex("\\s+"))
        if (tokens.isEmpty()) return null
        val digits = StringBuilder()
        for (t in tokens) {
            val d = digitWords[t] ?: return null
            digits.append(d)
        }
        return digits.toString().takeIf { it.length >= 7 }
    }
}
