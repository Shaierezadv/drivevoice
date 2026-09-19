package com.drivevoice.assistant

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drivevoice.assistant.actions.ActionExecutor
import com.drivevoice.assistant.actions.ActionResult
import com.drivevoice.assistant.nlu.IntentParser
import com.drivevoice.assistant.nlu.IntentType
import com.drivevoice.assistant.nlu.ParsedIntent
import com.drivevoice.assistant.voice.SpeechRecognizerHelper
import com.drivevoice.assistant.voice.TtsHelper
import kotlinx.coroutines.launch

enum class AssistantState {
    IDLE, LISTENING, PROCESSING, SPEAKING, AWAITING_CONFIRM
}

data class UiState(
    val assistantState: AssistantState = AssistantState.IDLE,
    val transcript: String = "",
    val lastAction: String = "",
    val pendingIntent: ParsedIntent? = null,
    val confirmPrompt: String = "",
    val confirmBeforeSensitive: Boolean = true,
    val error: String? = null
)

class DrivingViewModel(app: Application) : AndroidViewModel(app) {

    var ui by mutableStateOf(UiState())
        private set

    private val prefs = app.getSharedPreferences("drivevoice", Context.MODE_PRIVATE)
    private val executor = ActionExecutor(app.applicationContext)
    private var speech: SpeechRecognizerHelper? = null
    private var tts: TtsHelper? = null

    init {
        ui = ui.copy(
            confirmBeforeSensitive = prefs.getBoolean("confirm_before_sensitive", true)
        )
        tts = TtsHelper(app.applicationContext) { speaking ->
            if (speaking) {
                ui = ui.copy(assistantState = AssistantState.SPEAKING)
            } else if (ui.assistantState == AssistantState.SPEAKING) {
                ui = ui.copy(
                    assistantState = if (ui.pendingIntent != null)
                        AssistantState.AWAITING_CONFIRM else AssistantState.IDLE
                )
            }
        }
    }

    fun setConfirmBeforeSensitive(value: Boolean) {
        prefs.edit().putBoolean("confirm_before_sensitive", value).apply()
        ui = ui.copy(confirmBeforeSensitive = value)
    }

    fun toggleListen() {
        when (ui.assistantState) {
            AssistantState.LISTENING -> stopListening()
            AssistantState.IDLE, AssistantState.AWAITING_CONFIRM -> startListening()
            else -> { /* ignore while processing/speaking */ }
        }
    }

    fun startListening() {
        val ctx = getApplication<Application>()
        speech?.destroy()
        speech = SpeechRecognizerHelper(ctx, object : SpeechRecognizerHelper.Listener {
            override fun onListeningStarted() {
                ui = ui.copy(assistantState = AssistantState.LISTENING, error = null)
            }

            override fun onPartial(text: String) {
                ui = ui.copy(transcript = text)
            }

            override fun onResult(text: String) {
                ui = ui.copy(transcript = text, assistantState = AssistantState.PROCESSING)
                handleTranscript(text)
            }

            override fun onError(message: String) {
                ui = ui.copy(
                    assistantState = if (ui.pendingIntent != null)
                        AssistantState.AWAITING_CONFIRM else AssistantState.IDLE,
                    error = message,
                    lastAction = message
                )
                speak(message)
            }

            override fun onEndOfSpeech() {
                // wait for results
            }
        })
        tts?.stop()
        speech?.startListening()
    }

    fun stopListening() {
        speech?.stop()
        ui = ui.copy(
            assistantState = if (ui.pendingIntent != null)
                AssistantState.AWAITING_CONFIRM else AssistantState.IDLE
        )
    }

    private fun handleTranscript(text: String) {
        viewModelScope.launch {
            val parsed = IntentParser.parse(text)
            when (parsed.type) {
                IntentType.CONFIRM -> confirmPending()
                IntentType.CANCEL -> cancelPending()
                IntentType.UNKNOWN -> {
                    ui = ui.copy(
                        lastAction = parsed.summaryHe(),
                        assistantState = AssistantState.IDLE
                    )
                    speak("לא הבנתי. נסה שוב.")
                }
                IntentType.OPEN_APP -> {
                    val result = executor.prepareOrExecute(parsed, confirmed = true)
                    applyResult(result)
                }
                else -> {
                    if (ui.confirmBeforeSensitive && parsed.needsConfirmation) {
                        ui = ui.copy(
                            pendingIntent = parsed,
                            confirmPrompt = "לאשר: ${parsed.summaryHe()}?",
                            lastAction = "ממתין לאישור: ${parsed.summaryHe()}",
                            assistantState = AssistantState.AWAITING_CONFIRM
                        )
                        speak("לאשר: ${parsed.summaryHe()}? אמור כן או לא")
                    } else {
                        val result = executor.prepareOrExecute(parsed, confirmed = true)
                        applyResult(result)
                    }
                }
            }
        }
    }

    fun confirmPending() {
        val pending = ui.pendingIntent
        if (pending == null) {
            speak("אין פעולה ממתינה")
            return
        }
        val result = executor.prepareOrExecute(pending, confirmed = true)
        ui = ui.copy(pendingIntent = null, confirmPrompt = "")
        applyResult(result)
    }

    fun cancelPending() {
        ui = ui.copy(
            pendingIntent = null,
            confirmPrompt = "",
            lastAction = "בוטל",
            assistantState = AssistantState.IDLE
        )
        speak("בוטל")
    }

    private fun applyResult(result: ActionResult) {
        when (result) {
            is ActionResult.Success -> {
                ui = ui.copy(
                    lastAction = result.messageHe,
                    assistantState = AssistantState.IDLE,
                    pendingIntent = null,
                    confirmPrompt = ""
                )
                speak(result.messageHe)
            }
            is ActionResult.Failure -> {
                ui = ui.copy(
                    lastAction = result.messageHe,
                    assistantState = AssistantState.IDLE,
                    error = result.messageHe
                )
                speak(result.messageHe)
            }
            is ActionResult.NeedsConfirm -> {
                ui = ui.copy(
                    pendingIntent = result.intent,
                    confirmPrompt = result.messageHe,
                    lastAction = result.messageHe,
                    assistantState = AssistantState.AWAITING_CONFIRM
                )
                speak(result.messageHe)
            }
        }
    }

    private fun speak(text: String) {
        tts?.speak(text)
    }

    override fun onCleared() {
        speech?.destroy()
        tts?.shutdown()
        super.onCleared()
    }
}
