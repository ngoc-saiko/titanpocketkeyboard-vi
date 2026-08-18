# Testing Patterns

**Analysis Date:** 2026-08-18

## Test Framework

**Runner:**
- AndroidJUnit4 / JUnit 4.13.2 configured in `app/build.gradle.kts`
- Config: `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`
- Test dependencies:
  - `junit:junit:4.13.2` for unit tests
  - `androidx.test.ext:junit:1.1.5` for AndroidX test extensions
  - `androidx.test.espresso:espresso-core:3.5.1` for UI/instrumentation testing

**Assertion Library:**
- JUnit 4 assertions (`assertEquals`, `assertTrue`, etc.)
- No explicit mock framework configured

**Run Commands:**
```bash
# Unit tests (after test directory structure is added)
./gradlew test

# Instrumentation/UI tests (requires Android device or emulator)
./gradlew connectedAndroidTest

# All tests
./gradlew test connectedAndroidTest
```

## Test File Organization

**Location:**
- No test files currently exist in the repository
- Convention: `src/test/` for unit tests, `src/androidTest/` for instrumentation tests
- Test structure mirrors source: Tests for `io.github.oin.titanpocketkeyboard.InputMethodService` would go to `src/androidTest/java/io/github/oin/titanpocketkeyboard/InputMethodServiceTest.kt`

**Naming:**
- Expected pattern (not yet applied): `[ClassName]Test.kt` for unit tests
- Expected pattern: `[ClassName]InstrumentationTest.kt` for Android instrumentation tests
- Test methods use JUnit convention: `test[Description]()` or `should[Description]()`

**Structure:**
```
app/src/
├── main/
│   ├── java/io/github/oin/titanpocketkeyboard/
│   │   ├── InputMethodService.kt
│   │   ├── Modifier.kt
│   │   └── ...
│   └── res/
├── test/
│   └── java/io/github/oin/titanpocketkeyboard/
│       ├── InputMethodServiceTest.kt
│       ├── ModifierTest.kt
│       └── ...
└── androidTest/
    └── java/io/github/oin/titanpocketkeyboard/
        ├── InputMethodServiceInstrumentationTest.kt
        └── ...
```

## Test Structure

**Suite Organization:**
Since no tests exist, the expected pattern based on Android conventions would be:

```kotlin
class ModifierTest {
    private lateinit var modifier: Modifier

    @Before
    fun setUp() {
        modifier = Modifier()
    }

    @Test
    fun testInitialStateIsNotActive() {
        assertFalse(modifier.get())
    }

    @Test
    fun testKeyDownActivatesModifier() {
        modifier.onKeyDown()
        assertTrue(modifier.get())
    }
}
```

**Patterns:**
- Setup with `@Before` annotation (JUnit 4 style)
- Teardown with `@After` annotation if needed
- Assertion pattern: `assertTrue()`, `assertFalse()`, `assertEquals(expected, actual)`

## Mocking

**Framework:**
- No mock framework currently configured in `build.gradle.kts`
- For future use, Mockito would be appropriate choice
- Manual mocking via test doubles possible for interfaces like `RecognitionListener`

**Patterns:**
For speech recognition listener mocking (when added):
```kotlin
// Anonymous implementation of RecognitionListener
val mockListener = object : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) { }
    override fun onBeginningOfSpeech() { }
    // ... implement all required methods
}
```

**What to Mock:**
- Android framework components: `InputConnection`, `Context`, `SharedPreferences`
- External APIs: `SpeechRecognizer` for testing audio input handling
- System services: `Vibrator`, `ToneGenerator` for testing haptic/audio feedback

**What NOT to Mock:**
- Business logic classes: `Modifier`, `MultipressController`, `VietnameseTextInput`
- Local data structures: HashMaps, string transformations
- KeyEvent processing logic (test with real KeyEvent objects)

## Fixtures and Factories

**Test Data:**
No fixtures currently exist. Expected patterns:

For Vietnamese text input testing:
```kotlin
object VietnameseTextInputFixtures {
    val sampleWords = mapOf(
        "tien" to "tiền",  // with tone mark
        "ho" to "hô",      // with diacritical
        "da" to "đá"       // consonant modification
    )
    
    fun createVietnameseInput(): VietnameseTextInput = VietnameseTextInput()
}
```

For key event testing:
```kotlin
fun createKeyEvent(keyCode: Int, action: Int = KeyEvent.ACTION_DOWN): KeyEvent {
    return KeyEvent(0, 0, action, keyCode, 0, 0)
}
```

**Location:**
- Would reside in `src/test/java/io/github/oin/titanpocketkeyboard/fixtures/`
- Alternatively as companion objects in test classes

## Coverage

**Requirements:**
- No coverage targets enforced
- No JaCoCo or similar coverage tools configured

**View Coverage:**
- When configured, use:
```bash
./gradlew jacocoTestReport  # After adding JaCoCo plugin to build.gradle.kts
```

## Test Types

**Unit Tests:**
- Scope: Test individual Kotlin classes in isolation
- Approach: Test `Modifier`, `MultipressController`, `VietnameseTextInput` state machines
- Example targets:
  - `Modifier.onKeyDown()` and `onKeyUp()` state transitions
  - `VietnameseTextInput.processKey()` tone mark handling
  - `MultipressController.process()` multipress detection
  - String transformation logic in `VietnameseTextInput`

**Integration Tests:**
- Scope: Test interaction between classes (e.g., `InputMethodService` + `Modifier` + `MultipressController`)
- Approach: Setup full component stack, trigger key events, verify output
- Example targets:
  - Keyboard input flow from key press to character insertion
  - State synchronization across modifier classes
  - Vietnamese text input workflow (character → tone → output)

**E2E Tests:**
- Framework: Espresso (via `androidx.test.espresso:espresso-core`)
- Scope: Full application flow on Android device/emulator
- Not currently implemented
- Example targets:
  - Settings UI modifications
  - Keyboard input in various Android text fields
  - Speech-to-text integration end-to-end

## Common Patterns

**Async Testing:**
For speech recognition tests (when added):
```kotlin
@Test
fun testSpeechRecognitionResult() {
    val latch = CountDownLatch(1)
    var result: String? = null
    
    // Setup listener
    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            result = results?.getStringArrayList(...)?.get(0)
            latch.countDown()
        }
        // ... other methods
    })
    
    // Trigger and wait
    speechRecognizer.startListening(intent)
    latch.await(5, TimeUnit.SECONDS)
    
    assertEquals("expected text", result)
}
```

**Error Testing:**
For permission and error handling:
```kotlin
@Test
fun testMissingMicrophonePermissionHandled() {
    // Setup mocked context without permission
    val context = mock(Context::class.java)
    whenever(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO))
        .thenReturn(PackageManager.PERMISSION_DENIED)
    
    // Should request permission or show error
    // Assert permission request was triggered
}

@Test
fun testInvalidCharacterProcessingReturnsOriginal() {
    val input = VietnameseTextInput()
    val result = input.processKey('!')
    assertEquals("!", result)
}
```

## Testing Gaps

**Critical areas needing tests:**
- `VietnameseTextInput`: Complex tone mark and character modifier logic
- `MultipressController`: State machine for multipress detection
- `Modifier`: Timing-based lock/next behavior
- `InputMethodService.onKeyDown()`: Large, complex key handling logic
- Speech recognition integration: Permission handling and listener callbacks
- SharedPreferences integration: Preference loading and caching

---

*Testing analysis: 2026-08-18*
