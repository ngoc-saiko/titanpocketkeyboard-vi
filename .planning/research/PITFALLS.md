# Domain Pitfalls

**Domain:** Android IME — Vietnamese Telex input, modifier key state machines, unit testing of pure logic
**Researched:** 2026-08-18
**Source:** Direct codebase analysis of VietnameseTextInput.kt, Modifier.kt, MultipressController.kt, InputMethodService.kt
**Confidence:** HIGH (all findings are grounded in actual code paths)

---

## Critical Pitfalls

### Pitfall 1: Tone mark placement targets the first vowel, not the phonologically correct position

**What goes wrong:** `findFirstVowelIndex()` (VietnameseTextInput.kt:326) does two things: it special-cases a short list of known multi-vowel patterns (`toneMappingEnd`), then falls through to "find the first vowel in the buffer". For any vowel cluster not in `toneMappingEnd` (e.g. `uoi`, `oai` sub-sequences not yet listed, compound vowels with diacritics already applied) the tone mark lands on the leftmost vowel, which is phonologically wrong for Vietnamese. The recent bugs `quây` and `oà` are direct consequences — partial fixes were added inline (the `gi`/`qu` prefix skips at lines 336–340) rather than implementing the full ONGLIDE/NUCLEUS/CODA syllable model.

**Why it happens:** The algorithm grew by accretion: each new wrong output triggered a new special case in `toneMappingEnd` or an `if i < 2` guard, but the underlying model is "scan left to right, stop at first vowel" instead of "identify the syllable nucleus."

**Consequences:** Silent wrong output. The user sees `quây` instead of `quay` with tone, or a tone mark on `u` in `ươ` compounds. These bugs are invisible without exhaustive manual testing of all Telex sequences.

**Prevention:**
- Model the Vietnamese syllable as INITIAL-CONSONANT + MEDIAL-GLIDE + NUCLEUS-VOWEL + FINAL-CONSONANT. Tone goes on the nucleus, not the glide.
- The nucleus identification rule for Telex: in a vowel cluster, the nucleus is the rightmost vowel that can carry a diacritic, except when a final consonant follows a long vowel cluster, in which case the penultimate position gets the tone.
- Extend `toneMappingEnd` to a complete, exhaustive lookup rather than patching the scan loop with guards.
- Write a unit test for every vowel cluster listed in the Vietnamese phonology before touching the algorithm.

**Detection:** A test matrix covering all common double- and triple-vowel nuclei (`ươ`, `iê`, `uô`, `uyê`, `oai`, `oă`, `oa`, `uâ`, `uoi`, `ieu`, `uu`, `ii`) against all five tone marks will immediately surface wrong placements.

**Phase mapping:** Fix tone placement before writing unit tests. Tests written against the broken algorithm will codify the wrong behaviour and resist the fix.

---

### Pitfall 2: The w-revert path is incomplete — `ơ + w` produces `ơw` instead of `ow`

**What goes wrong:** `processKey('w')` calls `applyWCharModifiers()` which maps `"o" → "ơ"` but has no reverse rule for `"ơ" → "o"` when `w` is pressed again. The `reverseCharModifier` map (lines 100–115) correctly lists `"ơw" → "ow"` but `applyCharModifiers()` is only called after `buffer.append(char)`, and the `applyWCharModifiers()` path at line 190 returns early if it produced a change — meaning the 'w' is never appended to the buffer for the reverse check to fire.

**Why it happens:** The forward path (`o` + `w` → `ơ`) and the reverse path (`ơ` + `w` → `ow`) live in two different code branches. The forward path returns before the reverse path is evaluated. The `charModified` flag is also not set for `w`-modifier paths, so the `charModified` guard in `applyCharModifiers` does not protect the revert.

**Consequences:** Typing `o`, `w`, `w` produces `ơw` committed to the editor. The user must manually delete and retype. This is the stated active bug in PROJECT.md.

