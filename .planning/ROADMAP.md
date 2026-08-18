# Roadmap: TitanPocketKeyboard Vietnamese IME

## Overview

This milestone is a correctness and quality pass on an existing, feature-complete Android IME. The work proceeds in three horizontal layers: first enable a test infrastructure that allows JVM unit tests to compile against the core logic classes, then write a comprehensive unit test suite that documents correct Telex behavior (with red tests marking the known bugs), then apply all bug fixes so the red tests turn green and the codebase is free of the documented correctness and quality issues.

## Phases

**Phase Numbering:**

- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Test Infrastructure** - Add package declaration, MockK dependency, and verify test runner compiles
- [ ] **Phase 2: Unit Test Suite** - Write parameterized tests for VietnameseTextInput, Modifier, and MultipressController; red tests document known bugs
- [ ] **Phase 3: Bug Fixes and Cleanup** - Fix all ten documented bugs; red tests turn green; secondary quality and safety issues resolved

## Phase Details

### Phase 1: Test Infrastructure

**Goal**: Developers can run `./gradlew test` against `VietnameseTextInput` and `Modifier` without compile errors or missing-class failures
**Depends on**: Nothing (first phase)
**Requirements**: INFRA-01, INFRA-02, INFRA-03
**Success Criteria** (what must be TRUE):

  1. `./gradlew test` completes without compilation errors — the smoke test class in `src/test/` compiles and passes
  2. A test class in a declared package can import `VietnameseTextInput` without a "cannot access default-package class" error
  3. MockK is available on the test classpath so `mockkStatic(Log::class)` compiles and runs without `NoClassDefFoundError`

**Plans**: 0/1 plans executed

- [ ] 01-PLAN.md
- [x] 01-01-PLAN.md — Declare package on VietnameseTextInput, add MockK dependency, and prove with a passing smoke test via `./gradlew test`

### Phase 2: Unit Test Suite

**Goal**: A comprehensive unit test suite documents the correct Telex specification and exposes all known bugs as failing tests before any fix is applied
**Depends on**: Phase 1
**Requirements**: TEST-01, TEST-02, TEST-03, TEST-04, TEST-05, TEST-06, TEST-07, TEST-08, TEST-09, TEST-10, TEST-11, TEST-12
**Success Criteria** (what must be TRUE):

  1. Parameterized tests exist for all five tone marks (s/f/r/x/j) applied to every supported vowel type, and the test names describe the expected output
  2. Parameterized tests exist for all vowel modifier forward transforms (aw, aa, ow, oo, uw, ew, dd) and for their revert sequences; the w-revert cases (ow→ơ then w→ow) are present and visibly failing
  3. Tests exist for correct tone mark placement on multi-vowel syllables; the cases where `findFirstVowelIndex()` places the mark on the wrong vowel are present and visibly failing
  4. State-machine tests exist for Modifier hold, one-shot, and lock modes, including the specific sequence (Alt tap → non-transforming key → no metaState) that exposes the Alt one-shot bug in Vietnamese mode
  5. Tests exist for MultipressController consonant filtering, character substitution sequences, and `applyWCharModifiers()` last-character-only behavior

**Plans**: 4/4 plans executed

Plans:

- [x] 02-01-PLAN.md — End-to-end tracer: tone mark and Modifier hold tests proving both classes importable
- [x] 02-02-PLAN.md — VietnameseTextInput tone mark and vowel modifier tests (TEST-01, TEST-02)
- [x] 02-03-PLAN.md — Modifier state-machine tests and MultipressController consonant filtering and substitution tests
- [x] 02-04-PLAN.md — VietnameseTextInput revert, invalidSequences, tone placement, and applyWCharModifiers tests (TEST-03, TEST-04, TEST-05, TEST-12)

### Phase 3: Bug Fixes and Cleanup

**Goal**: All ten documented bugs are resolved; the previously failing tests pass; and the codebase is free of null-safety, permission, and documentation quality issues
**Depends on**: Phase 2
**Requirements**: FIX-01, FIX-02, FIX-03, FIX-04, FIX-05, FIX-06, FIX-07, FIX-08, FIX-09, FIX-10
**Success Criteria** (what must be TRUE):

  1. Typing `o + w` produces `ơ`; typing `ơ + w` produces `ow` — the revert path fires correctly and all w-revert red tests from Phase 2 now pass
  2. Tone marks land on the syllable nucleus, not the first buffer vowel — all tone placement red tests from Phase 2 now pass for single-vowel, diphthong, and triphthong patterns
  3. Alt one-shot works in Vietnamese mode: pressing Alt then any key (including a non-Telex character) consumes the modifier — the Modifier red test from Phase 2 now passes
  4. `applyWCharModifiers()` modifies only the last w-mappable vowel; `deleteLength` calculation does not leave text remnants after multi-character Telex replacements
  5. `speechRecognizer` access paths have null-safety checks; microphone permission requests are consolidated to a single location; `MPSUBST_*` constants and `invalidSequences` are documented with rationale

**Plans**: 4 plans

Plans:

- [ ] 03-01-PLAN.md — Tracer: fix FIX-01 w/d revert (charModified) in VietnameseTextInput; 3 RED revert tests turn green
- [ ] 03-02-PLAN.md — FIX-03 (nucleus tone placement), FIX-04 (last-vowel w-modifier), FIX-10 (document invalidSequences); remaining 3 RED tests turn green
- [ ] 03-03-PLAN.md — FIX-02 (Alt one-shot before Telex gate), FIX-05 (bounded deleteLength), FIX-06 (null-safe speechRecognizer), FIX-07 (consolidated mic permission) in InputMethodService
- [ ] 03-04-PLAN.md — FIX-08 (data-driven consonant set), FIX-09 (document MPSUBST_* constants) in MultipressController

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Test Infrastructure | 1/1 | Complete   | 2026-08-18 |
| 2. Unit Test Suite | 4/4 | In Progress|  |
| 3. Bug Fixes and Cleanup | 0/4 | Planned | - |
