---
phase: 03-bug-fixes-and-cleanup
verified: 2026-08-18T08:15:00Z
status: human_needed
score: 10/10 must-haves verified
behavior_unverified: 4
overrides_applied: 0
human_verification:
  - test: "Alt one-shot clears for non-transforming keys in Vietnamese mode (FIX-02)"
    expected: "After pressing Alt (one-shot) then a key that has no Telex transform (e.g. a consonant with no special mapping), the next key press does NOT receive the Alt modifier state"
    why_human: "Integration-level behavior requires Android InputConnection and hardware KeyEvent runtime — no JVM unit test can simulate the hardware keyboard path on a real device"
  - test: "deleteLength correctly avoids over-delete and text remnants on a real device (FIX-05)"
    expected: "When typing a multi-character Telex replacement (e.g. ơ replacing 'ow'), the deletion removes exactly the composed fragment and no adjacent text is lost or left as a remnant"
    why_human: "InputConnection.deleteSurroundingText path requires a live Android input field — no JVM unit test exercises the cursor/deletion interaction"
  - test: "Typing o+w+w revert produces 'ow' in a real input field (FIX-01 on-device)"
    expected: "On the Titan Pocket hardware keyboard in Vietnamese mode: type 'o', then 'w' (produces ơ), then 'w' again — the committed text changes to 'ow'"
    why_human: "The JVM unit tests confirm the VietnameseTextInput logic, but the end-to-end flow through InputMethodService to the text field requires a connected device"
  - test: "Speech recognition starts without crash when recognizer is null after an error restart (FIX-06)"
    expected: "If speech recognition fails (onError callback fires), a subsequent SYM+F press re-initializes the recognizer and begins listening — no NPE or silent failure"
    why_human: "SpeechRecognizer lifecycle and null-safety requires the Android runtime and microphone permission; cannot be exercised in JVM tests"
behavior_unverified_items:
  - truth: "In Vietnamese mode, pressing Alt (one-shot) then any key — including a non-Telex/non-transforming character — consumes the Alt modifier so it does not leak onto the following key (FIX-02)"
    test: "In Vietnamese mode: activate Alt one-shot (press+release Alt), then press a key with no Telex transformation — verify alt.get() is false on the subsequent key press"
    expected: "Alt modifier is cleared after the non-transforming key; the following key receives no Alt metaState"
    why_human: "The code places consumeModifierNext() before processKey() (line 433), but the invariant that the modifier clears for non-transforming keys in all code paths is a runtime state-transition that grep/presence checks cannot exercise"
  - truth: "The deleteLength calculation in the Telex replacement path never deletes more characters than exist after the last space, and never leaves stale characters after a multi-character Telex replacement (FIX-05)"
    test: "In a real text field, type a multi-character Telex sequence (e.g. 'ow' producing ơ, then apply a tone mark); verify the committed text matches expected and no remnant characters appear before the word"
    expected: "deleteLength = minOf(replacementLength, wordFragmentLength).coerceAtLeast(0) produces a correct deletion; no text remnants or over-deletion"
    why_human: "The computation logic is verifiable in code, but the actual deletion behavior via InputConnection.deleteSurroundingText with real cursor state requires a live Android device"
  - truth: "Every access to speechRecognizer uses a null-safe path; no code path can dereference a null speechRecognizer (FIX-06)"
    test: "Trigger an onError callback from the SpeechRecognizer, then immediately press SYM+F to start listening again — verify no crash and recognizer re-initializes"
    expected: "startSpeechListening() safely handles the restartSpeechRecognizer=true path; null recognizer after destroy is re-initialized before startListening is called"
    why_human: "The null-safe pattern (val recognizer = speechRecognizer; if (recognizer != null)) is present in code, but the runtime state machine through destroy/reinit requires the Android SpeechRecognizer API"
  - truth: "Microphone permission checking is consolidated into a single reusable helper called from all in-service request sites (FIX-07)"
    test: "In a device build without microphone permission: press the FUNCTION key (speech trigger), then SYM+F (second trigger) — verify both show the same Settings redirect + Toast, and RECORD_AUDIO is checked exactly once per path"
    expected: "ensureMicPermission() is invoked from both sites; user sees identical permission flow from both entry points"
    why_human: "While grep confirms a single checkSelfPermission in the file, the runtime flow through both trigger sites requires a real device with revoked microphone permission"
