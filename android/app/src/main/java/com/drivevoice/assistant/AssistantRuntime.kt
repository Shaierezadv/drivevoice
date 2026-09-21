package com.drivevoice.assistant

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.drivevoice.assistant.actions.ActionExecutor
import com.drivevoice.assistant.actions.ActionResult
import com.drivevoice.assistant.actions.ResolvedContact
import com.drivevoice.assistant.nlu.IntentParser
import com.drivevoice.assistant.nlu.IntentType
import com.drivevoice.assistant.nlu.ParsedIntent
import com.drivevoice.assistant.nlu.WakeWord
import com.drivevoice.assistant.service.DrivingForegroundService
import com.drivevoice.assistant.voice.SpeechRecognizerHelper
import com.drivevoice.assistant.voice.TtsHelper

enum class AssistantState {
    IDLE, LISTENING, LISTENING_WAKE, PROCESSING, SPEAKING, AWAITING_CONFIRM
}

enum class ListenMode { NONE, WAKE, COMMAND }

data class UiState(
    val assistantState: AssistantState = AssistantState.IDLE,
    val transcript: String = "",
    val lastAction: String = "",
    val pendingIntent: ParsedIntent? = null,
    val confirmPrompt: String = "",
    val confirmBeforeSensitive: Boolean = true,
    val backgroundListening: Boolean = true,
    val wakePhrase: String = WakeWord.DEFAULT_PHRASE,
    val whatsAppHidden: Boolean = false,
    val error: String? = null,
    val candidates: List<String> = emptyList()
)

/**
 * Process-scoped assistant. Survives Activity/ViewModel so the foreground
 * microphone service can keep listening for the wake word.
 */
class AssistantRuntime(private val app: Application) {

    var ui by mutableStateOf(UiState())
        private set

    private val prefs = app.getSharedPreferences("drivevoice", Context.MODE_PRIVATE)
    private val executor = ActionExecutor(app) { ui.whatsAppHidden }
    private var speech: SpeechRecognizerHelper? = null
    private var tts: TtsHelper? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var listenAfterTts = false
    private var commandAfterTts = false
    private var listenMode = ListenMode.NONE
    private var pendingCandidates: List<ResolvedContact> = emptyList()

