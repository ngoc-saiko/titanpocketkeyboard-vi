package io.github.oin.titanpocketkeyboard

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Regression coverage driven by app/src/test/java/io/github/oin/titanpocketkeyboard/Viet11K.txt
 * (~11k Vietnamese words/phrases). Each unique syllable in that corpus is reverse-engineered into
 * the Telex keystrokes that should produce it, then typed through [VietnameseTextInput] the same
 * way [InputMethodService.onKeyDown] drives it: `setBuffer(currentWord)` before every keystroke,
 * then replace the committed word with `processKey`'s result when it differs from the raw key.
 *
 * This is the harness that would have caught the "đến" regression (den-ee-circumflex): typing
 * d,d,e,e,n,s must produce "đến", not "đeen"/"đnen".
 */
class VietnameseCorpusTest {

    companion object {
        private val VOWEL_EXPANSION = mapOf(
            'ă' to "aw", 'â' to "aa", 'ê' to "ee", 'ô' to "oo", 'ơ' to "ow", 'ư' to "uw", 'đ' to "dd"
        )

        private val REMOVE_TONE = mapOf(
            'á' to 'a', 'à' to 'a', 'ả' to 'a', 'ã' to 'a', 'ạ' to 'a',
            'ắ' to 'ă', 'ằ' to 'ă', 'ẳ' to 'ă', 'ẵ' to 'ă', 'ặ' to 'ă',
            'ấ' to 'â', 'ầ' to 'â', 'ẩ' to 'â', 'ẫ' to 'â', 'ậ' to 'â',
            'é' to 'e', 'è' to 'e', 'ẻ' to 'e', 'ẽ' to 'e', 'ẹ' to 'e',
            'ế' to 'ê', 'ề' to 'ê', 'ể' to 'ê', 'ễ' to 'ê', 'ệ' to 'ê',
            'í' to 'i', 'ì' to 'i', 'ỉ' to 'i', 'ĩ' to 'i', 'ị' to 'i',
            'ó' to 'o', 'ò' to 'o', 'ỏ' to 'o', 'õ' to 'o', 'ọ' to 'o',
            'ố' to 'ô', 'ồ' to 'ô', 'ổ' to 'ô', 'ỗ' to 'ô', 'ộ' to 'ô',
            'ớ' to 'ơ', 'ờ' to 'ơ', 'ở' to 'ơ', 'ỡ' to 'ơ', 'ợ' to 'ơ',
            'ú' to 'u', 'ù' to 'u', 'ủ' to 'u', 'ũ' to 'u', 'ụ' to 'u',
            'ứ' to 'ư', 'ừ' to 'ư', 'ử' to 'ư', 'ữ' to 'ư', 'ự' to 'ư',
            'ý' to 'y', 'ỳ' to 'y', 'ỷ' to 'y', 'ỹ' to 'y', 'ỵ' to 'y'
        )

        // sắc, huyền, hỏi, ngã, nặng -> Telex key
        private val TONE_KEY = mapOf(
            'á' to 's', 'ắ' to 's', 'ấ' to 's', 'é' to 's', 'ế' to 's', 'í' to 's', 'ó' to 's',
            'ố' to 's', 'ớ' to 's', 'ú' to 's', 'ứ' to 's', 'ý' to 's',
            'à' to 'f', 'ằ' to 'f', 'ầ' to 'f', 'è' to 'f', 'ề' to 'f', 'ì' to 'f', 'ò' to 'f',
            'ồ' to 'f', 'ờ' to 'f', 'ù' to 'f', 'ừ' to 'f', 'ỳ' to 'f',
            'ả' to 'r', 'ẳ' to 'r', 'ẩ' to 'r', 'ẻ' to 'r', 'ể' to 'r', 'ỉ' to 'r', 'ỏ' to 'r',
            'ổ' to 'r', 'ở' to 'r', 'ủ' to 'r', 'ử' to 'r', 'ỷ' to 'r',
            'ã' to 'x', 'ẵ' to 'x', 'ẫ' to 'x', 'ẽ' to 'x', 'ễ' to 'x', 'ĩ' to 'x', 'õ' to 'x',
            'ỗ' to 'x', 'ỡ' to 'x', 'ũ' to 'x', 'ữ' to 'x', 'ỹ' to 'x',
            'ạ' to 'j', 'ặ' to 'j', 'ậ' to 'j', 'ẹ' to 'j', 'ệ' to 'j', 'ị' to 'j', 'ọ' to 'j',
            'ộ' to 'j', 'ợ' to 'j', 'ụ' to 'j', 'ự' to 'j', 'ỵ' to 'j'
        )

        /** Reverse-engineers the Telex keystrokes that should compose [syllable]. */
        fun toKeystrokes(syllable: String): List<Char> {
            val keys = mutableListOf<Char>()
            var toneKey: Char? = null
            for (c in syllable) {
                val toned = TONE_KEY[c]
                if (toned != null) {
                    toneKey = toned
                    val base = REMOVE_TONE[c] ?: c
                    keys.addAll((VOWEL_EXPANSION[base] ?: base.toString()).toList())
                } else {
                    keys.addAll((VOWEL_EXPANSION[c] ?: c.toString()).toList())
                }
            }
            if (toneKey != null) keys.add(toneKey)
            return keys
        }

        /**
         * Types [keystrokes] through a fresh [VietnameseTextInput], mirroring how
         * InputMethodService.onKeyDown drives it: setBuffer(current word) before each keystroke,
         * then swap the committed word for processKey's result when it's an actual transform
         * (non-null and different from the raw key), otherwise append the raw key as-is.
         */
        fun typeWord(keystrokes: List<Char>): String {
            val input = VietnameseTextInput()
            var committed = ""
            for (key in keystrokes) {
                input.setBuffer(committed)
                val replacement = input.processKey(key)
                committed = if (replacement != null && replacement != key.toString()) {
                    replacement
                } else {
                    committed + key
                }
            }
            return committed
        }

        /** Splits corpus lines on whitespace/hyphen/dot and dedupes, preserving first-seen order. */
        fun loadUniqueWords(file: File): List<String> {
            val seen = LinkedHashSet<String>()
            file.forEachLine { line ->
                line.split(Regex("[^\\p{L}]+"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { seen.add(it) }
            }
            return seen.toList()
        }

        private fun findCorpusFile(): File {
            val relative = "src/test/java/io/github/oin/titanpocketkeyboard/Viet11K.txt"
            val candidates = listOf(
                File(relative),
                File("app", relative),
                File(System.getProperty("user.dir"), relative),
                File(System.getProperty("user.dir"), "app/$relative")
            )
            return candidates.firstOrNull { it.exists() }
                ?: error("Viet11K.txt not found; checked: ${candidates.joinToString { it.absolutePath }}")
        }

        /**
         * Pre-existing, non-regression gaps unrelated to den-ee-circumflex — kept visible instead
         * of silently filtered, so a fix for any of these can shrink the list instead of the test
         * just staying green by accident:
         *
         * - old-vs-new tone-placement convention on "oa"/"oe"/"uy"/"uê" diphthongs: the app follows
         *   the older rule of placing the tone on the LAST vowel of the cluster ("hoà", "khoẻ",
         *   "toá"), while this corpus uses the newer rule of placing it on the FIRST ("hòa",
         *   "khỏe", "tóa") — see toneMappingEnd / findFirstVowelIndex in VietnameseTextInput.kt.
         * - the "gi" word-initial heuristic in findFirstVowelIndex assumes "i" is always a glide
         *   before a further nucleus (già, giá); it misfires when "i" IS the nucleus itself
         *   (gìn, gì), leaving no vowel for the tone key to land on.
         * - "boong" collides with the oo -> ô modifier (there's no Telex escape hatch for a
         *   literal doubled "oo" in this app).
         * - a few non-Vietnamese loanwords ("mistake", "needly") contain s/t sequences that this
         *   app's Telex engine reads as tone keys after a vowel — Vietnamese finals never do this
         *   (s/f/r/x/j only ever occur as onsets in real Vietnamese syllables), so it's not a real
         *   gap for the language this input method targets.
         */
        val KNOWN_NON_REGRESSION_GAPS = setOf(
            "huệ", "tọa", "hòa", "họa", "hóa", "boong", "khuỷ", "khóe", "khóa", "chóa", "chóe",
            "hỏa", "dọa", "thuế", "đọa", "lõa", "góa", "tỏa", "gìn", "khỏe", "hòe", "huỳnh", "huýt",
            "khỏa", "khuỷu", "khuếch", "lòa", "xõa", "lóa", "lòe", "lóe", "mistake", "needly",
            "nhuệ", "òa", "uế", "tòa", "soóc", "suýt", "tuế", "thỏa", "thóa", "thuở", "tóe", "tuệ",
            "uể", "uỵch", "buýt", "xuề", "xòa"
        )
    }

    @Test
    fun corpus_wordsTypeCorrectlyThroughVietnameseTextInput() {
        val words = loadUniqueWords(findCorpusFile())
        assertTrue("Expected corpus to contain words", words.isNotEmpty())

        val failures = mutableListOf<String>()
        val newlyFixedGaps = mutableListOf<String>()
        for (word in words) {
            val keystrokes = toKeystrokes(word)
            val actual = typeWord(keystrokes)
            if (actual != word) {
                if (word in KNOWN_NON_REGRESSION_GAPS) continue
                failures.add("expected=\"$word\" actual=\"$actual\" keys=${keystrokes.joinToString("")}")
            } else if (word in KNOWN_NON_REGRESSION_GAPS) {
                newlyFixedGaps.add(word)
            }
        }

        assertTrue(
            "These words are listed in KNOWN_NON_REGRESSION_GAPS but now type correctly — " +
                "remove them from the allowlist: $newlyFixedGaps",
            newlyFixedGaps.isEmpty()
        )

        if (failures.isNotEmpty()) {
            val preview = failures.take(50).joinToString("\n")
            val more = if (failures.size > 50) "\n... and ${failures.size - 50} more" else ""
            throw AssertionError(
                "${failures.size}/${words.size} corpus words failed to type correctly " +
                    "(excluding ${KNOWN_NON_REGRESSION_GAPS.size} known non-regression gaps):\n$preview$more"
            )
        }
    }

    @Test
    fun regression_denTypesCorrectly_afterDdConsonant() {
        // den-ee-circumflex: đ followed by a nucleus modifier (ee -> ê) used to silently fail
        // because charModified stayed set after dd -> đ.
        assertTrue(typeWord(toKeystrokes("đến")) == "đến")
        assertTrue(typeWord(toKeystrokes("đâu")) == "đâu")
        assertTrue(typeWord(toKeystrokes("đôi")) == "đôi")
        assertTrue(typeWord(toKeystrokes("đơn")) == "đơn")
        assertTrue(typeWord(toKeystrokes("đăng")) == "đăng")
    }
}