gaps: []
deferred: []
---

# Phase 3: Bug Fixes and Cleanup Verification Report

**Phase Goal:** Fix all 10 documented bugs (FIX-01 through FIX-10) in VietnameseTextInput, InputMethodService, and MultipressController so that the full 76-test suite passes with 0 failures and all previously-RED tests are GREEN.
**Verified:** 2026-08-18T08:15:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

All 10 FIX requirements verified. 4 truths are PRESENT_BEHAVIOR_UNVERIFIED (code present and wired, runtime invariants require device testing).

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | FIX-01: ơ+w→ow revert fires; ă+a→aa; đ+d→dd — three previously-RED RevertSequenceTest cases pass | VERIFIED | charModified=true set in w early-return (line 244); 'd' excluded from applyCharModifiers guard only for 'w' now (line 301); commit 8641bbf; all 6 RevertSequenceTest methods PASS in XML results |
| 2 | FIX-02: Alt one-shot consumed before Telex gate for non-transforming keys | PRESENT_BEHAVIOR_UNVERIFIED | consumeModifierNext() at line 433, before processKey() at line 436 — code is present and wired; runtime modifier-state transition not exercised by any JVM test |
| 3 | FIX-03: Tone mark lands on nucleus 'o' for bare "uo" diphthong; uo+s→uó | VERIFIED | toneMappingEnd["uo"]='o' at line 123; TonePlacementTest: 6 tests, 0 failures; commit b41e09f |
| 4 | FIX-04: applyWCharModifiers transforms only the LAST w-mappable vowel; ao+w→aơ, aa+w→aă | VERIFIED | Reversed scan in applyWCharModifiers (lines 269-278); WCharModifiersTest: 5 tests, 0 failures; commit 1dd238c |
| 5 | FIX-05: deleteLength clamped to word fragment, non-negative, no over-delete | PRESENT_BEHAVIOR_UNVERIFIED | wordFragmentLength + minOf + coerceAtLeast(0) at lines 451-452; FIX-05 comment present; runtime InputConnection behavior not exercisable in JVM tests |
| 6 | FIX-06: All speechRecognizer accesses null-safe; startSpeechListening() guards startListening | PRESENT_BEHAVIOR_UNVERIFIED | val recognizer = speechRecognizer; if (recognizer != null) pattern at lines 780-784; no unguarded speechRecognizer. dereferences found; SpeechRecognizer lifecycle requires runtime |
| 7 | FIX-07: Mic permission consolidated into ensureMicPermission(); single checkSelfPermission | VERIFIED | grep confirms 1 occurrence of checkSelfPermission(this, Manifest.permission.RECORD_AUDIO); ensureMicPermission() at line 753; both call sites (lines 316, 567) verified |
| 8 | FIX-08: consonant classification data-driven via firstLevelConsonantKeycodes Set<Int> | VERIFIED | firstLevelConsonantKeycodes field with default {KEYCODE_C, KEYCODE_S} confirmed by strings+Read; hardcoded arrayOf(KEYCODE_C, KEYCODE_S) absent; MultipressControllerTest: 11 tests, 0 failures; commit 6fc48fd |
| 9 | FIX-09: All 10 MPSUBST_* constants documented with rationale and usage context | VERIFIED | All 10 const val MPSUBST_* declarations confirmed with KDoc covering meaning, sentinel code point, and process() dispatch site; values unchanged (confirmed via strings extraction); commit 5ffc250 |
| 10 | FIX-10: invalidSequences documented with group rationale referencing FIX-10 and FEAT-01 | VERIFIED | KDoc block at lines 156-205 covers purpose, consumer reference, group rationale for each category; references FIX-10 at line 196 and FEAT-01 at line 202; InvalidSequenceTest: 4 tests, 0 failures; commit 024f007 |
| 11 | Full 76-test suite passes with 0 failures | VERIFIED | ./gradlew :app:testDebugUnitTest --rerun-tasks exits 0; XML results: 76 total tests, 0 failures, 0 errors across 10 test suites |

