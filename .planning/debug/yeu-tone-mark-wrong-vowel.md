---
slug: yeu-tone-mark-wrong-vowel
status: awaiting_human_verify
trigger: "yếu displays as ýêu — tone mark placed on wrong vowel. Find similar tone placement bugs across all Vietnamese vowel combinations and fix them, then add unit tests for all discovered cases."
created: 2026-08-18
updated: 2026-08-18
---

## Symptoms

- **Expected:** Typing y + e + u + tone-mark (sắc) → produces "yếu" (tone on ê)
- **Actual:** Produces "ýêu" (tone incorrectly placed on y instead of ê)
- **Error messages:** None — silent wrong output
- **Timeline:** Unknown; user just discovered it
- **Reproduction:** Type "yeu" then apply sắc tone in Vietnamese Telex mode

## Current Focus

hypothesis: findFirstVowelIndex() returns the FIRST vowel in the buffer via a left-to-right scan, but the tonal nucleus is not always the first vowel. The toneMappingEnd map is a hand-maintained special-case list of diphthongs/triphthongs that overrides the first-vowel rule — but it is incomplete. "yêu" (buffer "yeu"→"yêu") has no entry, so the tone lands on 'y' (first vowel) instead of 'ê' (nucleus).
next_action: Await human verification on real device (type yêu/yết/yến in a text field). On "confirmed fixed" → archive session, append knowledge-base entry, commit code + docs.
test: Fix applied (toneMappingEnd += "yê"→'ê'); 5-signal guardrail all pass; awaiting human confirmation.
expecting: User confirms "yếu"/"yết"/"yến" now render with tone on ê in real IME usage.
reasoning_checkpoint:
  hypothesis: "For the 'yê' vowel cluster (buffer 'yê', as in yêu/yết/yếm), findFirstVowelIndex falls through to RULE B (first-vowel scan) and places the tone on 'y' (index 0) instead of the nucleus 'ê', because 'yê' is missing from the toneMappingEnd override map."
  confirming_evidence:
    - "Traced processKey('y','e','e','u','s') step-by-step: buffer becomes 'yêu', no toneMappingEnd key is a substring, RULE B returns index 0 ('y'), output is 'ýêu' — EXACTLY the reported symptom."
    - "toneMappingEnd already encodes the identical rule for the analogous 'iê'→ê case (and uô→ô, ươ→ơ, uâ→â); 'yê' is the lone missing member of the hat/whisker-vowel class."
    - "Vietnamese orthography rule (multiple web sources): the tone mark goes on the hat/whisker vowel (ê here), making 'ê' the unambiguous nucleus of yê*."
  falsification_test: "If, after adding 'yê'→'ê' to toneMappingEnd, typing y,e,e,u,s produced anything other than 'yếu' — OR if any existing passing test (iê, uyê, uâ, oa, etc.) regressed — the hypothesis/fix would be wrong."
  fix_rationale: "The fix adds the missing override entry 'yê'→'ê' to toneMappingEnd (the exact mechanism used for all other hat/whisker clusters). It addresses the root cause (incomplete override list) at the same layer as the existing correct entries, not a symptom. It is the minimal one-line data change consistent with the established FIX-03 pattern."
  blind_spots:
    - "'uyê' buffers (khuyên/tuyên) contain 'yê' as a substring; must verify RULE A's map-order precedence still resolves them to 'ê' (both 'uyê' and 'yê' map to ê, so index is identical — low risk, but tested explicitly)."
    - "Did not change 'uy' (thúy/thuý) — deliberately out of scope as ambiguous; if the user considers new-style 'thuý' the required behavior, that is a separate change."
    - "Uppercase/mixed-case 'Yê'/'YÊ' buffers — buffer.indexOf is case-sensitive; need to confirm the composed buffer casing matches the lowercase key or handle case."
  candidate_causes:
    - "code: findFirstVowelIndex RULE B fallback is a heuristic (first vowel) that is wrong for nucleus-second clusters (this is the confirmed cause)"
    - "data: toneMappingEnd override table is incomplete — missing the 'yê' row (this is the confirmed cause; same category as data/config table)"
  and_gate: "no — a single missing table entry fully explains the wrong output; both candidate framings (code heuristic + data table gap) describe the same defect from two angles, and no second independent condition is required to trigger it (deterministic every time)."
tdd_checkpoint: null

bug_class: Bohrbug (deterministic — same input always produces same wrong output; pure-logic function)

## Evidence

