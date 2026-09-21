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

    @Test
    fun politeConfirm() {
        assertEquals(IntentType.CONFIRM, IntentParser.parse("כן בבקשה").type)
    }

    @Test
    fun fillerPrefixCall() {
        val r = IntentParser.parse("אפשר להתקשר ליוסי")
        assertEquals(IntentType.CALL, r.type)
        assertEquals("יוסי", r.contactName)
    }

    @Test
    fun punctuationDoesNotBreakCall() {
        val r = IntentParser.parse("התקשר ליוסי.")
        assertEquals(IntentType.CALL, r.type)
        assertEquals("יוסי", r.contactName)
    }

    @Test
    fun confirmDoesNotEatNavigateLikePhrase() {
        val r = IntentParser.parse("בצע ניווט לתל אביב")
        assertTrue(r.type != IntentType.CONFIRM)
    }

    @Test
    fun navigateToCity() {
        val r = IntentParser.parse("נווט לתל אביב")
        assertEquals(IntentType.NAVIGATE, r.type)
        assertEquals("תל אביב", r.destination)
    }

    @Test
    fun navigateTakeMe() {
        val r = IntentParser.parse("קח אותי לרמת גן")
        assertEquals(IntentType.NAVIGATE, r.type)
        assertEquals("רמת גן", r.destination)
    }

    @Test
    fun mediaNext() {
        val r = IntentParser.parse("שיר הבא")
        assertEquals(IntentType.MEDIA, r.type)
        assertEquals(MediaAction.NEXT, r.mediaAction)
    }

    @Test
    fun mediaPause() {
        val r = IntentParser.parse("השהה")
        assertEquals(IntentType.MEDIA, r.type)
        assertEquals(MediaAction.PAUSE, r.mediaAction)
    }

    @Test
    fun spokenHebrewPhoneDigits() {
        val r = IntentParser.parse("חייג אפס חמש אפס אחד שתיים שלוש ארבע חמש שש שבע")
        assertEquals(IntentType.CALL, r.type)
        assertEquals("0501234567", r.phoneNumber)
    }

    @Test
    fun awaitingConfirmLooseCancel() {
        val r = IntentParser.parse("לא תודה", awaitingConfirm = true)
        assertEquals(IntentType.CANCEL, r.type)
    }

    @Test
    fun whatsappSend() {
        val r = IntentParser.parse("שלח וואטסאפ ליוסי אני בדרך")
        assertEquals(IntentType.WHATSAPP, r.type)
        assertEquals("יוסי", r.contactName)
        assertEquals("אני בדרך", r.messageBody)
        assertTrue(r.needsConfirmation)
    }

    @Test
    fun whatsappShort() {
        val r = IntentParser.parse("וואטסאפ למיכל מחכה בחניה")
        assertEquals(IntentType.WHATSAPP, r.type)
        assertEquals("מיכל", r.contactName)
        assertEquals("מחכה בחניה", r.messageBody)
    }

    @Test
    fun openWhatsappStillOpensApp() {
        val r = IntentParser.parse("פתח וואטסאפ")
        assertEquals(IntentType.OPEN_APP, r.type)
        assertEquals("WhatsApp", r.appLabel)
    }

    @Test
    fun stopListen() {
        assertEquals(IntentType.STOP_LISTEN, IntentParser.parse("עצור האזנה").type)
        assertEquals(IntentType.CANCEL, IntentParser.parse("עצור").type)
    }
}
