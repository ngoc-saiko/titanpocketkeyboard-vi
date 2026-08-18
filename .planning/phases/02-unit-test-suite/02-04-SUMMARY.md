---
phase: 02-unit-test-suite
plan: "04"
subsystem: test
tags: [unit-test, VietnameseTextInput, revert, invalidSequences, tone-placement, wCharModifiers, known-red-tests, JUnit4]
requires: [02-unit-test-suite/02-02-SUMMARY.md]
provides: [VietnameseTextInputTest — revert (TEST-03), invalidSequences (TEST-04), tone-placement (TEST-05), wModifiers (TEST-12) coverage]
affects:
  - app/src/test/java/io/github/oin/titanpocketkeyboard/VietnameseTextInputTest.kt
tech_stack:
  added: []
  patterns: [JUnit4 @Test without @RunWith (non-parameterized), FIXME comments naming Phase 3 fix IDs, RED test documentation pattern]
key_files:
  created: []
  modified:
    - app/src/test/java/io/github/oin/titanpocketkeyboard/VietnameseTextInputTest.kt
decisions:
  - "Four new top-level test classes added (RevertSequenceTest, InvalidSequenceTest, TonePlacementTest, WCharModifiersTest) — consistent with established pattern of top-level classes in this file; avoids JUnit4 Parameterized runner scoping issues"
  - "revert_aw_thenA and revert_dd_thenD are RED (FAILED) rather than GREEN as the plan behavior section predicted — code trace reveals charModified is not set for 'w'-path (early return) or for 'd' (explicitly excluded line 234); FIX-01 scope is broader than plan described"
  - "6 RED tests total (3 revert + 1 tone placement + 2 wModifiers) — all assert correct Telex spec, all have FIXME comments naming Phase 3 fix IDs"
metrics:
  duration: 4 minutes
  completed: 2026-08-18
  tasks_completed: 2
  tasks_total: 2
  commits: 1
status: complete
actuals:
  tokens: 18000
  tasks: 2
  commits: 1
---

# Phase 02 Plan 04: Revert, InvalidSequences, Tone Placement, and wModifiers Tests Summary

## One-Liner

Extended VietnameseTextInputTest.kt with 21 new test methods documenting correct Telex spec for revert sequences, invalidSequences passthrough, multi-vowel tone placement, and applyWCharModifiers — 6 tests intentionally RED to target Phase 3 bug fixes.

## What Was Built

Extended `VietnameseTextInputTest.kt` with four new top-level test classes:

**RevertSequenceTest (6 cases — TEST-03):**
- `revert_aa_thenA_producesAa`: a+a→â, then a→"aa" — GREEN (charModified=true for 'a' via charModifiers map)
- `revert_oo_thenO_producesOo`: o+o→ô, then o→"oo" — GREEN
- `revert_ee_thenE_producesEe`: e+e→ê, then e→"ee" — GREEN
- `revert_ow_thenW_producesOw`: o+w→ơ (early return), then w — RED (FIXME FIX-01)
- `revert_aw_thenA_producesAa`: a+w→ă (early return), then a — RED (FIXME FIX-01 extended)
- `revert_dd_thenD_producesDd`: d+d→đ, then d — RED (FIXME FIX-01 extended — 'd' excluded from charModified)

**InvalidSequenceTest (4 cases — TEST-04):**
- `invalid_prBuffer_toneMarkPassesThrough`: buffer "pr" + 's' → "s" — GREEN
- `invalid_rrBuffer_vowelPassesThrough`: buffer "rr" + 'a' → "a" — GREEN
- `invalid_ouBuffer_toneMarkPassesThrough`: buffer "ou" + 's' → "s" — GREEN
- `invalid_singleZ_passesThrough`: processKey('z') → "z" (first guard fires) — GREEN

**TonePlacementTest (6 cases — TEST-05):**
- `tonePlacement_ươ_toneOnO`: buffer "ươ" + 's' → "ướ" (toneMappingEnd special case) — GREEN
- `tonePlacement_iê_toneOnE`: buffer "iê" + 's' → "iế" — GREEN
- `tonePlacement_uô_toneOnO`: buffer "uô" + 's' → "uố" — GREEN
- `tonePlacement_giPrefix_skipToA`: buffer "gia" + 's' → "giá" (gi-prefix skip) — GREEN
- `tonePlacement_quPrefix_skipToA`: buffer "qua" + 's' → "quá" (qu-prefix skip) — GREEN
- `tonePlacement_uoSuffix_toneOnSecondVowel`: buffer "uo" + 's' → RED (FIXME FIX-03; actual "úo")

