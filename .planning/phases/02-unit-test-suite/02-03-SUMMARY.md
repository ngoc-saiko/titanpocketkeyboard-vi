---
phase: 02-unit-test-suite
plan: "03"
subsystem: test
tags: [unit-test, Modifier, MultipressController, state-machine, MockK, TDD]
requires: [02-unit-test-suite/02-01-SUMMARY.md]
provides: [ModifierTest — 12 state-machine tests for hold/one-shot/lock/nextDidConsume, MultipressControllerTest — 11 tests for consonant filtering and multipress substitution cycling]
affects:
  - app/src/test/java/io/github/oin/titanpocketkeyboard/ModifierTest.kt
  - app/src/test/java/io/github/oin/titanpocketkeyboard/MultipressControllerTest.kt
tech_stack:
  added: []
  patterns: [MockK mockk<KeyEvent>() for Android framework mock without constructor, JUnit4 state-machine assertions, Thread.sleep for timing boundary tests]
key_files:
  created:
    - app/src/test/java/io/github/oin/titanpocketkeyboard/ModifierTest.kt
    - app/src/test/java/io/github/oin/titanpocketkeyboard/MultipressControllerTest.kt
  modified: []
decisions:
  - "Modifier has no Android framework dependency — no MockK needed; pure Kotlin state machine testable directly"
  - "MultipressController multipress cycling uses repeatCount=0 (quick re-tap), NOT repeatCount=1 (long-press repeat that advances longPressCount); tests corrected after discovering incorrect initial assumption"
  - "KEYCODE_* constants in test code replaced with raw integer literals (KEYCODE_C=31, KEYCODE_S=47) to avoid Android stub class-loading dependency; production code references compile as constant-folded ints"
  - "FIX-02 integration bug documented in oneShot_fxFix02_nextDidConsumeIdempotent with FIXME comment; unit test verifies the unit-level mechanism works, not the integration path"
  - "nextDidConsume_setsPreventNext_subsequentPress_behavesNormally documents that preventNext from nextDidConsume is overwritten by a subsequent onKeyDown that is not a double-tap"
metrics:
  duration: 7 minutes
  completed: 2026-08-18
  tasks_completed: 2
  tasks_total: 2
  commits: 2
status: complete
actuals:
  tokens: 17500
  tasks: 2
  commits: 2
---

# Phase 02 Plan 03: Modifier and MultipressController Test Suites Summary

## One-Liner

State-machine tests for Modifier (12 methods covering hold/one-shot/lock/nextDidConsume, TEST-06 to TEST-09) and MockK-based tests for MultipressController (11 methods covering consonant filtering and multipress substitution cycling, TEST-10 to TEST-11).

## What Was Built

### ModifierTest.kt

12 `@Test` methods organized into four groups covering the complete Modifier state machine:

**Hold mode (TEST-06):**
- `holdMode_keyDown_setsHeld` — after `onKeyDown()`, `isHeld()=true` and `get()=true`
- `holdMode_longHoldThenRelease_clearsAll` — sleep 400ms (>nextThreshold=350ms) + `onKeyUp()` → `get()=false`
- `holdMode_nextThresholdBoundary_exact350ms_doesNotSetNext` — sleep exactly 350ms; `(350 < 350)=false` → no one-shot fires

**One-shot mode (TEST-07):**
- `oneShot_pressAndQuickRelease_setsNext` — quick `onKeyDown()+onKeyUp()` → `next=true`, `get()=true`
- `oneShot_afterNextDidConsume_clearsNext` — `nextDidConsume()` clears `next`; `get()=false`
- `oneShot_fxFix02_nextDidConsumeIdempotent` — second `nextDidConsume()` call is idempotent; FIXME FIX-02 integration context documented

**Lock mode (TEST-08):**
- `lockMode_doubleTap_engagesLock` — two taps within 10ms → `isLocked()=true`; survives `onKeyUp()`
- `lockMode_thirdTap_clearsLock` — third tap within lockThreshold → `isLocked()=false`
- `lockMode_lockThresholdBoundary_exact250ms_doesNotLock` — sleep exactly 250ms; `(250 < 250)=false` → lock does not engage

**nextDidConsume (TEST-09):**
- `nextDidConsume_afterOneShot_clearsNext` — uses `activateForNext()` then `nextDidConsume()`; `get()=false`
- `nextDidConsume_whenNextFalse_isIdempotent` — calling when `next=false` is safe; `get()` stays `false`
- `nextDidConsume_setsPreventNext_subsequentPress_behavesNormally` — documents that `preventNext` from `nextDidConsume()` is overwritten by the next `onKeyDown()` when not a double-tap

No MockK needed — `Modifier.kt` has no Android framework imports.

### MultipressControllerTest.kt

11 `@Test` methods covering consonant filtering and multipress substitution:

