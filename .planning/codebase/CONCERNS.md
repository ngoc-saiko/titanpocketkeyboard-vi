# Codebase Concerns

**Analysis Date:** 2026-08-18

## Tech Debt

**Character Classification Hardcoding in MultipressController:**
- Issue: Consonant filtering uses hardcoded keycode checks with special case handling for 'C' and 'S' keys (line 106 in `app/src/main/java/io/github/oin/titanpocketkeyboard/MultipressController.kt`)
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/MultipressController.kt` (line 105)
- Impact: Difficult to extend consonant filtering to other languages; fragile design that requires code modification for new rules
- Fix approach: Implement a character classification system or configuration-driven approach instead of hardcoded keycode comparisons; move consonant/vowel classification to a separate data structure or enum

**Invalid Sequence Validation is Overly Broad:**
- Issue: The `invalidSequences` list in `VietnameseTextInput.kt` (lines 152-169) contains many hardcoded invalid character combinations for Vietnamese, but this approach doesn't scale and is difficult to maintain
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt` (lines 152-169)
- Impact: Adding or modifying Vietnamese language rules requires code changes; no clear separation of phonetic rules from implementation
- Fix approach: Extract rules into a configuration file or create a Vietnamese phonetic validator class; consider rule-based validation instead of blacklist approach

**Magic Character Constants:**
- Issue: Special substitution characters use Unicode private use area codes (MPSUBST_* constants, e.g., '￿', '￴') with minimal documentation
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/MultipressController.kt` (lines 8-44)
- Impact: Code is difficult to understand without deep knowledge of the system; risk of accidental collision with other systems using same Unicode ranges
- Fix approach: Document why private use area is needed; consider moving to a proper enum or sealed class; add comprehensive JavaDoc

## Known Bugs

**Tone Mark Application Logic Bug:**
- Issue: The `reverseTone()` function in `VietnameseTextInput.kt` searches for the FIRST vowel in the buffer (line 248), but Vietnamese tone marks should typically be applied to specific positions based on syllable structure
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt` (lines 243-264)
- Trigger: Typing Vietnamese words with multiple vowels will apply tone marks to incorrect positions
- Impact: Incorrect output for compound vowel structures in Vietnamese
- Workaround: None at code level; users must retype

**Speech Recognizer Lifecycle Management:**
- Issue: `speechRecognizer` is destroyed and recreated with `initSpeechRecornizer()` (note: misspelled method name), but no null-safety checks exist in some code paths
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt` (lines 330-338, 576-602)
- Trigger: Rapid key presses triggering speech recognition in quick succession or concurrent speech/text input
- Impact: Potential NullPointerException if `speechRecognizer` is null when accessed
- Workaround: Add null checks before accessing `speechRecognizer`

**Delete Surrounding Text Calculation Error:**
- Issue: In `onKeyDown()` method (lines 458-461), the `deleteLength` calculation uses `minOf()` which may not delete the correct number of characters when Vietnamese Telex transformations result in multi-character replacements
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt` (lines 458-465)
- Trigger: Vietnamese Telex input with character modifications that produce longer output than input
- Impact: Incorrect text replacement; may leave remnants of old text
- Workaround: Manual deletion and retyping

## Security Considerations

**Hardcoded Permission Handling:**
- Risk: Microphone permission (RECORD_AUDIO) is requested in multiple locations without consistent error handling; users may be confused by multiple permission prompts
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt` (lines 317-328, 578-589), `app/src/main/java/io/github/oin/titanpocketkeyboard/SettingsActivity.kt` (lines 34-39)
- Current mitigation: Permission requests are present; toast messages inform user
- Recommendations: Consolidate permission requests to a single location; implement permission state caching to avoid redundant checks; add fallback UI for when permissions are denied

**Speech Recognition Data Privacy:**
- Risk: User speech is sent to Google Speech Recognition service (EXTRA_LANGUAGE set to "vi-VN" on line 206); no local privacy notice or data handling disclosure
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt` (lines 202-259)
- Current mitigation: Requires RECORD_AUDIO permission
- Recommendations: Add explicit privacy notice in app settings; document data flow to external speech service; consider adding option to disable speech recognition; ensure compliance with GDPR/local regulations

**User Shortcuts in SharedPreferences:**
- Risk: User-defined shortcuts stored in SharedPreferences in plain text; no encryption or access restrictions
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt` (lines 495-505)
- Current mitigation: None
- Recommendations: Consider encrypting sensitive user data in SharedPreferences using EncryptedSharedPreferences; validate shortcut input to prevent injection

## Performance Bottlenecks

**Substring Operations in Telex Processing:**
- Problem: `getTextBeforeCursor()` called multiple times per keystroke (lines 414, 459) with up to 50 characters read each time
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt` (lines 414-417)
- Cause: Inefficient repeated text retrieval for both shortcut detection and Vietnamese Telex processing
- Improvement path: Cache the retrieved text within a single key event handler cycle; refactor to read text once and reuse

