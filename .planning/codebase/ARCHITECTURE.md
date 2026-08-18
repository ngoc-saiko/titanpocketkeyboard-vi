<!-- refreshed: 2026-08-18 -->
# Architecture

**Analysis Date:** 2026-08-18

## System Overview

```text
┌─────────────────────────────────────────────────────────────┐
│                    Android Input Method Service             │
│              InputMethodService (Entry Point)               │
│           `app/src/main/java/.../InputMethodService.kt`     │
└────────────┬────────────────┬──────────────────┬────────────┘
             │                │                  │
             ▼                ▼                  ▼
┌──────────────────────┬──────────────────┬─────────────────┐
│  Text Composition    │ Key Processing   │ Audio/Feedback  │
│  VietnameseTextInput │ MultipressCtrlr  │ Vibrator/Tones  │
│  `VietnameseText...` │ `Multipress...`  │ (AndroidAPIs)   │
└──────────────────────┴──────────────────┴─────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────┐
│              Modifier State Management                       │
│   Shift (Modifier), Alt (Modifier), Sym (SimpleModifier)    │
│              `app/src/main/java/.../Modifier.kt`            │
└─────────────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────┐
│              Settings & Preferences                          │
│  SettingsActivity / SharedPreferences configuration         │
│     `app/src/main/java/.../SettingsActivity.kt`             │
└─────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| InputMethodService | Main entry point; intercepts key events from hardware; manages keyboard state and user interaction | `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt` |
| VietnameseTextInput | Handles Vietnamese text composition; tone mark application; character modification (ă, â, ô, ơ, ư, đ) | `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt` |
| MultipressController | Multi-level multipress handling; accented character substitution based on templates; tone and diacritical mark selection | `app/src/main/java/io/github/oin/titanpocketkeyboard/MultipressController.kt` |
| Modifier | Virtual modifier key (Shift/Alt); supports hold, single-tap-for-next, and lock-by-double-tap modes | `app/src/main/java/io/github/oin/titanpocketkeyboard/Modifier.kt` |
| SimpleModifier | Simplified modifier (Sym key); supports hold and lock modes | `app/src/main/java/io/github/oin/titanpocketkeyboard/Modifier.kt` |
| SettingsActivity | User-facing settings UI; preference management; speech recognition permission handling | `app/src/main/java/io/github/oin/titanpocketkeyboard/SettingsActivity.kt` |

## Pattern Overview

**Overall:** Event-driven architecture with state management for multi-layer input processing.

**Key Characteristics:**
- Single-threaded Android Input Method Service with event callbacks
- Hardware keyboard interception through KeyEvent processing
- Stateful modifier and text composition tracking
- Template-driven character substitution system
- Configuration through Android SharedPreferences

## Layers

**Input Layer:**
- Purpose: Intercept hardware key events from Unihertz Titan Pocket keyboard
- Location: `InputMethodService.kt` - `onKeyDown()`, `onKeyUp()` methods
- Contains: Android lifecycle hooks, KeyEvent processing, speech recognition initialization
- Depends on: Android frameworks (InputMethodService, KeyEvent, SpeechRecognizer)
- Used by: Android system; receives hardware key events

**Processing Layer:**
- Purpose: Transform raw key events into text and composed characters
- Location: Multiple: `MultipressController.kt`, `VietnameseTextInput.kt`, `Modifier.kt`
- Contains: Character substitution logic, tone mark application, modifier state tracking
- Depends on: Key templates (defined in InputMethodService), modifier state
- Used by: InputMethodService to determine what text to commit

**Output Layer:**
- Purpose: Commit final text to the active text field
- Location: `InputMethodService.kt` - uses Android InputConnection
- Contains: Commitments to input field via `currentInputConnection?.commitText()`
- Depends on: Android InputConnection API
- Used by: Active text editor receiving the composed text

**Configuration Layer:**
- Purpose: Store and manage user preferences for keyboard behavior
- Location: `SettingsActivity.kt`, Android SharedPreferences
- Contains: Template selection, timing thresholds, feature toggles
- Depends on: Android SharedPreferences, Preference XML
- Used by: InputMethodService to read user settings at startup

## Data Flow

### Primary Request Path (Key Press to Text Output)

1. Hardware key press triggers `onKeyDown(KeyEvent)` in `InputMethodService` (`InputMethodService.kt:500+`)
2. Modifier state updated: Shift/Alt/Sym key presses update respective `Modifier` or `SimpleModifier` objects (`Modifier.kt`)
3. Multipress processing: Key is passed to `MultipressController.process()` which consults templates (`MultipressController.kt:88-173`)
4. Character substitution: Special codes (MPSUBST_*) are evaluated; dead keys and accents handled (`MultipressController.kt:118-143`)
5. Vietnamese text composition: If Vietnamese mode enabled, `VietnameseTextInput` applies tone marks based on vowel position (`VietnameseTextInput.kt:200+`)
6. Text commitment: Final character/string committed via `currentInputConnection?.commitText()` (`InputMethodService.kt:750+`)
7. Audio feedback: Optional vibration via `Vibrator` service and tone generation via `ToneGenerator` (`InputMethodService.kt:800+`)

### Vietnamese Composition Flow

1. Input character received (e.g., 'a')
2. `VietnameseTextInput.addChar()` called with buffer context (`VietnameseTextInput.kt:150+`)
3. Character checked against vowel set (`VietnameseTextInput.kt:10-31`)
4. Modifiers applied: 'w' for ă/ơ/ư, double vowel for â/ê/ô (`VietnameseTextInput.kt:76-98`)
5. Tone marks applied based on key presses (s/f/r/x/j) (`VietnameseTextInput.kt:72-74`)
6. Tone placement determined by vowel position (complex logic for multi-vowel handling) (`VietnameseTextInput.kt:200+`)
7. Result: Fully composed Vietnamese character (e.g., 'ắ')

**State Management:**
- `InputMethodService` holds: modifier states, multipress state, speech recognizer state, Vietnamese composition state
- Preferences: Stored in Android SharedPreferences; loaded at service startup via `updateFromPreferences()` (`InputMethodService.kt:700`)
- Session state: Reset on each text field focus change

## Key Abstractions

**Modifier Pattern:**
- Purpose: Virtual keyboard modifier that can be held, single-tapped-for-next-key, or locked via double-tap
- Examples: `Shift`, `Alt`, `Sym` modifiers in `InputMethodService.kt`
- Pattern: `Modifier` class with `onKeyDown()`, `onKeyUp()` state transitions and `get()` query method

**Multipress Substitution:**
- Purpose: Map a key+press-count to a character, with special codes for dead keys, modifiers, and mode changes
- Examples: Templates in `InputMethodService.kt` lines 63-132
- Pattern: `HashMap<Int, Array<Char>>` where index 0 = first-level, index 1+ = long-press levels; special constants (MPSUBST_*) for behavior

**Vietnamese Text Composition:**
- Purpose: Buffer Vietnamese text, apply character modifications and tone marks
- Examples: `VietnameseTextInput.addChar()`, `VietnameseTextInput.applyToneMark()`
- Pattern: StringBuilder + character/tone maps; tone placement logic based on vowel position

## Entry Points

**InputMethodService (Main Entry):**
- Location: `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt`
- Triggers: Android system creates instance when keyboard is selected; KeyEvents routed by OS
- Responsibilities: 
  - Manage service lifecycle (`onCreate()`, `onStartInputView()`)
  - Process hardware key events (`onKeyDown()`, `onKeyUp()`)
  - Manage speech recognition if enabled
  - Track and apply modifier states
  - Invoke multipress/Vietnamese composition
  - Commit text to input connection
  - Provide visual feedback (vibration, tones)

**SettingsActivity (Configuration Entry):**
- Location: `app/src/main/java/io/github/oin/titanpocketkeyboard/SettingsActivity.kt`
- Triggers: User navigates to Settings from launcher
- Responsibilities:
  - Display preference UI for keyboard configuration
  - Handle permission requests (RECORD_AUDIO for speech-to-text)
  - Persist settings to SharedPreferences
  - Support reset-to-defaults

## Architectural Constraints

- **Threading:** Single-threaded event loop. All key processing must be synchronous in `onKeyDown()`/`onKeyUp()`. Speech recognition runs in separate threads (Android API, not controlled here).
- **Global state:** `InputMethodService` instance is singleton per session. Modifier objects (`shift`, `alt`, `sym`) are instance members and persist across key presses. Vietnamese composition buffer (`vietnameseTelex`) is session-scoped.
- **Circular imports:** None detected. Dependency graph is acyclic: InputMethodService → MultipressController, VietnameseTextInput, Modifier.
- **Hardware coupling:** Tightly coupled to Unihertz Titan Pocket physical keyboard layout via KeyEvent key codes (KEYCODE_Q, KEYCODE_W, etc.). Key templates defined by hardware layout.
- **API level:** Minimum SDK 29 (Android 10); Target SDK 33. Uses AndroidX libraries for compatibility.
- **Input connection:** Dependency on active `InputConnection` to the focused text field. Some operations (e.g., text commit) fail silently if connection is null.

## Anti-Patterns

### Multi-responsibility in InputMethodService

**What happens:** `InputMethodService.kt` handles key routing, modifier state, Vietnamese composition, speech recognition, vibration feedback, preference loading, and text commitment—all in one ~800-line class.

**Why it's wrong:** Changes to any subsystem require editing a large, complex class. Testing individual features (e.g., tone mark logic) requires instantiating the entire service. Reuse of components (e.g., Vietnamese composition) in other contexts requires pulling in the entire service.

**Do this instead:** Split responsibilities further:
- Move Vietnamese composition to standalone class with pure functions/methods
- Extract preference loading to utility/manager
- Create feedback handler for vibration/tone logic
- Use dependency injection to wire components into InputMethodService

### Hardcoded Template Lists in InputMethodService

**What happens:** Character substitution templates (lines 63-132) are defined as a large HashMap literal in the class. Adding new language templates or modifying existing ones requires changing core service code.

**Why it's wrong:** Language templates are domain data, not control logic. Changes to templates should not require recompilation or touching the service class. Users cannot easily add custom templates.

**Do this instead:** Load templates from configuration file (JSON/XML). Define a `TemplateManager` that reads from app resources or user preferences. Keep InputMethodService focused on *using* templates, not defining them.

### Blocking Character Lookups in Composition

**What happens:** `VietnameseTextInput` uses large `setOf()` and `mapOf()` literals (lines 10-70) that are checked on every character addition. No optimization for large character sets.

**Why it's wrong:** On high-frequency input (fast typing), character set lookups add latency. Sets are appropriate, but should be pre-compiled or cached if performance becomes an issue.

**Do this instead:** Profile input latency. If measurable, convert character sets to compact integer ranges or bitsets. Cache frequently accessed mappings.

## Error Handling

**Strategy:** Permissive; failures are logged but do not stop processing.

**Patterns:**
- Null checks on `currentInputConnection` before committing text (fails silently if null)
- Try-catch around SpeechRecognizer initialization; if unavailable, feature is disabled
- Preference defaults ensure service continues if SharedPreferences missing or corrupted
- Log statements for debugging (via Android Log.d, Log.e) but no exception propagation to caller

## Cross-Cutting Concerns

**Logging:** Android Log class. DEBUG level used for state transitions (speech, modifier, composition). ERROR level for permission issues and speech recognition errors.

**Validation:** Input validation happens in:
- `InputMethodService.canUseSuggestions()` checks editor info for sensitive fields (password, email)
- `VietnameseTextInput.vowelMap` validates that characters are Vietnamese vowels before applying tones
- Preference loading validates seekbar values are within range; defaults on parse failure

**Authentication:** Permission-based. VIBRATE, RECORD_AUDIO, and INTERNET permissions declared in manifest. RECORD_AUDIO requested at runtime (SettingsActivity); service continues if denied (just disables speech-to-text).

---

*Architecture analysis: 2026-08-18*
