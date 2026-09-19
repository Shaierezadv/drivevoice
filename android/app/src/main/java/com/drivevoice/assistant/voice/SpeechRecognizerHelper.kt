package com.drivevoice.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * SpeechRecognizer wrapper — Hebrew he-IL.
 */
class SpeechRecognizerHelper(
    private val context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onListeningStarted()
        fun onPartial(text: String)
        fun onResult(text: String)
        fun onError(message: String)
        fun onEndOfSpeech()
    }

    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        stop()
        if (!isAvailable()) {
            listener.onError("זיהוי דיבור אינו זמין במכשיר")
            return
        }
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listener.onListeningStarted()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                listener.onEndOfSpeech()
            }

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "שגיאת אודיו"
                    SpeechRecognizer.ERROR_CLIENT -> "שגיאת לקוח"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "חסרה הרשאת מיקרופון"
                    SpeechRecognizer.ERROR_NETWORK -> "שגיאת רשת"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "פסק זמן רשת"
                    SpeechRecognizer.ERROR_NO_MATCH -> "לא זוהה דיבור"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "המזהה עסוק"
                    SpeechRecognizer.ERROR_SERVER -> "שגיאת שרת"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "לא נשמע דיבור"
                    else -> "שגיאת זיהוי ($error)"
                }
                listener.onError(msg)
            }

            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = texts?.firstOrNull().orEmpty()
                if (best.isNotBlank()) listener.onResult(best)
                else listener.onError("לא זוהה דיבור")
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val texts = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                texts?.firstOrNull()?.let { if (it.isNotBlank()) listener.onPartial(it) }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "he-IL")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "he-IL")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        r.startListening(intent)
    }

    fun stop() {
        try {
            recognizer?.stopListening()
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
    }

    fun destroy() = stop()
}
