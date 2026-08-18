# Project Research Summary

**Project:** TitanPocketKeyboard Vietnamese IME — bug-fix and test milestone
**Domain:** Android Input Method Engine — Vietnamese Telex composition on hardware keyboard
**Researched:** 2026-08-18
**Confidence:** HIGH

## Executive Summary

This project is a mature Android IME that intercepts hardware key events on the Unihertz Titan Pocket and composes Vietnamese text via the Telex input method. The codebase already has all major features implemented — the current milestone is a correctness and quality pass: fix three known Telex bugs, add a unit test suite that prevents regressions, and profile for performance. There is no new feature work. The research confirms that the bugs are localized, the fix strategies are clear, and the test approach is straightforward given the existing architectural seam between pure-logic classes and Android-coupled classes.

The recommended approach is to work in a strict order: add a `package` declaration to `VietnameseTextInput.kt` first (a prerequisite for test compilation), then write unit tests for `VietnameseTextInput` and `Modifier` using plain JUnit 4 with table-driven parameterized test cases, then apply the three bug fixes (w-revert, tone mark placement, Alt one-shot) with the tests serving as both specification and regression guard. MockK 1.14.9 is the only new dependency needed — and only to stub `android.util.Log` in `VietnameseTextInput` tests. Robolectric is not required for this milestone because all three bugs are in pure-logic classes.

The primary risk is writing tests against the currently-broken algorithm and inadvertently codifying wrong behavior. The mitigation is explicit: write tests that document the correct Telex specification (from authoritative Vietnamese phonology sources), observe them fail against the current code, then fix the code until they pass. The secondary risk is scope creep — the codebase has accumulated several additional issues (invalid-sequence blacklist, buffer-scope bug in `wCharModifiers`, `deleteLength` calculation) that are real but lower priority. These should be captured for a follow-on phase rather than addressed mid-milestone.

## Key Findings

### Recommended Stack

The project is on a sound stack (Kotlin 1.9.0, AGP 8.13.2, JUnit 4.13.2 already configured) and requires minimal changes to enable unit testing. The only addition for this milestone is `io.mockk:mockk:1.14.9` to stub `android.util.Log` in JVM tests. JUnit 4's `@RunWith(Parameterized::class)` is the right pattern for the large table-driven test suite needed for Telex composition cases.

Do not upgrade Kotlin, add JUnit 5, or add Robolectric during this milestone. Each of these adds risk without enabling anything that plain JUnit 4 + MockK cannot provide for the bugs being fixed.

**Core technologies:**
- `junit:junit:4.13.2` (already present): test runner — sufficient for parameterized Telex sequence tests
- `io.mockk:mockk:1.14.9` (add): stub `android.util.Log` so `VietnameseTextInput` tests run on JVM without `returnDefaultValues = true`
- `kotlin.test` (bundled): assertion library — zero additional dependency needed
- Robolectric 4.16.1 (deferred): only if `InputMethodService`-level tests are needed — they are not for this milestone

### Expected Features

The three bugs to fix are all regressions from the correct Telex specification. They are not missing features — they are deviations from the expected behavior that every Vietnamese Telex user assumes.

**Must fix (table stakes — broken behavior):**
- Vowel modifier w-revert: `ow -> o`, but `o + w` must produce `ow` (not `ow`) — the `applyWCharModifiers` path returns early before the reverse check fires
- Tone mark placement at correct syllable nucleus: `findFirstVowelIndex()` returns the first buffer vowel, but Vietnamese phonology requires the nucleus vowel (not the medial glide); the `toneMappingEnd` lookup must be complete and exhaustive
- Alt one-shot in Vietnamese mode: `consumeModifierNext()` is only called on successful Telex transforms; it must be called unconditionally on every character commit path

**Should have (quality, this milestone):**
- Unit tests for all Telex forward transforms, revert sequences, tone placements, backspace, buffer reset
- Unit tests for `Modifier` state machine (hold, one-shot, lock, double-press edge cases)

**Defer (follow-on phase):**
- `invalidSequences` blacklist refactor (currently substring-matches, should prefix-match)
- `wCharModifiers` scope bug (transforms all buffer chars, not just last vowel)
- `deleteLength` calculation correctness (uses post-transform length, not committed length)
- `isReverseTone` flag refactor to return value instead of shared state
- Kotlin upgrade to 2.x
- Performance profiling (no known hotspot; defer until correctness is established)

### Architecture Approach

