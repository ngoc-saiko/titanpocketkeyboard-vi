# Feature Landscape: Vietnamese Telex IME (Unihertz Titan Pocket)

**Domain:** Android hardware-keyboard IME — Vietnamese Telex input
**Researched:** 2026-08-18
**Overall confidence:** HIGH (spec cross-checked against Wikipedia Telex article, icanreadvietnamese.com tone placement rules, UniKey/ibus-unikey engine documentation, and source code inspection)

---

## Table Stakes

Features users expect. Missing = product feels broken or users abandon for a software keyboard.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Correct vowel modifier forward transforms | Core of Telex: `aa→â`, `ee→ê`, `oo→ô`, `aw→ă`, `ow→ơ`, `uw→ư`, `dd→đ` | Low | Already implemented; verify all cases covered |
| Correct tone mark forward transforms | `s→sắc`, `f→huyền`, `r→hỏi`, `x→ngã`, `j→nặng` on current vowel | Low | Already implemented |
| Vowel modifier revert (double-key escape) | `â+a→aa`, `ô+o→oo`, `ơ+w→ow` — typing the trigger again must undo the substitution | Low-Med | **KNOWN BUG**: `ơ+w` currently produces `ơw` not `ow`; `reverseCharModifier` map has the rule but the code path does not reach it correctly |
| Tone mark revert (same-key double-tap) | `á+s→as` — typing the same tone key again removes the tone | Low-Med | Partially implemented via `reverseTone()`; edge cases around `isReverseTone` flag may be unreliable |
| Correct tone mark placement — single vowel | Tone goes on the only vowel present | Low | Works when `findFirstVowelIndex()` returns correct index |
| Correct tone mark placement — vowel with existing quality diacritic | When a vowel already carries `^` or `horn` (ă â ê ô ơ ư), that vowel gets the tone mark regardless of position | Med | Partially handled via `toneMappingEnd`; rule "special diacritic wins" is the highest-priority placement rule per Vietnamese Typography standard |
| Correct tone mark placement — multi-vowel syllable | Diphthongs/triphthongs: `ươ→ơ gets mark`, `iê→ê gets mark`, `uô→ô gets mark`, `oa→a gets mark`, `oan→a gets mark` (closed syllable uses 2nd vowel) | High | **KNOWN BUG**: `findFirstVowelIndex()` finds FIRST vowel in buffer, not the phonologically correct nucleus; `toneMappingEnd` addresses a subset but misses the general open/closed syllable rule |
| Skip Telex in password/email/URI fields | Users must be able to type passwords with literal `s`, `f`, `ow`, etc. without Telex transformation | Low | Already guarded by `canUseSuggestions()` check |
| Alt key one-shot mode in Vietnamese mode | Press Alt, release, press next key → that key gets Alt metaState; same behavior as in English mode | Med | **KNOWN BUG**: Vietnamese mode condition at line 435 (`if (vietnameseMode && !event.isCtrlPressed && !sym.get() && telexOn)`) processes the key via Telex before `consumeModifierNext()` is called, but `alt.get()` is not checked so Alt+key is Telex-processed as a plain key |
| Backspace corrects Telex buffer | Backspace within a composing word removes last character and re-emits buffer; user can fix typos mid-word | Med | Implemented via `processKey('\b')` path; verify buffer-based delete length matches multi-byte Vietnamese characters |
| Buffer reset on word boundary | Telex state clears on space, enter, punctuation, or focus change so next word starts clean | Low | Implemented in `onUpdateSelection` and `onKeyDown` Enter handler; verify space key path |
| Shift+key produces uppercase Vietnamese | `Shift+a+w` → `Ă` (uppercase); tone marks apply to uppercase vowels | Low | Handled via `enhancedMetaState()` + uppercase variants in all maps; verify `wCharModifiers` uppercase paths |
| English mode bypass | When not in Vietnamese mode, all Telex keys pass through unmodified | Low | Already implemented via `vietnameseMode` flag |

---

## Tone Mark Placement — Authoritative Rules

