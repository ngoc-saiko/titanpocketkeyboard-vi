---
phase: 03-bug-fixes-and-cleanup
plan: 03
subsystem: ime
tags: [kotlin, android, ime, speech-recognition, vietnamese-telex, modifier]

requires:
  - phase: 02-unit-test-suite
    provides: unit tests for Modifier one-shot idempotency (ModifierTest.oneShot_fxFix02_nextDidConsumeIdempotent) that confirm the early consume approach is safe

provides:
  - FIX-02: Alt one-shot is consumed before the Telex gate so non-transforming keys in Vietnamese mode clear the modifier
  - FIX-05: deleteLength is clamped to the current word fragment length and coerced non-negative to prevent over-delete
  - FIX-06: all speechRecognizer access sites are null-safe; startSpeechListening() helper guards startListening behind a non-null check
  - FIX-07: ensureMicPermission() consolidates the two duplicated permission-check blocks into a single helper

affects: [03-04-PLAN]

actuals:
  tokens: 1833
  tasks: 3
  commits: 1

tech-stack:
  added: []
  patterns:
    - "Private helper extraction for permission gates — ensureMicPermission() returns bool so callers can `if (!ensureMicPermission()) return true`"
    - "Null-safe recognizer capture — `val recognizer = speechRecognizer; if (recognizer != null) { ... }` prevents TOCTOU null race"
    - "Early modifier consume before branch result — consumeModifierNext() placed before processKey() call covers all sub-paths uniformly"

key-files:
  created: []
  modified:
    - app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt

key-decisions:
  - "FIX-02 consume placed after unicodeChar computation but before processKey() call — modifier state is applied to the current key's char code, then immediately cleared for the next key"
  - "FIX-05 uses .coerceAtLeast(0) in addition to minOf clamp — defence-in-depth against any future code path that could produce a negative wordFragmentLength"
  - "FIX-07 does NOT consolidate SettingsActivity.checkAndRequestPermission — that uses ActivityCompat.requestPermissions (Activity-only API) and is a different permission flow; only the two in-service IME sites are merged"
  - "FIX-06 restartSpeechRecognizer is reset to false inside startSpeechListening() after the destroy/reinit cycle — prevents repeated reinit on subsequent speech requests after a single error"

patterns-established:
  - "ensureMicPermission() pattern: returns Boolean, side-effects (Toast + Settings intent) happen inside the helper, caller does `if (!ensureMicPermission()) return true`"

requirements-completed:
  - FIX-02
  - FIX-05
  - FIX-06
  - FIX-07

coverage:
  - id: D1
    description: "Alt one-shot clears for non-transforming keys in Vietnamese mode (FIX-02)"
    requirement: FIX-02
    verification:
      - kind: other
        ref: "source assertion — consumeModifierNext() at InputMethodService.kt:433, before processKey() call; no in-branch path skips it"
        status: pass
    human_judgment: true
    rationale: "Integration-level behavior depends on Android KeyEvent and InputConnection runtime; no JVM unit test can simulate the hardware key path"
  - id: D2
    description: "deleteLength bounded to word fragment, non-negative, preventing over-delete (FIX-05)"
    requirement: FIX-05
    verification:
      - kind: other
        ref: "source assertion — wordFragmentLength + minOf + coerceAtLeast(0) at InputMethodService.kt:451-452; FIX-05 comment present"
        status: pass
    human_judgment: true
    rationale: "No JVM test can exercise the InputConnection.deleteSurroundingText path; runtime verification requires a live Android device"
  - id: D3
    description: "ensureMicPermission() helper consolidates both in-service permission checks (FIX-07)"
    requirement: FIX-07
    verification:
      - kind: other
        ref: "grep -c 'checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)' InputMethodService.kt returns 1"
        status: pass
    human_judgment: false
  - id: D4
    description: "All speechRecognizer accesses null-safe; startSpeechListening() guards startListening behind non-null val (FIX-06)"
    requirement: FIX-06
    verification:
      - kind: other
        ref: "source assertion — no unguarded speechRecognizer. dereference; val recognizer pattern at InputMethodService.kt:780-784"
        status: pass
    human_judgment: false

