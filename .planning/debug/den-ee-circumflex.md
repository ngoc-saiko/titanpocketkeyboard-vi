---
status: awaiting_human_verify
trigger: "Regression bug: cannot type \"đến\" anymore. After typing đ, pressing \"ee\" no longer transforms to ê (Vietnamese Telex circumflex diacritic). This worked before but broke in a recent change."
created: 2026-08-19
updated: 2026-08-19
---

## Symptoms

- expected: Typing đ-e-e should compose "đê" (circumflex applied to the second 'e', per Telex rules), en route to "đến" with tone mark.
- actual: After typing đ, pressing "ee" does not transform to ê. The circumflex substitution fails specifically when preceded by đ.
- errors: None reported (silent failure — wrong/no character output, not a crash).
- timeline: Started after the "V2.0 fix (#1)" commit (b699975), which merged test infrastructure work (INFRA-01/02/03: packaging fixes, SmokeTest, mockk) into VietnameseTextInput.kt / related files.
- reproduction: Only broken when 'ee' immediately follows 'đ' (e.g. "đee" -> should give "đê"). Typing "ee" in other contexts (not preceded by đ) works fine and produces ê correctly.

## Current Focus

bug_class: Bohrbug (fully deterministic — same key sequence always fails)
hypothesis: `charModified` is a single buffer-global boolean. Commit b699975 (FIX-01, plan 03-01)
  removed the `char != 'd'` exclusion from the flag assignment in `applyCharModifiers`. Now `dd -> đ`
  sets `charModified = true`, which permanently routes every later `applyCharModifiers` call in the
  same syllable into the reverse-only branch. `ee` therefore never reaches the forward `charModifiers`
  lookup, so `đee` stays `đee` instead of becoming `đê`.
test: Drive `VietnameseTextInput.processKey` directly with d,d,e,e in a JVM unit test and assert "đê".
expecting: RED before fix (buffer "đee"), GREEN after.
next_action: Apply fall-through fix in applyCharModifiers, re-run full suite.

reasoning_checkpoint:
  hypothesis: "`charModified` is a buffer-global boolean that, once set by the `dd -> đ` onset
    transform, routes applyCharModifiers into the reverse-only branch for the remainder of the
    syllable — so the nucleus's own forward modifier (`ee -> ê`) is never consulted."
  confirming_evidence:
    - "git diff vs b699975^ shows the `char != 'd'` exclusion was removed from the charModified
       assignment; that is the only semantic change on this path."
    - "Direct observation via JVM repro test: d,d,e,e returns 'e' (expected 'đê'); d,d,a,a returns
       'a' (expected 'đâ'); d,d,o,o returns 'o' (expected 'đô')."
    - "Control cases pass in the SAME run: bare e,e -> 'ê', and d,d,d -> 'dd'. The failure is
       conditioned exactly on `đ` preceding a double-letter modifier, as the hypothesis predicts."
    - "Second-order confirmation: d,d,e,e,n,s returns 's' not 'đế'. Because 'ee' is left
       untransformed in the buffer, the invalidSequences blacklist (which contains 'ee') then
       rejects the tone key too — explaining why the whole word 'đến' fails, not just the ê."
  falsification_test: "If the hypothesis were wrong, the bare e,e control would also fail, or
    d,d,d revert would fail. Both pass. Conversely, making the forward branch reachable while
    charModified=true should turn exactly the 4 RED cases green and leave all others untouched."
  fix_rationale: "Reverse and forward pattern sets are DISJOINT over the same 2-char buffer suffix
    (reverse keys all begin with a composed non-ASCII char: đ/ê/â/ô/ơ/ă/ư; forward keys are pure
    ASCII pairs: dd/ee/aa/oo/ow/aw/uw). `buffer.endsWith(pattern)` already makes the reverse check
    positional. Therefore the `charModified` flag is not needed to gate the forward path — the fix
    is to let a non-matching reverse lookup FALL THROUGH to the forward lookup instead of
    short-circuiting via `else`. This addresses the root cause (a global flag standing in for a
    positional condition) rather than special-casing 'd' again."
  blind_spots: "Sequences of >3 repeated modifier letters (e.g. a,a,a,a) now re-fire the forward
    path where they previously fell through to no-op — this matches Unikey reference behavior but
    is asserted only indirectly by the existing suite. Uppercase/mixed-case paths rely on
    endsWith(ignoreCase=true) and existing map ordering; unchanged by this fix."
  candidate_causes:
    - "code: charModified global flag blocks forward modifier lookup (CONFIRMED)"
    - "data: invalidSequences blacklist contains 'ee'/'aa', which suppresses the tone key once the
       forward transform has failed (CONFIRMED as an amplifier, not the origin)"
    - "config: multipress/template mapping emits wrong char for the e key (REFUTED — bare 'ee'
       control produces 'ê' correctly, so key delivery is fine)"
  and_gate: "yes — two conditions must hold simultaneously for the FULL 'đến' failure: (1) the
    charModified flag blocks `ee -> ê`, AND (2) invalidSequences contains 'ee', which then rejects
    the tone key on the residual buffer. Condition (1) alone explains the reported 'ee does not
    become ê'; (1)+(2) explain why the entire word is unusable. Fixing (1) removes the trigger for
    (2) because 'ee' never survives in the buffer — so a single fix resolves both."

