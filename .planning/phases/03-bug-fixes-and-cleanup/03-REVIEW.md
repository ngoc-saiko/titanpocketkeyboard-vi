---
phase: 03-bug-fixes-and-cleanup
reviewed: 2026-08-18T00:00:00Z
depth: standard
files_reviewed: 3
files_reviewed_list:
  - app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt
  - app/src/main/java/io/github/oin/titanpocketkeyboard/MultipressController.kt
  - app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt
findings:
  critical: 3
  warning: 5
  info: 5
  total: 13
status: issues_found
---

# Phase 03: Code Review Report

**Reviewed:** 2026-08-18T00:00:00Z
**Depth:** standard
**Files Reviewed:** 3
**Status:** issues_found

## Summary

Three files were reviewed: the main IME service, the multipress controller, and the Vietnamese Telex input engine. The code compiles and the unit test suite passes (per prior phase verification), but several correctness bugs and one security-adjacent resource leak were found. The most serious finding is a dead code path that causes KEYCODE_DEL to be caught and handled at line 377 (clearing `toneAdded` and delegating to `deleteSurroundingText`) before execution can ever reach the Vietnamese-mode backspace handler at line 415, meaning the Telex buffer diverges from what is on screen and corruption accumulates silently. A ToneGenerator is also created on every mode-toggle without being released, leaking an audio HAL handle each time. Several quality issues were also found, including two unreachable private methods, a duplicate entry in the invalid-sequence blacklist, and a deprecated `toLowerCase()` call.

---

## Critical Issues

### CR-01: KEYCODE_DEL backspace path inside Vietnamese mode is dead code — buffer and screen diverge

**File:** `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt:377-385` and `415-424`

**Issue:** `onKeyDown` intercepts `KEYCODE_DEL` at line 377 unconditionally — before the `isLongPress` guard (line 388) and long before the `vietnameseMode` branch (line 414). The handler at lines 377-385 clears `toneAdded`, calls `finishComposingText()`, and deletes one character via `deleteSurroundingText`, then returns `true`. Execution never reaches the Vietnamese-mode backspace handler at lines 415-424, which is the only place where `vietnameseTelex.processKey('\b')` is called to keep the internal Telex buffer in sync with what is on screen.

Result: in Vietnamese mode, every backspace deletes a character from the screen but does not remove the corresponding character from the `buffer` in `VietnameseTextInput`. The buffer grows stale. On the next keypress, `setBuffer(lastWord)` at line 410 re-synchronises from `getTextBeforeCursor`, which partially papers over the issue for simple cases, but for composed characters (multi-char replacements written by `sendCharacter(replacement, true)`) the text before cursor may not match the buffer state assumed by `processKey`, causing incorrect or no Telex transformations.

**Fix:** Move the `KEYCODE_DEL` guard below the `vietnameseMode` block, or restructure so the Vietnamese-mode backspace is tried first:

```kotlin
// onKeyDown: handle DEL early only when NOT in Vietnamese mode
if ((event.keyCode == KeyEvent.KEYCODE_DEL || event.keyCode == KeyEvent.KEYCODE_FORWARD_DEL)
    && !vietnameseMode) {
    multipress.reset()
    consumeModifierNext()
    vietnameseTelex.toneAdded = false
    currentInputConnection?.finishComposingText()
    return currentInputConnection?.deleteSurroundingText(1, 0) ?: false
}
// ... rest of the method ...
// Inside the vietnameseMode block, DEL is now reachable:
if (event.keyCode == KeyEvent.KEYCODE_DEL) {
    val replacement = vietnameseTelex.processKey('\b')
    if (replacement != null) {
        currentInputConnection?.deleteSurroundingText(1, 0)
        sendCharacter(replacement, true)
    } else {
        vietnameseTelex.reset()
    }
    return true
}
// After the vietnameseMode block, handle DEL for the non-Vietnamese path:
if (event.keyCode == KeyEvent.KEYCODE_DEL || event.keyCode == KeyEvent.KEYCODE_FORWARD_DEL) {
    multipress.reset()
    consumeModifierNext()
    vietnameseTelex.toneAdded = false
    currentInputConnection?.finishComposingText()
    return currentInputConnection?.deleteSurroundingText(1, 0) ?: false
}
```