**Consonant filtering (TEST-10):**
- `consonantFilter_keycodeC_firstLevel_returnsBypass` — with `ignoreConsonantsOnFirstLevel=true`, KEYCODE_C at `longPressCount=0` returns BYPASS
- `consonantFilter_keycodeS_firstLevel_returnsBypass` — same for KEYCODE_S
- `consonantFilter_disabled_keycodeC_returnsSubstitution` — with filter disabled, KEYCODE_C returns substitution char
- `consonantFilter_keycodeBOnFirstLevel_notFiltered` — KEYCODE_B not in hardcoded set; returns substitution normally

**Multipress substitution (TEST-11):**
- `multipress_firstPress_returnsBypass` — first press always returns BYPASS (else branch)
- `multipress_secondPress_sameKey_returnsFirstSubstitution` — quick re-tap returns `subst[0]='x'`
- `multipress_thirdPress_returnsSecondSubstitution` — third quick-tap returns `subst[1]='y'`
- `multipress_cyclicWrapAround_returnsFirst` — after cycling through all chars, next returns `subst[0]` again
- `multipress_differentKey_resetsState` — different key goes to else branch; returns BYPASS
- `multipress_timeout_resetsBypass` — 800ms sleep (>multipressThreshold=750ms) → second tap returns BYPASS
- `multipress_dedup_sameSubstitutionTwice_returnsNothing` — duplicate char returned twice → second returns MPSUBST_NOTHING

MockK `mockk<KeyEvent>()` used to avoid constructor Stub! errors. KEYCODE constants replaced with raw integers in test code (KEYCODE_C=31, KEYCODE_S=47).

## Verification Results

| Test Suite | Tests | Passed | Failed | Skipped |
|------------|-------|--------|--------|---------|
| ModifierTest | 12 | 12 | 0 | 0 |
| MultipressControllerTest | 11 | 11 | 0 | 0 |
| ToneMarkTracerTest | 2 | 2 | 0 | 0 |
| ToneMarkTest | 20 | 20 | 0 | 0 |
| VowelModifierTest | 7 | 7 | 0 | 0 |
| SmokeTest | 3 | 3 | 0 | 0 |
| **Total** | **55** | **55** | **0** | **0** |

`./gradlew :app:testDebugUnitTest` exits 0. BUILD SUCCESSFUL.

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| No MockK in ModifierTest | Modifier.kt has no Android framework imports; direct instantiation is simpler and more maintainable |
| Raw integer keycodes in MultipressControllerTest | Avoids potential Android stub class-loading issues when accessing `KeyEvent.KEYCODE_*` static fields in test code; values are stable constants from the Android API |
| Quick-tap (repeatCount=0) for multipress cycling tests | Discovered during execution: `repeatCount=1` increments `longPressCount` (advances substitution LEVEL) and resets `count=0`, causing every long-press to return `subst[0]`; cycling through substitutions requires `repeatCount=0` (quick re-taps) |
| FIXME FIX-02 comment in test | Documents the integration-level bug context without writing a test for a bug in a class out of scope; the unit mechanism works correctly |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Corrected repeatCount assumption in multipress tests**

- **Found during:** Task 2 first test run (2 tests failed: `multipress_thirdPress_returnsSecondSubstitution`, `multipress_cyclicWrapAround_returnsFirst`)
- **Issue:** Initial tests used `repeatCount=1` for multipress cycling, but `repeatCount=1` increments `longPressCount` and resets `count=0`, making every call return `subst[0]`. The dedup check then fires `MPSUBST_NOTHING`. Actual cycling behavior uses `repeatCount=0` (user releases and retaps quickly).
- **Fix:** Changed all multipress cycling tests to use `repeatCount=0`; updated comments to explain the distinction between quick-tap (`repeatCount=0`) and long-press repeat (`repeatCount=1`).
- **Files modified:** MultipressControllerTest.kt
- **Commit:** de001fa (same commit; fix applied before committing)

The consonant filter tests (which correctly used `repeatCount=1` to trigger the `longPressCount` branch and then verify filter behavior at `longPressCount=0` after reset) were unaffected.

## Known Stubs

None — all test methods make concrete assertions against real class behavior. No placeholder data.

## Threat Flags

None — no new network endpoints, auth paths, or trust boundaries. Test data contains only integer key codes and ASCII character constants.

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| ModifierTest.kt exists | FOUND at app/src/test/java/io/github/oin/titanpocketkeyboard/ModifierTest.kt |
| MultipressControllerTest.kt exists | FOUND at app/src/test/java/io/github/oin/titanpocketkeyboard/MultipressControllerTest.kt |
| Task 1 commit 9e521e8 exists | FOUND |
| Task 2 commit de001fa exists | FOUND |
| ModifierTest: 12 tests, 0 failures | CONFIRMED |
| MultipressControllerTest: 11 tests, 0 failures | CONFIRMED |
| Full suite: 55 tests, 0 failures | CONFIRMED — BUILD SUCCESSFUL |
