package com.drivevoice.assistant.actions

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumbersTest {

    @Test
    fun localIsraeliToIntl() {
        assertEquals("972501234567", PhoneNumbers.toWhatsAppIntl("0501234567"))
        assertEquals("972501234567", PhoneNumbers.toWhatsAppIntl("050-123-4567"))
    }

    @Test
    fun alreadyIntl() {
        assertEquals("972501234567", PhoneNumbers.toWhatsAppIntl("972501234567"))
    }
}
