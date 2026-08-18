---
phase: 03-bug-fixes-and-cleanup
plan: 01
subsystem: testing
tags: [kotlin, android, vietnamese-ime, telex, unit-tests, bug-fix]

# Dependency graph
requires:
  - phase: 02-unit-test-suite
    provides: RED tests for FIX-01 revert sequences in RevertSequenceTest
provides:
  - VietnameseTextInput.processKey w-path now sets charModified=true and persists buffer after transform
  - VietnameseTextInput.applyCharModifiers d-path now sets charModified=true enabling đd→dd revert
  - FIX-01 fully resolved: ơw→ow, ăa→aa, đd→dd all revert correctly
affects: [03-02-PLAN, future tone-placement and wModifiers fixes]

# Actuals (#2632)
actuals:
  tokens: 313
  tasks: 1
  commits: 1

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "w-modifier path: persist transformed buffer via setBuffer() and set charModified=true before early-return so reverseCharModifier lookup fires on the next modifier keypress"
    - "d-modifier path: include 'd' in charModified=true setting — the w exclusion remains correct (w triggers applyWCharModifiers which handles its own buffer persistence)"

key-files:
  created: []
  modified:
    - app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt

key-decisions:
  - "Path 1 fix (w early-return): call setBuffer(newStr) to persist the w-transformed buffer so the next keypress sees the composed character (e.g. 'ơ'), then set charModified=true so the reverseCharModifier path fires"
  - "Path 2 fix (d exclusion): remove 'd' from the charModified guard — the reverseCharModifier map already has đd→dd entries; only the flag was missing"
  - "Dead variable `val check = char != 'd'` removed — it was never consumed"
  - "w remains excluded from charModified in applyCharModifiers because w-transforms are handled by the early-return path (Path 1), not the charModifiers map path"

patterns-established:
  - "FIX pattern: read RED test → targeted source change in VietnameseTextInput.kt → ./gradlew :app:testDebugUnitTest → GREEN; proven reusable for 03-02"

requirements-completed: [FIX-01]

coverage:
  - id: D1
    description: "Telex revert ow→ow: processKey('o'), processKey('w') → ơ, then processKey('w') → ow"
    requirement: FIX-01
    verification:
      - kind: unit
        ref: "app/src/test/java/io/github/oin/titanpocketkeyboard/VietnameseTextInputTest.kt#RevertSequenceTest.revert_ow_thenW_producesOw"
        status: pass
    human_judgment: false
  - id: D2
    description: "Telex revert aw→aa: processKey('a'), processKey('w') → ă, then processKey('a') → aa"
    requirement: FIX-01
    verification:
      - kind: unit
        ref: "app/src/test/java/io/github/oin/titanpocketkeyboard/VietnameseTextInputTest.kt#RevertSequenceTest.revert_aw_thenA_producesAa"
        status: pass
    human_judgment: false
  - id: D3
    description: "Telex revert dd→dd: processKey('d'), processKey('d') → đ, then processKey('d') → dd"
    requirement: FIX-01
    verification:
      - kind: unit
        ref: "app/src/test/java/io/github/oin/titanpocketkeyboard/VietnameseTextInputTest.kt#RevertSequenceTest.revert_dd_thenD_producesDd"
        status: pass
    human_judgment: false
  - id: D4
    description: "No regression: all 6 RevertSequenceTest methods pass (including 3 previously-green controls: revert_aa, revert_oo, revert_ee)"
    verification:
      - kind: unit
        ref: "app/src/test/java/io/github/oin/titanpocketkeyboard/VietnameseTextInputTest.kt#RevertSequenceTest"
        status: pass
    human_judgment: false
  - id: D5
    description: "Full suite: 76 tests, exactly 3 failures remaining (all owned by 03-02: tonePlacement_uoSuffix, wModifiers_twoSameVowels, wModifiers_twoDistinctVowels)"
    verification:
      - kind: unit
        ref: "./gradlew :app:testDebugUnitTest — 76 tests completed, 3 failed"
        status: pass
    human_judgment: false

# Metrics
duration: 2min
completed: 2026-08-18
status: complete
---

# Phase 3 Plan 01: Fix FIX-01 — w/d Telex Revert Bug Summary

**charModified now set for w-modifier (via setBuffer+flag in early-return path) and d-modifier (via removed d-exclusion guard), enabling ơw→ow, ăa→aa, đd→dd reversal in VietnameseTextInput**

## Performance

- **Duration:** 2 min
- **Started:** 2026-08-18T07:26:48Z
- **Completed:** 2026-08-18T07:28:14Z
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments

- Three previously-RED RevertSequenceTest cases now GREEN: revert_ow_thenW_producesOw, revert_aw_thenA_producesAa, revert_dd_thenD_producesDd
- Fixed Path 1 (w early-return in processKey): transformed buffer now persisted via setBuffer() and charModified set to true, so the next modifier keypress finds the composed character and the reverseCharModifier lookup fires
- Fixed Path 2 (d exclusion in applyCharModifiers): removed 'd' from the charModified guard; the reverseCharModifier map already had đd→dd entries, only the flag was missing
- Removed dead variable `val check = char != 'd'` (never used)
- Total suite: 76 tests, exactly 3 failures remaining — all owned by 03-02 (FIX-03 and FIX-04); no regressions introduced

## Task Commits

1. **Task 1: Fix FIX-01 — set charModified for w-triggered and d-triggered transforms** - `8641bbf` (fix)

## Files Created/Modified

- `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt` - Two-part fix: w early-return path now calls setBuffer(newStr) and sets charModified=true; applyCharModifiers no longer excludes 'd' from charModified; dead check variable removed

## Decisions Made

- Path 1 (w): call `setBuffer(newStr)` to persist the w-transformed buffer, then set `charModified = true`. Without buffer persistence the reverseCharModifier lookup would still fail because the next processKey call would apply applyWCharModifiers to the un-updated buffer and return the same transformed string again, never entering the append path.
- Path 2 (d): simply remove 'd' from the `if (char != 'd' && char != 'w')` guard. 'w' stays excluded because w-transforms go through the early-return path (Path 1), not through charModifiers map, so charModified is handled there.
- The reverseCharModifier map data was already correct — no data changes needed, only control flow.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- The `--tests` filter flag works only on `testDebugUnitTest`, not the aggregate `test` task. Used `./gradlew :app:testDebugUnitTest --tests "..."` throughout.

## Next Phase Readiness

- FIX-01 fully resolved; the fix pattern (read RED test → targeted source change → ./gradlew :app:testDebugUnitTest GREEN) is proven end-to-end
- Ready for 03-02 (FIX-03 tone placement + FIX-04 wModifiers last-char-only semantics)
- 3 remaining RED tests are exactly the ones documented for 03-02

---
*Phase: 03-bug-fixes-and-cleanup*
*Completed: 2026-08-18*