The codebase has a natural, well-defined seam between pure-logic classes and Android-coupled classes. `VietnameseTextInput.kt` and `Modifier.kt` have zero or near-zero Android dependencies and are directly instantiable in JVM unit tests. `InputMethodService.kt` is Android-coupled and should not be unit-tested in this milestone. All three active bugs are in the pure-logic tier — this means every fix can be verified with fast JVM tests and no emulator.

**Major components:**
1. `VietnameseTextInput` — all Vietnamese composition state, tone mark application, character modifier maps; pure logic, directly testable
2. `Modifier` / `SimpleModifier` — three-state modifier tracking (held/one-shot/lock), timing-based state machine; pure logic, directly testable
3. `MultipressController` — multi-level long-press substitution; accepts `KeyEvent` but only reads stable properties, testable with real `KeyEvent` objects
4. `InputMethodService` — key event routing, Android lifecycle, text commitment; Android-coupled, not unit-testable without Robolectric; out of scope for this milestone's test suite

### Critical Pitfalls

1. **Writing tests against broken behavior** — tests written to match current wrong output will resist the fix and give false confidence. Always write tests against the correct Telex specification first, observe them fail, then fix.
2. **`VietnameseTextInput` is in the default package** — tests in a declared package cannot import default-package classes. Add `package io.github.oin.titanpocketkeyboard` to `VietnameseTextInput.kt` and update the import in `InputMethodService.kt` before writing any test code.
3. **w-revert has two separate code paths** — `applyWCharModifiers()` and `applyCharModifiers()` are separate branches; the reverse check in `reverseCharModifier` is only reachable from `applyCharModifiers`, not from `applyWCharModifiers`. Fixing revert requires unifying these paths or adding the reverse check to `applyWCharModifiers` directly.
4. **Tone mark placement requires exhaustive diphthong table, not guards** — the current approach of adding `if i < 2` guards and special-casing each new bug is the root cause of accumulation. The fix must extend `toneMappingEnd` to a complete lookup covering all Vietnamese vowel clusters, then remove the scan-loop guards.
5. **`consumeModifierNext()` is conditionally called in Vietnamese mode** — it only fires on a successful Telex transform. Moving it to fire unconditionally on every character commit path is the correct fix and must be tested with a Modifier state-machine test that fires a non-transforming key after Alt tap.

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Package Declaration and Test Infrastructure
**Rationale:** The single highest-leverage change is adding the `package` declaration to `VietnameseTextInput.kt`. Without it, no test class with a declared package can import the class. This must be first. Simultaneously, add MockK to `build.gradle.kts` and verify `./gradlew test` runs the existing (empty) test suite without errors.
**Delivers:** A working test runner that can compile tests against `VietnameseTextInput` and `Modifier`.
**Addresses:** Pitfall 8 (default package), Pitfall 10 (no mocking library).
**Avoids:** The trap of writing test infrastructure after tests, which creates compile failures mid-development.

### Phase 2: VietnameseTextInput Unit Test Suite
**Rationale:** Tests must be written before fixes so each fix can be verified and so regressions are caught. The test suite is the specification document for the correct Telex behavior.
**Delivers:** Parameterized test cases covering all 5 tone marks x all vowels, all vowel modifier transforms (aa->a^, ow->o+, uw->u+, dd->d-, ee->e^, aw->a^), revert sequences (including the known-failing w-revert cases as red tests), tone placement for single-vowel and diphthong cases (including known-failing multi-vowel cases as red tests), backspace behavior, and buffer reset.
**Uses:** JUnit 4 `@RunWith(Parameterized::class)`, MockK `mockkStatic(Log::class)`.
**Research flag:** Standard patterns — no additional research needed.

### Phase 3: Modifier Unit Test Suite
**Rationale:** Modifier state machine tests are independent of VietnameseTextInput and should be written before the Alt one-shot fix is applied. They document the intended contract and make the fix safe.
**Delivers:** Tests for hold, one-shot tap-release, lock double-tap, `nextDidConsume` deactivation, and the specific Alt-in-Vietnamese-mode sequence.
**Avoids:** Pitfall 7 (timing edge cases in double-press lock logic), Pitfall 3 (Alt one-shot fix applied without contract documentation).

### Phase 4: Bug Fixes (w-revert, tone placement, Alt one-shot)
**Rationale:** All three fixes land together after tests establish the specification. The red tests from Phase 2 and 3 turn green as the fixes are applied.
**Delivers:** Correct w-revert behavior; correct tone mark placement on syllable nucleus for all Vietnamese diphthong/triphthong patterns; correct Alt one-shot behavior in Vietnamese mode.
**Fix order within phase:** (1) w-revert (isolated to `applyWCharModifiers`), (2) tone placement (extend `toneMappingEnd` exhaustively, verify with full diphthong test matrix), (3) Alt one-shot (move `consumeModifierNext()` call to unconditional position).
**Avoids:** Pitfall 1 (first-vowel scan), Pitfall 2 (split code paths), Pitfall 3 (conditional `consumeModifierNext`).

