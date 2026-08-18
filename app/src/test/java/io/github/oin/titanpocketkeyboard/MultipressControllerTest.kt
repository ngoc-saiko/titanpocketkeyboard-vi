package io.github.oin.titanpocketkeyboard

import android.view.KeyEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MultipressController — consonant filtering (TEST-10) and
 * multipress character substitution sequences (TEST-11).
 *
 * KeyEvent is an Android framework class whose constructor throws RuntimeException("Stub!")
 * under JVM tests. We use mockk<KeyEvent>() to create mock instances without invoking
 * the constructor, stubbing keyCode, repeatCount, getUnicodeChar(), and unicodeChar.
 *
 * KeyEvent.KEYCODE_* constants are public static final int values whose initializers
 * are constant expressions — they are compiled into the bytecode constant pool and do NOT
 * go through the Stub! mechanism. Tests use the raw integer values to avoid any class
 * loading dependency on the Android stub jar in test code; the production code in
 * MultipressController.process() accesses them internally where constant-folding applies.
 *
 * Raw keycode values used (matching android.view.KeyEvent constants):
 *   KEYCODE_A = 29, KEYCODE_B = 30, KEYCODE_C = 31, KEYCODE_S = 47
 */
class MultipressControllerTest {

    // Raw integer values matching Android KeyEvent constants (compile-time constants)
    private val KEYCODE_A = 29  // KeyEvent.KEYCODE_A
    private val KEYCODE_B = 30  // KeyEvent.KEYCODE_B
    private val KEYCODE_C = 31  // KeyEvent.KEYCODE_C
    private val KEYCODE_S = 47  // KeyEvent.KEYCODE_S

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Creates a mock KeyEvent with the given keyCode and repeatCount.
     * getUnicodeChar(metaState) returns 0 (unused in these tests).
     * unicodeChar returns 0 (unused in these tests).
     */
    private fun makeKeyEvent(keyCode: Int, repeatCount: Int): KeyEvent {
        val e = mockk<KeyEvent>()
        every { e.keyCode } returns keyCode
        every { e.repeatCount } returns repeatCount
        every { e.getUnicodeChar(any()) } returns 0
        every { e.unicodeChar } returns 0
        return e
    }

    /**
     * Builds a minimal MultipressController with a single substitution level containing
     * entries for KEYCODE_C ('c'), KEYCODE_S ('s'), KEYCODE_A ('x','y','z'), and KEYCODE_B ('b').
     */
    private fun buildController(ignoreConsonants: Boolean = false): MultipressController {
        val substitutions = Array(1) {
            hashMapOf(
                KEYCODE_C to arrayOf('c'),
                KEYCODE_S to arrayOf('s'),
                KEYCODE_A to arrayOf('x', 'y', 'z'),
                KEYCODE_B to arrayOf('b')
            )
        }
        return MultipressController(substitutions).apply {
            ignoreConsonantsOnFirstLevel = ignoreConsonants
        }
    }

    // ── Consonant filtering (TEST-10) ────────────────────────────────────────

    /**
     * When ignoreConsonantsOnFirstLevel=true and longPressCount=0, pressing KEYCODE_C
     * returns MPSUBST_BYPASS instead of the substitution character.
     *
     * Sequence: prime with repeatCount=0 (sets last=KEYCODE_C), then call with
     * repeatCount=1 (triggers the multipress branch, longPressCount incremented to 0).
     */
    @Test
    fun consonantFilter_keycodeC_firstLevel_returnsBypass() {
        val controller = buildController(ignoreConsonants = true)

        // Prime: first press sets last=KEYCODE_C, returns MPSUBST_BYPASS (not multipress yet)
        val prime = controller.process(makeKeyEvent(KEYCODE_C, 0), 0)
        assertEquals("first press should return MPSUBST_BYPASS", MPSUBST_BYPASS, prime)

        // Second press within default multipressThreshold (750ms) with repeatCount=1:
        // enters multipress branch; ++longPressCount → longPressCount=0 after clamp; count=0;
        // ignoreConsonantsOnFirstLevel=true AND longPressCount=0 AND keyCode in {KEYCODE_C,KEYCODE_S}
        // → returns MPSUBST_BYPASS
        val result = controller.process(makeKeyEvent(KEYCODE_C, 1), 0)
        assertEquals("KEYCODE_C on first multipress level with ignoreConsonants=true should return MPSUBST_BYPASS",
            MPSUBST_BYPASS, result)
    }

    /**
     * When ignoreConsonantsOnFirstLevel=true and longPressCount=0, pressing KEYCODE_S
     * returns MPSUBST_BYPASS instead of the substitution character.
     */
    @Test
    fun consonantFilter_keycodeS_firstLevel_returnsBypass() {
        val controller = buildController(ignoreConsonants = true)

        // Prime with KEYCODE_S
        controller.process(makeKeyEvent(KEYCODE_S, 0), 0)

        // Multipress: should be filtered
        val result = controller.process(makeKeyEvent(KEYCODE_S, 1), 0)
        assertEquals("KEYCODE_S on first multipress level with ignoreConsonants=true should return MPSUBST_BYPASS",
            MPSUBST_BYPASS, result)
    }