---

### CR-02: ToneGenerator leaks audio HAL handle on every mode toggle

**File:** `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt:683-685`

**Issue:** `playNotificationSound()` creates a new `ToneGenerator` object on every call but never calls `toneGen.release()`. `ToneGenerator` holds a native audio HAL handle (`AudioTrack` backed resource). Each unreleased instance is a resource leak. The method is called on every invocation of `toggleInputMode()` (line 310), which fires on every Shift+Space press. On a device with a session lasting many hours and frequent mode switches, this will exhaust audio resources, potentially causing subsequent `ToneGenerator` constructions to fail silently or throw.

**Fix:**

```kotlin
private fun playNotificationSound() {
    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
    try {
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
        // Block long enough for the 200ms tone to complete before releasing.
        // Alternatively, store toneGen as a member and release in onDestroy().
    } finally {
        toneGen.release()
    }
}
```

A better long-term fix is to make `toneGen` a class member, initialise it once in `onCreate`, and `release()` it in `onDestroy`.

---

### CR-03: `onRmsChanged` fires on every audio frame and logs unconditionally — can saturate Logcat and slow the main thread

**File:** `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt:218-220`

**Issue:** `onRmsChanged` is called approximately 10 times per second by Android's speech recognition engine while listening. The current implementation logs every call unconditionally via `Log.d`. On a device running API 29 the Android logger is synchronous on the calling thread. During a 30-second dictation session this generates ~300 log lines. In production builds where logging is not stripped this adds measurable overhead and floods Logcat, making it useless for debugging real events. This is classified Critical because the callback runs on the main (UI) thread and the volume of logging under real use degrades latency of subsequent key event handling in the same thread.

**Fix:** Remove the log statement from `onRmsChanged` entirely, or gate it behind a debug flag:

```kotlin
override fun onRmsChanged(rmsdB: Float) {
    // High-frequency callback — do not log in production
}
```

---

## Warnings

### WR-01: `findFirstVowelIndex` returns -1 for the nucleus vowel when diphthong target vowel is itself absent from buffer

**File:** `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt:396-398`

**Issue:** The diphthong special-case loop (lines 396-398) finds a matching diphthong key in the buffer, then calls `buffer.indexOf(toneMappingEnd[key].toString())` to locate the target vowel to mark. If the target vowel character is not present in the buffer as a plain character (for example because it was already composed to a toned form, or the buffer stores a different case), `buffer.indexOf` returns -1, and the function returns -1 to the caller. The caller (`applyToneMark` line 318) then skips the "add tone" branch (`lastVowelIndex != -1 && !toneAdded`) entirely and falls into the `reverseTone` branch, producing incorrect output for diphthongs.

Concrete example: buffer is "ướ" (already toned ơ), then pattern "ươ" matches but `buffer.indexOf("ơ")` is -1 because the buffer has "ớ" not "ơ". The tone mark is reversed instead of being applied correctly.

**Fix:** After `buffer.indexOf(key)` confirms the diphthong is present, search for the target vowel in both its plain and toned forms (i.e. search for any character whose base in `removeToneMap` equals the target vowel), or use a dedicated "find vowel at position" helper that strips tones during comparison:

```kotlin
for (key in toneMappingEnd.keys) {
    val index = buffer.indexOf(key)
    if (index != -1) {
        val targetBase = toneMappingEnd[key]!!
        // find the position of targetBase or any toned variant of it
        val pos = buffer.indices.firstOrNull { i ->
            buffer[i] == targetBase || removeToneMap[buffer[i]] == targetBase
        }
        if (pos != null) return pos
    }
}
```

---

### WR-02: `applyToneMark` can silently append the original Telex character to the buffer when the vowel index search fails

