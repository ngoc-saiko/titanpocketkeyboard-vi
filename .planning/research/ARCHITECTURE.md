# Architecture Patterns for Android IME Unit Testing

**Domain:** Android Input Method Engine — Vietnamese text composition
**Researched:** 2026-08-18
**Confidence:** MEDIUM (code analysis HIGH; external keyboard project research LOW — limited public documentation on test internals)

---

## The Core Architectural Insight

The existing code has a natural, exploitable seam between pure logic and Android-coupled code. This seam is not theoretical — it is already present in the codebase today. The test strategy should be designed around it rather than working against it.

```
Pure Logic Tier (JVM-testable, no Android)      Android-Coupled Tier (needs framework)
----------------------------------------------  ----------------------------------------
VietnameseTextInput.kt                          InputMethodService.kt
  - zero Android API calls                        - InputConnection (commits text)
  - only: android.util.Log (removable)            - Android lifecycle callbacks
  - all Maps/Sets are stdlib                       - KeyEvent routing
  - StringBuilder state machine                   - SharedPreferences
                                                  - Vibrator / ToneGenerator
Modifier.kt / SimpleModifier.kt                   - SpeechRecognizer
  - only: System.currentTimeMillis()
  - pure state machine

MultipressController.kt (partial)
  - accepts KeyEvent but only reads:
    e.keyCode, e.repeatCount,
    e.getUnicodeChar(metaState), e.unicodeChar
  - these are readable from a real KeyEvent
    constructed with KeyEvent(action, code)
```

This means: **VietnameseTextInput and Modifier can be tested with zero mocking, zero Robolectric, zero instrumentation.** They are ordinary Kotlin classes. The only obstacle in VietnameseTextInput is one `import android.util.Log` call — it is not actually invoked in any live path in the file currently, so plain JUnit in `src/test/` works immediately.

---

## Component Boundaries

### Boundary 1: VietnameseTextInput (Pure Logic)

**What it owns:**
- All Vietnamese text composition state (`buffer: StringBuilder`)
- Tone mark application (`applyToneMark`, `reverseTone`)
- Character modification maps (`charModifiers`, `wCharModifiers`, `reverseCharModifier`)
- Vowel position detection (`findFirstVowelIndex`)
- Invalid sequence blacklist (`invalidSequences`)

**What it does NOT own:**
- When to call `processKey` (InputMethodService decides)
- How to commit the result to a text field (InputMethodService owns `InputConnection`)
- Whether Vietnamese mode is active (InputMethodService decides)

**Communicates with:**
- InputMethodService: receives `Char` in, returns `String?` out
- Nobody else

**Testability:** Direct instantiation. `VietnameseTextInput()` takes no constructor arguments. Feed `Char`, read `String?`. Assert. No mocks needed.

### Boundary 2: Modifier / SimpleModifier (Pure State Machine)

**What it owns:**
- Three-state modifier tracking: held, next (one-shot), lock
- Timing logic for double-press lock and long-press hold distinction
- `get()` query method

**What it does NOT own:**
- Which physical key triggers it
- How the modifier is used downstream

**Communicates with:**
- InputMethodService: receives `onKeyDown()` / `onKeyUp()`, returns `get()` boolean

**Testability:** Direct instantiation. The timing dependency (`System.currentTimeMillis()`) is the only complication. For logical state transitions that do not cross time thresholds, tests can call `onKeyDown()`/`onKeyUp()` in rapid succession and assert `get()`. For threshold-sensitive tests (lock behavior, next behavior), use `Thread.sleep()` sparingly or expose a clock seam (see Pitfalls section).

### Boundary 3: MultipressController (Partial Android Dependency)

**What it owns:**
- Multi-level long-press substitution logic
- Timing-based multipress window
- Substitution constant resolution (MPSUBST_*)

**What it does NOT own:**
- The substitution table (injected via constructor `substitutions`)
- When to call it (InputMethodService)

