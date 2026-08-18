package io.github.oin.titanpocketkeyboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * State-machine tests for the Modifier class.
 *
 * Covers:
 *  - Hold mode (TEST-06): onKeyDown() sets held=true; long hold then onKeyUp() clears all.
 *  - One-shot mode (TEST-07): press+release sets next=true; nextDidConsume() clears it.
 *  - Lock mode (TEST-08): double-tap within 250ms toggles lock; third tap clears it.
 *  - nextDidConsume() (TEST-09): clears next, sets preventNext; idempotent.
 *
 * Modifier has no Android framework imports — no MockK needed.
 */
class ModifierTest {

    private lateinit var modifier: Modifier

    @Before
    fun setUp() {
        modifier = Modifier()
    }

    // ── Hold mode (TEST-06) ──────────────────────────────────────────────────

    /**
     * After onKeyDown(), held=true and get()=true.
     * ToneMarkTracerTest already documents quick-release behavior;
     * this test focuses on the mid-hold state (before onKeyUp).
     */
    @Test
    fun holdMode_keyDown_setsHeld() {
        modifier.onKeyDown()

        assertTrue("isHeld() should be true while key is down", modifier.isHeld())
        assertTrue("get() should be true while held", modifier.get())
        assertFalse("isLocked() should be false in hold (not double-tap)", modifier.isLocked())
    }

    /**
     * After a long hold (>350ms nextThreshold) followed by onKeyUp():
     *  - next = !lock && elapsed < nextThreshold && !preventNext
     *  - elapsed 400ms ≥ 350ms → next = false
     *  - held = false
     *  - get() = false
     *
     * Timing note: sleep 400ms to exceed nextThreshold=350ms with a safe margin.
     */
    @Test
    fun holdMode_longHoldThenRelease_clearsAll() {
        modifier.onKeyDown()
        assertTrue("should be held immediately after keyDown", modifier.isHeld())

        // Sleep 400ms — nextThreshold=350ms; 400 > 350 so elapsed >= nextThreshold → next=false
        Thread.sleep(400)
        modifier.onKeyUp()

        assertFalse("get() should be false after long-hold release", modifier.get())
        assertFalse("isHeld() should be false after onKeyUp()", modifier.isHeld())
        assertFalse("isLocked() should be false after long-hold release", modifier.isLocked())
    }

    /**
     * Boundary test: elapsed exactly 350ms — strict-less-than means no one-shot fires.
     *
     * onKeyUp() computes: next = !lock && (t - lastTime < nextThreshold) && !preventNext
     * With nextThreshold=350 and elapsed=350: (350 < 350) = false → next = false.
     *
     * Timing note: sleep exactly 350ms to probe the nextThreshold=350ms strict boundary.
     */
    @Test
    fun holdMode_nextThresholdBoundary_exact350ms_doesNotSetNext() {
        modifier.onKeyDown()

        // Sleep exactly 350ms — boundary: (350 < 350) is false, so next is NOT set
        Thread.sleep(350)
        modifier.onKeyUp()

        assertFalse(
            "get() should be false when elapsed == nextThreshold (strict less-than boundary)",
            modifier.get()
        )
    }

    // ── One-shot mode (TEST-07) ──────────────────────────────────────────────

    /**
     * Quick press + release (elapsed ~0ms < 350ms nextThreshold) sets next=true.
     * get() returns true via 'next'. held=false after onKeyUp().
     */
    @Test
    fun oneShot_pressAndQuickRelease_setsNext() {
        modifier.onKeyDown()
        // No sleep — elapsed ~0ms < 350ms nextThreshold → next = true
        modifier.onKeyUp()

        assertTrue("get() should be true after quick press+release (next=true)", modifier.get())
        assertFalse("isHeld() should be false after onKeyUp()", modifier.isHeld())
    }