**File:** `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt:326-331`

**Issue:** When `lastVowelIndex == -1` (no vowel found) and `toneAdded` is false (no previous tone), `applyToneMark` falls through to `reverseTone(originalChar)` at line 326. If `reverseTone` also returns false (no toned vowel found to reverse), execution falls to line 330: `buffer.append(originalChar)`. The originalChar here is the **Telex modifier key** character itself (e.g. `'s'`, `'f'`, `'r'`, `'x'`, or `'j'`) — not a Vietnamese character. The method then returns `null`. The caller in `InputMethodService.onKeyDown` at line 438 treats a `null` return as "no transformation" and falls through to `super.onKeyDown`, which commits the raw modifier key character to the field.

Result: the buffer now contains the Telex modifier key character appended silently (`buffer.append(originalChar)` at line 330), but the screen shows whatever `super.onKeyDown` committed. Buffer and screen are now out of sync. On the next character the buffer-based logic will produce incorrect replacements.

**Fix:** Do not silently append to the buffer when there is nothing to transform. Return `null` without modifying the buffer, and let the caller commit the raw character:

```kotlin
private fun applyToneMark(toneMark: Char, originalChar: Char): String? {
    // ... existing code up to reverseTone(originalChar, false) ...
    if (reverseTone(originalChar)) {
        return buffer.toString()
    }
    // Do NOT append to buffer — buffer state is already correct; caller handles pass-through
    return null
}
```

---

### WR-03: `setBuffer` called with the `lastWord` derived from `getTextBeforeCursor` on every non-modifier keydown, including Enter/punctuation

**File:** `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt:409-410`

**Issue:** Line 410 unconditionally calls `vietnameseTelex.setBuffer(lastWord)` before the `vietnameseMode` check at line 414. This means every keydown event in non-Vietnamese mode also resets and reloads the Telex buffer from screen text. While functionally harmless in non-Vietnamese mode (the buffer is never used), it introduces an implicit ordering assumption: the buffer is valid only immediately after line 410, and any path that reaches `processKey` without passing through `setBuffer` on the same keydown cycle sees a stale buffer.

More critically, when `vietnameseMode` is false and the user types punctuation (which is not a printing key, so it falls through to `super.onKeyDown`), `setBuffer` still runs and may overwrite a valid buffer that was being maintained from a prior Vietnamese-mode session fragment if the user toggled modes mid-word.

**Fix:** Move `setBuffer(lastWord)` inside the `if (vietnameseMode ...)` block so it only runs when Telex processing is actually going to happen:

```kotlin
if (vietnameseMode && !event.isCtrlPressed && !sym.get() && telexOn
        && event.keyCode != KeyEvent.KEYCODE_ENTER) {
    vietnameseTelex.setBuffer(lastWord) // moved here
    if (event.keyCode == KeyEvent.KEYCODE_DEL) { ... }
    ...
}
```

---

### WR-04: `initSpeechRecornizer()` is public and can be called externally, bypassing null-safety and restart guards

**File:** `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt:201`

**Issue:** `initSpeechRecornizer` is declared without an access modifier, making it `public` by default in Kotlin. The method unconditionally creates a new `SpeechRecognizer` without first checking whether `speechRecognizer` is already non-null. Calling it twice (e.g. from a test harness, future code, or accidentally) leaks the first recognizer because `destroy()` is never called on it before the reference is overwritten. The name contains a spelling error (`Recornizer`) that makes it harder to grep and locate.

**Fix:**
```kotlin
private fun initSpeechRecognizer() { // note: fix spelling, make private
    speechRecognizer?.destroy() // guard against double-init
    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    // ... rest of the method
}
```

---

### WR-05: `MultipressController.count` initialised to `1` but reset sets it to `0`; first multipress press uses count=0 index

**File:** `app/src/main/java/io/github/oin/titanpocketkeyboard/MultipressController.kt:196` and `206`