**Android coupling:** Accepts `KeyEvent` but only reads stable properties. `KeyEvent` can be constructed in JVM unit tests:

```kotlin
KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A)
// sufficient for .keyCode and .repeatCount
// .getUnicodeChar(metaState) returns 0 for minimal constructor — test with full constructor if needed
```

**Testability:** Partial — MPSUBST_BYPASS / MPSUBST_NOTHING / literal char returns are testable with real `KeyEvent` objects in `src/test/`. The special substitution constants that call `e.getUnicodeChar(metaState)` require a real `KeyEvent` with a proper metastate, which the `KeyEvent(long, long, int, int, int, int)` constructor supports. No mocking needed for these either.

### Boundary 4: InputMethodService (Android-Coupled, Not Unit-Testable)

**What it owns:**
- Key event routing
- Modifier orchestration
- Mode switching (English vs Vietnamese)
- Text commitment via `currentInputConnection`
- Lifecycle management
- Audio/haptic feedback

**Android coupling:** Extends `android.inputmethodservice.InputMethodService`. Requires Android runtime to instantiate. Cannot run on JVM without Robolectric.

**Testability:** Not directly unit-testable. Options in order of effort:
1. Robolectric with `ServiceController` (AnySoftKeyboard approach — HIGH complexity, HIGH value)
2. Extract an interface for `InputConnection` and mock it (MEDIUM complexity)
3. Acceptance test via Espresso on device (LOW fragility, SLOW feedback)

**Decision for this milestone:** Do not test InputMethodService directly. Focus unit tests on the three pure-logic classes. Integration bugs in InputMethodService surface through the existing production bug pattern (tone issues) which are all traceable to VietnameseTextInput logic.

---

## Data Flow

### Key Press to Text Output (Full Chain)

```
Hardware KeyEvent
       │
       ▼
InputMethodService.onKeyDown()
  ├── Modifier.onKeyDown()  ← state update, no output
  ├── MultipressController.process(e, metaState)
  │     └── returns: Char (substitution or MPSUBST_* constant)
  │
  ├── [if Vietnamese mode and char is printable]
  │     VietnameseTextInput.processKey(char)
  │       └── returns: String? (composed text, or null for backspace-to-empty)
  │
  └── currentInputConnection?.commitText(result, 1)
```

### VietnameseTextInput Internal Flow (the testable core)

```
processKey(char: Char): String?
  │
  ├── char not in modifiableChars AND not a tone key?
  │     └── return char.toString() immediately (passthrough)
  │
  ├── buffer contains invalidSequence?
  │     └── return char.toString() immediately (passthrough)
  │
  ├── char == tone key (s/f/r/x/j)?
  │     └── applyToneMark(toneMark, char)
  │           ├── different tone from last? → reverseTone() first
  │           ├── findFirstVowelIndex(buffer, toneMapping)
  │           │     ├── check toneMappingEnd special cases (ươ, iê, uô...)
  │           │     └── scan buffer for first vowel (skipping gi/qu prefixes)
  │           └── setCharAt(index, vowel+tone) → return buffer.toString()
  │
  ├── char == 'w'?
  │     └── applyWCharModifiers() → map each char in buffer through wCharModifiers
  │
  └── char is modifiable (a/e/i/o/u/d/w)?
        └── buffer.append(char)
              └── applyCharModifiers(char)
                    ├── charModified? → reverseCharModifier (âa → aa)
                    └── else: charModifiers (aa → â, ow → ơ, ...)
```

This internal flow is a pure function of `buffer` state plus the input character. The buffer is fully observable via `setBuffer()` / `reset()`. Every branch is testable with a table of `(initial_buffer, input_char) → expected_output` pairs.

---

## Recommended Test Architecture

### Tier 1: Pure JVM Unit Tests (`src/test/`)

**No additional dependencies required.** The existing `testImplementation("junit:junit:4.13.2")` is sufficient.

