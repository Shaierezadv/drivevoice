package com.drivevoice.assistant

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drivevoice.assistant.actions.ActionExecutor
import com.drivevoice.assistant.actions.ActionResult
import com.drivevoice.assistant.actions.ResolvedContact
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
    val error: String? = null,
    val candidates: List<String> = emptyList()
)

class DrivingViewModel(app: Application) : AndroidViewModel(app) {

    var ui by mutableStateOf(UiState())
        private set

    private val prefs = app.getSharedPreferences("drivevoice", Context.MODE_PRIVATE)
    private val executor = ActionExecutor(app.applicationContext)
    private var speech: SpeechRecognizerHelper? = null
    private var tts: TtsHelper? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var listenAfterTts = false
    private var pendingCandidates: List<ResolvedContact> = emptyList()

    init {
        ui = ui.copy(
            confirmBeforeSensitive = prefs.getBoolean("confirm_before_sensitive", true)
        )
        tts = TtsHelper(app.applicationContext) { speaking ->
            onMain {
                if (speaking) {
                    ui = ui.copy(assistantState = AssistantState.SPEAKING)
                } else {
                    val shouldListen = listenAfterTts
                    listenAfterTts = false
                    val awaiting = ui.pendingIntent != null
                    ui = ui.copy(
                        assistantState = if (awaiting) {
                            AssistantState.AWAITING_CONFIRM
                        } else {
                            AssistantState.IDLE
                        }
                    )
                    if (shouldListen && awaiting) {
                        startListening(fromTts = true)
                    }
                }
            }
        }
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else mainHandler.post(block)
    }

    fun setConfirmBeforeSensitive(value: Boolean) {
        prefs.edit().putBoolean("confirm_before_sensitive", value).apply()
        ui = ui.copy(confirmBeforeSensitive = value)
    }

    fun toggleListen() {
        when (ui.assistantState) {
            AssistantState.LISTENING -> stopListening()
            AssistantState.IDLE, AssistantState.AWAITING_CONFIRM, AssistantState.SPEAKING ->
                startListening()
            else -> { /* ignore while processing */ }
        }
    }

    fun startListening(fromTts: Boolean = false) {
        val ctx = getApplication<Application>()
        speech?.destroy()
        speech = SpeechRecognizerHelper(ctx, object : SpeechRecognizerHelper.Listener {
            override fun onListeningStarted() {
                onMain { ui = ui.copy(assistantState = AssistantState.LISTENING, error = null) }
            }

            override fun onPartial(text: String) {
                onMain { ui = ui.copy(transcript = text) }
            }

            override fun onResult(text: String) {
                onMain {
                    ui = ui.copy(transcript = text, assistantState = AssistantState.PROCESSING)
                    handleTranscript(text)
                }
            }

            override fun onError(message: String) {
                onMain {
                    val awaiting = ui.pendingIntent != null
                    ui = ui.copy(
                        assistantState = if (awaiting) {
                            AssistantState.AWAITING_CONFIRM
                        } else {
                            AssistantState.IDLE
                        },
                        error = message,
                        lastAction = message
                    )
                    if (!awaiting) speak(message, listenAfter = false)
                }
            }

            override fun onEndOfSpeech() {
                // wait for results
            }
        })
        if (!fromTts) {
            listenAfterTts = false
            tts?.stop()
        }
        speech?.startListening()
    }

    fun stopListening() {
        speech?.stop()
        ui = ui.copy(
            assistantState = if (ui.pendingIntent != null) {
                AssistantState.AWAITING_CONFIRM
            } else {
                AssistantState.IDLE
            }
        )
    }

