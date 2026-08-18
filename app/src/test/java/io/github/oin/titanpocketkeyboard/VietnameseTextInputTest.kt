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

// ---------------------------------------------------------------------------
// TEST-03 — Revert sequences (charModifier reverse path)
// ---------------------------------------------------------------------------

/**
 * Tests that typing a modifier key twice reverts the composed character back to
 * the raw input sequence (e.g., ă + 'a' → "aa").
 *
 * Known bugs documented per REQUIREMENTS.md:
 *   FIX-01: charModified not set for 'w'; the ơw→ow and ưw→uw revert paths never fire.
 */
class RevertSequenceTest {

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

    /** a+w → ă, then +a → reverseCharModifier "ăa"→"aa". charModified=true for 'a', so revert fires. */
    @Test
    fun revert_aw_thenA_producesAa() {
        val vti = VietnameseTextInput()
        vti.processKey('a')
        vti.processKey('w')  // buffer="ă", charModified NOT set (w excluded per line 235)
        // After aw: buffer is "ă", charModified=false (w not in set)
        // But wait — 'a' + 'w' fires applyWCharModifiers first (w branch), then checks charModifiers
        // Actually for 'a'+w: 'a' is appended (charModified stays false for 'a' alone),
        // then processKey('w'): w branch — applyWCharModifiers on "a" → wCharModifiers["a"]="ă" → "ă" != "a"
        // returns "ă" without appending. So charModified is never set via applyCharModifiers "aw"→"ă".
        // The reverseCharModifier path needs charModified=true.
        // For aw→ă: the 'w' branch returns early (line 193: return newStr). charModified is not set.
        // Therefore revert_aw_thenA may also be RED. Let's verify by tracing code:
        //   processKey('a'): not in toneMarks, not 'w', appends 'a', applyCharModifiers('a'): no match → false, returns "a"
        //   processKey('w'): w branch: applyWCharModifiers() on "a" → "ă", "ă"!="a" → returns "ă" (early return, charModified=false)
        //   processKey('a'): not 'w', appends 'a' → buffer="ăa", applyCharModifiers('a'): charModified=false, checks charModifiers: "ăa" not in charModifiers → false, returns "a"
        // Result: "a" (processKey returns char.toString() for last 'a' since no modifier matched)
        // Actually the buffer is "ăa" and processKey returns "a" — NOT "aa".
        // The revert path requires charModified=true. For 'a'+w path, charModified is never set.
        // FIXME FIX-01 also affects aw revert: charModified not set via w-early-return path.
        // The aa→â path DOES set charModified. Let's confirm with revert_aa_thenA below.
        // For this test: a+w→ă uses the w-early-return, so charModified=false. Typing 'a' appends and no revert.
        // CORRECT SPEC: aw reversal should give "aa". Currently gives "a" (last processKey returns "a").
        // FIXME FIX-01: charModified not set when 'w' causes modification via applyWCharModifiers early-return path; revert of aw sequence does not fire; buffer becomes "ăa", processKey returns "a"
        val result = vti.processKey('a')
        assertEquals("revert a+w then a: correct spec is aa", "aa", result)
    }

    /** a+a → â (charModified=true via charModifiers map), then +a → reverseCharModifier "âa"→"aa". */
    @Test
    fun revert_aa_thenA_producesAa() {
        val vti = VietnameseTextInput()
        vti.processKey('a')  // buffer="a"
        vti.processKey('a')  // applyCharModifiers: "aa"→"â", charModified=true (char='a', not 'd', not 'w')
        val result = vti.processKey('a')  // charModified=true → reverseCharModifier "âa"→"aa" → returns "aa"
        assertEquals("revert a+a then a: should produce aa", "aa", result)
    }

    /** o+o → ô (charModified=true), then +o → reverseCharModifier "ôo"→"oo". */
    @Test
    fun revert_oo_thenO_producesOo() {
        val vti = VietnameseTextInput()
        vti.processKey('o')
        vti.processKey('o')  // "oo"→"ô", charModified=true
        val result = vti.processKey('o')  // reverseCharModifier "ôo"→"oo"
        assertEquals("revert o+o then o: should produce oo", "oo", result)
    }

