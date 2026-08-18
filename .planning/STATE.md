---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
current_phase: 03
current_phase_name: bug-fixes-and-cleanup
status: executing
stopped_at: Completed 03-02-PLAN.md — FIX-03/FIX-04/FIX-10 fixes; full suite 76/0
last_updated: "2026-08-18T07:42:54.351Z"
last_activity: 2026-08-18
last_activity_desc: Phase 01 execution started
progress:
  total_phases: 3
  completed_phases: 2
  total_plans: 9
  completed_plans: 8
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-08-18)

**Core value:** Vietnamese input must be accurate and predictable — every key sequence produces the correct character, every time, on any input field.
**Current focus:** Phase 03 — bug-fixes-and-cleanup

## Current Position

Phase: 03 (bug-fixes-and-cleanup) — EXECUTING
Plan: 3 of 4
Status: Ready to execute
Last activity: 2026-08-18 — Phase 03 execution started

Progress: [█████████░] 89%

## Performance Metrics

**Velocity:**

- Total plans completed: 0
- Average duration: -
- Total execution time: -

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| - | - | - | - |

**Recent Trend:**

- Last 5 plans: none yet
- Trend: -

*Updated after each plan completion*
**Per-Plan Metrics:**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 02-unit-test-suite P01 | 4m | 1 tasks | 1 files |
| Phase 02-unit-test-suite P02-02 | 3m | 1 tasks | 2 files |
| Phase 02-unit-test-suite P03 | 7m | 2 tasks | 2 files |
| Phase 02-unit-test-suite P04 | 4m | 2 tasks | 1 files |
| Phase 03 P03-03 | 4m | 3 tasks | 1 files |
| Phase 03 P04 | 6m | 2 tasks | 1 files |
| Phase 03 P02 | 3m | 3 tasks | 1 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Init: Fix CONCERNS before new features — known bugs compound; unit tests on broken logic are misleading
- Init: Unit test VietnameseTextInput first — core composition logic is highest risk, zero coverage
- Init: Write tests against correct Telex spec (not current behavior), observe failures, then fix
- [Phase ?]: Immediate onKeyUp sets next=true (not cleared); get() remains true until nextDidConsume() — Modifier hold-tap lifecycle boundary documented in test
- [Phase ?]: processKey('a') returns char.toString() 'a' — bare vowel commits immediately rather than buffering silently
- [Phase ?]: 'y' added to modifiableChars — was in toneMapping/vowelMap but not in the buffering gate; processKey('y') now buffers the vowel enabling y+s→ý tone composition
- [Phase ?]: Modifier state-machine requires no MockK — no Android imports; pure Kotlin testable directly
- [Phase ?]: MultipressController multipress cycling uses repeatCount=0 (quick retap), not repeatCount=1 (long-press level advance)
- [Phase ?]: FIX-02 integration bug documented in test comment; unit mechanism works correctly; integration path in InputMethodService is deferred to Phase 3
- [Phase ?]: revert_aw_thenA and revert_dd_thenD are RED — FIX-01 scope is broader: charModified not set via w-early-return path or for 'd' (explicitly excluded at line 234)
- [Phase ?]: 6 RED tests total document correct Telex spec for bugs FIX-01 (extended), FIX-03, FIX-04 — Phase 3 must fix all 6 red test paths
- [Phase ?]: FIX-02 consume placed after unicodeChar computation but before processKey() call — modifier state is applied to the current key's char code, then immediately cleared for the next key
- [Phase ?]: FIX-07 does NOT consolidate SettingsActivity.checkAndRequestPermission — that uses ActivityCompat.requestPermissions (Activity-only API); only the two in-service IME sites are merged
- [Phase ?]: FIX-06 restartSpeechRecognizer reset to false inside startSpeechListening() after destroy/reinit — prevents repeated reinit after a single recognition error
- [Phase ?]: FIX-08: data-driven consonant set {KEYCODE_C, KEYCODE_S} replaces hardcoded arrayOf literal in MultipressController — FIXME resolved
- [Phase ?]: FIX-09: all 10 MPSUBST_* constants documented with code point rationale and process() dispatch site
- [Phase ?]: FIX-04: move 'w' branch before invalidSequences check — 'w' is a buffer modifier, not a new char; blacklist check orthogonal
- [Phase ?]: FIX-03: toneMappingEnd['uo']='o' — lowest-risk nucleus fix for uo diphthong, mirrors existing oa/oe entries
- [Phase ?]: FIX-10: documentation-only; FEAT-01 rule-based validator is the planned v2 replacement

### Pending Todos

None yet.

### Blockers/Concerns

- INFRA: `VietnameseTextInput.kt` is in the default package; tests with declared packages cannot import it until INFRA-01 is applied — this is the first action in Phase 1
- RISK: Tests must document correct Telex behavior first; writing tests against current broken behavior would codify wrong output and resist the Phase 3 fixes

## Deferred Items

Items acknowledged and carried forward (v2 scope):

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| Testing | Robolectric InputMethodService lifecycle tests (TEST-V2-01) | v2 | Init |
| Testing | Instrumented UI tests for end-to-end typing (TEST-V2-02) | v2 | Init |
| Performance | Profile latency under fast typing (PERF-01) | v2 | Init |
| Performance | Reduce hotspots to <16ms per key event (PERF-02) | v2 | Init |
| Features | Rule-based phonetic validator to replace invalidSequences blacklist (FEAT-01) | v2 | Init |
| Features | Privacy notice for speech data sent to Google (FEAT-02) | v2 | Init |

## Session Continuity

Last session: 2026-08-18T07:42:54.343Z
Stopped at: Completed 03-02-PLAN.md — FIX-03/FIX-04/FIX-10 fixes; full suite 76/0
Resume file: None