### Phase 5: Secondary Bug Cleanup and Performance Profiling
**Rationale:** After the three primary bugs are fixed and tests are green, secondary issues (invalid-sequence blacklist, `wCharModifiers` scope, `deleteLength` calculation) can be addressed safely with tests already in place. Performance profiling requires a stable, correct implementation first.
**Delivers:** Cleaner composition logic, correct `deleteLength` tracking via `committedLength`, `invalidSequences` changed to prefix-match, performance profile report.
**Research flag:** May need brief investigation into Android Studio profiler for IME services if hotspots are found.

### Phase Ordering Rationale

- Package declaration must precede all test writing — it is a compile-time prerequisite.
- Tests must precede fixes — each fix needs a failing test to verify it and a passing test to prevent regression.
- `VietnameseTextInput` tests before `Modifier` tests because the Telex bugs are higher severity and the test suite is larger.
- Secondary bugs deferred to Phase 5 because they require the primary fixes to be stable first, and fixing them mid-milestone risks destabilizing the primary bug fixes.
- Performance profiling last because premature optimization before correctness is established produces misleading results.

### Research Flags

Phases with standard patterns (skip research-phase):
- **Phase 1:** Standard Android Gradle dependency addition and package refactor — well-documented.
- **Phase 2:** JUnit 4 parameterized tests + MockK — well-documented; code analysis is the primary input.
- **Phase 3:** Modifier state-machine testing — pure Kotlin, no domain research needed.
- **Phase 4:** Bug fix implementations are fully specified in FEATURES.md and PITFALLS.md with exact code locations and line numbers.
- **Phase 5:** If performance profiling surfaces IME-specific hotspots, may need brief research into `InputConnection` commit batching patterns.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All technologies already in the project; only MockK is new; version confirmed from official releases |
| Features | HIGH | Bugs verified by source code inspection; Telex spec cross-checked against Wikipedia, icanreadvietnamese.com, and ibus-unikey reference implementation |
| Architecture | HIGH | Direct code analysis of all source files; architectural seam is factual, not inferred |
| Pitfalls | HIGH | All pitfalls grounded in specific code paths with line-number references; not inferred from general knowledge |

**Overall confidence:** HIGH

### Gaps to Address

- **`toneMappingEnd` completeness**: Research identifies which vowel clusters are missing but does not enumerate every Vietnamese vowel cluster combination. During Phase 4, cross-reference the complete Vietnamese phonology vowel cluster list against the table before finalizing tests.
- **`deleteLength` fix approach**: Two approaches are documented (track `committedLength` counter vs. always delete to last space). Validate with a multi-step composition test before choosing the implementation strategy.
- **Performance**: No known hotspot. Profiling is required before any optimization. Do not guess.

## Sources

### Primary (HIGH confidence)
- Direct source code analysis: `VietnameseTextInput.kt`, `Modifier.kt`, `MultipressController.kt`, `InputMethodService.kt` — all findings
- [Telex input method — Wikipedia](https://en.wikipedia.org/wiki/Telex_(input_method)) — tone key assignments, vowel modifier rules
- [Rules of Tone Mark Placement — icanreadvietnamese.com](https://icanreadvietnamese.com/blog/14-rule-of-tone-mark-placement) — authoritative placement rules
- [Android local unit tests — official docs](https://developer.android.com/training/testing/local-tests) — test infrastructure patterns
- [MockK releases](https://github.com/mockk/mockk/releases) — version 1.14.9 confirmed

### Secondary (MEDIUM confidence)
- [Vietnamese Typography — tone marks](https://vietnamesetypography.com/tone-marks/) — placement rules cross-check
- [ibus-unikey source](https://github.com/vn-input/ibus-unikey) — reference implementation for double-key revert state machine
- [Robolectric GitHub releases](https://github.com/robolectric/robolectric/releases) — version 4.16.1 for optional future use
- [Go Nhanh core engine](https://mintlify.wiki/khaphanspace/gonhanh.org/technical/core-engine) — confirms double-key revert design pattern

### Tertiary (LOW confidence)
- AnySoftKeyboard test infrastructure (single GitHub fetch) — used to assess Robolectric complexity; confirms high overhead for IME testing
- General Robolectric documentation at robolectric.org — consulted for optional Tier 3 test strategy

---
*Research completed: 2026-08-18*
*Ready for roadmap: yes*