    /**
     * o+w → ơ via applyWCharModifiers early-return, then +w — CORRECT SPEC: should return "ow".
     *
     * FIXME FIX-01: charModified not set for 'w'; the early-return path in processKey (line 193)
     * returns "ơ" without setting charModified. When 'w' is typed again, charModified=false so
     * the reverse path is skipped. processKey appends 'w', then checks charModifiers: "ơw" not
     * in charModifiers (only plain ascii pairs like "ow" are keys) → returns "w".
     * Actual result: buffer="ơw", processKey returns "w" — not "ow".
     */
    @Test
    fun revert_ow_thenW_producesOw() {
        val vti = VietnameseTextInput()
        vti.processKey('o')
        vti.processKey('w')  // applyWCharModifiers "o"→"ơ", early return, charModified NOT set
        // FIXME FIX-01: charModified not set for w; revert path skipped; currently returns "w" (last char appended)
        val result = vti.processKey('w')
        assertEquals("revert o+w then w: correct spec is ow", "ow", result)
    }

    /** e+e → ê (charModified=true), then +e → reverseCharModifier "êe"→"ee". */
    @Test
    fun revert_ee_thenE_producesEe() {
        val vti = VietnameseTextInput()
        vti.processKey('e')
        vti.processKey('e')  // "ee"→"ê", charModified=true
        val result = vti.processKey('e')  // reverseCharModifier "êe"→"ee"
        assertEquals("revert e+e then e: should produce ee", "ee", result)
    }

    /** d+d → đ (charModified NOT set — 'd' is excluded per line 234), then +d. */
    @Test
    fun revert_dd_thenD_producesDd() {
        val vti = VietnameseTextInput()
        vti.processKey('d')
        vti.processKey('d')  // "dd"→"đ", charModified=false (char=='d' excluded)
        // charModified=false for 'd' → reverseCharModifier path won't fire
        // processKey('d'): appends 'd' → buffer="đd", applyCharModifiers('d'): charModified=false,
        // checks charModifiers: "đd" not a key → false. Returns "d".
        // CORRECT SPEC: dd revert should give "dd".
        // FIXME FIX-01 (secondary): charModified not set for 'd' either; revert of dd sequence fails similarly.
        // However, unlike 'w', the reverseCharModifier does have "đd"→"dd". The bug is only that charModified=false.
        // For now test asserts correct spec "dd" — will fail until Phase 3.
        val result = vti.processKey('d')
        assertEquals("revert d+d then d: correct spec is dd", "dd", result)
    }
}

// ---------------------------------------------------------------------------
// TEST-04 — Invalid sequences passthrough
// ---------------------------------------------------------------------------

/**
 * Tests that buffers containing entries from invalidSequences cause processKey()
 * to pass through the typed character unchanged instead of applying Telex transforms.
 */
class InvalidSequenceTest {

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

    /** Buffer "pr" is in invalidSequences — tone mark 's' passes through as "s". */
    @Test
    fun invalid_prBuffer_toneMarkPassesThrough() {
        val vti = VietnameseTextInput()
        vti.setBuffer("pr")
        val result = vti.processKey('s')
        assertEquals("pr buffer + s: tone mark should pass through", "s", result)
    }

    /** Buffer "rr" is in invalidSequences — vowel 'a' passes through as "a". */
    @Test
    fun invalid_rrBuffer_vowelPassesThrough() {
        val vti = VietnameseTextInput()
        vti.setBuffer("rr")
        val result = vti.processKey('a')
        assertEquals("rr buffer + a: vowel should pass through", "a", result)
    }

    /** Buffer "ou" is in invalidSequences — tone mark 's' passes through as "s". */
    @Test
    fun invalid_ouBuffer_toneMarkPassesThrough() {
        val vti = VietnameseTextInput()
        vti.setBuffer("ou")
        val result = vti.processKey('s')
        assertEquals("ou buffer + s: tone mark should pass through", "s", result)
    }

