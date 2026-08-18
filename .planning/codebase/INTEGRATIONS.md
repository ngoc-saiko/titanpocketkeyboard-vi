# External Integrations

**Analysis Date:** 2026-08-18

## APIs & External Services

**Voice Input:**
- Android Speech Recognition (SpeechRecognizer) - Voice-to-text recognition
  - SDK/Client: android.speech.SpeechRecognizer
  - Context: Integrated in `InputMethodService.kt` for voice input support
  - Permission: `android.permission.RECORD_AUDIO`

**Search Integration:**
- Google Quick Search Box (com.google.android.googlequicksearchbox)
  - Query intent: Package-level query permission configured in `AndroidManifest.xml`
  - Purpose: System search integration

## Data Storage

**Databases:**
- Android SharedPreferences
  - Client: androidx.preference.PreferenceManager
  - Location: Default system preferences storage
  - Usage: Stores user keyboard settings (multipress threshold, modifier thresholds, accent templates, etc.)
  - Configuration file: `app/src/main/res/xml/preferences.xml`

**User Dictionary:**
- Android UserDictionary
  - Provider: android.provider.UserDictionary
  - Usage: Access to system user dictionary for text input
  - Integration in: `InputMethodService.kt`

**File Storage:**
- Local filesystem only - No cloud/remote storage
- App-specific storage for configuration and state

**Caching:**
- None detected - Uses direct system APIs

## Authentication & Identity

**Auth Provider:**
- Not applicable - Application does not use authentication
- No user accounts required
- Operates as local Input Method Engine (IME) only

## Monitoring & Observability

**Error Tracking:**
- None detected - No error tracking service integrated

**Logs:**
- Android Logcat logging via `android.util.Log`
- Used in `VietnameseTextInput.kt` and `InputMethodService.kt` for debug output
- No centralized logging service

## CI/CD & Deployment

**Hosting:**
- GitHub Releases - APK distribution (referenced in README.md)
- Repository: https://github.com/oin/TitanPocketKeyboard (Vietnamese fork)

**CI Pipeline:**
- Not detected - No CI/CD configuration files found
- Manual build and release process likely

**Build Output:**
- APK generation via Gradle build system
- Release signing configuration referenced in `app/build.gradle.kts` (debug signing config)

## Environment Configuration

**Required env vars:**
- None detected - Application does not use environment variables

**Secrets location:**
- None - Application stores no secrets
- All configuration is public (no API keys, credentials)

## Permissions & System Integration

**Android Manifest Permissions:**
- `android.permission.VIBRATE` - Haptic feedback for key presses
- `android.permission.RECORD_AUDIO` - Voice input/speech recognition
- `android.permission.INTERNET` - Network access
- `android.permission.FOREGROUND_SERVICE` - Long-running service operation
- `android.permission.BIND_INPUT_METHOD` - IME registration (system permission, not granted by user)

**System Integration:**
- Input Method Engine (IME) framework via `android.inputmethodservice.InputMethodService`
- Registered as system input method service in `AndroidManifest.xml`
- Queries Google Quick Search Box package for integration

## Webhooks & Callbacks

**Incoming:**
- None detected

**Outgoing:**
- None detected

## Content Providers & Data Access

**Android Content Providers:**
- android.provider.Settings - Access to system settings
- android.provider.UserDictionary - Access to system user dictionary words
- Both accessed read-only in `InputMethodService.kt`

---

*Integration audit: 2026-08-18*