This section codifies the correct algorithm so bugs can be identified precisely.

**Rule priority order (highest wins):**

1. If a vowel in the buffer already has a quality diacritic (ă â ê ô ơ ư), that vowel receives the tone mark. Example: `thuở` — ở gets the mark because ơ has the horn.
2. If the sequence matches a known diphthong/triphthong with a canonical nucleus:
   - `ươ` → mark goes on `ơ` (second)
   - `iê` / `yê` / `uyê` → mark goes on `ê` (second)
   - `uô` → mark goes on `ô` (second)
   - `oe` / `oai` / `oa` / `oă` / `uâ` → mark goes on `a`/`ă`/`â`/`e` (the non-`o`/`u` vowel)
3. If the syllable is closed (ends in a consonant), and contains two bare vowels, mark the second vowel (e.g., `phiến` → `ê`).
4. If the syllable is open (no final consonant), mark the first vowel.
5. Skip onset vowels that are part of compound consonant onsets: `gi-` treats `i` as onset (not vowel nucleus unless syllable is just `gi`), `qu-` treats `u` as onset.

**Current implementation status:** `toneMappingEnd` encodes rules for `ươ`, `iê`, `uô`, `oe`, `uyê`, `oai`, `oa`, `oă`, `uâ` — covering the most common diphthong cases. The general open/closed syllable rule (rule 3/4 above) is NOT implemented; `findFirstVowelIndex()` always returns the first vowel after onset skipping. This is the root cause of the known tone mark bug.

---

## Revert Sequence Rules — Authoritative Specification

**Vowel modifier revert:** Typing the exact same modifier key again after a substitution undoes it and outputs the two literal characters.
- `o → o`, then `w → ow, buffer becomes ơ`, then `w again → ow` (revert: ơ+w → ow)
- `a → a`, then `a → aa, buffer becomes â`, then `a → aaa? No: â+a → aa` (revert)
- Implementation: `reverseCharModifier` map contains these rules. Bug: for the `ow→ơ` case the `charModified` flag is not set (because `char == 'w'` is excluded at line 232-234 in `applyCharModifiers`), so the revert branch (`if (charModified)`) is never taken.

**Tone mark revert:** Typing a different tone key replaces the existing tone. Typing the same tone key again when no vowel is available appends the literal key character.
- `a+s → á`, then `s → as` (same-key revert, `toneAdded` reset)
- `a+s → á`, then `f → àf? No: à` (different tone replaces previous)

**Escape from Telex:** Standard Telex implementations offer `z` to suppress transformation or backslash as escape. This codebase does not implement `z`-as-escape (it currently lists `z` in `ignoredChars`). Not a bug — it is a deliberate simplification appropriate for hardware-keyboard context where typing speed is lower.

---

## Differentiators

Features that set this product apart from software-keyboard Vietnamese apps. Not expected by all users, but valued.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Hardware keyboard physical feel | No on-screen keyboard needed; typing on Titan Pocket QWERTY is the point | N/A | Core product identity, not a software feature |
| Alt-key one-shot modifier (all modes) | Typing `Alt+key` one-handed after releasing Alt is ergonomic on small form factor | Med | Already implemented in Modifier.kt; only broken specifically in Vietnamese mode |
| Shift+Space Vietnamese/English toggle | Instant mode switch without leaving current app | Low | Already implemented; preserve and test |
| Voice input (vi-VN) | Speak and get Vietnamese text without typing complex Telex sequences | High | Already implemented; fragile lifecycle; lower priority than accuracy bugs |
| User-defined text shortcuts | Type abbreviation + Enter to expand to full text | Med | Implemented via SharedPreferences; no UI polish needed for this milestone |
| Multipress character selection | Long-press for accented variants | Med | Already implemented via MultipressController |
| Auto-capitalization after sentence end | Less friction for longer text | Low | Already implemented |
| Haptic + audio feedback | Physical confirmation of keypress on small device | Low | Already implemented |

---

## Anti-Features