    /**
     * After one-shot (next=true), nextDidConsume() clears next and sets preventNext=true.
     * get() returns false.
     */
    @Test
    fun oneShot_afterNextDidConsume_clearsNext() {
        modifier.onKeyDown()
        modifier.onKeyUp() // sets next=true
        assertTrue("precondition: get() should be true", modifier.get())

        modifier.nextDidConsume()

        assertFalse("get() should be false after nextDidConsume()", modifier.get())
    }

    /**
     * TEST-07 / FIX-02 — nextDidConsume() is idempotent: calling it twice leaves
     * next=false and preventNext=true with no exception.
     *
     * FIXME FIX-02: Integration bug in InputMethodService.onKeyDown() — consumeModifierNext()
     * is called AFTER the Telex gate, so in Vietnamese mode the Alt one-shot flag is never
     * consumed when a non-transforming key is pressed. The call order must be reversed so
     * consumeModifierNext() runs BEFORE the Telex gate for the alt flag to clear.
     * This unit test verifies that nextDidConsume() works correctly in isolation;
     * the integration-level bug lives in InputMethodService.onKeyDown().
     */
    @Test
    fun oneShot_fxFix02_nextDidConsumeIdempotent() {
        modifier.onKeyDown()
        modifier.onKeyUp() // sets next=true

        modifier.nextDidConsume() // first call: next=false, preventNext=true
        assertFalse("get() should be false after first nextDidConsume()", modifier.get())

        modifier.nextDidConsume() // second call: idempotent — next stays false, preventNext=true
        assertFalse("get() should still be false after second nextDidConsume()", modifier.get())
    }

    // ── Lock mode (TEST-08) ──────────────────────────────────────────────────

    /**
     * Double-tap (two onKeyDown within lockThreshold=250ms) engages lock.
     * After second onKeyDown: lock=true, isLocked()=true, get()=true.
     * After subsequent onKeyUp(): held=false, lock stays true, get()=true still.
     */
    @Test
    fun lockMode_doubleTap_engagesLock() {
        // First tap — sets one-shot
        modifier.onKeyDown()
        modifier.onKeyUp()

        // Second tap within lockThreshold=250ms (10ms elapsed, well within 250ms)
        Thread.sleep(10)
        modifier.onKeyDown()

        assertTrue("isLocked() should be true after double-tap", modifier.isLocked())
        assertTrue("get() should be true when locked", modifier.get())

        // Release — lock persists; held clears; next=false because !lock is false
        modifier.onKeyUp()

        assertTrue("get() should still be true after releasing locked modifier", modifier.get())
        assertTrue("isLocked() should persist after onKeyUp()", modifier.isLocked())
        assertFalse("isHeld() should be false after onKeyUp()", modifier.isHeld())
    }

    /**
     * Third tap after lock clears the lock.
     * Sequence: onKeyDown (1st), onKeyUp, onKeyDown (2nd, within 250ms) → lock=true,
     * onKeyUp, onKeyDown (3rd, within 250ms) → lock toggles to false, onKeyUp+sleep(400) → get()=false.
     */
    @Test
    fun lockMode_thirdTap_clearsLock() {
        // First tap
        modifier.onKeyDown()
        modifier.onKeyUp()

        // Second tap — engages lock
        Thread.sleep(10)
        modifier.onKeyDown()
        modifier.onKeyUp()
        assertTrue("precondition: lock should be engaged before third tap", modifier.isLocked())

        // Third tap within lockThreshold — lock toggles from true to false
        Thread.sleep(10)
        modifier.onKeyDown()
        modifier.onKeyUp()

        // After third tap releases: next = !lock(false) && elapsed<350 && !preventNext(true=set by 3rd onKeyDown)
        // preventNext is set by the 3rd onKeyDown because t-lastTime < lockThreshold → preventNext=true
        // So next = false; get() = lock(false) || held(false) || next(false) = false
        assertFalse("isLocked() should be false after third tap clears lock", modifier.isLocked())
        assertFalse("get() should be false after lock is cleared", modifier.get())
    }

