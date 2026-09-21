package com.drivevoice.assistant.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Hebrew TextToSpeech helper. Speaking callbacks are always posted on the main thread.
 */
class TtsHelper(
    context: Context,
    private val onSpeakingChanged: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pending: String? = null
    private val main = Handler(Looper.getMainLooper())

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val he = Locale("he", "IL")
            val result = tts?.setLanguage(he)
            ready = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ready) {
                tts?.language = Locale.getDefault()
                ready = true
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    notifySpeaking(true)
                }

                override fun onDone(utteranceId: String?) {
                    notifySpeaking(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    notifySpeaking(false)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    notifySpeaking(false)
                }
            })
            pending?.let {
                pending = null
                speak(it)
            }
        }
    }

    private fun notifySpeaking(speaking: Boolean) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onSpeakingChanged(speaking)
        } else {
            main.post { onSpeakingChanged(speaking) }
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
        notifySpeaking(false)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