**String Builder Operations in VietnameseTextInput:**
- Problem: Multiple `buffer.setCharAt()` and `buffer.replace()` operations on StringBuilder with subsequent string conversions
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt` (lines 203-210, 229-238)
- Cause: Inefficient string manipulations for tone and character modifier application
- Improvement path: Optimize buffer operations; reduce intermediate string conversions; batch replacements

**Map Lookups in Hot Path:**
- Problem: Nested map lookups in tone application logic (lines 302-304) executed on every character input
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt` (lines 302-305)
- Cause: No caching of character-to-diacritical mappings during typing
- Improvement path: Pre-compute or cache lookup results; consider using direct arrays for frequently accessed mappings

## Fragile Areas

**Telex Engine State Management:**
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt`, `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt`
- Why fragile: Multiple state variables (`buffer`, `toneAdded`, `isReverseTone`, `charModified`, `lastToneMark`) are not always synchronized; complex interactions between character modification and tone mark application
- Safe modification: Add comprehensive unit tests before modifying tone or character logic; use state machine pattern to ensure consistent state transitions; add invariant checks
- Test coverage: No unit tests exist for `VietnameseTextInput` class

**Multipress Controller State:**
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/MultipressController.kt` (lines 67-71)
- Why fragile: State variables (`last`, `lastTime`, `count`, `longPressCount`, `lastSubstitution`) are tightly coupled with timing logic; reset() called from multiple locations but not consistently
- Safe modification: Extract timing logic to separate utility class; add defensive reset calls; ensure reset is called from all entry points
- Test coverage: No unit tests for edge cases like rapid key presses or timeout scenarios

**Speech Recognizer Lifecycle:**
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt` (lines 185-189, 202-259)
- Why fragile: `speechRecognizer` is nullable and recreated in multiple places; `isListening` and `restartSpeechRecognizer` flags can get out of sync
- Safe modification: Use a lifecycle wrapper class for speech recognizer; ensure all state changes are atomic; add state validation before operations
- Test coverage: No integration tests for speech recognition flow

## Scaling Limits

**Memory Usage with Large Shortcut Maps:**
- Current capacity: Shortcuts loaded entirely into memory from SharedPreferences
- Limit: No documented limit; potential OOM if user defines thousands of shortcuts
- Scaling path: Implement lazy loading or paged access; use database instead of SharedPreferences for large datasets; add size validation and warnings

**Input Buffer Size:**
- Current capacity: `StringBuilder` in `VietnameseTextInput` grows without bounds
- Limit: Memory constraints of Android device
- Scaling path: Implement maximum buffer size; add buffer overflow handling

## Dependencies at Risk

**Deprecated targetSdk Version:**
- Risk: `targetSdk = 33` is not current (Android 14/API 34 available); potential compatibility issues with newer Android versions
- Files: `app/build.gradle.kts` (line 13)
- Impact: App may not be installable on future Android versions; missing latest security and privacy features
- Migration plan: Update to `targetSdk = 34` (Android 14); test thoroughly on latest devices; ensure permission handling complies with latest Android requirements

**Outdated Dependency Versions:**
- Risk: androidx dependencies may have security patches available
- Files: `app/build.gradle.kts` (lines 41-47)
- Current: androidx.core:core-ktx:1.9.0, androidx.appcompat:appcompat:1.6.1
- Migration plan: Update to latest stable versions; run dependency vulnerability scan; test for breaking changes

**Kotlin Version:**
- Risk: kotlin version 1.9.0 is not the latest stable
- Files: `build.gradle.kts` (line 4)
- Impact: Missing language features and compiler optimizations
- Migration plan: Update to latest 1.10.x or 2.0.x; verify compatibility with Android Gradle Plugin

## Missing Critical Features

**No Error Recovery in Telex Processing:**
- Problem: If Telex processing fails or produces unexpected output, no rollback mechanism exists
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt` (lines 435-469)
- Blocks: Users cannot undo/recover from malformed Vietnamese text without manual deletion

**No Input Method Suggestions:**
- Problem: Auto-capitalization exists but no suggestion/autocorrection system
- Impact: Limited usability for longer text entry; users must type everything manually

**No Fallback for Speech Recognition Failures:**
- Problem: If speech recognition fails, no text is produced; no error message shown in all cases
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt` (line 235)
- Impact: Silent failures when speech recognition encounters errors

## Test Coverage Gaps

**No Unit Tests for Core Logic:**
- What's not tested: `VietnameseTextInput.processKey()`, `MultipressController.process()`, tone mark application
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt`, `app/src/main/java/io/github/oin/titanpocketkeyboard/MultipressController.kt`
- Risk: Regressions in Vietnamese text input or multipress logic will go undetected; character composition issues impossible to validate without manual testing
- Priority: High - Core IME functionality is completely untested

**No Integration Tests for Modifier System:**
- What's not tested: Shift/Alt/Sym modifier state transitions, modifier locking, auto-capitalization interactions
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/Modifier.kt`, `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt` (modifier handling)
- Risk: Modifier state management bugs will only surface during manual testing
- Priority: High

**No Tests for Speech Recognition:**
- What's not tested: Permission flows, speech recognizer lifecycle, error handling
- Files: `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt` (lines 202-259)
- Risk: Speech features untested; cannot verify correct behavior on different Android versions
- Priority: Medium

---

*Concerns audit: 2026-08-18*