**Target classes:**
- `VietnameseTextInput` — highest priority, most complex, zero dependencies
- `Modifier` — state machine with timing, medium priority
- `SimpleModifier` — simpler variant of Modifier
- `MultipressController` — with real `KeyEvent` objects, medium priority

**Structure:**

```
app/src/test/java/io/github/oin/titanpocketkeyboard/
├── VietnameseTextInputTest.kt      ← primary target
├── ModifierTest.kt
├── SimpleModifierTest.kt
├── MultipressControllerTest.kt
└── fixtures/
    └── TelexFixtures.kt            ← shared input→expected tables
```

**Test class pattern for VietnameseTextInput:**

```kotlin
class VietnameseTextInputTest {
    private lateinit var input: VietnameseTextInput

    @Before
    fun setUp() {
        input = VietnameseTextInput()
    }

    @After
    fun tearDown() {
        input.reset()
    }

    // Group by scenario, not by method name
    @Test fun `a then s produces á`() {
        input.processKey('a')
        assertEquals("á", input.processKey('s'))
    }

    @Test fun `o then w produces ơ`() {
        input.processKey('o')
        assertEquals("ơ", input.processKey('w'))
    }

    @Test fun `ơ then w should revert to ow`() {
        // Bug case: ơ + w → should produce "ow" not "ơw"
        input.processKey('o')
        input.processKey('w')   // buffer is now "ơ"
        val result = input.processKey('w')
        assertEquals("ow", result)
    }
}
```

**Test pattern for Modifier timing-independent states:**

```kotlin
class ModifierTest {
    private val modifier = Modifier()

    @Before fun setUp() = modifier.reset()

    @Test fun `initial state is inactive`() {
        assertFalse(modifier.get())
    }

    @Test fun `held while key is down`() {
        modifier.onKeyDown()
        assertTrue(modifier.get())
        modifier.onKeyUp()
        // next activated on quick tap
        assertTrue(modifier.get())  // one-shot for next key
    }

    @Test fun `nextDidConsume clears one-shot`() {
        modifier.onKeyDown()
        modifier.onKeyUp()
        modifier.nextDidConsume()
        assertFalse(modifier.get())
    }
}
```

### Tier 2: Instrumented Tests (`src/androidTest/`) — Deferred

Required for: InputMethodService integration, InputConnection verification, SharedPreferences, actual key event routing.

Do not add instrumented tests in this milestone. The surface bugs (tone mark placement, w-revert, alt modifier) are all traceable to VietnameseTextInput and Modifier logic — both testable in Tier 1.

### Tier 3: Robolectric — Not Recommended for This Milestone

Robolectric would allow testing InputMethodService in JVM context but adds:
- Large dependency (~20MB runtime)
- Complex ServiceController lifecycle setup (seen in AnySoftKeyboard: requires Mockito + Shadow setup)
- SDK pinning requirements
- Additional CI configuration

The cost is not justified when the bugs are in pure-logic classes. Revisit if bugs are found in the InputMethodService routing layer.

---

## Build Order for Phase Implementation

The dependency graph determines implementation order:

```
Phase 1: Test infrastructure
  ├── Verify src/test/ directory exists (convention: already in Android module)
  ├── Confirm JUnit 4.13.2 testImplementation resolves
  └── Write one smoke test to validate the runner (no Android needed)

Phase 2: VietnameseTextInput test suite
  ├── Depends on: Phase 1
  ├── No new dependencies required
  ├── Cover: tone mark application (all 5 tones × all vowels)
  ├── Cover: char modifiers (aa→â, ow→ơ, uw→ư, dd→đ, ee→ê, aw→ă)
  ├── Cover: w-revert bug (ơ+w = "ow" regression test)
  ├── Cover: tone placement (findFirstVowelIndex for gi-, qu-, special sequences)
  ├── Cover: invalidSequence passthrough
  └── Cover: reverseTone and tone-change scenarios

Phase 3: Modifier test suite
  ├── Depends on: Phase 1
  ├── No new dependencies required
  ├── Cover: hold state
  ├── Cover: one-shot (tap-release pattern)
  ├── Cover: lock (double-tap within threshold)
  └── Cover: nextDidConsume deactivation

Phase 4: Bug fixes
  ├── Depends on: Phase 2 and 3 (tests prove the fix)
  ├── Fix w-revert: reverseCharModifier entry for ơ+w → ow
  ├── Fix tone placement: findFirstVowelIndex to use last vowel not first
  └── Fix alt one-shot: Modifier state machine for alt key in Vietnamese mode
```

