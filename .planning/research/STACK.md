# Technology Stack: Android IME Testing

**Project:** TitanPocketKeyboard Vietnamese IME — bug-fix and test milestone
**Researched:** 2026-08-18
**Overall confidence:** HIGH (core choices), MEDIUM (version pins, due to fast-moving ecosystem)

---

## Context: What Needs Testing

There are two distinct testing problems in this project:

**Problem A — Pure logic classes (no Android dependency):**
`VietnameseTextInput.kt` imports only `android.util.Log`. `Modifier.kt` has zero Android imports. These classes contain the highest-risk bugs and have zero test coverage. They can be tested entirely on the JVM with no device, no emulator, no Robolectric.

**Problem B — Android-coupled classes:**
`MultipressController.kt` uses `android.view.KeyEvent` (constants and `getUnicodeChar()`). `InputMethodService.kt` subclasses Android's IME framework. These require either Android-stub mocking or Robolectric.

The recommended strategy is: solve Problem A first (cheapest, most value), then solve Problem B with Robolectric or MockK stubs only where needed.

---

## Recommended Stack

### Test Framework: JUnit 4 (keep existing)

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| `junit:junit` | **4.13.2** (already present) | Test runner, `@Test`, `@Before`, `@After`, parameterized | Already configured; JUnit 5 on Android requires an extra Gradle plugin (`de.mannodermaus.android-junit5`) that adds real complexity for zero concrete benefit in this codebase. JUnit 4 parameterized tests via `@RunWith(Parameterized::class)` are sufficient for the key-sequence table-driven tests needed here. |

**Do NOT switch to JUnit 5.** The `de.mannodermaus.android-junit5` plugin works but requires additional Gradle configuration, has historically lagged behind AGP, and the project already has JUnit 4 infrastructure. The tests needed here are simple enough that JUnit 5 features (nested classes, `@ParameterizedTest` with `@MethodSource`) are not worth the migration cost.

### Mocking: MockK

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| `io.mockk:mockk` | **1.14.9** | Mock `android.util.Log`, `android.view.KeyEvent`, `InputConnection` | Kotlin-native mocking library. Handles Kotlin `object`, `companion object`, `final` classes, and top-level functions without extra config. Mockito-Kotlin works but requires an extra `mockito-kotlin` wrapper artifact; MockK is idiomatic for Kotlin-first code. |

`android.util.Log` is the only Android dependency in `VietnameseTextInput.kt`. Mock it with `mockkStatic(Log::class)` so tests run on the JVM without `returnDefaultValues = true` (which silently swallows real bugs).

For `MultipressController.kt`, mock `KeyEvent` with `mockk<KeyEvent>()` — the class uses `KeyEvent.KEYCODE_*` integer constants (which are plain `Int` literals in bytecode, no mock needed) and `e.getUnicodeChar(metaState)` (which is a regular method call, easily stubbed).

### Android Stubs: Robolectric (only for InputMethodService tests)

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| `org.robolectric:robolectric` | **4.16.1** | Run tests against Android SDK classes on JVM | Only needed if you write tests that exercise `InputMethodService` subclass behavior or need `InputConnection` running through the real Android text-editing pipeline. For `VietnameseTextInput` and `Modifier`, this is unnecessary overhead. |
| `androidx.test:core-ktx` | **1.6.1** | Kotlin extensions for Robolectric + AndroidX test rules | Required companion when using Robolectric with AndroidX. |

**Robolectric usage is optional for this milestone.** The bugs to fix (`reverseTone()`, `w`-revert, modifier one-shot) are all in pure-logic or lightly-coupled classes. Start without Robolectric. Add it only if a test genuinely needs to exercise the `InputMethodService` lifecycle or `InputConnection` protocol — which is unlikely for the core Telex bug fixes.

Robolectric 4.16.1 adds SDK 36 (Android Baklava) support and requires JDK 17+ for SDK 34+ targets. The project targets SDK 33, so JDK 17 with Robolectric 4.16.1 is compatible.

### Assertions: Kotlin built-in + optional Kotest assertions-only

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| `kotlin.test` | bundled with Kotlin 1.9.0 | `assertEquals`, `assertTrue`, `assertFailsWith` | Zero extra dependency; sufficient for most assertions. |
| `io.kotest:kotest-assertions-core` | **6.2.4** (optional) | Fluent infix assertions: `result shouldBe "đã"` | Kotest assertions-only (NOT the full Kotest test runner) can be added alongside JUnit 4 for more readable assertion messages. Pure JVM, no Android dependency. Only add this if the team finds vanilla `assertEquals` messages inadequate. |

**Do NOT add the full Kotest test framework.** Kotest 6.x requires Kotlin 2.2 and JDK 11+. The project is on Kotlin 1.9.0. Using just `kotest-assertions-core` is framework-independent (works with JUnit 4 runner) but Kotest 6.x's Kotlin requirement means you would need to upgrade Kotlin first. At Kotlin 1.9.0, skip Kotest entirely — use `kotlin.test` assertions which ship for free.

---

## What NOT to Use

