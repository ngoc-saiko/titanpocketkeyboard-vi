package io.github.oin.titanpocketkeyboard

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToneMarkTracerTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    /**
     * Telex spec: pressing 'a' then 's' should produce "á" (acute accent on 'a').
     * toneMarks maps 's' to '\'' and toneMapping maps 'a' + '\'' to 'á'.
     * If this test fails, the bug is in the current implementation — do NOT adjust the
     * assertion to match wrong output. The assertion documents the CORRECT Telex spec.
     */
    @Test
    fun toneMarkS_onBareA_producesAcute() {
        val vti = VietnameseTextInput()

        // Load 'a' into buffer — bare vowel commits itself and returns "a"
        val firstResult = vti.processKey('a')
        assertEquals("a", firstResult)

        // Apply tone mark 's' — should produce "á" per Telex spec
        val result = vti.processKey('s')
        assertEquals("á", result)
    }

    /**
     * Modifier hold-mode: pressing down activates the modifier (held=true → get()=true),
     * releasing quickly sets next=true (get() still true), and after nextDidConsume()
     * both held and next are cleared (get()=false).
     */
    @Test
    fun modifier_hold_activatesAndReleases() {
        val modifier = Modifier()

        // Before any press, modifier is inactive
        assertFalse("modifier should be inactive before first press", modifier.get())

        // Press down — held becomes true
        modifier.onKeyDown()
        assertTrue("modifier should be active after onKeyDown()", modifier.get())

        // Release quickly (~0ms elapsed) — held clears, but next becomes true because
        // elapsed time < nextThreshold (350ms). get() remains true via next=true.
        modifier.onKeyUp()
        assertTrue(
            "modifier should still be active after immediate release (next=true)",
            modifier.get()
        )

        // nextDidConsume() clears the 'next' flag — modifier becomes fully inactive
        modifier.nextDidConsume()
        assertFalse("modifier should be inactive after nextDidConsume()", modifier.get())
    }
}