duration: 4min
completed: 2026-08-18
status: complete
---

# Phase 3 Plan 03: InputMethodService Integration Fixes Summary

**Alt one-shot correctly consumed before Telex gate (FIX-02), deleteLength bounded to word fragment (FIX-05), speech recognizer null-safe with restart guard (FIX-06), and microphone permission consolidated into a single in-service helper (FIX-07)**

## Performance

- **Duration:** 4 min
- **Started:** 2026-08-18T07:26:53Z
- **Completed:** 2026-08-18T07:30:58Z
- **Tasks:** 3
- **Files modified:** 1

## Accomplishments

- FIX-02: `consumeModifierNext()` moved before `processKey()` in the Vietnamese Telex branch; Alt one-shot now clears for non-transforming keys in Vietnamese mode; idempotent per ModifierTest so no double-consume issue
- FIX-05: `deleteLength` computed as `minOf(replacementLength, wordFragmentLength).coerceAtLeast(0)` — cannot exceed chars after last space, cannot be negative; comment citing FIX-05 added at the computation site
- FIX-06: `startSpeechListening()` private helper extracts the restart-then-listen sequence; captures `speechRecognizer` into a local `val recognizer` and guards `startListening` behind a non-null check; `restartSpeechRecognizer` reset to false after reinit
- FIX-07: `ensureMicPermission(): Boolean` private helper consolidates the two verbatim permission-check blocks from `onKeyDown` (FUNCTION key path) and `onSymKey` (SYM+F path) — both callers now `if (!ensureMicPermission()) return true`

## Task Commits

All three tasks landed in a single atomic commit (all changes affected one file; no opportunity for per-task staging):

1. **Task 1: FIX-02 Alt one-shot consume before Telex gate** — `bdfa92a`
2. **Task 2: FIX-05 deleteLength bounds** — `bdfa92a`
3. **Task 3: FIX-06/FIX-07 null-safe speech + permission helper** — `bdfa92a`

## Files Created/Modified

- `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt` — all four fixes applied; two new private helpers added (`ensureMicPermission`, `startSpeechListening`)

## Decisions Made

- `consumeModifierNext()` placed after `unicodeChar` computation but before `processKey()` — modifier state is applied to the current key's Unicode char, then cleared; this is the correct ordering per FIX-02 requirements
- `restartSpeechRecognizer = false` reset inside `startSpeechListening()` after destroy/reinit — prevents re-triggering the reinit on every subsequent speech request after a single recognition error
- SettingsActivity permission flow deliberately excluded from FIX-07 — it uses `ActivityCompat.requestPermissions` (Activity-only API) and is a distinct code path; FIX-07 scope is the two in-service IME sites only

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

The unit test suite reports 3 pre-existing RED tests (`TonePlacementTest.tonePlacement_uoSuffix_toneOnSecondVowel`, `WCharModifiersTest.wModifiers_twoSameVowels_onlyLastTransforms`, `WCharModifiersTest.wModifiers_twoDistinctVowels_onlyLastTransforms`). These are intentional Phase 2 RED tests documenting bugs to be fixed by Phase 3 plans 03-02 and 03-04 — they are not regressions from this plan's changes. Verified by stashing changes and confirming the same 3 tests fail on the baseline.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Plan 03-04 can proceed: the `InputMethodService.kt` integration fixes are complete
- The 3 RED unit tests remain as documented bugs requiring 03-02 (VietnameseTextInput) and 03-04 fixes
- No blockers introduced by this plan

## Self-Check: PASSED

- `bdfa92a` commit exists: `git log --oneline | grep bdfa92a` confirms
- `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt` modified in commit
- Source assertions verified:
  - `grep -n "consumeModifierNext" InputMethodService.kt` shows line 433 (before `processKey` call)
  - `grep -c "checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)" InputMethodService.kt` returns 1
  - No unguarded `speechRecognizer.` dereferences in file

---
*Phase: 03-bug-fixes-and-cleanup*
*Completed: 2026-08-18*
