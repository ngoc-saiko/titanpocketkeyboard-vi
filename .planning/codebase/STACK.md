# Technology Stack

**Analysis Date:** 2026-08-18

## Languages

**Primary:**
- Kotlin 1.9.0 - Primary language for Android application development
- Java - Some interop with Android framework classes

**Secondary:**
- XML - Resource files, layouts, configuration

## Runtime

**Environment:**
- Android Runtime (ART)
- Target SDK: 33
- Minimum SDK: 29
- Compile SDK: 34

**Build System:**
- Gradle 8.13
- Gradle Wrapper (gradle-wrapper.properties)

## Frameworks

**Core Android:**
- Android Framework (Input Method Service framework)
- AndroidX 1.x - Modern Android support library

**Testing:**
- JUnit 4.13.2 - Unit testing framework
- Espresso 3.5.1 - UI/instrumented testing framework
- Android Test Extension (androidx.test.ext) 1.1.5 - Testing utilities

**UI/Preferences:**
- Material Design 1.11.0 - Material Design components
- AndroidX Preference (androidx.preference-ktx) 1.2.1 - Preference management and UI
- AndroidX AppCompat 1.6.1 - Backward compatibility for Android UI

## Key Dependencies

**Critical:**
- androidx.core:core-ktx 1.9.0 - Kotlin extension functions for Android framework
- androidx.appcompat:appcompat 1.6.1 - Backward-compatible UI components and activities

**Audio & Sensors:**
- Android Audio Framework (android.media) - ToneGenerator for key sounds
- Android Vibration Framework (android.os.Vibrator) - Haptic feedback
- Android Speech Recognition (android.speech.SpeechRecognizer) - Voice input support

**Text Input:**
- Android Input Method Framework (android.inputmethodservice.InputMethodService) - IME base
- Android Text Input APIs (android.text.InputType, android.view.inputmethod.EditorInfo) - Text field handling

## Configuration

**Environment:**
- JVM Memory: -Xmx2048m (configured in gradle.properties)
- File Encoding: UTF-8
- Kotlin Code Style: official
- AndroidX enabled: true
- Non-transitive R class: true (resource isolation)

**Build:**
- `build.gradle.kts` - Top-level Gradle configuration
- `app/build.gradle.kts` - App module configuration
- `gradle.properties` - Project-wide Gradle settings
- `gradle/wrapper/gradle-wrapper.properties` - Gradle wrapper configuration

**IDE:**
- Android Studio configuration files in `.idea/`
- Project configured for Android Studio with Gradle plugin 8.13.2

## Platform Requirements

**Development:**
- Kotlin 1.9.0
- Gradle 8.13
- Android SDK for APIs 29-34
- Java 1.8 compiler/target compatibility

**Production:**
- Android device running API 29 (Android 10) or higher
- Target device: Unihertz Titan Pocket (physical keyboard)
- Permissions required:
  - `android.permission.VIBRATE` - Haptic feedback
  - `android.permission.RECORD_AUDIO` - Voice input
  - `android.permission.INTERNET` - Network access
  - `android.permission.FOREGROUND_SERVICE` - Long-running services
  - `android.permission.BIND_INPUT_METHOD` - IME registration (system permission)

---

*Stack analysis: 2026-08-18*