**Prevention:**
- In `applyWCharModifiers()`, check if the last character of the current buffer is already a `w`-derived vowel (`ơ`, `ă`, `ư`) and if so return the reverse string instead of the forward transform.
- Alternatively, unify the forward and reverse logic under `applyCharModifiers` by treating `w` consistently: always append it, then check `reverseCharModifier` first (as `charModified` does for `a`/`e`/`o`/`u`) before checking `charModifiers`.
- The unit test: assert `processKey('o'); processKey('w'); processKey('w')` produces `"ow"` (or emits `"ow"` as the replacement string), not `"ơw"`.

**Detection:** Any test that types a `w`-modified vowel and then types `w` again will catch this immediately.

**Phase mapping:** Fix alongside the tone placement bug. Both touch `applyToneMark`/`processKey` and interact — fixing revert logic first avoids a test matrix explosion where tone-on-`ơ` sequences are tested against a broken revert.

---

### Pitfall 3: Alt one-shot modifier does not reset after first non-modifier key in Vietnamese mode

**What goes wrong:** In `onKeyDown`, when `vietnameseMode` is true and a printing key is pressed, the code calls `consumeModifierNext()` after the Telex branch returns (line 467). However, when the Telex branch falls through to `super.onKeyDown()` (line 473 — the "no transformation" path), `consumeModifierNext()` is still called on line 467 before the fallthrough, but only if `replacement != null && replacement != unicodeChar.toString()`. If Telex returns `null` or returns the same char, `consumeModifierNext()` is skipped, so `alt.next` is never cleared.

More concretely: the `alt` Modifier's `next` mode activates on quick-tap + release (`onKeyUp` sets `next = true`). `nextDidConsume()` must be called after every printing key to clear it. In Vietnamese mode the `consumeModifierNext()` call is conditional — it only fires on a successful Telex transform. On a key that Telex passes through unchanged, `alt.next` is never cleared, leaving Alt active for the next keypress.

**Why it happens:** The Vietnamese-mode branch has its own early-return paths that bypass the `consumeModifierNext()` call that the non-Vietnamese path (line 484) always executes.

**Consequences:** After pressing Alt + a key that Telex does not transform, Alt remains active for the subsequent key press. Manifests as a character being sent in Alt mode when the user expects the plain character. The bug is input-field-specific because some apps consume Alt silently.

**Prevention:**
- `consumeModifierNext()` must be called on every path that commits a character, not only on successful Telex transforms. Move it outside the `if (replacement != null && replacement != unicodeChar.toString())` block and call it unconditionally at the end of the Vietnamese-mode branch.
- Unit-testable proxy: extract a `ModifierState` class that is injected into `VietnameseTextInput`. Then test that after a non-transforming key the modifier's `next` flag is false.

**Detection:** Write a Modifier state-machine test: press Alt (onKeyDown + onKeyUp), then press a key that Telex does not transform (e.g. `'z'`), assert `alt.get() == false`. Without the fix this assertion fails.

**Phase mapping:** Fix this before writing modifier state-machine tests. The test will fail on the broken state until the fix is applied; testing first gives a false "everything works" result if the test is written incorrectly against the current broken behaviour.

---

## Moderate Pitfalls

### Pitfall 4: `invalidSequences` blacklist blocks legitimate Telex input at buffer level

**What goes wrong:** `processKey` checks `invalidSequences.any { bufferStr.contains(it) }` (line 179). Several entries are single English consonants or digraphs that do appear in real Vietnamese words mid-sequence. For example, `"nd"` is listed as invalid, but `"ngh"` starts with `n` which has no issue — yet if the buffer ever contains `nd` as a substring (which can happen with `đ` revert sequences), Telex is silently disabled for that buffer. Entries like `"ou"` block the sequence needed to type `"oup"` style transliterations. Any single character listed (`"f"`, `"w"`, `"z"`, `"j"`) causes the check to fire after those chars appear in the buffer, which can happen when Telex reverts a modified character and the unmodified form temporarily contains those chars.

**Consequences:** Silent pass-through of the character without Telex processing when the user expects composition. No error, no feedback.

