package com.drivevoice.assistant.nlu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HebrewNumbersTest {

    @Test
    fun replacesLongSpokenPhone() {
        val raw = "חייג אפס חמש אפס אחד שתיים שלוש ארבע חמש שש שבע"
        val out = HebrewNumbers.replaceSpokenDigits(raw)
        assertEquals("חייג 0501234567", out)
    }

    @Test
    fun leavesShortNonPhoneWords() {
        val raw = "שלח הודעה לדני תגיע בעוד עשר דקות"
        assertEquals(raw, HebrewNumbers.replaceSpokenDigits(raw))
    }

    @Test
    fun spokenRunToDigits() {
        assertEquals(
            "0501234567",
            HebrewNumbers.spokenRunToDigits("אפס חמש אפס אחד שתיים שלוש ארבע חמש שש שבע")
        )
    }

    @Test
    fun spokenRunRejectsShort() {
        assertNull(HebrewNumbers.spokenRunToDigits("אפס חמש"))
    }
}
