# TitanPocketKeyboard Vietnamese IME

## What This Is

A custom Android Input Method (IME) for the Unihertz Titan Pocket hardware keyboard, adding Vietnamese language support via Telex/multipress input. The app intercepts hardware key events and transforms them into properly composed Vietnamese characters with tone marks and diacritics.

## Core Value

Vietnamese input must be accurate and predictable — every key sequence should produce the correct character, every time, on any input field.

## Requirements

### Validated

- ✓ Hardware key interception via Android InputMethodService — existing
- ✓ Vietnamese Telex input (tone marks: s, f, r, x, j; vowel modifiers: a, e, o, w, d) — existing
- ✓ Multipress controller for accented character selection — existing
- ✓ Shift/Alt modifier keys with hold, single-tap, and lock modes — existing
- ✓ Audio feedback (key sounds via ToneGenerator) — existing
- ✓ Haptic feedback (vibration on keypress) — existing
- ✓ Voice input via Google Speech Recognition (vi-VN) — existing
- ✓ Settings UI (SettingsActivity + SharedPreferences) — existing
- ✓ English mode switching — existing

### Active

- [ ] Fix all issues documented in `.planning/codebase/CONCERNS.md` (tech debt, known bugs, security)
- [ ] Fix Alt key sticky modifier: in Vietnamese mode, alt should work as one-shot (press, release, press another key) on all input fields, matching English mode behavior
- [ ] Fix Telex w-revert: `o + w = ơ`, but `ơ + w` should produce `ow` (not `ơw`)
- [ ] Fix tone mark placement: `reverseTone()` applies mark to first vowel, not the correct syllable position
- [ ] Add unit tests covering Vietnamese Telex input composition logic (`VietnameseTextInput`)
- [ ] Identify and fix performance bottlenecks (profile-guided — no known hotspot yet)

### Out of Scope

- New language support beyond Vietnamese — not planned for this milestone
- Visual/on-screen keyboard UI — hardware keyboard only
- Cloud sync or account features — local IME only
- iOS/cross-platform port — Android only

## Context

- **Hardware target**: Unihertz Titan Pocket (physical QWERTY keyboard, small form factor)
- **Architecture**: Event-driven single-threaded Android IME; key processing chain is `InputMethodService → MultipressController → VietnameseTextInput → Modifier`
- **Known fragile areas**: `VietnameseTextInput.kt` (tone mark logic, invalid sequence blacklist), `MultipressController.kt` (hardcoded consonant classification), `Modifier.kt` (one-shot modifier state)
- **Test infrastructure exists** (JUnit 4 + Espresso configured) but has no tests written yet
- **Recent bug fixes**: alt+del deleting whole line, quây and oà tone issues — pattern of Telex edge cases surfacing in production

## Constraints

- **Tech stack**: Kotlin + Android Framework — no third-party text processing libraries
- **API level**: minSdk 29, targetSdk 33 — no modern API shortcuts for older devices
- **No test environment for hardware key events**: Espresso/instrumented tests cannot easily simulate physical keyboard events; unit tests must target pure logic classes

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Fix CONCERNS before new features | Known bugs compound; unit tests on broken logic are misleading | — Pending |
| Unit test `VietnameseTextInput` first | Core composition logic is highest risk, has zero coverage | — Pending |
| Profile before optimizing performance | No known hotspot; avoid premature optimization | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-08-18 after initialization*