**Prevention:**
- Replace the substring-contains check with a prefix check (`bufferStr.startsWith(it)` or check only the beginning of the buffer).
- Better: validate against the buffer at commit time, not during in-progress composition — invalid sequences only matter for the final committed word, not for intermediate states.
- Write tests: type `n`, `d` and assert Telex is still active (no pass-through of the next vowel).

**Detection:** Test sequences that pass through a temporarily-invalid substring during composition and verify Telex still fires on the next character.

**Phase mapping:** Address during the unit-test writing phase. Discovering this via tests (rather than fixing blindly) avoids breaking sequences that currently rely on the existing blacklist behaviour.

---

### Pitfall 5: Buffer state is not reset on cursor movement or external selection change

**What goes wrong:** `onUpdateSelection` (InputMethodService.kt:275) only resets `vietnameseTelex` if the character immediately before the cursor is whitespace or empty. If the user taps mid-word to reposition the cursor, or if an autocomplete replaces text, the buffer still contains the old word. The next keypress runs Telex on a stale buffer, producing replacement strings computed against the wrong prefix.

**Consequences:** Typing into a repositioned cursor produces garbled output — the replacement string is calculated from the old word's buffer, then deleted characters from the wrong position.

**Prevention:**
- Reset the buffer whenever `newSelStart != oldSelStart || newSelEnd != oldSelEnd` unless the change is the direct result of a character committed by the IME (use a "committed" flag or cursor-tracking variable).
- Alternatively, always re-derive the buffer from `getTextBeforeCursor` at the start of each `onKeyDown` rather than carrying state across events. The code already does this partially at line 431 (`vietnameseTelex.setBuffer(lastWord)`), so the risk is manageable but the reset-in-`onUpdateSelection` is still fragile.

**Detection:** Manually position cursor mid-word and type a Telex vowel; or write a unit test that calls `setBuffer` with a stale value and verifies the output is derived from the new buffer, not an accumulated one.

**Phase mapping:** Fix during or immediately after the modifier/revert fix phase. Stale buffer state will produce confusing test failures if tests are run in sequence without explicit resets.

---

### Pitfall 6: `deleteLength` calculation uses `minOf` against post-transform length, not pre-transform length

**What goes wrong:** In `onKeyDown` (line 461):
```kotlin
val deleteLength = minOf(replacementLength, textBeforeCursor.length - lastSpaceIndex - 1)
```
`replacementLength` is the length of the Telex replacement string (which may be longer than the characters actually committed to the editor if a previous replacement already absorbed some characters). If a tone mark is applied, the replacement is the full buffer (e.g. 3 characters), but only 2 characters were committed previously (because the third was already in the buffer from a prior replacement). This causes one character to be left behind in the editor.

**Consequences:** Orphaned characters left in the editor after Telex replacements on words with multiple composition steps. The existing bugs with `quây` and `oà` are partially explained by this.

**Prevention:**
- Track how many characters the IME has actually committed to the editor for the current word, not just how long the replacement string is. A `committedLength` counter on `VietnameseTextInput` would make this testable.
- Or: always delete to the last space (delete `textBeforeCursor.length - lastSpaceIndex - 1` characters unconditionally), then commit the full replacement. This is simpler and correct because `setBuffer(lastWord)` already reads the full word.

**Detection:** Test: type `q`, `u`, `a`, `y` then apply tone mark `f`. Assert the editor contains exactly `quày` with no orphaned characters.

**Phase mapping:** Fix alongside the tone placement bug. Both affect what gets committed and deleted.

---

### Pitfall 7: `Modifier.onKeyDown` double-press lock check uses wall-clock time but `lastTime` can be 0

**What goes wrong:** `Modifier.onKeyDown()` (Modifier.kt:44):
```kotlin
if(t - lastTime < lockThreshold) {
    lock = !lock
    preventNext = true
}
```
On first press, `lastTime = 0`. `System.currentTimeMillis()` is in the billions of milliseconds since epoch. `t - 0` is always far larger than `lockThreshold` (250 ms), so the first press never locks. This is intentional and correct. However, after `reset()` is called (which sets `lastTime = 0`), if two rapid keypresses occur within `lockThreshold` ms of each other but the modifier was reset in between (e.g. after `onStartInput`), the first press after reset will correctly not lock, but the second may fire the lock branch unexpectedly depending on when `reset()` ran relative to `lastTime`.