    /**
     * Lock threshold boundary: second onKeyDown exactly 250ms after first.
     * (250 < 250) = false → lock does NOT engage at the strict boundary.
     *
     * Timing note: sleep exactly 250ms to probe lockThreshold=250ms strict boundary.
     */
    @Test
    fun lockMode_lockThresholdBoundary_exact250ms_doesNotLock() {
        // First tap — sets one-shot
        modifier.onKeyDown()
        modifier.onKeyUp()

        // Second tap at exactly 250ms — strict less-than means lock does NOT toggle
        Thread.sleep(250)
        modifier.onKeyDown()

        assertFalse(
            "isLocked() should be false when elapsed == lockThreshold (strict less-than boundary)",
            modifier.isLocked()
        )
    }

    // ── nextDidConsume (TEST-09) ─────────────────────────────────────────────

    /**
     * Dedicated coverage for nextDidConsume() clearing the one-shot state.
     */
    @Test
    fun nextDidConsume_afterOneShot_clearsNext() {
        // Use activateForNext() to set next=true directly (simulates InputMethodService shortcut)
        modifier.activateForNext()
        assertTrue("precondition: get() should be true after activateForNext()", modifier.get())

        modifier.nextDidConsume()

        assertFalse("get() should be false after nextDidConsume()", modifier.get())
    }

    /**
     * nextDidConsume() when next=false is idempotent — no exception, state unchanged (still false).
     */
    @Test
    fun nextDidConsume_whenNextFalse_isIdempotent() {
        // Fresh modifier — next=false, lock=false, held=false, get()=false
        assertFalse("precondition: fresh modifier get() should be false", modifier.get())

        // Call nextDidConsume() with next already false — should not throw, should remain false
        modifier.nextDidConsume()

        assertFalse("get() should remain false after nextDidConsume() called with next=false", modifier.get())
    }

    /**
     * nextDidConsume() sets preventNext=true, which blocks the next quick-press from
     * re-triggering one-shot (the subsequent onKeyDown sets preventNext=lock||next when not
     * double-tapping; but first let us verify that a quick press after nextDidConsume does NOT
     * produce get()=true).
     *
     * Sequence: activate one-shot → nextDidConsume() → quick onKeyDown()+onKeyUp() → get()=false.
     *
     * Analysis: After nextDidConsume(): next=false, preventNext=true.
     * Next onKeyDown() (not double-tap): preventNext = lock(false) || next(false) = false. Overwrites preventNext!
     * Then onKeyUp(): next = !lock(false) && elapsed<350 && !preventNext(false) → next=true.
     * So get()=true again. The preventNext set by nextDidConsume() is CLEARED by a subsequent onKeyDown()
     * that is not a double-tap. This is correct behavior — the test documents it.
     */
    @Test
    fun nextDidConsume_setsPreventNext_subsequentPress_behavesNormally() {
        // Activate one-shot and consume it
        modifier.activateForNext()
        modifier.nextDidConsume()
        assertFalse("precondition: get() should be false after consume", modifier.get())

        // After nextDidConsume: preventNext=true
        // A new quick press: onKeyDown() resets preventNext = lock||next = false||false = false
        // Then onKeyUp() with elapsed<350ms: next = !lock && elapsed<350 && !preventNext = true
        // So the key gets consumed, and a NEW one-shot is started on the next keyUp
        modifier.onKeyDown()
        modifier.onKeyUp() // elapsed~0ms → next=true (preventNext was overwritten by onKeyDown)

        // Document: preventNext from nextDidConsume does NOT survive across a fresh onKeyDown()
        // The new onKeyDown overwrites it. The key-tap after consume starts a fresh one-shot.
        assertTrue(
            "After nextDidConsume then a fresh quick tap, get()=true (new one-shot started; preventNext cleared by onKeyDown)",
            modifier.get()
        )
    }
}