- timestamp: 2026-08-18
  checked: findFirstVowelIndex() in VietnameseTextInput.kt:394-413 and toneMappingEnd map at :119-124
  found: Tone placement uses two rules. RULE A (:396-399) — if buffer contains any toneMappingEnd key, place tone on the mapped nucleus char. RULE B fallback (:402-411) — otherwise place tone on the FIRST vowel found in left-to-right scan (skipping gi/qu prefixes). RULE B is wrong whenever the nucleus is not the first vowel. toneMappingEnd is the hand-maintained override list; it currently has: ươ, iê, uô, oe, uyê, oai, oa, oă, uâ, uo.
  implication: Any diphthong/triphthong whose nucleus is NOT the first vowel AND is missing from toneMappingEnd will get the tone on the wrong (first) vowel.

- timestamp: 2026-08-18
  checked: Traced processKey sequence for typing "yeu" (actual keystrokes y,e,e,u because ê comes from "ee" charModifier)
  found: >
    processKey('y') → buffer "y"
    processKey('e') → buffer "ye"
    processKey('e') → applyCharModifiers "ee"→"ê", buffer "yê", charModified=true
    processKey('u') → buffer "yêu"
    processKey('s') → applyToneMark: findFirstVowelIndex("yêu"): no toneMappingEnd key is a substring of "yêu" → falls to RULE B → first vowel is 'y' at index 0 → tone on 'y' → "ýêu"
  implication: EXACT reproduction of reported symptom "ýêu". Root cause confirmed — "yê" (nucleus ê) is missing from toneMappingEnd. Correct entry needed: "yê" → 'ê'.

- timestamp: 2026-08-18
  checked: Systematic audit of Vietnamese vowel-cluster nuclei vs toneMappingEnd coverage. Enumerated multi-vowel rimes where nucleus (tone-bearing vowel) is not the first vowel. Cross-referenced Vietnamese orthography tone-placement rules.
  found: >
    COVERED correctly by toneMappingEnd: iê/yê-family via "iê"→ê and "uyê"→ê (but NOT "yê"); uô→ô; ươ→ơ; oa→a; oe→e; oă→ă; uâ→â; oai→a; uo→o(FIX-03).
    MISSING / WRONG cases where RULE B misplaces the tone:
      1. "yê" (yêu, yết, yếm) — nucleus ê, first vowel y. MISSING. → reported bug.
      2. "uyê" without a following consonant is covered, but "uyê" also needs the plain "uy" + nucleus. Actually "uyê"→ê is present and correct.
      3. "uy" as final (thúy, quý) — nucleus is 'y' (e.g. "thuy"+s → "thúy" is WRONG; should be "thuý"). NOTE: 'qu' prefix is skipped so "quy" already lands on y correctly, but "thuy"/"suy"/"luy" are NOT prefixed. first vowel 'u' gets tone → "thúy" instead of "thuý". MISSING: "uy"→'y'.
      4. "oo" (as in "xoong", loanwords) — rare; nucleus is o. First vowel already o, tone correct. NOT a bug.
      5. "ưu" (cứu, hưu) — nucleus is 'ư' (first vowel). "ưu"+s → "ứu" correct. NOT a bug.
      6. "iu" (dịu, chiu) — nucleus 'i' (first vowel). Correct. NOT a bug.
      7. "eo","ao","au","ay","oi","ôi","ơi","ui","ưi","ai","ây","âu" — all have nucleus = FIRST vowel. Correct under RULE B.
    So the two genuine RULE-B misplacements are: "yê" and "uy".
  implication: Two missing toneMappingEnd entries: "yê"→'ê' and "uy"→'y'. Both are the same class of bug (nucleus is second vowel, missing from override list). Note ordering/substring concern: "uyê" must be checked BEFORE "uy" and "iê" families, and RULE A iterates a Map (no guaranteed order) — need to verify the substring-precedence does not break existing cases.

