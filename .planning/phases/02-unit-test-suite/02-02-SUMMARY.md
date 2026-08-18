---
phase: 02-unit-test-suite
plan: "02"
subsystem: test
tags: [unit-test, VietnameseTextInput, Parameterized, JUnit4, tone-marks, vowel-modifiers]
requires: [02-unit-test-suite/02-01-SUMMARY.md]
provides: [VietnameseTextInputTest — parameterized tone mark (TEST-01) and vowel modifier (TEST-02) coverage]
affects:
  - app/src/test/java/io/github/oin/titanpocketkeyboard/VietnameseTextInputTest.kt
  - app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt
tech_stack:
  added: []
  patterns: [JUnit4 @RunWith(Parameterized::class), companion @JvmStatic @Parameterized.Parameters, MockK @Before/@After Log-stub isolation]
key_files:
  created:
    - app/src/test/java/io/github/oin/titanpocketkeyboard/VietnameseTextInputTest.kt
  modified:
    - app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt
decisions:
  - "'y' added to modifiableChars to enable tone mark composition — 'y' was in toneMapping and vowelMap but not modifiableChars, so processKey('y') short-circuited before buffering; fix is minimal and localized"
  - "Two separate top-level parameterized classes (ToneMarkTest, VowelModifierTest) preferred over a single class with inner classes — avoids JUnit4 Parameterized runner scoping issues and keeps test data clearly separated"
  - "setBuffer() used for pre-modified vowels (ă, â, ê, ô, ơ, ư) to isolate tone mark application from vowel modifier logic — exactly as specified in plan"
metrics:
  duration: 3 minutes
  completed: 2026-08-18
  tasks_completed: 1
  tasks_total: 1
  commits: 1
status: complete
actuals:
  tokens: 44000
  tasks: 1
  commits: 1
---

# Phase 02 Plan 02: Tone Mark and Vowel Modifier Tests Summary

## One-Liner

Parameterized JUnit4 tests covering all 20 tone mark combinations (TEST-01) and all 7 vowel modifier transforms (TEST-02) for VietnameseTextInput, plus a bug fix adding 'y' to modifiableChars.

## What Was Built

Created `VietnameseTextInputTest.kt` with two parameterized test classes:

**ToneMarkTest (20 cases — TEST-01):**
- Bare vowels 'a' + all 5 tone marks (s/f/r/x/j) → á/à/ả/ã/ạ (5 cases)
- Pre-loaded 'ă' + all 5 tone marks → ắ/ằ/ẳ/ẵ/ặ (5 cases)
- Pre-loaded 'â' + 's' → ấ (1 case)
- Bare 'e' + 's' → é (1 case)
- Pre-loaded 'ê' + 's' → ế (1 case)
- Bare 'i' + 's' → í (1 case)
- Bare 'o' + 's' → ó (1 case)
- Pre-loaded 'ô' + 's' → ố (1 case)
- Pre-loaded 'ơ' + 's' → ớ (1 case)
- Bare 'u' + 's' → ú (1 case)
- Pre-loaded 'ư' + 's' → ứ (1 case)
- Bare 'y' + 's' → ý (1 case)

**VowelModifierTest (7 cases — TEST-02):**
- aw → ă, aa → â, ow → ơ, oo → ô, uw → ư, ee → ê, dd → đ

Both classes use `mockkStatic(Log::class)` / `unmockkStatic(Log::class)` in `@Before`/`@After` for Log-stub isolation, matching the established pattern from SmokeTest and ToneMarkTracerTest.

## Verification Results

| Test Class | Tests | Result |
|------------|-------|--------|
| ToneMarkTest | 20 | PASSED |
| VowelModifierTest | 7 | PASSED |
| ToneMarkTracerTest | 2 | PASSED |
| SmokeTest | 3 | PASSED |
| ModifierTest | 12 | PASSED |
| **Total** | **44** | **100% — BUILD SUCCESSFUL** |

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| Two separate top-level parameterized classes | JUnit4 Parameterized runner works cleanly with top-level classes; inner class approach can cause runner scoping issues |
| setBuffer() for pre-modified vowels | Isolates tone mark application from vowel modifier path — exactly per plan spec; prevents combined-path coupling in tone tests |
| 'y' added to modifiableChars (bug fix) | 'y' is a Vietnamese vowel in toneMapping and vowelMap but was not buffered by processKey; adding it makes y+s→ý work correctly |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] 'y' not in modifiableChars caused processKey('y') to short-circuit**
- **Found during:** Task 1 (ToneMarkTest toneMarkApplied[y+s → ý] FAILED)
- **Issue:** `processKey` guards with `if (char !in modifiableChars && char !in toneMarks.keys) { return char.toString() }`. Since 'y' was not in `modifiableChars`, it returned "y" immediately without buffering, making subsequent tone mark processing impossible.
- **Fix:** Added 'y' to `modifiableChars` in `VietnameseTextInput.kt` (line 17). This is a minimal, localized change — 'y' was already in `toneMapping` and `vowelMap`, so it was always intended to support tone marks.
- **Files modified:** `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt`
- **Commit:** c499564

## Known Stubs

None — all parameterized test cases make concrete assertions against real class behavior. No placeholder data, no hardcoded empty values.

## Threat Flags

None — no new network endpoints, auth paths, or trust boundaries introduced. Test fixtures contain only public Telex spec string literals (Vietnamese vowels and tone-mark characters from the Unicode standard).

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| VietnameseTextInputTest.kt exists | FOUND |
| 02-02-SUMMARY.md exists | FOUND |
| Task commit c499564 exists | FOUND |
| ToneMarkTest has 20 test cases | CONFIRMED (test report) |
| VowelModifierTest has 7 test cases | CONFIRMED (test report) |
| All 44 tests pass, BUILD SUCCESSFUL | CONFIRMED |
