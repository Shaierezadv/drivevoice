package com.drivevoice.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * SpeechRecognizer wrapper — Hebrew he-IL, with a short retry on transient errors.
 * Must be created/started on the main thread.
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
    private val main = Handler(Looper.getMainLooper())
    private var retries = 0
    private var destroyed = false
    private var loopOnFailure = false

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(loopOnFailure: Boolean = false) {
        this.loopOnFailure = loopOnFailure
        runOnMain { startInternal(resetRetries = true) }
    }

    fun stop() {
        destroyed = true
        main.removeCallbacksAndMessages(null)
        try {
            recognizer?.stopListening()
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
    }

    fun destroy() = stop()

    private fun startInternal(resetRetries: Boolean) {
        if (destroyed) return
        if (resetRetries) retries = 0
        teardownRecognizer()
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
                if (!destroyed && loopOnFailure &&
                    error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                ) {
                    val delay = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 700L else 350L
                    main.postDelayed({ startInternal(resetRetries = true) }, delay)
                    return
                }
                val retryable = error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                    error == SpeechRecognizer.ERROR_CLIENT ||
                    error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                    error == SpeechRecognizer.ERROR_NETWORK
                if (!destroyed && retryable && retries < 2) {
                    retries++
                    val delay = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 700L else 400L
                    main.postDelayed({ startInternal(resetRetries = false) }, delay)
                    return
                }
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
                if (best.isNotBlank()) {
                    listener.onResult(best)
                } else if (loopOnFailure && !destroyed) {
                    main.postDelayed({ startInternal(resetRetries = true) }, 350)
                } else {
                    listener.onError("לא זוהה דיבור")
                }
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
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
        }
        r.startListening(intent)
    }

    private fun teardownRecognizer() {
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else main.post(block)
    }
}