**Score:** 10/10 truths verified (4 present, behavior-unverified — code wired, runtime invariants not exercised)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt` | FIX-01/03/04/10 fixes | VERIFIED | charModified set in w-path; applyWCharModifiers reversed scan; toneMappingEnd["uo"]='o'; invalidSequences KDoc |
| `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt` | FIX-02/05/06/07 fixes | VERIFIED | consumeModifierNext before processKey; bounded deleteLength; ensureMicPermission(); startSpeechListening() |
| `app/src/main/java/io/github/oin/titanpocketkeyboard/MultipressController.kt` | FIX-08/09 fixes | VERIFIED | firstLevelConsonantKeycodes Set<Int>; 10 MPSUBST_* KDocs |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| processKey 'w' branch | charModified flag | setBuffer(newStr) + charModified=true at line 244 | WIRED | w early-return now persists buffer and sets flag before return |
| applyCharModifiers | reverseCharModifier lookup | charModified flag guard at line 287 | WIRED | 'd' no longer excluded; only 'w' excluded (handled by early-return path) |
| onKeyDown Telex branch | processKey call | consumeModifierNext() at line 433 precedes processKey() at line 436 | WIRED | FIX-02: consume fires for all sub-paths including non-transforming keys |
| deleteLength | deleteSurroundingText | minOf(replacementLength, wordFragmentLength).coerceAtLeast(0) at line 452 | WIRED | FIX-05: double-clamped; guarded by deleteLength > 0 at line 454 |
| firstLevelConsonantKeycodes | first-level filter check | keyCode in firstLevelConsonantKeycodes at line 232 | WIRED | FIX-08: Set membership replaces hardcoded arrayOf literal |

### Data-Flow Trace (Level 4)

Not applicable — this phase modifies pure logic functions with no external data sources. All transforms operate on the in-memory composition buffer (StringBuilder) and return strings to the caller.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| RevertSequenceTest all 6 pass | XML: testDebugUnitTest results | tests=6, failures=0 | PASS |
| WCharModifiersTest all 5 pass (FIX-04) | XML: testDebugUnitTest results | tests=5, failures=0 | PASS |
| TonePlacementTest all 6 pass (FIX-03) | XML: testDebugUnitTest results | tests=6, failures=0 | PASS |
| InvalidSequenceTest all 4 pass (FIX-10) | XML: testDebugUnitTest results | tests=4, failures=0 | PASS |
| MultipressControllerTest all 11 pass (FIX-08/09) | XML: testDebugUnitTest results | tests=11, failures=0 | PASS |
| Full suite 76 tests, 0 failures | ./gradlew :app:testDebugUnitTest --rerun-tasks | BUILD SUCCESSFUL, 76/0 | PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|---------|
| FIX-01 | 03-01 | w/d revert bug — charModified for w-triggered and d-triggered transforms | SATISFIED | Code: charModified=true in w-path (line 244); 'd' excluded removed; 3 RED tests now GREEN |
| FIX-02 | 03-03 | Alt one-shot in Vietnamese mode — consumeModifierNext before Telex gate | SATISFIED (source assertion) | consumeModifierNext() at line 433, before processKey() line 436 |
| FIX-03 | 03-02 | Tone mark on correct vowel — uo→uó | SATISFIED | toneMappingEnd["uo"]='o' at line 123; TonePlacementTest RED case GREEN |
| FIX-04 | 03-02 | applyWCharModifiers last-char-only | SATISFIED | Reversed scan at lines 269-278; 2 RED WCharModifiersTest cases GREEN |
| FIX-05 | 03-03 | deleteLength bounded to word fragment | SATISFIED (source assertion) | minOf + coerceAtLeast(0) at line 452; FIX-05 comment at line 446 |
| FIX-06 | 03-03 | speechRecognizer null-safety | SATISFIED (source assertion) | val recognizer = speechRecognizer pattern; no unguarded dereferences |
| FIX-07 | 03-03 | Mic permission consolidated | SATISFIED | grep: single RECORD_AUDIO check; both call sites verified |
| FIX-08 | 03-04 | Consonant classification data-driven | SATISFIED | firstLevelConsonantKeycodes Set<Int>; no hardcoded array literal |
| FIX-09 | 03-04 | MPSUBST_* constants documented | SATISFIED | 10 constants with KDoc; values unchanged |
| FIX-10 | 03-02 | invalidSequences documented | SATISFIED | KDoc with group rationale, FIX-10 and FEAT-01 references |

**Note:** REQUIREMENTS.md traceability table still shows FIX-01 as "Pending" (unchecked checkbox) despite the fix being implemented (commit 8641bbf) and all 6 RevertSequenceTest cases passing. This is a documentation-only inconsistency — the implementation is complete. REQUIREMENTS.md should be updated to mark FIX-01 as complete.

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|---------|--------|
| REQUIREMENTS.md | FIX-01 marked `- [ ]` (unchecked) and "Pending" in traceability table | Warning | Documentation inconsistency — implementation exists and tests pass; no code impact |

No TBD, FIXME, or XXX markers found in any of the three modified source files.

### Human Verification Required

#### 1. Alt One-Shot Modifier in Vietnamese Mode (FIX-02)

**Test:** In Vietnamese input mode on the Titan Pocket, activate Alt one-shot (press and release the Alt key), then press a character key that has no Telex transformation (e.g. 'b', 'p', 'n'). Then press another character key.
**Expected:** The second key press does NOT receive the Alt modifier. The Alt indicator disappears after the first non-transforming key.
**Why human:** consumeModifierNext() is at the correct position in the source (line 433, before processKey() at line 436), but the state-machine invariant that the modifier clears for all non-transforming Vietnamese-mode code paths requires runtime Android KeyEvent routing to verify.

#### 2. Delete Length Bounds on Device (FIX-05)

**Test:** In a text field on the device, type a multi-character Telex sequence: type 'o', 'w' (committed as ơ), then add a tone mark (e.g. 's' → ớ). Then continue typing.
**Expected:** No text remnants before the composed word; no characters from a previous word are deleted; the cursor position is correct after each transformation.
**Why human:** The deleteLength computation is verified in code but InputConnection.deleteSurroundingText with real cursor state in a live text field cannot be exercised in JVM tests.

#### 3. Speech Recognizer Null Safety After Error (FIX-06)

**Test:** On the device, trigger speech recognition (SYM+F or FUNCTION key), then force an error (e.g. speak nothing or deny audio mid-session). After the error, trigger speech recognition again.
**Expected:** The second attempt re-initializes the recognizer and begins listening; no ANR, crash, or NPE.
**Why human:** The startSpeechListening() null-safe pattern is present in code, but the destroy/reinit sequence through restartSpeechRecognizer requires the Android SpeechRecognizer runtime.

#### 4. Microphone Permission Flow from Both Trigger Sites (FIX-07)

**Test:** On a device with RECORD_AUDIO permission revoked, trigger speech from the FUNCTION key and separately from SYM+F.
**Expected:** Both triggers show identical behavior: a Toast "Please grant microphone permission in Settings" and navigation to app Settings. No duplicate permission check dialogs.
**Why human:** grep confirms a single checkSelfPermission in the file, but the user-visible permission flow through both trigger sites requires a device with permission revoked.

### Gaps Summary

No blocking gaps found. The phase goal is achieved at the code and unit test level:
- All 10 bugs (FIX-01 through FIX-10) are implemented in the three source files.
- The full 76-test JVM suite passes with 0 failures (confirmed by fresh ./gradlew :app:testDebugUnitTest run).
- All 6 previously-RED tests are GREEN.
- No unresolved debt markers in modified source files.

One documentation inconsistency: REQUIREMENTS.md still marks FIX-01 as `[ ]` (Pending). The implementation is confirmed present and tested. This should be updated to `[x]` and "Complete" in the traceability table.

Four integration-level behaviors (FIX-02, FIX-05, FIX-06, FIX-07) require on-device verification because they depend on Android InputConnection, Modifier state machine with real hardware events, or SpeechRecognizer runtime. The code changes for these are present and wired correctly.

---

_Verified: 2026-08-18T08:15:00Z_
_Verifier: Claude (gsd-verifier)_