**WCharModifiersTest (5 cases — TEST-12):**
- `wModifiers_singleVowel_transforms`: a + w → "ă" — GREEN
- `wModifiers_nonVowelThenVowel_lastCharTransforms`: buffer "ba" + w → "bă" — GREEN (non-vowel passes through)
- `wModifiers_oroakSpecialCase`: buffer "oa" + w → "oă" (ơă→oă special case) — GREEN
- `wModifiers_twoDistinctVowels_onlyLastTransforms`: buffer "ao" + w → RED (FIXME FIX-04; actual "ăơ")
- `wModifiers_twoSameVowels_onlyLastTransforms`: buffer "aa" + w → RED (FIXME FIX-04; actual "ăă")

## Verification Results

| Test Class | Total | PASSED | FAILED |
|------------|-------|--------|--------|
| ToneMarkTest | 20 | 20 | 0 |
| VowelModifierTest | 7 | 7 | 0 |
| ToneMarkTracerTest | 2 | 2 | 0 |
| SmokeTest | 3 | 3 | 0 |
| ModifierTest | 12 | 12 | 0 |
| MultipressControllerTest | 11 | 11 | 0 |
| RevertSequenceTest | 6 | 3 | 3 |
| InvalidSequenceTest | 4 | 4 | 0 |
| TonePlacementTest | 6 | 5 | 1 |
| WCharModifiersTest | 5 | 3 | 2 |
| **Total** | **76** | **70** | **6** |

All 6 FAILED tests are intentional — they assert the correct Telex spec and fail because of documented bugs (FIX-01, FIX-03, FIX-04). No compilation errors. BUILD FAILED only due to AssertionError from RED tests.

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| Four separate top-level test classes | Consistent with 02-02 pattern; clean separation per TEST-XX requirement; avoids JUnit4 inner class runner issues |
| revert_aw_thenA and revert_dd_thenD marked RED | Code trace confirmed charModified is not set via the w-early-return path or for 'd' (explicitly excluded at line 234) — both share FIX-01 root cause; correct spec asserted, test will fail until Phase 3 |
| All FIXME comments name Phase 3 fix IDs | Directly traceable to REQUIREMENTS.md fix entries; each comment states actual vs. expected behavior |

## Deviations from Plan

### Auto-fixed Issues

None.

### Plan Analysis Correction (deviation from plan behavior section)

**Deviation: revert_aw_thenA and revert_dd_thenD are RED, not GREEN as plan predicted**

- **Found during:** Task 1 implementation — code trace of processKey() execution path
- **Plan said:** `revert_aw_thenA` EXPECTED GREEN (charModifier set for 'a'); `revert_dd_thenD` EXPECTED GREEN
- **Actual code behavior:**
  - For `a+w`: `processKey('w')` takes the early-return path (line 193: `if (buffer.toString() != newStr) return newStr`). This early return means `applyCharModifiers()` is never called, so `charModified` is never set. When 'a' is typed third, `charModified=false` → no reversal.
  - For `d+d→đ`: `applyCharModifiers()` IS called, but line 234 explicitly excludes 'd': `if (char != 'd' && char != 'w')` means `charModified` is never set for 'd'.
- **Resolution:** Tests assert correct Telex spec ("aa" and "dd" respectively) with FIXME comments documenting FIX-01 extended scope. This is correct documentation of the bug surface — more RED tests means better coverage of the known issue.
- **Final RED count:** 6 (vs. plan's expected 4) — 2 additional revert tests reveal broader FIX-01 impact
- **Impact on Phase 3:** Phase 3 must also fix the `revert_aw_thenA` and `revert_dd_thenD` paths

## Known Stubs

None — all tests make concrete assertions against real class behavior. No placeholder data.

## Threat Flags

None — no new network endpoints, auth paths, or trust boundaries introduced. Test fixtures contain only public Telex spec string literals (Vietnamese vowels, Unicode characters from the Telex standard). FIXME comments reference only internal bug IDs matching REQUIREMENTS.md public entries.

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| VietnameseTextInputTest.kt extended with 21 new test methods | CONFIRMED |
| 02-04-SUMMARY.md created | CONFIRMED |
| Commit 3d6e31c exists | CONFIRMED |
| RevertSequenceTest: 6 tests (3 PASSED, 3 FAILED) | CONFIRMED |
| InvalidSequenceTest: 4 tests, all PASSED | CONFIRMED |
| TonePlacementTest: 6 tests (5 PASSED, 1 FAILED) | CONFIRMED |
| WCharModifiersTest: 5 tests (3 PASSED, 2 FAILED) | CONFIRMED |
| All 6 FAILED tests have FIXME comments naming fix IDs | CONFIRMED |
| No compilation errors (only AssertionError failures) | CONFIRMED |
| All 44 prior tests (from 02-01 through 02-03) still PASSED | CONFIRMED |
