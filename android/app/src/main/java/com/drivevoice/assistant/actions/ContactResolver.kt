package com.drivevoice.assistant.actions

import android.content.Context
import android.provider.ContactsContract

data class ResolvedContact(
    val displayName: String,
    val phoneNumber: String
)

/**
 * Lookup contact by display name contains (case-insensitive substring).
 */
class ContactResolver(private val context: Context) {

    fun findByDisplayNameContains(query: String): ResolvedContact? {
        if (query.isBlank()) return null
        val cr = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%$query%")
        val sort = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        cr.query(uri, projection, selection, args, sort)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val name = cursor.getString(nameIdx) ?: query
                val number = cursor.getString(numIdx)?.replace(Regex("[^\\d+]"), "") ?: return null
                if (number.length >= 7) {
                    return ResolvedContact(name, number)
                }
            }
        }
        return null
    }

    fun lookupPhone(contactName: String?, phoneNumber: String?): String? {
        if (!phoneNumber.isNullOrBlank()) return phoneNumber.replace(Regex("[^\\d+]"), "")
        if (!contactName.isNullOrBlank()) return findByDisplayNameContains(contactName)?.phoneNumber
        return null
    }
}