The more concrete risk: `preventNext` is set in `onKeyDown` but `onKeyUp` uses it without a null guard (`next = !lock && t - lastTime < nextThreshold && !preventNext`). If `preventNext = true` is set from the double-press branch but `onKeyUp` is never called (e.g. the key is held), `preventNext` remains true and the next `onKeyUp` will incorrectly suppress `next`.

**Consequences:** Modifier one-shot mode (tap to activate for next key) silently fails after double-press sequences that are interrupted mid-way.

**Prevention:**
- Unit-test the full Modifier state machine for all sequences: single tap, double tap, hold, hold-then-tap, double-tap-interrupted-by-hold.
- The `Modifier` class is pure Kotlin with no Android dependencies — it is the ideal first unit-test target.

**Detection:** A test that calls `onKeyDown()`, `onKeyDown()` (within lockThreshold), then `onKeyUp()`, and asserts `get() == true` (locked) will surface any timing-related edge cases without needing a device.

**Phase mapping:** Write Modifier state-machine tests first — before touching the modifier fix for Vietnamese mode. The tests will document the intended contract and make the Alt one-shot fix (Pitfall 3) much safer.

---

## Minor Pitfalls

### Pitfall 8: `VietnameseTextInput` is in the default package — JUnit tests require special import

**What goes wrong:** `VietnameseTextInput.kt` has no `package` declaration (line 1 of the file). It is in the default package. Kotlin/Java unit tests in `src/test/java/...` with a declared package cannot import classes from the default package directly. `import VietnameseTextInput` works from within `InputMethodService.kt` (also partially in the unnamed package via `import VietnameseTextInput`) but will fail in a test class that declares its own package.

**Consequences:** The first unit test written for `VietnameseTextInput` will fail to compile unless the test class is also placed in the default package (which is bad practice and breaks test organisation) or the class is moved into the proper package.

**Prevention:**
- Add `package io.github.oin.titanpocketkeyboard` to `VietnameseTextInput.kt` before writing any tests. This is the single highest-leverage change to enable testing.
- Verify that `InputMethodService.kt`'s `import VietnameseTextInput` is updated to the full qualified form after the package is added.

**Detection:** The first `./gradlew test` run after adding a test class with a package declaration will fail with "unresolved reference: VietnameseTextInput" if the package is missing.

**Phase mapping:** This must be done as the very first step of the unit-test phase, before any test code is written.

---

### Pitfall 9: `wCharModifiers` applies to every character in the buffer — not just the last one

**What goes wrong:** `applyWCharModifiers()` (line 203) uses `buffer.map { char -> wCharModifiers[char.toString()] ?: char }`. This maps every character in the buffer through the `w` modifier table, not just the last vowel. For a buffer like `"hoà"` (h, o with tone), pressing `w` would attempt to transform both `o` → `ơ` and `à` → `ằ` simultaneously, producing `"hờằ"` or a similar broken sequence rather than `"hòw"`.

**Consequences:** Multi-character words that contain any `w`-mappable vowel mid-word get all vowels transformed when `w` is pressed as a revert/escape. This can produce nonsense output for longer in-progress words.

**Prevention:**
- `applyWCharModifiers()` should only transform the last character (or the last vowel cluster), not the entire buffer.
- Rewrite to check only `buffer.last()` or the trailing vowel substring.

**Detection:** Test: set buffer to `"ha"`, call `processKey('w')`. Expect `"hăw"` or that only `a` is affected. If `h` is also transformed (it should not be, but `h` is not in the map), extend the test to `"hoa"` — expect `"hoă"` not `"hơă"`.

**Phase mapping:** Catch during unit-test writing. The test will expose the bug without requiring a device.

---

### Pitfall 10: No `MockK` or `Mockito` dependency — Android-touching code cannot be tested without real devices