    /**
     * When ignoreConsonantsOnFirstLevel=false (default), KEYCODE_C is NOT filtered —
     * multipress substitution returns the mapped character.
     */
    @Test
    fun consonantFilter_disabled_keycodeC_returnsSubstitution() {
        val controller = buildController(ignoreConsonants = false)

        // Prime
        controller.process(makeKeyEvent(KEYCODE_C, 0), 0)

        // Multipress: filtering disabled, should return substitution 'c'
        val result = controller.process(makeKeyEvent(KEYCODE_C, 1), 0)
        assertEquals("KEYCODE_C multipress with ignoreConsonants=false should return substitution",
            'c', result)
    }

    /**
     * KEYCODE_B is NOT in the hardcoded consonant set {KEYCODE_C, KEYCODE_S}.
     * Even with ignoreConsonantsOnFirstLevel=true, KEYCODE_B should return its substitution.
     */
    @Test
    fun consonantFilter_keycodeBOnFirstLevel_notFiltered() {
        val controller = buildController(ignoreConsonants = true)

        // Prime
        controller.process(makeKeyEvent(KEYCODE_B, 0), 0)

        // Multipress: KEYCODE_B is not in the filtered set → substitution is returned
        val result = controller.process(makeKeyEvent(KEYCODE_B, 1), 0)
        assertEquals("KEYCODE_B should not be filtered by consonant filter",
            'b', result)
    }

    // ── Multipress substitution sequences (TEST-11) ──────────────────────────

    /**
     * First press of any key returns MPSUBST_BYPASS (sets last to that key, but
     * is not yet in the multipress branch — it goes to the else branch).
     */
    @Test
    fun multipress_firstPress_returnsBypass() {
        val controller = buildController()

        val result = controller.process(makeKeyEvent(KEYCODE_A, 0), 0)
        assertEquals("first press should always return MPSUBST_BYPASS", MPSUBST_BYPASS, result)
    }

    /**
     * Second quick-tap of the same key within multipressThreshold (repeatCount=0)
     * returns the first element of the substitution array.
     *
     * Substitution for KEYCODE_A: ['x', 'y', 'z']
     * After first press (else branch): last=KEYCODE_A, count=0, longPressCount=0.
     * Second press (repeatCount=0): enters multipress branch; repeatCount!=1 && !=2+, so
     * count is NOT reset; longPressCount stays 0; subst[0][count=0]='x'; ++count=1.
     *
     * Note: repeatCount=0 is a quick re-tap (user releases and presses again). The
     * within-level cycling ('x'->'y'->'z') uses quick taps (repeatCount=0).
     * repeatCount=1 advances the substitution LEVEL (changes longPressCount), not within-level count.
     */
    @Test
    fun multipress_secondPress_sameKey_returnsFirstSubstitution() {
        val controller = buildController()

        // Prime: first press (else branch — not multipress yet)
        controller.process(makeKeyEvent(KEYCODE_A, 0), 0)

        // Second quick-tap (repeatCount=0, within threshold) — enters multipress branch
        // count=0 → subst[0]='x'
        val result = controller.process(makeKeyEvent(KEYCODE_A, 0), 0)
        assertEquals("second quick-tap should return first substitution 'x'", 'x', result)
    }

    /**
     * Third quick-tap (same key, within threshold) returns second element of substitution array.
     * After second press: count=1, longPressCount=0.
     * Third press (repeatCount=0): count=1 → subst[1]='y'; ++count=2.
     */
    @Test
    fun multipress_thirdPress_returnsSecondSubstitution() {
        val controller = buildController()

        // Prime
        controller.process(makeKeyEvent(KEYCODE_A, 0), 0)
        controller.process(makeKeyEvent(KEYCODE_A, 0), 0) // 'x', count=1

        val result = controller.process(makeKeyEvent(KEYCODE_A, 0), 0)
        assertEquals("third quick-tap should return second substitution 'y'", 'y', result)
    }

    /**
     * After all elements are exhausted, the count wraps around to 0 and the next quick-tap
     * returns the first element again (cyclic behavior).
     *
     * Sequence using quick taps (repeatCount=0):
     *   prime (BYPASS), tap2 ('x'), tap3 ('y'), tap4 ('z', count→0), tap5 ('x' again).
     */
    @Test
    fun multipress_cyclicWrapAround_returnsFirst() {
        val controller = buildController()

        // All quick taps (repeatCount=0)
        controller.process(makeKeyEvent(KEYCODE_A, 0), 0) // prime → BYPASS
        controller.process(makeKeyEvent(KEYCODE_A, 0), 0) // 'x', count=1
        controller.process(makeKeyEvent(KEYCODE_A, 0), 0) // 'y', count=2
        controller.process(makeKeyEvent(KEYCODE_A, 0), 0) // 'z', count wraps to 0

        // Fifth quick-tap: count=0 → 'x' again (lastSubstitution='z', no dedup)
        val result = controller.process(makeKeyEvent(KEYCODE_A, 0), 0)
        assertEquals("after cycling through all elements, next quick-tap returns 'x' again", 'x', result)
    }

