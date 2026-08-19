package io.github.oin.titanpocketkeyboard

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Regression guard for debug session `den-ee-circumflex`.
 *
 * Bug: after `dd` composed `đ`, no further double-letter vowel modifier fired in the same
 * syllable — `d,d,e,e` produced the raw buffer `đee` instead of `đê`, so `đến` (and every other
 * `đ` + modified-vowel syllable: `đâu`, `đôi`, `đăng`, ...) could not be typed.
 *
 * Root cause: `charModified` is a buffer-global flag. Commit b699975 removed the `char != 'd'`
 * exclusion from its assignment, so `dd -> đ` set the flag and permanently routed
 * `applyCharModifiers` into the reverse-only branch for the rest of the syllable. `đ` is an onset
 * consonant, not the syllable nucleus, so the nucleus vowel still needs its own modifier.
 *
 * Fix: let a non-matching reverse lookup fall through to the forward lookup instead of
 * short-circuiting. Reverse patterns (`đd`, `êe`, `âa`, ...) and forward patterns (`dd`, `ee`,
 * `aa`, ...) are disjoint at the same 2-char suffix, so reverse still wins where it applies.
 *
 * Oracle type: specified (Vietnamese Telex input rules).
 */
class DenEeCircumflexRegressionTest {

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

    // -----------------------------------------------------------------------
    // Core repro — the exact reported sequence
    // -----------------------------------------------------------------------

    /** d,d,e,e must compose "đê": `dd` -> đ (onset), then `ee` -> ê (nucleus). */
    @Test
    fun ddThenEe_producesDCircumflexE() {
        val vti = VietnameseTextInput()
        vti.processKey('d')
        assertEquals("dd should compose đ", "đ", vti.processKey('d'))
        vti.processKey('e')
        assertEquals("ee after đ should compose ê", "đê", vti.processKey('e'))
    }

    /** Full reported word: d,d,e,e,n,s -> tone lands on the ê nucleus, giving "đế" (+ the passthrough n). */
    @Test
    fun denTelexSequence_placesToneOnECircumflex() {
        val vti = VietnameseTextInput()
        vti.processKey('d')
        vti.processKey('d')
        vti.processKey('e')
        vti.processKey('e')
        assertEquals("n is a passthrough consonant", "n", vti.processKey('n'))
        assertEquals("s must acute the ê nucleus", "đế", vti.processKey('s'))
    }

    // -----------------------------------------------------------------------
    // Boundary neighbors — same equivalence class (đ + any double-letter modifier)
    // -----------------------------------------------------------------------

    /** đâu family: d,d,a,a -> "đâ". */
    @Test
    fun ddThenAa_producesDCircumflexA() {
        val vti = VietnameseTextInput()
        vti.processKey('d')
        vti.processKey('d')
        vti.processKey('a')
        assertEquals("aa after đ should compose â", "đâ", vti.processKey('a'))
    }

    /** đôi family: d,d,o,o -> "đô". */
    @Test
    fun ddThenOo_producesDCircumflexO() {
        val vti = VietnameseTextInput()
        vti.processKey('d')
        vti.processKey('d')
        vti.processKey('o')
        assertEquals("oo after đ should compose ô", "đô", vti.processKey('o'))
    }

    /** đường family: the w-modifier path after đ must keep working (it used a different code path). */
    @Test
    fun ddThenUwOw_producesDuongStem() {
        val vti = VietnameseTextInput()
        vti.processKey('d')
        vti.processKey('d')
        vti.processKey('u')
        assertEquals("uw after đ should compose ư", "đư", vti.processKey('w'))
        vti.processKey('o')
        assertEquals("ow after đư should compose ơ", "đươ", vti.processKey('w'))
    }

    // -----------------------------------------------------------------------
    // Controls — prove the bug was đ-conditioned, and that the revert path stays intact
    // -----------------------------------------------------------------------

    /** Control: ee with no preceding đ was never broken. */
    @Test
    fun eeWithoutLeadingDd_stillProducesECircumflex() {
        val vti = VietnameseTextInput()
        vti.processKey('e')
        assertEquals("bare ee should compose ê", "ê", vti.processKey('e'))
    }

    /** Guardrail: the dd revert introduced by FIX-01 must not regress — d,d,d -> "dd". */
    @Test
    fun dddStillRevertsToDd() {
        val vti = VietnameseTextInput()
        vti.processKey('d')
        vti.processKey('d')
        assertEquals("third d must revert đ back to dd", "dd", vti.processKey('d'))
    }

    /** Guardrail: đ revert must still fire even after the nucleus was modified — d,d,e,e,d -> "đêd" untouched. */
    @Test
    fun revertAfterNucleusModifier_doesNotCorruptBuffer() {
        val vti = VietnameseTextInput()
        vti.processKey('d')
        vti.processKey('d')
        vti.processKey('e')
        vti.processKey('e')
        // 'd' here is a coda-position keystroke, not a revert of the đ onset (đ is not the last char)
        assertEquals("trailing d must not revert the non-adjacent đ", "d", vti.processKey('d'))
    }
}