    init {
        ui = ui.copy(
            confirmBeforeSensitive = prefs.getBoolean("confirm_before_sensitive", true),
            backgroundListening = prefs.getBoolean("background_listen", true),
            wakePhrase = prefs.getString("wake_phrase", WakeWord.DEFAULT_PHRASE)
                ?: WakeWord.DEFAULT_PHRASE,
            whatsAppHidden = prefs.getBoolean("whatsapp_hidden", false)
        )
        tts = TtsHelper(app) { speaking ->
            onMain {
                if (speaking) {
                    ui = ui.copy(assistantState = AssistantState.SPEAKING)
                } else {
                    val shouldCommand = listenAfterTts
                    val forceCommand = commandAfterTts
                    listenAfterTts = false
                    commandAfterTts = false
                    val awaiting = ui.pendingIntent != null
                    when {
                        shouldCommand && (awaiting || forceCommand) ->
                            startListening(fromTts = true, mode = ListenMode.COMMAND)
                        ui.backgroundListening && !awaiting ->
                            startListening(fromTts = true, mode = ListenMode.WAKE)
                        else -> ui = ui.copy(
                            assistantState = if (awaiting) {
                                AssistantState.AWAITING_CONFIRM
                            } else {
                                AssistantState.IDLE
                            }
                        )
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

    fun setWakePhrase(value: String) {
        val v = value.trim().ifBlank { WakeWord.DEFAULT_PHRASE }
        prefs.edit().putString("wake_phrase", v).apply()
        ui = ui.copy(wakePhrase = v)
    }

    fun setWhatsAppHidden(value: Boolean) {
        prefs.edit().putBoolean("whatsapp_hidden", value).apply()
        ui = ui.copy(whatsAppHidden = value)
    }

    fun setBackgroundListening(value: Boolean) {
        prefs.edit().putBoolean("background_listen", value).apply()
        ui = ui.copy(backgroundListening = value)
        if (value) {
            ensureBackgroundService()
            if (ui.pendingIntent == null &&
                ui.assistantState != AssistantState.LISTENING &&
                ui.assistantState != AssistantState.SPEAKING
            ) {
                startListening(mode = ListenMode.WAKE)
            }
        } else {
            DrivingForegroundService.stop(app)
            if (listenMode == ListenMode.WAKE) stopListening()
        }
    }

    fun ensureBackgroundService() {
        if (!ui.backgroundListening) return
        try {
            DrivingForegroundService.start(app)
        } catch (e: Exception) {
            ui = ui.copy(error = e.message, lastAction = "לא ניתן להפעיל מיקרופון ברקע")
        }
    }

    fun onServiceStarted() {
        if (ui.backgroundListening &&
            ui.pendingIntent == null &&
            ui.assistantState != AssistantState.LISTENING &&
            ui.assistantState != AssistantState.SPEAKING &&
            ui.assistantState != AssistantState.PROCESSING
        ) {
            startListening(fromTts = true, mode = ListenMode.WAKE)
        }
    }

    fun onServiceDestroyed() {
        if (!ui.backgroundListening && listenMode == ListenMode.WAKE) {
            stopListening()
        }
    }

    fun toggleListen() {
        when (ui.assistantState) {
            AssistantState.LISTENING -> {
                stopListening()
                if (ui.backgroundListening) {
                    startListening(mode = ListenMode.WAKE)
                }
            }
            AssistantState.LISTENING_WAKE ->
                startListening(mode = ListenMode.COMMAND)
            AssistantState.IDLE, AssistantState.AWAITING_CONFIRM, AssistantState.SPEAKING ->
                startListening(mode = ListenMode.COMMAND)
            else -> { }
        }
    }

    fun startListening(fromTts: Boolean = false, mode: ListenMode = ListenMode.COMMAND) {
        listenMode = mode
        speech?.destroy()
        speech = SpeechRecognizerHelper(app, object : SpeechRecognizerHelper.Listener {
            override fun onListeningStarted() {
                onMain {
                    ui = ui.copy(
                        assistantState = if (listenMode == ListenMode.WAKE) {
                            AssistantState.LISTENING_WAKE
                        } else {
                            AssistantState.LISTENING
                        },
                        error = null
                    )
                }
            }

            override fun onPartial(text: String) {
                onMain { ui = ui.copy(transcript = text) }
            }

            override fun onResult(text: String) {
                onMain {
                    ui = ui.copy(transcript = text)
                    if (listenMode == ListenMode.WAKE) {
                        handleWake(text)
                    } else {
                        ui = ui.copy(assistantState = AssistantState.PROCESSING)
                        handleTranscript(text)
                    }
                }
            }

            override fun onError(message: String) {
                onMain {
                    if (listenMode == ListenMode.WAKE) {
                        startListening(fromTts = true, mode = ListenMode.WAKE)
                        return@onMain
                    }
                    val awaiting = ui.pendingIntent != null
                    ui = ui.copy(
                        assistantState = if (awaiting) {
                            AssistantState.AWAITING_CONFIRM
                        } else if (ui.backgroundListening) {
                            AssistantState.LISTENING_WAKE
                        } else {
                            AssistantState.IDLE
                        },
                        error = message,
                        lastAction = message
                    )
                    if (!awaiting && ui.backgroundListening) {
                        startListening(fromTts = true, mode = ListenMode.WAKE)
                    } else if (!awaiting) {
                        speak(message, listenAfter = false)
                    }
                }
            }

            override fun onEndOfSpeech() {}
        })
        if (!fromTts && mode == ListenMode.COMMAND) {
            listenAfterTts = false
            commandAfterTts = false
            tts?.stop()
        }
        speech?.startListening(loopOnFailure = mode == ListenMode.WAKE)
    }

    fun stopListening() {
        listenMode = ListenMode.NONE
        speech?.stop()
        ui = ui.copy(
            assistantState = if (ui.pendingIntent != null) {
                AssistantState.AWAITING_CONFIRM
            } else {
                AssistantState.IDLE
            }
        )
    }

    private fun handleWake(text: String) {
        if (IntentParser.parse(text).type == IntentType.STOP_LISTEN) {
            handleTranscript(text)
            return
        }
        val match = WakeWord.match(text, ui.wakePhrase)
        if (!match.hit) {
            startListening(fromTts = true, mode = ListenMode.WAKE)
            return
        }
        if (match.rest.isBlank()) {
            ui = ui.copy(lastAction = "מילת הפעלה — דבר פקודה")
            commandAfterTts = true
            speak("כן?", listenAfter = true)
        } else {
            ui = ui.copy(assistantState = AssistantState.PROCESSING)
            handleTranscript(match.rest)
        }
    }

    private fun handleTranscript(text: String) {
        val awaiting = ui.pendingIntent != null
        val parsed = IntentParser.parse(text, awaitingConfirm = awaiting)

        if (parsed.type == IntentType.STOP_LISTEN) {
            setBackgroundListening(false)
            ui = ui.copy(lastAction = "האזנה ברקע כובתה", assistantState = AssistantState.IDLE)
            speak("האזנה כובתה")
            return
        }

        if (pendingCandidates.isNotEmpty() &&
            awaiting &&
            parsed.type != IntentType.CONFIRM &&
            parsed.type != IntentType.CANCEL
        ) {
            applyDisambiguation(text)
            return
        }

        if (awaiting && parsed.type != IntentType.CONFIRM && parsed.type != IntentType.CANCEL) {
            ui = ui.copy(
                lastAction = "יש פעולה ממתינה. אמור כן או לא",
                assistantState = AssistantState.AWAITING_CONFIRM
            )
            speak("יש פעולה ממתינה. אמור כן או לא", listenAfter = true)
            return
        }

        when (parsed.type) {
            IntentType.CONFIRM -> confirmPending()
            IntentType.CANCEL -> cancelPending()
            IntentType.UNKNOWN -> {
                ui = ui.copy(lastAction = parsed.summaryHe())
                speak("לא הבנתי. נסה שוב.")
            }
            IntentType.OPEN_APP, IntentType.NAVIGATE, IntentType.MEDIA -> {
                applyResult(executor.prepareOrExecute(parsed, confirmed = true))
            }
            IntentType.STOP_LISTEN -> { }
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
                    applyResult(executor.prepareOrExecute(parsed, confirmed = true))
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
            filtered.isEmpty() -> speak("לא מצאתי התאמה. אמור את השם שוב", listenAfter = true)
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
}
