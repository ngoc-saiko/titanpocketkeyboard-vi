---
status: testing
phase: 03-bug-fixes-and-cleanup
source: [03-VERIFICATION.md]
started: 2026-08-18T07:50:00Z
updated: 2026-08-18T07:50:00Z
---

## Current Test

number: 1
name: FIX-02 — Alt one-shot clears on non-transforming keys in Vietnamese mode
expected: |
  In Vietnamese mode, pressing Alt (one-shot) then any key — including a
  non-Telex/non-transforming character such as a number or punctuation — consumes
  the Alt modifier so it does not leak onto the following key.
awaiting: user response

## Tests

### 1. FIX-02 — Alt one-shot clears on non-transforming keys in Vietnamese mode
expected: |
  In Vietnamese mode: enable Alt one-shot (single tap), then press a non-transforming
  key (e.g. '1', '.', 'q'). The Alt modifier must be consumed — the key after it must
  NOT receive Alt. Verify by typing a sequence and confirming no stale modifier leaks.
result: [pending]

### 2. FIX-05 — deleteLength avoids over-delete with live InputConnection
expected: |
  In Vietnamese mode, type a multi-character Telex replacement (e.g. "ơ" from "o+w").
  Press Backspace. Verify: exactly the composed character is deleted, no raw Telex
  characters remain visible, and the cursor is in the correct position. Also test at
  start-of-field (no preceding space) — backspace must not over-delete.
result: [pending]

### 3. FIX-06 — SpeechRecognizer null-safety through destroy/reinit lifecycle
expected: |
  Start dictation, then immediately cancel or navigate away mid-session to trigger
  SpeechRecognizer destruction. Resume typing. The app must not crash (no NPE on
  speechRecognizer access). Repeat 2–3 times to exercise the reinit path.
result: [pending]

### 4. FIX-07 — Identical permission flow from both FUNCTION and SYM+F triggers
expected: |
  On a device with microphone permission revoked: trigger speech from FUNCTION key —
  confirm permission dialog appears. Deny it, then trigger from SYM+F — confirm the
  same dialog appears (not a crash or silent failure). Both paths must reach the same
  ensureMicPermission() helper.
result: [pending]

## Summary

total: 4
passed: 0
issues: 0
pending: 4
skipped: 0
blocked: 0

## Gaps
