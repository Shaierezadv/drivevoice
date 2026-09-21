package com.drivevoice.assistant.actions

import android.content.Context
import android.provider.ContactsContract

data class ResolvedContact(
    val displayName: String,
    val phoneNumber: String,
    val starred: Boolean = false
)

data class NameBodyMatch(
    val candidates: List<ResolvedContact>,
    val body: String
)

/**
 * Lookup contact by display name. Prefers exact / prefix / starred matches.
 * Multi-word names are resolved by trying the longest prefix of the utterance.
 */
class ContactResolver(private val context: Context) {

    fun findAllByDisplayNameContains(query: String, limit: Int = 12): List<ResolvedContact> {
        val safe = sanitize(query)
        if (safe.isBlank()) return emptyList()
        val cr = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%$safe%")
        val sort =
            "${ContactsContract.CommonDataKinds.Phone.STARRED} DESC, " +
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        val out = LinkedHashMap<String, ResolvedContact>()
        cr.query(uri, projection, selection, args, sort)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val starIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
            while (cursor.moveToNext() && out.size < limit) {
                val name = cursor.getString(nameIdx) ?: continue
                val number = cursor.getString(numIdx)?.replace(Regex("[^\\d+]"), "") ?: continue
                if (number.length < 7) continue
                val starred = starIdx >= 0 && cursor.getInt(starIdx) == 1
                val key = "$name|$number"
                if (key !in out) {
                    out[key] = ResolvedContact(name, number, starred)
                }
            }
        }
        return rank(out.values.toList(), safe)
    }

    fun resolveNameAndBody(rest: String): NameBodyMatch? {
        val tokens = rest.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null
        val max = minOf(tokens.size, 4)
        for (len in max downTo 1) {
            val name = tokens.subList(0, len).joinToString(" ")
            val body = tokens.drop(len).joinToString(" ")
            val matches = findAllByDisplayNameContains(name)
            if (matches.isNotEmpty()) return NameBodyMatch(matches, body)
        }
        return null
    }

    fun lookupPhone(contactName: String?, phoneNumber: String?): String? {
        if (!phoneNumber.isNullOrBlank()) return phoneNumber.replace(Regex("[^\\d+]"), "")
        if (!contactName.isNullOrBlank()) {
            val all = findAllByDisplayNameContains(contactName)
            return pickUnique(all, contactName)?.phoneNumber
        }
        return null
    }

    fun pickUnique(matches: List<ResolvedContact>, query: String): ResolvedContact? {
        if (matches.isEmpty()) return null
        if (matches.size == 1) return matches[0]
        val exact = matches.filter { it.displayName.equals(query, ignoreCase = true) }
        if (exact.size == 1) return exact[0]
        val starred = matches.filter { it.starred }
        if (starred.size == 1) return starred[0]
        return null
    }

    private fun rank(list: List<ResolvedContact>, query: String): List<ResolvedContact> =
        list.sortedWith(
            compareByDescending<ResolvedContact> { it.displayName.equals(query, ignoreCase = true) }
                .thenByDescending { it.displayName.startsWith(query, ignoreCase = true) }
                .thenByDescending { it.starred }
                .thenBy { it.displayName }
        )

    private fun sanitize(query: String): String =
        query.replace(Regex("[%_']"), "").trim()
}
