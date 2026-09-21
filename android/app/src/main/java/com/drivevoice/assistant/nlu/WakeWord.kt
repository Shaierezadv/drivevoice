package com.drivevoice.assistant.nlu

data class WakeMatch(
    val hit: Boolean,
    val rest: String
)

/**
 * Detects Hebrew/English wake phrases and returns the command after them.
 */
object WakeWord {

    const val DEFAULT_PHRASE = "היי דרייב"

    private val builtIn = listOf(
        DEFAULT_PHRASE,
        "הי דרייב",
        "היי דראיב",
        "היי drive",
        "hey drive",
        "אוקיי דרייב",
        "אוקי דרייב",
        "okay drive",
        "ok drive",
        "היי עוזר"
    )

    fun match(raw: String, customPhrase: String = DEFAULT_PHRASE): WakeMatch {
        val n = norm(raw)
        if (n.isBlank()) return WakeMatch(false, "")
        val phrases = (listOf(customPhrase) + builtIn)
            .map { norm(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedByDescending { it.length }
        for (p in phrases) {
            val idx = n.indexOf(p)
            if (idx >= 0) {
                val after = n.substring(idx + p.length).trim()
                return WakeMatch(true, after)
            }
        }
        return WakeMatch(false, n)
    }

    fun norm(s: String): String =
        s.trim()
            .replace('\u200f', ' ')
            .replace('\u200e', ' ')
            .replace(Regex("[.,;:!?()\\[\\]\"'`״׳]"), " ")
            .replace(Regex("\\s+"), " ")
            .lowercase()
}
