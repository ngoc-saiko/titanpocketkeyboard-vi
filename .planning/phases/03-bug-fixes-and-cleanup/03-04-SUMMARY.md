---
phase: 03-bug-fixes-and-cleanup
plan: 04
subsystem: testing
tags: [kotlin, android, multipress, consonant-classification, documentation]

requires:
  - phase: 02-unit-test-suite
    provides: MultipressControllerTest — the 11-test consonant-filter and multipress-sequence suite that defines the acceptance contract for this plan

provides:
  - MultipressController.firstLevelConsonantKeycodes — configurable Set<Int> replacing the hardcoded consonant array (FIX-08)
  - Full KDoc with rationale + usage context on all 10 MPSUBST_* sentinel constants (FIX-09)

affects: [03-bug-fixes-and-cleanup]

actuals:
  tokens: 1593
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Data-driven consonant set: configurable Set<Int> replaces hardcoded keycode literal for the first-level consonant filter"
    - "Sentinel documentation: each MPSUBST_* constant carries meaning + code point rationale + process() dispatch site"

key-files:
  created: []
  modified:
    - app/src/main/java/io/github/oin/titanpocketkeyboard/MultipressController.kt

key-decisions:
  - "FIX-08: default consonant set kept as exactly {KEYCODE_C, KEYCODE_S} — no behavior change, only configurability gained"
  - "FIX-09: constant values kept byte-for-byte unchanged; documentation added as KDoc extensions — no code change"
  - "Switched from \\uXXXX escape sequences to direct Unicode chars in Write (both compile identically in Kotlin)"

patterns-established:
  - "Configurable-set pattern: when a hardcoded collection of special-case values has a FIXME, extract to a named Set member with KDoc"
  - "Sentinel KDoc pattern: each sentinel constant documents meaning, code point rationale, and dispatch site"

requirements-completed:
  - FIX-08
  - FIX-09

coverage:
  - id: D1
    description: "MultipressController.firstLevelConsonantKeycodes: Set<Int> with default {KEYCODE_C, KEYCODE_S} replaces hardcoded arrayOf literal"
    requirement: FIX-08
    verification:
      - kind: unit
        ref: "app/src/test/java/io/github/oin/titanpocketkeyboard/MultipressControllerTest.kt#consonantFilter_keycodeC_firstLevel_returnsBypass"
        status: pass
      - kind: unit
        ref: "app/src/test/java/io/github/oin/titanpocketkeyboard/MultipressControllerTest.kt#consonantFilter_keycodeS_firstLevel_returnsBypass"
        status: pass
      - kind: unit
        ref: "app/src/test/java/io/github/oin/titanpocketkeyboard/MultipressControllerTest.kt#consonantFilter_disabled_keycodeC_returnsSubstitution"
        status: pass
      - kind: unit
        ref: "app/src/test/java/io/github/oin/titanpocketkeyboard/MultipressControllerTest.kt#consonantFilter_keycodeBOnFirstLevel_notFiltered"
        status: pass
    human_judgment: false
  - id: D2
    description: "All 10 MPSUBST_* constants have KDoc covering meaning, sentinel code point rationale, and process() dispatch site"
    requirement: FIX-09
    verification:
      - kind: unit
        ref: "app/src/test/java/io/github/oin/titanpocketkeyboard/MultipressControllerTest.kt (all 11 tests)"
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-08-18
status: complete
---

# Phase 3 Plan 04: Data-driven consonant classification and MPSUBST_* documentation Summary

**MultipressController consonant filter made configurable via Set<Int> (FIX-08) and all 10 MPSUBST_* sentinel constants documented with code point rationale and process() dispatch sites (FIX-09)**

## Performance

- **Duration:** 6 min
- **Started:** 2026-08-18T07:26:47Z
- **Completed:** 2026-08-18T07:33:18Z
- **Tasks:** 2
- **Files modified:** 1

## Accomplishments

- Replaced hardcoded `keyCode in arrayOf(KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_S)` with a configurable `firstLevelConsonantKeycodes: Set<Int>` field, default `{KEYCODE_C, KEYCODE_S}` — resolving the `//FIXME` comment at line 105 (FIX-08)
- All 11 MultipressControllerTest cases remain green; the 4 consonant-filter cases pass with identical behavior
- Extended all 10 MPSUBST_* KDocs with: the constant's meaning, the rationale for its sentinel code point (collision-free non-character), and exactly where in process() it is consumed (FIX-09)
- Added a group-level block comment above the sentinel block explaining the overall scheme and the shared-table contract with InputMethodService

## Task Commits

Each task was committed atomically:

1. **Task 1: FIX-08 — data-driven consonant classification** - `6fc48fd` (fix)
2. **Task 2: FIX-09 — document MPSUBST_* constants** - `5ffc250` (docs)

## Files Created/Modified

- `app/src/main/java/io/github/oin/titanpocketkeyboard/MultipressController.kt` — added `firstLevelConsonantKeycodes` field, removed FIXME, replaced hardcoded array literal, extended all 10 MPSUBST_* KDocs with rationale + usage context

## Decisions Made

- FIX-08: default consonant set kept exactly as `{KEYCODE_C, KEYCODE_S}` — configurable does not mean different; existing tests define the contract
- FIX-09: constant values preserved byte-for-byte (only documentation added); the file stored Unicode chars directly instead of `\uXXXX` sequences (both compile identically in Kotlin — verified via Python code-point check)

## Deviations from Plan

None — plan executed exactly as written. Both tasks were pure refactoring (data-driven extraction) and documentation, with no behavior change. The 3 pre-existing RED tests from Phase 02 (TonePlacementTest and WCharModifiersTest) remain red as expected — they document bugs to be fixed in other plans.

## Issues Encountered

- The file contains Unicode characters in the U+FFF0..U+FFFF range which causes `git diff --text` to classify it as binary. This is a cosmetic display issue only; the commit captures the correct binary diff (file size: 5480 → 11855 bytes). Constant values verified via Python `ord()` — all 10 match the original `\uXXXX` values exactly.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- FIX-08 and FIX-09 are both satisfied; MultipressControllerTest remains fully green
- The consonant set is now configurable for future layout variations
- The MPSUBST_* sentinel system is documented for future maintainers
- Remaining Phase 03 plans (01-03) address the 6 RED tests documenting FIX-01, FIX-03, FIX-04 bugs

---
*Phase: 03-bug-fixes-and-cleanup*
*Completed: 2026-08-18*