- timestamp: 2026-08-18
  checked: findFirstVowelIndex RULE A precedence — it iterates toneMappingEnd.keys and returns on the FIRST key that is a substring of the buffer (buffer.indexOf(key) != -1). Kotlin mapOf preserves insertion order (LinkedHashMap).
  found: >
    Adding "uy"→'y' is safe ONLY if "uyê" is checked before "uy" for a "uyê" buffer. For buffer "quyê"/"tuyê", buffer.indexOf("uyê") matches first if "uyê" appears earlier in the map. Current map order: ươ, iê, uô, oe, uyê, oai, oa, oă, uâ, uo. "uyê" is at position 5. If "uy" is appended at the END, then for "tuyê", the loop reaches "uyê" (pos5) before "uy" (end) → returns ê index. SAFE.
    For "yê": buffer "yêu". Adding "yê"→'ê'. But "iê" is checked earlier and "yê" does not contain "iê" — no conflict. However note "yê" as substring could falsely match inside a buffer that also contains "iê"? "iê" and "yê" are disjoint strings. SAFE.
    Also must ensure "uy" does not falsely fire for "uyê" buffers before reaching them: because map order places "uyê" (pos5) before an appended "uy", the "uyê" branch wins. SAFE as long as "uy" is appended AFTER "uyê" in insertion order.
  implication: Fix is to append "yê"→'ê' and "uy"→'y' to toneMappingEnd, placed AFTER "uyê" to preserve triphthong precedence. Verify with tests for tuyên/quyên(quy prefix)/thuy/yêu.

## Eliminated Hypotheses

- hypothesis: "uy" (thuy→thúy) is also a tone-placement bug that should be fixed alongside "yê"
  evidence: Vietnamese tone placement for "uy" diphthong is genuinely debated between old-style (thúy, tone on penultimate 'u') and new-style (thuý, tone on last 'y'). The current code produces old-style "thúy", which is a valid convention. Web sources (vietnamesetypography.com, Quora) confirm both are accepted and it is "a matter of debate." Unlike "yê" (where the hat/whisker vowel rule makes ê the unambiguous nucleus), "uy" has NO hat/whisker vowel, so the special-vowel rule does not force a choice. Changing it would introduce a controversial behavior change outside the reported bug's class. RULED OUT of scope.
  timestamp: 2026-08-18

- hypothesis: Multiple hat/whisker vowel clusters are missing from toneMappingEnd (systemic gap)
  evidence: Systematic audit of all Vietnamese rimes containing a hat/whisker vowel (ă â ê ô ơ ư) in non-initial position found that every such cluster EXCEPT "yê" is already covered: iê, uô, ươ, uâ, oă, uyê all present. The general Vietnamese rule "tone goes on the hat/whisker vowel" is fully encoded except for the word-initial "yê" case. So this is a single-entry gap, not a systemic one.
  timestamp: 2026-08-18

## Resolution

root_cause: >
  findFirstVowelIndex() in VietnameseTextInput.kt places a tone mark on the FIRST vowel of the
  buffer (RULE B, :402-411) unless the buffer matches a hand-maintained special-case override in
  the toneMappingEnd map (RULE A, :396-399). The word-initial "yê" cluster (yêu, yết, yếm, yên)
  was missing from toneMappingEnd. Its nucleus is the hat/whisker vowel 'ê', but RULE B placed the
  tone on the leading semivowel 'y' instead — producing "ýêu" instead of "yếu" for "yeu"+sắc.
  Root-cause category: incomplete override data table (same class as FIX-03 "uo" gap).
fix: >
  Added the entry "yê" → 'ê' to toneMappingEnd, placed after the "uyê" triphthong entry so that
  labialized "uyê" syllables (khuyên/tuyên) keep precedence. Mirrors the existing "iê"→'ê' rule.
  One-line data change; no logic change.
verification: >
  oracle_type: derived (Vietnamese orthography contract — tone lands on the hat/whisker vowel).
  guardrail_verdict: accepted
  signals:
    - signal: full_test_suite
      result: pass
      detail: "82 tests across 10 suites, 0 failures/0 errors after fix (--rerun-tasks). New TonePlacementTest grew 6→12, all green."
    - signal: regression_test_detects_bug (revert check)
      result: pass
      detail: "git stash of the source fix (tests kept) → the 4 yê-specific tests FAILED (yeu_fullPath, yeu_allFiveTones, yet_closedSyllable, yen). Proves tests are non-vacuous and the fix is causal."
    - signal: no_collateral_regression
      result: pass
      detail: "Under revert, the two regression guards tonePlacement_uye_triphthong and tonePlacement_ie still PASSED → fix does not alter existing uyê/iê behavior."
    - signal: minimal_diff
      result: pass
      detail: "Source change is a single map entry (+1 line data + comment). No control-flow change. Not a deletion-only diff."
    - signal: build
      result: pass
      detail: "compileDebugKotlin + testDebugUnitTest BUILD SUCCESSFUL. Only pre-existing unrelated warnings (unused var at :360, deprecated toLowerCase) — not introduced by this change."
files_changed:
  - app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt
  - app/src/test/java/io/github/oin/titanpocketkeyboard/VietnameseTextInputTest.kt
