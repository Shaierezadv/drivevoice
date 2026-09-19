# DriveVoice — עוזר נהיגה (Android MVP)

Hebrew-first hands-free driving assistant: voice → call, SMS, email, open apps.

- **Package:** `com.drivevoice.assistant`
- **Stack:** Kotlin, Jetpack Compose, Material 3
- **minSdk 26 / targetSdk 34**
- **NLU:** on-device `IntentParser` (no paid APIs)

## Open in Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (Hedgehog / Iguana+ recommended).
2. **File → Open** → select this folder: `android/` (the one containing `settings.gradle.kts`).
3. Let Gradle sync. If the wrapper JAR is missing, Android Studio will offer to use its embedded Gradle, or run:
   ```bash
   gradle wrapper --gradle-version 8.2
   ```
4. Connect a device/emulator (Google Play services recommended for SpeechRecognizer).
5. Run configuration **app**.

### CLI build (optional)

```bash
cd android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

If `./gradlew` is not executable yet, generate the wrapper from Android Studio or a local Gradle 8.2+ install.

## Permissions

| Permission | Why |
|------------|-----|
| RECORD_AUDIO | SpeechRecognizer |
| READ_CONTACTS | Lookup by display name contains |
| CALL_PHONE | ACTION_CALL after confirm |
| SEND_SMS | SmsManager / SMS intent after confirm |
| POST_NOTIFICATIONS | API 33+ |

First launch shows a Hebrew permissions screen.

## Screens

1. **Permissions** — list + continue  
2. **Driving Mode** — large mic, transcript, last action, Yes/No confirm, RTL  
3. **Settings** — he-IL, confirm-before-sensitive toggle  
4. **Help** — command examples (see `COMMANDS.md`)

## Voice flow

1. Tap mic → SpeechRecognizer `he-IL`  
2. `IntentParser.parse(transcript)`  
3. Sensitive intents (CALL/SMS/EMAIL) → TTS ask + on-screen כן/לא  
4. Confirm → `ActionExecutor` (CALL / SMS / mailto / launch app)

## Project layout

```
android/
  app/src/main/java/com/drivevoice/assistant/
    nlu/IntentParser.kt
    actions/ActionExecutor.kt, ContactResolver.kt
    voice/SpeechRecognizerHelper.kt, TtsHelper.kt
    ui/screens/...
    DrivingViewModel.kt, MainActivity.kt
  app/src/test/.../IntentParserTest.kt
  COMMANDS.md  PRIVACY.md  README.md
```

## Privacy

See [PRIVACY.md](PRIVACY.md). No DriveVoice cloud NLU; speech recognition uses the device/OEM service.

## Non-goals (MVP)

Always-on wake word, full Android Auto, WhatsApp auto-send without UI, cloud NLU.