tdd_checkpoint:
  test_file: "app/src/test/java/io/github/oin/titanpocketkeyboard/DenEeCircumflexRegressionTest.kt"
  status: "green"
  failure_output: "expected:<[đê]> but was:<[e]>; expected:<[đâ]> but was:<[a]>; expected:<[đô]> but was:<[o]>; expected:<[đế]> but was:<[s]>"

## Evidence

- timestamp: 2026-08-19
  checked: `git show b699975^:VietnameseTextInput.kt` vs working tree — `applyCharModifiers`
  found: Parent had `val check = char != 'd'` / `if (char != 'd' && char != 'w') { charModified = true }`.
    Current has only `if (char != 'w') { charModified = true }`. The `char != 'd'` exclusion was
    deliberately removed by commit-message item "fix(03-01) ... Path 2 (d exclusion): remove 'd' from
    the charModified guard in applyCharModifiers, so dd->đ sets charModified=true and the đd->dd revert
    path fires on the next 'd'".
  implication: The regression is a direct, intentional-but-overreaching change. `đ` is an ONSET
    CONSONANT, not the syllable nucleus — marking the syllable "already modified" after `dd` blocks the
    nucleus vowel's own modifier.

- timestamp: 2026-08-19
  checked: Hand-trace of processKey for d,d,e,e
  found: d -> buffer "d"; d -> "dd" matches charModifiers -> "đ", charModified=true; e -> buffer "đe",
    charModified=true so reverse branch, no reverseCharModifier match, returns false; e -> buffer "đee",
    STILL reverse branch, "đee" does not end with any reverseCharModifier key ("êe" etc.) -> false.
    Forward rule `"ee" -> "ê"` is never consulted.
  implication: Confirms mechanism. Blast radius is far wider than "đến": every đ + modified-vowel
    syllable is broken (đâu, đôi, đơn, đường, đăng, đến...), which is a large fraction of Vietnamese.

- timestamp: 2026-08-19
  checked: Corpus differential over Viet11K.txt (4516 unique words), full failure sets dumped under
    both the buggy and fixed source via a throwaway harness (since the corpus assertion truncates
    its preview at take(50), making count-only comparison misleading).
  found: BEFORE (buggy) 127/4516 failing; AFTER (fix) 50/4516 failing. Exact set diff:
    NEWLY BROKEN = 0, NEWLY FIXED = 77 (điên, điếm, điều, điện, đuôi, đuốc, đâm, đâu, đây, đê, đêm,
    đô, đôi, đông, đấm, đầu, đậu, đề, đố, đồng, độ, động, ...).
  implication: The fix is a strict improvement — every one of the 77 recovered words is a
    `đ` + modified-vowel syllable, exactly the class the hypothesis predicted, and nothing
    regressed. The residual 50 are pre-existing and unrelated (old-style vs new-style tone
    placement on oa/oe rimes — hoà vs hòa; uê/uy nucleus selection; plus non-Vietnamese words
    like "mistake"/"needly").