---

## Patterns to Follow

### Pattern: Table-Driven Tests for Character Transformation

Vietnamese composition is fundamentally a lookup table problem. Express it as one.

```kotlin
@RunWith(Parameterized::class)
class TelexCompositionTest(
    private val sequence: String,
    private val expected: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} → {1}")
        fun cases() = listOf(
            arrayOf("as", "á"),
            arrayOf("af", "à"),
            arrayOf("ar", "ả"),
            arrayOf("ax", "ã"),
            arrayOf("aj", "ạ"),
            arrayOf("ow", "ơ"),
            arrayOf("owf", "ờ"),
            arrayOf("aas", "ấ"),
            arrayOf("oos", "ố"),
            arrayOf("uws", "ứ"),
        )
    }

    @Test fun compose() {
        val input = VietnameseTextInput()
        var result: String? = null
        for (char in sequence) result = input.processKey(char)
        assertEquals(expected, result)
    }
}
```

This pattern makes adding regression cases for every new Telex bug trivial — one line per bug report.

### Pattern: Buffer Seeding via setBuffer()

`VietnameseTextInput.setBuffer(string)` allows tests to start mid-composition without replaying a full character sequence:

```kotlin
@Test fun `reverseTone removes tone from toned vowel`() {
    input.setBuffer("bà")
    input.toneAdded = true
    // type a different tone
    val result = input.processKey('s')
    assertEquals("bás", result)  // or whatever the specified behavior is
}
```

This is important for testing edge cases in the middle of a word without relying on processKey being correct for the setup steps.

### Anti-Pattern: Testing InputMethodService Directly

Do not write a test that tries to instantiate `InputMethodService` in JVM context. It extends Android's `InputMethodService` and will throw `RuntimeException: Stub!` on any Android API call. Mock everything or use Robolectric — both add significant overhead. The correct boundary is to test the logic classes independently.

### Anti-Pattern: Mocking VietnameseTextInput

The entire point is that VietnameseTextInput is pure logic. Mocking it to test something else abandons the value of the test. Test it real or test the component that uses it with a stub boundary.

---

## Scalability Considerations

| Concern | Now (0 tests) | At 50 tests | At 200+ tests |
|---------|--------------|-------------|----------------|
| Test execution time | N/A | <1s (JVM, no emulator) | ~5s (still JVM) |
| Test discovery | gradle test | gradle test | gradle test |
| Coverage visibility | None | Add JaCoCo plugin | JaCoCo report gate in CI |
| Android-coupled tests | None | Add Robolectric if InputMethodService bugs surface | Full instrumented suite |
| Mutation testing | Out of scope | Pitest for Kotlin | Consider on core composition logic |

---

## Sources

- Direct code analysis of `/app/src/main/java/io/github/oin/titanpocketkeyboard/` — HIGH confidence (source of truth)
- Android developer documentation: [Build local unit tests](https://developer.android.com/training/testing/local-tests) — MEDIUM confidence (official, verified via webfetch)
- AnySoftKeyboard base test class (via webfetch of raw GitHub file): uses Robolectric + Mockito + RxJava schedulers for full-service testing — LOW confidence (single fetch, not cross-verified)
- General Robolectric documentation at robolectric.org — LOW confidence (web search, not fetched)