**Issue:** `count` is declared with initial value `1` (line 196), but `reset()` sets it to `0` (line 206). When a key is first seen (`else` branch of `last == keyCode`, lines 290-294), `count` is set to `0`. On the second press of the same key within the threshold (entering the `if` branch at line 218), the index chosen is `count` (line 241), which is `0`. After indexing, `count` is incremented to `1`. On the third press, index `1` is used, and so on.

The initial value of `1` on declaration is irrelevant because both `reset()` and the `else` branch overwrite `count` to `0` before the `count` field is used for indexing. This creates a latent confusion: a reader expects `count` to start at `1` (declaration), but all actual execution paths start it at `0`. If `reset()` is made to set `count = 1` to match the declaration (a reasonable change), the index on the first multipress would jump to `1`, skipping the first substitution entry.

**Fix:** Change the declaration to match the operational initial value:

```kotlin
private var count: Int = 0  // was 1; reset() and else-branch both set to 0
```

---

## Info

### IN-01: `getExistingTone` and `getBaseVowel` are private methods that are never called — dead code

**File:** `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt:352-368`

**Issue:** Both `getExistingTone` (line 352) and `getBaseVowel` (line 361) are `private` and have no callers anywhere in the file. They are dead code. Their presence suggests either a refactoring left them stranded, or they were written speculatively. Either way, they should be removed to reduce maintenance surface.

**Fix:** Delete both methods. If needed in future they can be recovered from version history.

---

### IN-02: Duplicate `"uv"` entry in `invalidSequences` list

**File:** `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt:216`

**Issue:** The string `"uv"` appears twice on line 216: `"av", "ev", "uv", "iv", "uv"`. The duplicate is harmless (the `any { }` short-circuits on first match) but indicates a copy-paste error and clutters the list.

**Fix:** Remove the second `"uv"`:
```kotlin
"av", "ev", "uv", "iv",
```

---

### IN-03: `toLowerCase()` is deprecated since Kotlin 1.5 — use `lowercaseChar()` instead

**File:** `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt:405` and `408`

**Issue:** `buffer[0].toLowerCase()` and `buffer[1].toLowerCase()` use the deprecated `Char.toLowerCase()` extension. The project targets Kotlin 1.9.0 where the replacement is `Char.lowercaseChar()`. Using deprecated APIs will produce compiler warnings that eventually become errors in future Kotlin versions.

**Fix:**
```kotlin
buffer[0].lowercaseChar() == 'g' && buffer[1].lowercaseChar() == 'i'
// and
buffer[0].lowercaseChar() == 'q' && buffer[1].lowercaseChar() == 'u'
```

---

### IN-04: `getUserDictionaryShortcuts` is a public method that queries `UserDictionary.Words` but is commented out and never called

**File:** `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt:400` and `496-514`

**Issue:** `getUserDictionaryShortcuts` is defined as a `public` (no modifier = public in Kotlin) method but is only referenced in a commented-out line (line 400). The live code uses `getStoredShortcuts()` instead. The unused method performs a `ContentResolver.query` against `UserDictionary.Words.CONTENT_URI`, which requires no special permission at API 29 but adds unnecessary surface. Retaining commented-out code alongside an active replacement creates confusion about which code path is authoritative.

**Fix:** Either remove `getUserDictionaryShortcuts` and the comment at line 400, or replace `getStoredShortcuts` with it and remove the dead method — but do not keep both.

---

### IN-05: `onRmsChanged` high-frequency callback with no rate limiting aside from log removal (see CR-03)

**File:** `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt:218`

**Issue:** The `onRmsChanged` override contains only a `Log.d` call (addressed in CR-03). After the log is removed, the override becomes an empty method that overrides an interface contract it does not use. The empty override is not harmful but should be retained to document that the callback was consciously considered. A brief comment would suffice.

**Fix:** After removing the log, add a comment:
```kotlin
override fun onRmsChanged(rmsdB: Float) {
    // intentionally ignored — high-frequency callback, no UI to update
}
```

---

_Reviewed: 2026-08-18T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