    /**
     * 'z' is in ignoredChars and not in modifiableChars or toneMarks.keys — the first guard
     * in processKey fires and returns "z" immediately without any buffer check.
     */
    @Test
    fun invalid_singleZ_passesThrough() {
        val vti = VietnameseTextInput()
        val result = vti.processKey('z')
        assertEquals("z is not modifiable: should pass through as z", "z", result)
    }
}

// ---------------------------------------------------------------------------
// TEST-05 — Multi-vowel tone placement (nucleus rule)
// ---------------------------------------------------------------------------

/**
 * Tests tone mark placement on multi-vowel sequences. The correct spec is that the
 * tone mark lands on the nucleus vowel, not necessarily the first vowel.
 *
 * toneMappingEnd covers ươ, iê, uô, oe, uyê, oai, oa, oă, uâ as special cases.
 * For "uo" — no toneMappingEnd key matches, so findFirstVowelIndex finds 'u' at index 0.
 *
 * Known bug documented per REQUIREMENTS.md:
 *   FIX-03: findFirstVowelIndex returns first vowel index (0) for "uo"; tone lands on 'u'
 *   giving "úo" instead of correct nucleus placement on 'o' giving "uó".
 */
class TonePlacementTest {

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

    /**
     * "uo" buffer + 's' — CORRECT SPEC: nucleus is 'o', tone should land on 'o' giving "uó".
     *
     * FIXME FIX-03: findFirstVowelIndex returns index 0 (u); tone lands on u giving "úo" instead of "uó".
     * This test is KNOWN RED until Phase 3 fix.
     */
    @Test
    fun tonePlacement_uoSuffix_toneOnSecondVowel() {
        val vti = VietnameseTextInput()
        vti.setBuffer("uo")
        // FIXME FIX-03: findFirstVowelIndex returns index 0 (u); tone lands on u giving úo instead of uó
        val result = vti.processKey('s')
        assertEquals("uo+s: correct spec is uó (nucleus on o)", "uó", result)
    }

    /** "ươ" buffer + 's' — toneMappingEnd["ươ"]='ơ'; index of 'ơ' in "ươ" is 1; tone on 'ơ' → "ướ". */
    @Test
    fun tonePlacement_ươ_toneOnO() {
        val vti = VietnameseTextInput()
        vti.setBuffer("ươ")
        val result = vti.processKey('s')
        assertEquals("ươ+s: tone on ơ giving ướ", "ướ", result)
    }

    /** "iê" buffer + 's' — toneMappingEnd["iê"]='ê'; index of 'ê' in "iê" is 1; tone on 'ê' → "iế". */
    @Test
    fun tonePlacement_iê_toneOnE() {
        val vti = VietnameseTextInput()
        vti.setBuffer("iê")
        val result = vti.processKey('s')
        assertEquals("iê+s: tone on ê giving iế", "iế", result)
    }

    /** "uô" buffer + 's' — toneMappingEnd["uô"]='ô'; index of 'ô' in "uô" is 1; tone on 'ô' → "uố". */
    @Test
    fun tonePlacement_uô_toneOnO() {
        val vti = VietnameseTextInput()
        vti.setBuffer("uô")
        val result = vti.processKey('s')
        assertEquals("uô+s: tone on ô giving uố", "uố", result)
    }

    /**
     * "gia" buffer + 's' — findFirstVowelIndex skips gi-prefix (i < 2 and len > 2 and starts with "gi");
     * first non-skipped vowel is 'a' at index 2; tone on 'a' → "giá".
     */
    @Test
    fun tonePlacement_giPrefix_skipToA() {
        val vti = VietnameseTextInput()
        vti.setBuffer("gia")
        val result = vti.processKey('s')
        assertEquals("gia+s: gi-prefix skip, tone on a giving giá", "giá", result)
    }