| Rejected Option | Why Not |
|-----------------|---------|
| Espresso | Cannot simulate hardware key events from physical QWERTY keyboard; confirmed by PROJECT.md. Tests for IME itself (not the target app) are not Espresso's domain. |
| JUnit 5 + `de.mannodermaus.android-junit5` | Extra plugin complexity; AGP compatibility lag; no concrete benefit over JUnit 4 parameterized tests for this use case. |
| `android { testOptions { unitTests.returnDefaultValues = true } }` | Silently masks Android-method-not-mocked failures; lets tests pass even when production code is broken. Mock `android.util.Log` explicitly instead. |
| Mockito (plain) | Mockito struggles with Kotlin final-by-default classes; requires `@MockitoSettings(strictness = LENIENT)` or an `extensions/org.mockito.plugins.MockMaker` file. MockK is simpler for Kotlin. |
| Full Kotest (test runner) | Requires Kotlin 2.2 to use v6.x; project is on Kotlin 1.9.0. Version mismatch would require Kotlin upgrade before tests could be written — wrong order of operations. |
| UI/instrumented tests for IME logic | The IME service is not exposed to instrumented test runners via `ServiceTestRule`; Android CTS uses host-side test infrastructure that is not available to app developers. Not feasible for automated local testing. |

---

## Testing Patterns for This Codebase

### Pattern 1: Pure logic tests (no mocking needed)

`Modifier.kt` has zero Android dependencies. Tests can be plain JUnit 4:

```kotlin
class ModifierTest {
    private val modifier = Modifier()

    @Test
    fun `single press enables one-shot mode`() {
        modifier.onKeyDown()
        modifier.onKeyUp()
        assertTrue(modifier.get())          // active after single tap
    }

    @Test
    fun `one-shot consumed after printing key`() {
        modifier.onKeyDown()
        modifier.onKeyUp()
        modifier.consume()
        assertFalse(modifier.get())
    }
}
```

### Pattern 2: Table-driven Telex sequence tests

JUnit 4 `@RunWith(Parameterized::class)` for key-sequence → expected-output tables:

```kotlin
@RunWith(Parameterized::class)
class TelexSequenceTest(
    private val description: String,
    private val keySequence: List<Char>,
    private val expectedOutput: String
) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf("ow produces ơ", listOf('o', 'w'), "ơ"),
            arrayOf("ow+w reverts to ow", listOf('o', 'w', 'w'), "ow"),
            arrayOf("toà with tone j", listOf('t', 'o', 'a', 'j'), "toà"),
            // ... full regression table
        )
    }

    @Before
    fun setUp() { mockkStatic(Log::class); every { Log.d(any(), any()) } returns 0 }

    @Test
    fun `telex sequence produces expected output`() {
        val input = VietnameseTextInput()
        keySequence.forEach { input.processKey(it) }
        assertEquals(expectedOutput, input.buffer.toString())
    }
}
```

### Pattern 3: MockK for android.util.Log stub

```kotlin
@Before
fun stubAndroidLog() {
    mockkStatic(Log::class)
    every { Log.d(any(), any()) } returns 0
    every { Log.w(any(), any<String>()) } returns 0
}

@After
fun tearDown() { unmockkStatic(Log::class) }
```

This is the only mocking needed for `VietnameseTextInput` tests. No Robolectric required.

### Pattern 4: MultipressController (KeyEvent constants)

`KeyEvent.KEYCODE_*` constants are plain `Int` values compiled into bytecode — they do not require Android framework to resolve. Only `e.getUnicodeChar(metaState)` needs a mock:

```kotlin
val fakeEvent = mockk<KeyEvent>()
every { fakeEvent.unicodeChar } returns 'o'.code
every { fakeEvent.getUnicodeChar(any()) } returns 'o'.code
```

No Robolectric needed for `MultipressController` unit tests.

---

## Gradle Changes Required

Add to `app/build.gradle.kts`:

```kotlin
dependencies {
    // Existing (keep)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Add: MockK for Android (JVM unit tests)
    testImplementation("io.mockk:mockk:1.14.9")

    // Add only if InputMethodService-level tests are needed:
    // testImplementation("org.robolectric:robolectric:4.16.1")
    // testImplementation("androidx.test:core-ktx:1.6.1")
}
```

No plugin changes needed. No JUnit 5 plugin. No Kotlin upgrade required.

The test source root `app/src/test/java/` is standard for local JVM tests and should already be recognized by the Android Gradle Plugin 8.13.2 present in this project.

---

## Dependency Upgrade Notes

These are separate from the testing milestone but relevant context:

| Component | Current | Recommended | Risk |
|-----------|---------|-------------|------|
| Kotlin | 1.9.0 | 2.0.x or 2.1.x | Medium — AGP 8.x is compatible; verify with `kotlinOptions.jvmTarget` |
| AGP | 8.13.2 | Already current | Low |
| `androidx.core:core-ktx` | 1.9.0 | 1.13.x | Low |
| `targetSdk` | 33 | 34 | Medium — test on target hardware after bump |

Do NOT upgrade Kotlin as part of the test-writing phase. Kotlin 2.x introduces K2 compiler and changes that could break the build mid-milestone. Upgrade as a separate, dedicated change.

---

## Sources

- [Android local unit tests — official docs](https://developer.android.com/training/testing/local-tests)
- [Robolectric GitHub releases](https://github.com/robolectric/robolectric/releases) — latest stable 4.16.1
- [MockK GitHub releases](https://github.com/mockk/mockk/releases) — latest 1.14.11 (mockk-android 1.14.9)
- [Kotest releases](https://github.com/kotest/kotest/releases) — 6.2.4, requires Kotlin 2.2
- [android-junit5 Gradle plugin](https://github.com/aurae/android-junit5) — available but not recommended here
- [JUnit 4 parameterized tests in Kotlin](https://proandroiddev.com/kotlin-unit-tests-with-parameters-e37aab2b36f6)
- [MockK Android quickstart](https://mockk.io/ANDROID.html)
- [Robolectric strategies — Android Developers](https://developer.android.com/training/testing/local-tests/robolectric)
