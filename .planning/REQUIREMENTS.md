# Requirements: TitanPocketKeyboard Vietnamese IME

**Defined:** 2026-08-18
**Core Value:** Vietnamese input must be accurate and predictable — every key sequence produces the correct character, every time, on any input field.

## v1 Requirements

### Infrastructure

- [ ] **INFRA-01**: Add `package io.github.oin.titanpocketkeyboard` declaration to `VietnameseTextInput.kt` so JVM unit tests can compile
- [ ] **INFRA-02**: Add `testImplementation("io.mockk:mockk:1.14.9")` to `app/build.gradle.kts` for mocking `android.util.Log` in JVM tests
- [ ] **INFRA-03**: A smoke test in `src/test/` compiles and passes, confirming test infrastructure is functional

### Unit Tests — VietnameseTextInput

- [ ] **TEST-01**: Parameterized tests cover all tone mark forward transforms (s→sắc, f→huyền, r→hỏi, x→ngã, j→nặng) applied to all vowel types
- [ ] **TEST-02**: Parameterized tests cover vowel modifier forward transforms (aw→ă, aa→â, ow→ơ, oo→ô, uw→ư, ew→ê, dd→đ)
- [ ] **TEST-03**: Parameterized tests cover Telex revert sequences — typing the modifier key a second time reverts to the literal characters (e.g. `ow` → `ơ`, then `w` again → `ow`)
- [ ] **TEST-04**: Parameterized tests cover `invalidSequences` passthrough — invalid Vietnamese combinations are passed through unchanged
- [ ] **TEST-05**: Tests confirm tone mark is placed on the correct vowel in multi-vowel syllables (quality-diacritic wins; diphthong table for bare vowels)

### Unit Tests — Modifier

- [ ] **TEST-06**: State-machine tests cover Modifier hold mode — Alt held for duration applies metaState, releasing removes it
- [ ] **TEST-07**: State-machine tests cover Modifier one-shot mode — press+release then next key gets metaState, subsequent key does not
- [ ] **TEST-08**: State-machine tests cover Modifier lock mode — double-tap locks; all subsequent keys get metaState until unlocked
- [ ] **TEST-09**: Tests verify `nextDidConsume()` correctly clears the one-shot state

### Unit Tests — MultipressController

- [ ] **TEST-10**: Tests cover consonant filtering — consonant keys do not trigger vowel-modifier substitution
- [ ] **TEST-11**: Tests cover multipress character substitution sequences using the template system
- [ ] **TEST-12**: Tests verify `applyWCharModifiers()` only transforms the last character, not the entire buffer

### Bug Fixes

- [ ] **FIX-01**: `ơ + w` produces `ow` (not `ơw`) — fix `applyCharModifiers()` to set `charModified = true` for `w`-triggered transforms so the revert path fires correctly
- [ ] **FIX-02**: Alt one-shot works in Vietnamese mode on all input field types — fix `onKeyDown()` to call `consumeModifierNext()` before the Telex gate, not after
- [ ] **FIX-03**: Tone marks are placed on the correct vowel in multi-vowel syllables — replace `findFirstVowelIndex()` logic with quality-diacritic priority rule followed by the diphthong table
- [ ] **FIX-04**: `VietnameseTextInput.applyWCharModifiers()` only modifies the last w-mappable vowel in the buffer, not all occurrences
- [ ] **FIX-05**: `deleteLength` calculation correctly handles Vietnamese Telex multi-character replacements without leaving text remnants
- [ ] **FIX-06**: `speechRecognizer` null-safety checks are present in all access paths in `InputMethodService.kt`
- [ ] **FIX-07**: Microphone permission requests are consolidated to a single location with consistent error handling
- [ ] **FIX-08**: Consonant classification in `MultipressController` is data-driven rather than hardcoded keycode checks
- [ ] **FIX-09**: `MPSUBST_*` magic character constants are documented with rationale and usage context
- [ ] **FIX-10**: `invalidSequences` blacklist is extracted to a configuration-driven validator or clearly documented rule set

## v2 Requirements

### Extended Testing

- **TEST-V2-01**: Robolectric-based integration tests for `InputMethodService` lifecycle (service start, key routing, InputConnection commits)
- **TEST-V2-02**: Instrumented UI tests for end-to-end typing sequences on a real or emulated device

### Performance

- **PERF-01**: Profile keyboard response latency under fast typing (>5 characters/second) and identify hotspots
- **PERF-02**: Reduce any identified latency hotspots to under 16ms per key event

### Future Features

- **FEAT-01**: Rule-based Vietnamese phonetic validator to replace the `invalidSequences` blacklist
- **FEAT-02**: Privacy notice in settings disclosing speech data sent to Google Speech Recognition

## Out of Scope

| Feature | Reason |
|---------|--------|
| New language support | Out of milestone scope — hardware keyboard targeting is Vietnamese only |
| On-screen keyboard UI | Hardware keyboard only |
| VNI input mode | Telex only for this milestone; VNI is not currently implemented |
| OAuth / account features | Not an IME concern |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| INFRA-01 | Phase 1 | Pending |
| INFRA-02 | Phase 1 | Pending |
| INFRA-03 | Phase 1 | Pending |
| TEST-01 | Phase 2 | Pending |
| TEST-02 | Phase 2 | Pending |
| TEST-03 | Phase 2 | Pending |
| TEST-04 | Phase 2 | Pending |
| TEST-05 | Phase 2 | Pending |
| TEST-06 | Phase 2 | Pending |
| TEST-07 | Phase 2 | Pending |
| TEST-08 | Phase 2 | Pending |
| TEST-09 | Phase 2 | Pending |
| TEST-10 | Phase 2 | Pending |
| TEST-11 | Phase 2 | Pending |
| TEST-12 | Phase 2 | Pending |
| FIX-01 | Phase 3 | Pending |
| FIX-02 | Phase 3 | Pending |
| FIX-03 | Phase 3 | Pending |
| FIX-04 | Phase 3 | Pending |
| FIX-05 | Phase 3 | Pending |
| FIX-06 | Phase 3 | Pending |
| FIX-07 | Phase 3 | Pending |
| FIX-08 | Phase 3 | Pending |
| FIX-09 | Phase 3 | Pending |
| FIX-10 | Phase 3 | Pending |

**Coverage:**
- v1 requirements: 23 total
- Mapped to phases: 23
- Unmapped: 0 ✓

---
*Requirements defined: 2026-08-18*
*Last updated: 2026-08-18 — traceability updated to 3-phase roadmap (coarse granularity)*