    /**
     * "qua" buffer + 's' — findFirstVowelIndex skips qu-prefix (i < 2 and len > 2 and starts with "qu");
     * first non-skipped vowel is 'a' at index 2; tone on 'a' → "quá".
     */
    @Test
    fun tonePlacement_quPrefix_skipToA() {
        val vti = VietnameseTextInput()
        vti.setBuffer("qua")
        val result = vti.processKey('s')
        assertEquals("qua+s: qu-prefix skip, tone on a giving quá", "quá", result)
    }

    // -----------------------------------------------------------------------
    // Regression: yeu-tone-mark-wrong-vowel — "yê" cluster nucleus is 'ê', not 'y'.
    // toneMappingEnd["yê"]='ê'. Before the fix, RULE B placed the tone on 'y' → "ýêu".
    // Oracle: derived (Vietnamese orthography — tone lands on the hat/whisker vowel 'ê').
    // -----------------------------------------------------------------------

    /**
     * Reported case: typing y,e,e,u composes buffer "yêu" (ee→ê), then 's' applies sắc.
     * Correct: tone on nucleus 'ê' → "yếu". Was "ýêu" before the fix.
     * This exercises the full processKey input path, not just a preloaded buffer.
     */
    @Test
    fun tonePlacement_yeu_fullPath_toneOnE() {
        val vti = VietnameseTextInput()
        vti.processKey('y')
        vti.processKey('e')
        vti.processKey('e')   // ee → ê, buffer "yê"
        vti.processKey('u')   // buffer "yêu"
        val result = vti.processKey('s')
        assertEquals("yeu(full path)+s: nucleus ê giving yếu", "yếu", result)
    }

    /** Boundary: open-syllable "yêu" preloaded + all 5 tone marks land on 'ê'. */
    @Test
    fun tonePlacement_yeu_allFiveTones() {
        val cases = mapOf('s' to "yếu", 'f' to "yều", 'r' to "yểu", 'x' to "yễu", 'j' to "yệu")
        for ((toneKey, expected) in cases) {
            val vti = VietnameseTextInput()
            vti.setBuffer("yêu")
            val result = vti.processKey(toneKey)
            assertEquals("yêu+$toneKey: tone on nucleus ê", expected, result)
        }
    }

    /** Boundary: closed-syllable "yêt" (yết) — nucleus still 'ê' → "yết". */
    @Test
    fun tonePlacement_yet_closedSyllable_toneOnE() {
        val vti = VietnameseTextInput()
        vti.setBuffer("yêt")
        val result = vti.processKey('s')
        assertEquals("yêt+s: closed syllable, tone on ê giving yết", "yết", result)
    }

    /** Boundary: "yên" (yến) — nucleus 'ê' → "yến". */
    @Test
    fun tonePlacement_yen_toneOnE() {
        val vti = VietnameseTextInput()
        vti.setBuffer("yên")
        val result = vti.processKey('s')
        assertEquals("yên+s: tone on ê giving yến", "yến", result)
    }

    /**
     * Regression guard: labialized triphthong "uyê" (khuyên/tuyên) contains "yê" as a substring.
     * "uyê" must keep precedence and still resolve to nucleus 'ê'. Both map to 'ê', so the
     * fix must not disturb this. Buffer "khuyê" + 's' → "khuyế".
     */
    @Test
    fun tonePlacement_uye_triphthong_stillToneOnE() {
        val vti = VietnameseTextInput()
        vti.setBuffer("khuyê")
        val result = vti.processKey('s')
        assertEquals("khuyê+s: triphthong nucleus ê giving khuyế", "khuyế", result)
    }

    /**
     * Regression guard: the analogous "iê" cluster (tiên) must remain unaffected — tone on 'ê'.
     */
    @Test
    fun tonePlacement_ie_stillToneOnE() {
        val vti = VietnameseTextInput()
        vti.setBuffer("tiê")
        val result = vti.processKey('s')
        assertEquals("tiê+s: nucleus ê giving tiế", "tiế", result)
    }
}

