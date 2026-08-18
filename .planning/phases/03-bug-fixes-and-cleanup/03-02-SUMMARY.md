---
phase: 03-bug-fixes-and-cleanup
plan: 02
subsystem: core-composition
tags: [kotlin, android, vietnamese-ime, telex, bug-fix, tone-placement, w-modifier]

# Dependency graph
requires:
  - phase: 03-bug-fixes-and-cleanup
    plan: 01
    provides: FIX-01 charModified flag for w/d revert paths (used by RevertSequenceTest as regression baseline)
provides:
  - VietnameseTextInput.applyWCharModifiers now transforms only the LAST w-mappable vowel (last-char semantics)
  - VietnameseTextInput.processKey w-branch runs BEFORE invalidSequences guard so modifier keys bypass the blacklist
  - toneMappingEnd["uo"]='o' resolves the nucleus of bare uo diphthong to index 1 (FIX-03)
  - invalidSequences documented with KDoc rationale for each group (FIX-10)
affects: [future phonotactics work, FEAT-01 rule-based validator v2]

# Actuals (#2632)
actuals:
  tokens: 6250
  tasks: 3
  commits: 3

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "last-char-only modifier: scan buffer reversed, replace first match found, return immediately — avoids transforming all instances of a w-mappable vowel"
    - "modifier key ordering: process 'w' BEFORE invalidSequences check — 'w' is a buffer-level modifier, not a new character, so blacklist entries in the buffer must not block it"
    - "toneMappingEnd extension: add 'uo'→'o' mirroring 'oa'→'a', 'oe'→'e' (non-u/i vowel in the pair is the nucleus)"

key-files:
  created: []
  modified:
    - app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt

key-decisions:
  - "FIX-04 applyWCharModifiers: scan reversed, replace first w-mappable vowel found and return — eliminates the all-chars map and the now-dead ωă→oă special-case replacement"
  - "FIX-04 processKey reordering: move the 'w' branch before the invalidSequences check so that pressing 'w' on a buffer containing a blacklisted sequence (e.g. 'aa') still applies the vowel modifier as expected"
  - "FIX-03 fix: add toneMappingEnd['uo']='o' — lowest-risk approach matching existing map entries for other falling diphthongs; avoids redesigning findFirstVowelIndex algorithm"
  - "FIX-10 KDoc: documentation-only change; list entries byte-for-byte identical; runtime behavior unchanged"

patterns-established:
  - "When a Telex modifier key ('w') needs to transform a buffer containing a blacklisted sequence, the modifier branch must precede the invalidSequences guard in processKey"

requirements-completed: [FIX-03, FIX-04, FIX-10]

coverage:
  - id: D1
    description: "ao+w → aơ: only last vowel 'o' transforms, first 'a' is unchanged (FIX-04)"
    requirement: FIX-04
    verification:
      - kind: unit
        ref: "WCharModifiersTest.wModifiers_twoDistinctVowels_onlyLastTransforms"
        status: pass
    human_judgment: false
  - id: D2
    description: "aa+w → aă: only last vowel 'a' transforms; w-branch runs before invalidSequences check (FIX-04)"
    requirement: FIX-04
    verification:
      - kind: unit
        ref: "WCharModifiersTest.wModifiers_twoSameVowels_onlyLastTransforms"
        status: pass
    human_judgment: false
  - id: D3
    description: "Single-vowel and prefix cases unchanged: a+w→ă, ba+w→bă, oa+w→oă (FIX-04 no regression)"
    requirement: FIX-04
    verification:
      - kind: unit
        ref: "WCharModifiersTest (singleVowel, nonVowelThenVowel, oroakSpecialCase)"
        status: pass
    human_judgment: false
  - id: D4
    description: "FIX-01 revert sequences unaffected by applyWCharModifiers rewrite (RevertSequenceTest fully green)"
    verification:
      - kind: unit
        ref: "RevertSequenceTest (all 6 methods)"
        status: pass
    human_judgment: false
  - id: D5
    description: "uo+s → uó: tone mark lands on nucleus 'o' at index 1 via toneMappingEnd['uo']='o' (FIX-03)"
    requirement: FIX-03
    verification:
      - kind: unit
        ref: "TonePlacementTest.tonePlacement_uoSuffix_toneOnSecondVowel"
        status: pass
    human_judgment: false
  - id: D6
    description: "Existing toneMappingEnd special cases unchanged: ươ→ướ, iê→iế, uô→uố, gi-prefix, qu-prefix (no regression)"
    verification:
      - kind: unit
        ref: "TonePlacementTest (ươ, iê, uô, giPrefix, quPrefix)"
        status: pass
    human_judgment: false
  - id: D7
    description: "invalidSequences documented with group rationale; runtime behavior byte-for-byte identical (FIX-10)"
    requirement: FIX-10
    verification:
      - kind: unit
        ref: "InvalidSequenceTest (all methods)"
        status: pass
    human_judgment: false
  - id: D8
    description: "Full suite: 76 tests, 0 failures — all 6 Phase-2 RED tests green across 03-01 and 03-02"
    verification:
      - kind: unit
        ref: "./gradlew :app:testDebugUnitTest — 76 tests completed, 0 failed"
        status: pass
    human_judgment: false

