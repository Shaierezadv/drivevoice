package com.drivevoice.assistant.nlu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentParserTest {

    @Test
    fun callByContactName() {
        val r = IntentParser.parse("התקשר ליוסי")
        assertEquals(IntentType.CALL, r.type)
        assertEquals("יוסי", r.contactName)
        assertNull(r.phoneNumber)
        assertTrue(r.needsConfirmation)
    }

    @Test
    fun callByContactVariant() {
        val r = IntentParser.parse("תתקשר למיכל")
        assertEquals(IntentType.CALL, r.type)
        assertEquals("מיכל", r.contactName)
    }

    @Test
    fun dialNumber() {
        val r = IntentParser.parse("חייג 0501234567")
        assertEquals(IntentType.CALL, r.type)
        assertEquals("0501234567", r.phoneNumber)
    }

    @Test
    fun callToNumberPhrase() {
        val r = IntentParser.parse("התקשר למספר 03-7520432")
        assertEquals(IntentType.CALL, r.type)
        assertNotNull(r.phoneNumber)
        assertTrue(r.phoneNumber!!.contains("037520432") || r.phoneNumber!!.contains("03"))
    }

    @Test
    fun smsWithBody() {
        val r = IntentParser.parse("שלח הודעה לדני תגיע בעוד עשר דקות")
        assertEquals(IntentType.SMS, r.type)
        assertEquals("דני", r.contactName)
        assertEquals("תגיע בעוד עשר דקות", r.messageBody)
        assertTrue(r.needsConfirmation)
    }

    @Test
    fun smsVariant() {
        val r = IntentParser.parse("שלח SMS ליוסי אני בדרך")
        assertEquals(IntentType.SMS, r.type)
        assertEquals("יוסי", r.contactName)
        assertEquals("אני בדרך", r.messageBody)
    }

    @Test
    fun smsShort() {
        val r = IntentParser.parse("הודעה למיכל מחכה בחניה")
        assertEquals(IntentType.SMS, r.type)
        assertEquals("מיכל", r.contactName)
        assertEquals("מחכה בחניה", r.messageBody)
    }

    @Test
    fun emailWithSubjectAndBody() {
        val r = IntentParser.parse("שלח מייל ל name@example.com נושא פגישה תוכן נתראה מחר")
        assertEquals(IntentType.EMAIL, r.type)
        assertEquals("name@example.com", r.email)
        assertEquals("פגישה", r.subject)
        assertEquals("נתראה מחר", r.messageBody)
        assertTrue(r.needsConfirmation)
    }

    @Test
    fun emailShort() {
        val r = IntentParser.parse("מייל ל hello@test.co.il נושא שלום תוכן היי")
        assertEquals(IntentType.EMAIL, r.type)
        assertEquals("hello@test.co.il", r.email)
        assertEquals("שלום", r.subject)
        assertEquals("היי", r.messageBody)
    }

    @Test
    fun openWaze() {
        val r = IntentParser.parse("פתח ווייז")
        assertEquals(IntentType.OPEN_APP, r.type)
        assertEquals("Waze", r.appLabel)
    }

    @Test
    fun openMusic() {
        val r = IntentParser.parse("תפתח מוזיקה")
        assertEquals(IntentType.OPEN_APP, r.type)
        assertEquals("Music", r.appLabel)
    }

    @Test
    fun openWhatsapp() {
        val r = IntentParser.parse("פתח וואטסאפ")
        assertEquals(IntentType.OPEN_APP, r.type)
        assertEquals("WhatsApp", r.appLabel)
    }

    @Test
    fun openMaps() {
        val r = IntentParser.parse("פתח מפות")
        assertEquals(IntentType.OPEN_APP, r.type)
        assertEquals("Maps", r.appLabel)
    }

    @Test
    fun confirmWords() {
        listOf("כן", "אשר", "בצע").forEach {
            assertEquals(IntentType.CONFIRM, IntentParser.parse(it).type)
        }
    }

    @Test
    fun cancelWords() {
        listOf("לא", "בטל", "עצור").forEach {
            assertEquals(IntentType.CANCEL, IntentParser.parse(it).type)
        }
    }

    @Test
    fun unknown() {
        val r = IntentParser.parse("מה מזג האוויר")
        assertEquals(IntentType.UNKNOWN, r.type)
    }

    @Test
    fun summaryHeCall() {
        val r = IntentParser.parse("התקשר ליוסי")
        assertTrue(r.summaryHe().contains("יוסי"))
    }
}