**What goes wrong:** The test dependencies in `build.gradle.kts` are only `junit:junit:4.13.2`. There is no mocking framework. `InputMethodService` and the modifier flow between it and `VietnameseTextInput` involve `currentInputConnection`, `KeyEvent`, and Android system services. These cannot be instantiated in JVM unit tests without either mocking or an instrumented test.

**Consequences:** Tests for `InputMethodService`-level logic (the `deleteLength` calculation, the `consumeModifierNext` call placement) require either an emulator (slow, brittle) or a mock of `InputConnection`. Without a mocking library, developers are tempted to write instrumented tests for logic that should be unit-testable.

**Prevention:**
- Add `testImplementation("io.mockk:mockk:1.13.x")` for Kotlin-idiomatic mocking, or `testImplementation("org.mockito.kotlin:mockito-kotlin:5.x")`.
- Keep the pure logic (`VietnameseTextInput`, `Modifier`, `MultipressController`) free of Android imports — they currently are, which is good. Test those classes directly with plain JUnit. Reserve mocking for the thin `InputMethodService` integration tests.
- Do not add Android-framework imports to `VietnameseTextInput` — that would force it into instrumented tests.

**Detection:** `./gradlew test` will succeed for pure-Kotlin tests without mocking. The missing mocking library only becomes a blocker when attempting to test `InputMethodService` paths.

**Phase mapping:** Decide at test phase start which classes to test in JVM vs instrumented. For this milestone, JVM-only tests of `VietnameseTextInput` and `Modifier` are sufficient and avoid the mocking dependency entirely.

---

### Pitfall 11: `isReverseTone` flag is public and reset to `false` at the top of `processKey` — callers must read it immediately

**What goes wrong:** `isReverseTone` is set to `true` inside `reverseTone()` and then the caller (`InputMethodService.onKeyDown`) reads it after `processKey()` returns (line 455). But `processKey()` sets `isReverseTone = false` at line 172 — the very start of the method. If `processKey` is called twice in rapid succession (e.g. in a test loop), the second call resets the flag before the caller checks it from the first call.

**Consequences:** In real usage this is safe because `onKeyDown` reads the flag immediately after calling `processKey`. In unit tests, if a helper method calls `processKey` in a loop and checks `isReverseTone` after the loop, it will always see `false`. This creates tests that appear to pass but do not actually test the reverse-tone path.

**Prevention:**
- Return `isReverseTone` as part of the result of `processKey` (or return a sealed class `CompositionResult` that carries the replacement string and a `didReverse` flag). This removes the shared-state footgun.
- At minimum, document the contract: "caller must read `isReverseTone` before calling `processKey` again."

**Detection:** Write a test that calls `processKey` to trigger a reverse-tone and asserts `isReverseTone == true` immediately after that single call. If the test passes, the timing is fine. If a future refactor batches calls, the test will silently start returning `false`.

**Phase mapping:** Address during unit-test writing as a test-design discipline issue. The fix (returning a result object) can be deferred to a refactor phase.

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Add `package` to VietnameseTextInput | Pitfall 8 — tests won't compile without it | Do this first, update imports in InputMethodService |
| Fix w-revert (`ow` sequence) | Pitfall 2 — two code branches must be unified | Unify under `applyCharModifiers`; test `o+w+w` → `"ow"` |
| Fix tone placement | Pitfall 1 — "first vowel" algorithm is wrong | Build full nucleus lookup table before patching guards |
| Fix Alt one-shot in Vietnamese mode | Pitfall 3 — `consumeModifierNext` is conditional | Move call outside the transform-success branch |
| Write Modifier state-machine tests | Pitfall 7 — timing edge cases in double-press | Test all sequences: tap, double-tap, hold, interrupted |
| Write VietnameseTextInput unit tests | Pitfalls 4, 5, 9, 11 — discovered during test writing | Test invalid-sequence boundary, buffer-map scope, isReverseTone timing |
| Write deleteLength tests | Pitfall 6 — deleteLength uses wrong base | Track `committedLength` separately; test multi-step compositions |
| Add MockK if needed | Pitfall 10 — no mocking library in build file | For this milestone, avoid mocking by testing pure classes only |
