---
phase: 02-unit-test-suite
plan: "01"
subsystem: test
tags: [tracer, unit-test, VietnameseTextInput, Modifier, MockK]
requires: [01-test-infrastructure/01-01-SUMMARY.md]
provides: [ToneMarkTracerTest — importability + behavior proof for VietnameseTextInput and Modifier]
affects: [app/src/test/java/io/github/oin/titanpocketkeyboard/ToneMarkTracerTest.kt]
tech_stack:
  added: []
  patterns: [MockK @Before/@After Log-stub isolation, JUnit4 assertions]
key_files:
  created:
    - app/src/test/java/io/github/oin/titanpocketkeyboard/ToneMarkTracerTest.kt
  modified: []
decisions:
  - "Immediate release after onKeyDown sets next=true (not held=false+next=false), so get() returns true after onKeyUp; nextDidConsume() is required to fully deactivate — test documents this boundary behavior explicitly"
  - "processKey('a') returns 'a' as a String (char.toString()), not null — bare vowel commits immediately rather than buffering silently"
metrics:
  duration: 4 minutes
  completed: 2026-08-18
  tasks_completed: 1
  tasks_total: 1
  commits: 1
status: complete
actuals:
  tokens: 3200
  tasks: 1
  commits: 1
---

# Phase 02 Plan 01: ToneMarkTracerTest End-to-End Tracer Summary

## One-Liner

End-to-end tracer proving VietnameseTextInput and Modifier are importable and testable under JUnit4 with MockK Log-stub isolation — two passing tests, zero regressions in SmokeTest.

## What Was Built

Created `ToneMarkTracerTest.kt` with two `@Test` methods:

1. **`toneMarkS_onBareA_producesAcute`** — proves the Telex composition path: `processKey('a')` loads 'a' into the internal buffer (returns "a"), then `processKey('s')` applies tone mark `'\'` via `toneMarks`, looks up `toneMapping['a']['\'' ]` = 'á', and returns "á". This is the CORRECT Telex spec; if it had failed, the bug would be in the implementation.

2. **`modifier_hold_activatesAndReleases`** — proves Modifier hold-mode state transitions: `onKeyDown()` → `held=true` → `get()=true`; immediate `onKeyUp()` (elapsed ~0ms < nextThreshold 350ms) → `next=true`, `held=false` → `get()=true` still; `nextDidConsume()` → `next=false` → `get()=false`. Documents the "next" intermediary state explicitly.

Both tests use the established `mockkStatic(Log::class)` / `unmockkStatic(Log::class)` pattern from `SmokeTest.kt` for @Before/@After scoped Log stub isolation.

## Verification Results

| Test | Result |
|------|--------|
| `ToneMarkTracerTest > toneMarkS_onBareA_producesAcute` | PASSED |
| `ToneMarkTracerTest > modifier_hold_activatesAndReleases` | PASSED |
| `SmokeTest > packagedClassIsImportable` | PASSED |
| `SmokeTest > mockkStaticLogWorks` | PASSED |
| `SmokeTest > runnerExecutesCoreLogic` | PASSED |

Total: 5/5 tests passing, 0 failures. `BUILD SUCCESSFUL`.

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| Assert `get()=true` after immediate `onKeyUp()` | `Modifier.onKeyUp()` sets `next=true` when elapsed < nextThreshold; `get()` returns `lock \|\| held \|\| next` — so held=false but next=true keeps get()=true. Test documents actual behavior, not assumed behavior. |
| Use `nextDidConsume()` to complete deactivation | This is the canonical way to clear the `next` flag; asserting get()=false after it proves the full hold-tap lifecycle |
| Keep `@Before`/`@After` structure from SmokeTest | Consistency across the test suite; prevents MockK from leaking across tests if a future test fails mid-setup |

## Deviations from Plan

None — plan executed exactly as written. The `modifier_hold_activatesAndReleases` test correctly predicted the `next=true` intermediate state, and the code confirmed it.

## Known Stubs

None — both test methods make concrete assertions against real class behavior. No placeholder data, no hardcoded empty values.

## Threat Flags

None — no new network endpoints, auth paths, or trust boundaries introduced. Test fixtures contain only public Telex spec string literals ('a', 'á').

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| ToneMarkTracerTest.kt exists | FOUND |
| 02-01-SUMMARY.md exists | FOUND |
| Task commit 79fdc28 exists | FOUND |
