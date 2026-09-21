# DriveVoice — עוזר נהיגה (Android)

Hebrew-first driving assistant: voice → call, SMS, navigate, media, open apps.

- **Package:** `com.drivevoice.assistant`
- **Stack:** Kotlin, Jetpack Compose, Material 3
- **minSdk 26 / targetSdk 34**
- **NLU:** on-device `IntentParser` (no paid APIs)
- **Version:** 1.0.1

## Open in Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (Hedgehog / Iguana+ recommended).
2. **File → Open** → select this folder: `android/` (the one containing `settings.gradle.kts`).
3. Let Gradle sync.
4. Connect a device/emulator (Google Play services recommended for SpeechRecognizer).
5. Run configuration **app**.

### CLI build (optional)

```bash
cd android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Permissions

| Permission | Why |
|------------|-----|
| RECORD_AUDIO | SpeechRecognizer |
| READ_CONTACTS | Lookup by display name |
| CALL_PHONE | ACTION_CALL after confirm |
| SEND_SMS | SmsManager after confirm |

First launch shows a Hebrew permissions screen.

## Screens

1. **Permissions** — list + continue
2. **Driving Mode** — large mic, transcript, last action, Yes/No confirm, RTL, keep-screen-on
3. **Settings** — he-IL, confirm-before-sensitive toggle
4. **Help** — command examples (see `COMMANDS.md`)

## Voice flow

1. Tap mic → SpeechRecognizer `he-IL` (retries transient errors)
2. `IntentParser.parse(transcript)`
3. Sensitive intents (CALL/SMS/EMAIL) → TTS ask + on-screen כן/לא
4. After the prompt, listening starts automatically for כן/לא
5. Confirm → `ActionExecutor` (CALL / SMS / mailto / Waze-Maps / media keys / launch app)
6. Several matching contacts → speak names and wait for the full name

## Project layout

```
android/
  app/src/main/java/com/drivevoice/assistant/
    nlu/IntentParser.kt, HebrewNumbers.kt, ParsedIntent.kt
    actions/ActionExecutor.kt, ContactResolver.kt
    voice/SpeechRecognizerHelper.kt, TtsHelper.kt
    ui/screens/...
    DrivingViewModel.kt, MainActivity.kt
  app/src/test/.../IntentParserTest.kt, HebrewNumbersTest.kt
  COMMANDS.md  PRIVACY.md  README.md
```

## Privacy

See [PRIVACY.md](PRIVACY.md). No DriveVoice cloud NLU; speech recognition uses the device/OEM service.

## Non-goals (still later)

Always-on wake word, full Android Auto, WhatsApp auto-send without UI, cloud NLU, incoming-call handling.
