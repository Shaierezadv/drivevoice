package com.drivevoice.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Hebrew TextToSpeech helper.
 */
class TtsHelper(
    context: Context,
    private val onSpeakingChanged: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pending: String? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val he = Locale("he", "IL")
            val result = tts?.setLanguage(he)
            ready = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ready) {
                // Fallback to default locale if Hebrew voice missing
                tts?.language = Locale.getDefault()
                ready = true
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeakingChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    onSpeakingChanged(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onSpeakingChanged(false)
                }
            })
            pending?.let {
                pending = null
                speak(it)
            }
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!ready) {
            pending = text
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dv-${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
        onSpeakingChanged(false)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
