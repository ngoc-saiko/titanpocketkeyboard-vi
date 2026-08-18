package io.github.oin.titanpocketkeyboard

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Unit tests for VietnameseTextInput — tone mark forward transforms (TEST-01) and
 * vowel modifier transforms (TEST-02).
 *
 * Parameterized runner is used for the two families that each have many cases.
 * Non-parameterized helper tests live in ToneMarkTracerTest; this file extends coverage.
 *
 * Plan 02-04 will append revert, invalidSequences, tone placement, and wModifiers tests
 * to this same file without conflicts.
 */

// ---------------------------------------------------------------------------
// TEST-01 — Tone mark forward transforms
// ---------------------------------------------------------------------------

/**
 * Parameterized test: for each (preloadedBuffer, toneKey, expected) triple, creates a
 * fresh VietnameseTextInput, optionally pre-loads the buffer via setBuffer(), then applies
 * the tone mark key via processKey() and asserts the returned string equals expected.
 *
 * When preloadedBuffer is null the test calls processKey(vowelChar) first to load a bare
 * vowel through the normal input path, matching the tracer pattern.
 */
@RunWith(Parameterized::class)
class ToneMarkTest(
    private val description: String,
    private val vowelChar: Char?,         // non-null → call processKey(vowelChar) to load buffer
    private val preloadedBuffer: String?, // non-null → call setBuffer(preloadedBuffer) instead
    private val toneKey: Char,
    private val expected: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun toneMarkParameters(): List<Array<Any?>> = listOf(
            // --- bare 'a' + all 5 tone marks ---
            arrayOf("a+s → á",  'a', null, 's', "á"),
            arrayOf("a+f → à",  'a', null, 'f', "à"),
            arrayOf("a+r → ả",  'a', null, 'r', "ả"),
            arrayOf("a+x → ã",  'a', null, 'x', "ã"),
            arrayOf("a+j → ạ",  'a', null, 'j', "ạ"),

            // --- ă (preloaded) + all 5 tone marks ---
            arrayOf("ă+s → ắ",  null, "ă", 's', "ắ"),
            arrayOf("ă+f → ằ",  null, "ă", 'f', "ằ"),
            arrayOf("ă+r → ẳ",  null, "ă", 'r', "ẳ"),
            arrayOf("ă+x → ẵ",  null, "ă", 'x', "ẵ"),
            arrayOf("ă+j → ặ",  null, "ă", 'j', "ặ"),

            // --- â (preloaded) + s ---
            arrayOf("â+s → ấ",  null, "â", 's', "ấ"),

            // --- bare 'e' + s ---
            arrayOf("e+s → é",  'e', null, 's', "é"),

            // --- ê (preloaded) + s ---
            arrayOf("ê+s → ế",  null, "ê", 's', "ế"),

            // --- bare 'i' + s ---
            arrayOf("i+s → í",  'i', null, 's', "í"),

            // --- bare 'o' + s ---
            arrayOf("o+s → ó",  'o', null, 's', "ó"),

            // --- ô (preloaded) + s ---
            arrayOf("ô+s → ố",  null, "ô", 's', "ố"),

            // --- ơ (preloaded) + s ---
            arrayOf("ơ+s → ớ",  null, "ơ", 's', "ớ"),

            // --- bare 'u' + s ---
            arrayOf("u+s → ú",  'u', null, 's', "ú"),

            // --- ư (preloaded) + s ---
            arrayOf("ư+s → ứ",  null, "ư", 's', "ứ"),

            // --- bare 'y' + s ---
            arrayOf("y+s → ý",  'y', null, 's', "ý")
        )
    }

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun toneMarkApplied() {
        val vti = VietnameseTextInput()
        if (preloadedBuffer != null) {
            vti.setBuffer(preloadedBuffer)
        } else {
            // Load the bare vowel through the normal input path
            vti.processKey(vowelChar!!)
        }
        val result = vti.processKey(toneKey)
        assertEquals(description, expected, result)
    }
}

// ---------------------------------------------------------------------------
// TEST-02 — Vowel modifier forward transforms
// ---------------------------------------------------------------------------

/**
 * Parameterized test: for each (keySequence, expectedReturn) pair, creates a fresh
 * VietnameseTextInput, feeds each character in the sequence via processKey(), and
 * asserts the LAST return value equals the expected modified vowel string.
 */
@RunWith(Parameterized::class)
class VowelModifierTest(
    private val description: String,
    private val keySequence: List<Char>,
    private val expected: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun vowelModifierParameters(): List<Array<Any?>> = listOf(
            arrayOf("aw → ă",  listOf('a', 'w'), "ă"),
            arrayOf("aa → â",  listOf('a', 'a'), "â"),
            arrayOf("ow → ơ",  listOf('o', 'w'), "ơ"),
            arrayOf("oo → ô",  listOf('o', 'o'), "ô"),
            arrayOf("uw → ư",  listOf('u', 'w'), "ư"),
            arrayOf("ee → ê",  listOf('e', 'e'), "ê"),
            arrayOf("dd → đ",  listOf('d', 'd'), "đ")
        )
    }

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun vowelModifierApplied() {
        val vti = VietnameseTextInput()
        var lastResult: String? = null
        for (ch in keySequence) {
            lastResult = vti.processKey(ch)
        }
        assertEquals(description, expected, lastResult)
    }
}