Features to deliberately NOT build in this milestone.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Autocomplete / word suggestions | Requires dictionary, candidate UI, and significant new code surface; out of scope per PROJECT.md | Keep IME suggestions off; focus on correctness of what IS typed |
| Autocorrect | Same reason; also risks corrupting correct Telex sequences | Not planned |
| Cloud sync of shortcuts | Adds authentication, network permissions, privacy surface; PROJECT.md explicitly excludes | Local SharedPreferences is sufficient |
| On-screen soft keyboard | Hardware keyboard only; Titan Pocket has no touchscreen keyboard need | Not planned |
| VNI or VIQR input methods | Adds complexity; Telex is the dominant method for this device context | Telex only |
| New language support | Out of scope per PROJECT.md | Not planned |
| Phonetic validation (reject non-Vietnamese sequences) | The `invalidSequences` blacklist is already tech debt; expanding it makes things worse | Fix the three known bugs first; validation is a future refactor |
| iOS / cross-platform port | Android InputMethodService architecture is Android-only | Not planned |

---

## Feature Dependencies

```
Correct buffer tracking
    → Vowel modifier forward transforms work
        → Vowel modifier revert (reverseCharModifier) works
            → w-revert bug fix (charModified flag must be set for 'w' transforms)

Correct buffer tracking
    → Tone mark forward transforms work
        → Tone mark placement (findFirstVowelIndex must return correct nucleus)
            → Multi-vowel tone mark fix (open/closed syllable rule or diphthong table expansion)

Modifier.kt one-shot state (next flag)
    → Alt one-shot works in English mode (already working)
        → Alt one-shot works in Vietnamese mode (bug: Telex path does not check alt.get())
            → Alt one-shot fix (skip Telex processing when alt.get() is true OR pass metaState correctly)

Unit tests on VietnameseTextInput
    → Safe to refactor tone mark placement
    → Safe to fix charModified flag for 'w' transforms
    → Regressions detected before they reach users
```

---

## MVP Recommendation for This Milestone

The milestone goal is "fix bugs, add unit tests, improve performance." Priority order:

1. **Unit tests for VietnameseTextInput** — must come first so subsequent fixes can be verified and regressions caught. Cover: all forward transforms, all revert sequences, tone mark placement for single-vowel and diphthong cases, backspace behavior, buffer reset.

2. **Fix w-revert bug** — set `charModified = true` for `ow→ơ` and `uw→ư` transforms (currently excluded by `char != 'w'` guard); confirm `reverseCharModifier` entries are reached.

3. **Fix tone mark placement bug** — `findFirstVowelIndex()` must return the phonological nucleus, not the first buffer vowel. The `toneMappingEnd` lookup is the right architecture; it needs to be complete and to prioritize quality-diacritic vowels over bare vowels.

4. **Fix Alt one-shot in Vietnamese mode** — guard the Telex processing block with `!alt.get()` or route Alt+key through the normal path before reaching the `if (vietnameseMode && ...)` block.

5. **Performance profiling** — profile after correctness fixes; do not optimize before tests exist.

Defer: autocomplete, VNI support, cloud sync, any new UI.

---

## Sources

- [Telex (input method) — Wikipedia](https://en.wikipedia.org/wiki/Telex_(input_method))
- [Rules of Tone Mark Placement — icanreadvietnamese.com](https://icanreadvietnamese.com/blog/14-rule-of-tone-mark-placement)
- [Tone Marks — Vietnamese Typography](https://vietnamesetypography.com/tone-marks/)
- [Common Vietnamese Input Methods — VietUnicode](https://vietunicode.sourceforge.net/inputmethod.html)
- [Core Engine Processing Pipeline — Gõ Nhanh](https://mintlify.wiki/khaphanspace/gonhanh.org/technical/core-engine) — confirms double-key revert state machine design
- [ibus-unikey source — GitHub](https://github.com/vn-input/ibus-unikey) — reference implementation
- Source code inspection: `VietnameseTextInput.kt`, `InputMethodService.kt`, `Modifier.kt` (2026-08-18)
