# DriveVoice — עוזר נהיגה (Android)

Hebrew-first driving assistant: wake word → call, SMS, WhatsApp, navigate, media, open apps.

- **Package:** `com.drivevoice.assistant`
- **Stack:** Kotlin, Jetpack Compose, Material 3
- **minSdk 26 / targetSdk 34**
- **NLU:** on-device `IntentParser` (no paid APIs)
- **Version:** 1.1.0

## Open in Android Studio

1. Install [Android Studio](https://developer.android.com/studio).
2. **File → Open** → `android/` (the folder with `settings.gradle.kts`).
3. Connect a device/emulator (Google Play services recommended for SpeechRecognizer).
4. Run **app**.

```bash
cd android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Permissions

| Permission | Why |
|------------|-----|
| RECORD_AUDIO | SpeechRecognizer + background mic |
| READ_CONTACTS | Lookup by display name |
| CALL_PHONE | ACTION_CALL after confirm |
| SEND_SMS | SmsManager after confirm |
| POST_NOTIFICATIONS | Ongoing "listening" notification |
| FOREGROUND_SERVICE_MICROPHONE | Keep mic alive in background |
| SYSTEM_ALERT_WINDOW | Optional cover while auto-sending WhatsApp |
| Accessibility (optional) | Auto-tap WhatsApp send in hidden mode |

## Voice flow

1. Background service listens for **«היי דרייב»** (customizable).
2. Command (or the rest of the same utterance) → `IntentParser`.
3. Sensitive intents → TTS + כן/לא (auto-listen after the prompt).
4. WhatsApp: open chat **or** hidden auto-send (settings).
5. After the action, listening returns to the wake word.

Say **עצור האזנה** or use the notification action to stop the background mic.

## Privacy

See [PRIVACY.md](PRIVACY.md). No DriveVoice cloud NLU.

## Still later

On-device wake-word model (Porcupine/Sherpa), Android Auto, incoming-call handling.
