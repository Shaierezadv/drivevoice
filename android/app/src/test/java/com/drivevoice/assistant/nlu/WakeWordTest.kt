package com.drivevoice.assistant.nlu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordTest {

    @Test
    fun detectsDefaultAndStripsCommand() {
        val m = WakeWord.match("היי דרייב התקשר ליוסי")
        assertTrue(m.hit)
        assertEquals("התקשר ליוסי", m.rest)
    }

    @Test
    fun wakeOnly() {
        val m = WakeWord.match("היי דרייב")
        assertTrue(m.hit)
        assertEquals("", m.rest)
    }

    @Test
    fun customPhrase() {
        val m = WakeWord.match("שלום רובוט נווט הביתה", "שלום רובוט")
        assertTrue(m.hit)
        assertEquals("נווט הביתה", m.rest)
    }

    @Test
    fun noWake() {
        val m = WakeWord.match("התקשר ליוסי")
        assertFalse(m.hit)
    }
}