    /**
     * Pressing a different key resets state: next press after a key change returns MPSUBST_BYPASS.
     */
    @Test
    fun multipress_differentKey_resetsState() {
        val controller = buildController()

        // Prime KEYCODE_A, then quick-tap multipress
        controller.process(makeKeyEvent(KEYCODE_A, 0), 0)
        controller.process(makeKeyEvent(KEYCODE_A, 0), 0) // 'x'

        // Different key — goes to else branch: count=0, longPressCount=0, last=KEYCODE_B
        val result = controller.process(makeKeyEvent(KEYCODE_B, 0), 0)
        assertEquals("different key should return MPSUBST_BYPASS (state reset)", MPSUBST_BYPASS, result)
    }

    /**
     * Two calls with more than multipressThreshold milliseconds elapsed between them.
     * The second call should be treated as a fresh first press and return MPSUBST_BYPASS.
     *
     * Timing note: sleep 800ms > multipressThreshold=750ms to ensure timeout expires
     * with safe margin. The second call goes to the else branch (last==keyCode but t-lastTime >= 750ms).
     */
    @Test
    fun multipress_timeout_resetsBypass() {
        val controller = buildController()

        // First quick-tap (prime)
        controller.process(makeKeyEvent(KEYCODE_A, 0), 0)

        // timing test: multipressThreshold=750ms; sleep 800ms to ensure timeout expires
        Thread.sleep(800)

        // Second quick-tap after timeout: t-lastTime >= 750ms → else branch → MPSUBST_BYPASS
        val result = controller.process(makeKeyEvent(KEYCODE_A, 0), 0)
        assertEquals("second quick-tap after 800ms timeout should return MPSUBST_BYPASS", MPSUBST_BYPASS, result)
    }

    /**
     * Deduplication: if process() would return the same substitution character twice in a row,
     * the second return is MPSUBST_NOTHING.
     *
     * The lastSubstitution field tracks the last returned char. If the same char would be
     * returned again (e.g., after wrapping count), MPSUBST_NOTHING is returned instead.
     *
     * Sequence to trigger dedup: cycle through all 3 chars ('x','y','z'), wrap to count=0,
     * next call returns 'x' again — but lastSubstitution='z'. No dedup yet.
     * To hit dedup on 'x': We need 'x' returned, then 'x' returned again.
     * This can only happen when: (1) 'x' from count=0 returned, (2) count wraps again back to 'x'.
     * Full cycle: prime, 'x'(count→1), 'y'(count→2), 'z'(count→0), 'x' again (count→1)=dedup with 'z'? No.
     * Actually dedup fires when `lastSubstitution == substitution`. Since lastSubstitution is set
     * after each returned char, 'x' appears twice in a 6-step sequence:
     * step 2: 'x', step 5: 'x' after full wrap; lastSubstitution at step 5 is 'z' (from step 4).
     * So dedup does NOT fire on natural cycling.
     *
     * A dedup fires when the same char would be returned on consecutive calls.
     * This is NOT possible in the normal cycle (each step returns a different indexed char).
     * Dedup can only fire when the substitution array has the same char at consecutive positions,
     * OR when longPressCount changes produce the same substitution.
     *
     * For this test, use a substitution array with a repeated char: KEYCODE_A → ['x', 'x'].
     * Prime → 'x' (count→1) → 'x' again → dedup → MPSUBST_NOTHING.
     */
    @Test
    fun multipress_dedup_sameSubstitutionTwice_returnsNothing() {
        // Use a substitution array with duplicate characters to trigger dedup
        val substitutions = Array(1) {
            hashMapOf(KEYCODE_A to arrayOf('x', 'x'))
        }
        val controller = MultipressController(substitutions)

        // Prime (first press → else branch)
        controller.process(makeKeyEvent(KEYCODE_A, 0), 0)

        // Second quick-tap: count=0 → 'x', lastSubstitution='x', count→1
        val first = controller.process(makeKeyEvent(KEYCODE_A, 0), 0)
        assertEquals("second quick-tap should return 'x'", 'x', first)

        // Third quick-tap: count=1 → 'x' again (subst[1]='x'), lastSubstitution='x' → MPSUBST_NOTHING
        val second = controller.process(makeKeyEvent(KEYCODE_A, 0), 0)
        assertEquals("third quick-tap returning same char should return MPSUBST_NOTHING (dedup)", MPSUBST_NOTHING, second)
    }
}