# Metrics
duration: 3min
completed: 2026-08-18
status: complete
---

# Phase 3 Plan 02: Fix FIX-03/FIX-04/FIX-10 — Tone Placement, W-Modifier Scope, and invalidSequences Documentation Summary

**Last-char-only w-modifier semantics, correct 'uo' nucleus placement via toneMappingEnd, and KDoc rationale on invalidSequences — all 3 previously-RED tests green; full suite 76/0**

## Performance

- **Duration:** 3 min
- **Started:** 2026-08-18T07:37:35Z
- **Completed:** 2026-08-18T07:41:00Z
- **Tasks:** 3
- **Files modified:** 1

## Accomplishments

- FIX-04: Rewrote `applyWCharModifiers` to scan the buffer reversed and replace only the LAST w-mappable vowel. ao+w→aơ; aa+w→aă; a+w→ă; ba+w→bă; oa+w→oă all correct. The dead "ωă"→"oă" post-processing was removed (last-char semantics makes it unreachable).
- FIX-04 (processKey reorder): Moved the 'w' branch BEFORE the `invalidSequences` check. The 'w' key is a buffer-level modifier, not a new character being appended. The invalidSequences guard is designed to block characters from being processed when the buffer forms an invalid Vietnamese syllable — but that check is irrelevant to 'w' which transforms the existing buffer vowel. The "aa"+w case was being blocked by "aa" being in the blacklist; reordering fixes this.
- FIX-03: Added `"uo" → 'o'` to the `toneMappingEnd` map, mirroring existing entries for other falling diphthongs (`"oa"→'a'`, `"oe"→'e'`). `findFirstVowelIndex` now resolves the nucleus of a bare "uo" buffer to index 1 ('o'), placing the tone correctly: uo+s→uó.
- FIX-10: Added comprehensive KDoc block on `invalidSequences` documenting: purpose, consumer reference, and the rationale for each group (single invalid letters, consonant clusters absent from Vietnamese phonotactics, invalid vowel/rime combinations, numerals, special characters). References FIX-10 and the FEAT-01 v2 successor. Zero runtime behavior change.

## Task Commits

1. **Task 1: Fix FIX-04 — applyWCharModifiers transforms only the last w-mappable vowel** - `1dd238c` (fix)
2. **Task 2: Fix FIX-03 — tone mark lands on nucleus 'o' for bare uo diphthong** - `b41e09f` (fix)
3. **Task 3: FIX-10 — document the invalidSequences blacklist with rationale** - `024f007` (docs)

## Files Created/Modified

- `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt` — Three targeted changes: (1) applyWCharModifiers rewritten with reversed scan + single replacement; (2) processKey 'w' branch moved before invalidSequences check; (3) "uo"→'o' added to toneMappingEnd; (4) KDoc rationale on invalidSequences

## Decisions Made

- FIX-04 `applyWCharModifiers` rewrite: reversed scan produces correct last-char-only semantics without special-casing individual vowel pairs. The "ωă"→"oă" replacement was dead code after the rewrite and was removed.
- FIX-04 processKey reordering: moved 'w' branch before invalidSequences. Semantic justification: 'w' is a Telex modifier that transforms the buffer's vowel; the invalidSequences guard exists to block character *appending* when the buffer is non-Vietnamese; those concerns are orthogonal.
- FIX-03: extending toneMappingEnd is the lowest-risk approach. It reuses the existing nucleus-resolution mechanism without touching `findFirstVowelIndex`'s algorithm, preserving all existing green special cases.
- FIX-10: documentation-only. The list entries are byte-for-byte identical to before. FEAT-01 (rule-based phonetic validator) is the planned v2 successor and is explicitly referenced.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] w-branch order in processKey blocked "aa"+w via invalidSequences**

- **Found during:** Task 1 verification
- **Issue:** The test `wModifiers_twoSameVowels_onlyLastTransforms` sets `buffer = "aa"` then calls `processKey('w')`. With the w-branch AFTER the invalidSequences check, "aa" (in the blacklist) caused processKey to return "w" instead of "aă". The test comment claimed the actual result was "ăă" (from the old all-chars map), but the actual failure was "w" (blocked by invalidSequences).
- **Fix:** Moved the 'w' branch before the invalidSequences check in processKey. The 'w' key's semantics as a buffer modifier make this the correct ordering.
- **Files modified:** `VietnameseTextInput.kt`
- **Commit:** 1dd238c

## Known Stubs

None.

## Self-Check: PASSED

- `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt` — FOUND
- `.planning/phases/03-bug-fixes-and-cleanup/03-02-SUMMARY.md` — FOUND
- Commit 1dd238c — FOUND
- Commit b41e09f — FOUND
- Commit 024f007 — FOUND
- Full suite: 76 tests, 0 failures — VERIFIED

---
*Phase: 03-bug-fixes-and-cleanup*
*Completed: 2026-08-18*