- timestamp: 2026-08-19
  checked: Fix-acceptance guardrail signal 4 — revert the fix and confirm the bug returns.
  found: With the fix reverted, the suite goes from 1 failure to 6: the 4 new DenEeCircumflex
    cases plus `VietnameseCorpusTest.regression_denTypesCorrectly_afterDdConsonant` (a test the
    user authored independently during this session for the same bug) plus the corpus test.
    Restoring the fix returns it to 1.
  implication: Causal link between this specific edit and the symptom is established in both
    directions — the fix is not incidental.

## Eliminated

- hypothesis: "MultipressController maps the 'e' key incorrectly, so the second 'e' never reaches
    VietnameseTextInput as 'e'."
  evidence: Bare e,e (no preceding đ) composes 'ê' correctly in the same test run, and
    MultipressControllerTest is green throughout. Key delivery is not involved; the defect is
    entirely inside VietnameseTextInput.applyCharModifiers.
  timestamp: 2026-08-19

- hypothesis: "Restore the parent commit's `char != 'd'` exclusion on the charModified assignment."
  evidence: Viable for the reported symptom, but it re-breaks `revert_dd_thenD_producesDd`, the
    test FIX-01 was written to satisfy (d,d,d must yield "dd" — that revert needs charModified set
    after dd -> đ). Rejected in favour of the fall-through fix, which satisfies both: verified
    d,d,d -> "dd" and d,d,e,e -> "đê" green in the same run.
  timestamp: 2026-08-19

## Resolution

root_cause: |
  `VietnameseTextInput.applyCharModifiers` used a buffer-GLOBAL `charModified` flag as an
  `if/else` switch between the reverse-modifier lookup and the forward-modifier lookup. Commit
  b699975 (FIX-01, plan 03-01) removed the `char != 'd'` exclusion from that flag's assignment so
  the `dd -> đ` revert could work. But `đ` is an ONSET CONSONANT, not the syllable nucleus — once
  `dd` set the flag, the `else` locked the remainder of the syllable into reverse-only lookups, so
  the nucleus vowel could never take its own modifier and `đ`+`ee` stayed `đee`.
  Contributing second condition (AND-gate): the residual, untransformed `ee` left in the buffer
  then matched the `invalidSequences` blacklist (which contains "ee"/"aa"), causing the following
  tone keystroke to be rejected too — which is why the whole word `đến` failed, not merely the ê.
  Fixing the first condition removes the trigger for the second, since `ee` no longer survives.
fix: |
  In `applyCharModifiers`, converted the `if (charModified) { reverse } else { forward }` switch
  into `if (charModified) { reverse }` followed by an unconditional fall-through to the forward
  lookup. Safe because the two pattern sets are disjoint over the same 2-char buffer suffix:
  reverseCharModifier keys all begin with a composed non-ASCII char (đ, ê, â, ô, ơ, ă, ư) while
  charModifiers keys are pure-ASCII pairs (dd, ee, aa, oo, ow, aw, uw) — so reverse still wins
  wherever it legitimately applies, and `buffer.endsWith()` already makes that check positional.
verification:
  signal_1_repro_test: "PASS — DenEeCircumflexRegressionTest 8/8 green (was 4 RED pre-fix)."
  signal_2_regression_suite: "PASS with one documented pre-existing exception — 92 tests, 1 failure
    (VietnameseCorpusTest.corpus_words), which is red both before and after this change and belongs
    to the user's in-progress out-of-scope corpus work. All 91 other tests green, including the
    FIX-01 revert tests this fix had to preserve."
  signal_3_diff_shape: "PASS — not a deletion-only or assertion-weakening diff; a control-flow
    correction with documented rationale. No test was modified or relaxed."
  signal_4_revert_test: "PASS — reverting the fix reintroduces 5 bug-specific failures; restoring
    it clears them."
  signal_5_corpus_differential: "PASS — 127 -> 50 corpus failures; 0 newly broken, 77 newly fixed."
  oracle_type: "specified (Vietnamese Telex input rules)"
  guardrail_verdict: accepted
files_changed:
  - "app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt (applyCharModifiers: else -> fall-through)"
  - "app/src/test/java/io/github/oin/titanpocketkeyboard/DenEeCircumflexRegressionTest.kt (new, 8 tests)"