    private fun handleTranscript(text: String) {
        viewModelScope.launch {
            val awaiting = ui.pendingIntent != null
            val parsed = IntentParser.parse(text, awaitingConfirm = awaiting)

            if (pendingCandidates.isNotEmpty() &&
                awaiting &&
                parsed.type != IntentType.CONFIRM &&
                parsed.type != IntentType.CANCEL
            ) {
                applyDisambiguation(text)
                return@launch
            }

            if (awaiting && parsed.type != IntentType.CONFIRM && parsed.type != IntentType.CANCEL) {
                ui = ui.copy(
                    lastAction = "יש פעולה ממתינה. אמור כן או לא",
                    assistantState = AssistantState.AWAITING_CONFIRM
                )
                speak("יש פעולה ממתינה. אמור כן או לא", listenAfter = true)
                return@launch
            }

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
                IntentType.OPEN_APP, IntentType.NAVIGATE, IntentType.MEDIA -> {
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
                        speak("לאשר: ${parsed.summaryHe()}? אמור כן או לא", listenAfter = true)
                    } else {
                        val result = executor.prepareOrExecute(parsed, confirmed = true)
                        applyResult(result)
                    }
                }
            }
        }
    }

    private fun applyDisambiguation(text: String) {
        val needle = text.trim()
        val filtered = pendingCandidates.filter {
            it.displayName.contains(needle, ignoreCase = true)
        }
        when {
            filtered.size == 1 -> {
                val chosen = filtered.first()
                val pending = ui.pendingIntent ?: return
                pendingCandidates = emptyList()
                val updated = pending.copy(
                    contactName = chosen.displayName,
                    phoneNumber = chosen.phoneNumber
                )
                ui = ui.copy(
                    pendingIntent = updated,
                    candidates = emptyList(),
                    confirmPrompt = "לאשר: ${updated.summaryHe()}?",
                    lastAction = "נבחר ${chosen.displayName}",
                    assistantState = AssistantState.AWAITING_CONFIRM
                )
                speak("לאשר: ${updated.summaryHe()}? אמור כן או לא", listenAfter = true)
            }
            filtered.isEmpty() -> {
                speak("לא מצאתי התאמה. אמור את השם שוב", listenAfter = true)
            }
            else -> {
                pendingCandidates = filtered
                val names = filtered.take(3).joinToString(", ") { it.displayName }
                ui = ui.copy(candidates = filtered.map { it.displayName })
                speak("מצאתי כמה: $names. אמור את השם המלא", listenAfter = true)
            }
        }
    }

    fun confirmPending() {
        val pending = ui.pendingIntent
        if (pending == null) {
            speak("אין פעולה ממתינה")
            return
        }
        if (pendingCandidates.size > 1) {
            speak("בחר שם קודם", listenAfter = true)
            return
        }
        val result = executor.prepareOrExecute(pending, confirmed = true)
        ui = ui.copy(pendingIntent = null, confirmPrompt = "", candidates = emptyList())
        pendingCandidates = emptyList()
        applyResult(result)
    }

    fun cancelPending() {
        pendingCandidates = emptyList()
        ui = ui.copy(
            pendingIntent = null,
            confirmPrompt = "",
            lastAction = "בוטל",
            assistantState = AssistantState.IDLE,
            candidates = emptyList()
        )
        speak("בוטל")
    }

    private fun applyResult(result: ActionResult) {
        when (result) {
            is ActionResult.Success -> {
                pendingCandidates = emptyList()
                ui = ui.copy(
                    lastAction = result.messageHe,
                    assistantState = AssistantState.IDLE,
                    pendingIntent = null,
                    confirmPrompt = "",
                    candidates = emptyList()
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
                speak(result.messageHe, listenAfter = true)
            }
            is ActionResult.NeedsDisambiguation -> {
                pendingCandidates = result.candidates
                ui = ui.copy(
                    pendingIntent = result.intent,
                    confirmPrompt = result.messageHe,
                    lastAction = result.messageHe,
                    assistantState = AssistantState.AWAITING_CONFIRM,
                    candidates = result.candidates.map { it.displayName }
                )
                speak(result.messageHe, listenAfter = true)
            }
        }
    }

    private fun speak(text: String, listenAfter: Boolean = false) {
        listenAfterTts = listenAfter
        tts?.speak(text)
    }

    override fun onCleared() {
        speech?.destroy()
        tts?.shutdown()
        super.onCleared()
    }
}