// ---------------------------------------------------------------------------
// TEST-12 — applyWCharModifiers last-char-only semantics
// ---------------------------------------------------------------------------

/**
 * Tests for applyWCharModifiers() verifying that only the LAST w-mappable vowel
 * should be transformed.
 *
 * Current implementation maps EVERY character in the buffer via buffer.map{}.
 * This correctly handles single-vowel and non-vowel-prefix cases, but fails
 * when multiple w-mappable vowels are present.
 *
 * Known bugs documented per REQUIREMENTS.md:
 *   FIX-04: applyWCharModifiers transforms all w-mappable vowels, not just the last;
 *   "ao"+w → "ăơ" (should be "aơ"), "aa"+w → "ăă" (should be "aă").
 */
class WCharModifiersTest {

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

    /** Single vowel 'a' + 'w': wCharModifiers["a"]="ă"; single char buffer → only one transform → "ă". */
    @Test
    fun wModifiers_singleVowel_transforms() {
        val vti = VietnameseTextInput()
        vti.processKey('a')  // buffer="a"
        val result = vti.processKey('w')  // applyWCharModifiers: "a"→"ă"; "ă"!="a" → return "ă"
        assertEquals("a+w: single vowel transforms to ă", "ă", result)
    }

    /**
     * Buffer "ba" + 'w': wCharModifiers has no entry for "b" (passes through), "a"→"ă".
     * Current implementation maps all chars: 'b'→'b', 'a'→"ă" → "bă".
     * This matches correct spec (last char 'a' is transformed). EXPECTED GREEN.
     */
    @Test
    fun wModifiers_nonVowelThenVowel_lastCharTransforms() {
        val vti = VietnameseTextInput()
        vti.setBuffer("ba")
        val result = vti.processKey('w')
        assertEquals("ba+w: non-vowel b passes through, a→ă giving bă", "bă", result)
    }

    /**
     * Buffer "ao" + 'w' — CORRECT SPEC: only last 'o' should transform → "aơ".
     *
     * FIXME FIX-04: current code transforms all w-mappable vowels; actual result is "ăơ" not "aơ".
     * This test is KNOWN RED until Phase 3 fix.
     */
    @Test
    fun wModifiers_twoDistinctVowels_onlyLastTransforms() {
        val vti = VietnameseTextInput()
        vti.setBuffer("ao")
        // FIXME FIX-04: current code transforms all w-mappable vowels; actual result is ăơ not aơ
        val result = vti.processKey('w')
        assertEquals("ao+w: only last vowel o transforms giving aơ", "aơ", result)
    }

    /**
     * Buffer "aa" + 'w' — CORRECT SPEC: only last 'a' should transform → "aă".
     *
     * FIXME FIX-04: current code transforms all w-mappable vowels; actual result is "ăă" not "aă".
     * This test is KNOWN RED until Phase 3 fix.
     */
    @Test
    fun wModifiers_twoSameVowels_onlyLastTransforms() {
        val vti = VietnameseTextInput()
        vti.setBuffer("aa")
        // FIXME FIX-04: current code transforms all w-mappable vowels; actual result is ăă not aă
        val result = vti.processKey('w')
        assertEquals("aa+w: only last vowel a transforms giving aă", "aă", result)
    }

    /**
     * Buffer "oa" + 'w' — applyWCharModifiers maps: 'o'→"ơ", 'a'→"ă" → "ơă".
     * Then the "ơă"→"oă" special-case replacement fires → "oă".
     * This special case partially corrects the bad intermediate but does not implement
     * last-char-only semantics. Result "oă" matches correct spec for "oa". EXPECTED GREEN.
     */
    @Test
    fun wModifiers_oroakSpecialCase() {
        val vti = VietnameseTextInput()
        vti.setBuffer("oa")
        val result = vti.processKey('w')
        // oa+w: wCharModifiers maps o→ơ, a→ă giving "ơă", then "ơă"→"oă" special case fires
        assertEquals("oa+w: ơă special-case replacement gives oă", "oă", result)
    }
}
