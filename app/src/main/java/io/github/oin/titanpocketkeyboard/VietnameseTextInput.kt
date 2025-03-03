import android.util.Log

class VietnameseTextInput {
    private var buffer = StringBuilder()
    public var isReverseTone = false
    public var toneAdded = false
    private var lastToneMark: Char? = null
    private var charModified = false

    private val vowelMap = setOf(
        'a', 'ă', 'â', 'e', 'ê', 'i', 'o', 'ô', 'ơ', 'u', 'ư', 'y',
        'A', 'Ă', 'Â', 'E', 'Ê', 'I', 'O', 'Ô', 'Ơ', 'U', 'Ư', 'Y'
    )

    public val modifiableChars = setOf ('a', 'e', 'i', 'o', 'u', 'd', 'w')

    private val tonedVowelSet = setOf(
        'á', 'à', 'ả', 'ã', 'ạ', 'ắ', 'ằ', 'ẳ', 'ẵ', 'ặ', 'ấ', 'ầ', 'ẩ', 'ẫ', 'ậ',
        'é', 'è', 'ẻ', 'ẽ', 'ẹ', 'ế', 'ề', 'ể', 'ễ', 'ệ',
        'í', 'ì', 'ỉ', 'ĩ', 'ị',
        'ó', 'ò', 'ỏ', 'õ', 'ọ', 'ố', 'ồ', 'ổ', 'ỗ', 'ộ', 'ớ', 'ờ', 'ở', 'ỡ', 'ợ',
        'ú', 'ù', 'ủ', 'ũ', 'ụ', 'ứ', 'ừ', 'ử', 'ữ', 'ự',
        'ý', 'ỳ', 'ỷ', 'ỹ', 'ỵ',

        'Á', 'À', 'Ả', 'Ã', 'Ạ', 'Ắ', 'Ằ', 'Ẳ', 'Ẵ', 'Ặ', 'Ấ', 'Ầ', 'Ẩ', 'Ẫ', 'Ậ',
        'É', 'È', 'Ẻ', 'Ẽ', 'Ẹ', 'Ế', 'Ề', 'Ể', 'Ễ', 'Ệ',
        'Í', 'Ì', 'Ỉ', 'Ĩ', 'Ị',
        'Ó', 'Ò', 'Ỏ', 'Õ', 'Ọ', 'Ố', 'Ồ', 'Ổ', 'Ỗ', 'Ộ', 'Ớ', 'Ờ', 'Ở', 'Ỡ', 'Ợ',
        'Ú', 'Ù', 'Ủ', 'Ũ', 'Ụ', 'Ứ', 'Ừ', 'Ử', 'Ữ', 'Ự',
        'Ý', 'Ỳ', 'Ỷ', 'Ỹ', 'Ỵ'
    )

    private val removeToneMap = mapOf(
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

        'ý' to 'y', 'ỳ' to 'y', 'ỷ' to 'y', 'ỹ' to 'y', 'ỵ' to 'y',

        // Uppercase letters
        'Á' to 'A', 'À' to 'A', 'Ả' to 'A', 'Ã' to 'A', 'Ạ' to 'A',
        'Ắ' to 'Ă', 'Ằ' to 'Ă', 'Ẳ' to 'Ă', 'Ẵ' to 'Ă', 'Ặ' to 'Ă',
        'Ấ' to 'Â', 'Ầ' to 'Â', 'Ẩ' to 'Â', 'Ẫ' to 'Â', 'Ậ' to 'Â',

        'É' to 'E', 'È' to 'E', 'Ẻ' to 'E', 'Ẽ' to 'E', 'Ẹ' to 'E',
        'Ế' to 'Ê', 'Ề' to 'Ê', 'Ể' to 'Ê', 'Ễ' to 'Ê', 'Ệ' to 'Ê',

        'Í' to 'I', 'Ì' to 'I', 'Ỉ' to 'I', 'Ĩ' to 'I', 'Ị' to 'I',

        'Ó' to 'O', 'Ò' to 'O', 'Ỏ' to 'O', 'Õ' to 'O', 'Ọ' to 'O',
        'Ố' to 'Ô', 'Ồ' to 'Ô', 'Ổ' to 'Ô', 'Ỗ' to 'Ô', 'Ộ' to 'Ô',
        'Ớ' to 'Ơ', 'Ờ' to 'Ơ', 'Ở' to 'Ơ', 'Ỡ' to 'Ơ', 'Ợ' to 'Ơ',

        'Ú' to 'U', 'Ù' to 'U', 'Ủ' to 'U', 'Ũ' to 'U', 'Ụ' to 'U',
        'Ứ' to 'Ư', 'Ừ' to 'Ư', 'Ử' to 'Ư', 'Ữ' to 'Ư', 'Ự' to 'Ư',

        'Ý' to 'Y', 'Ỳ' to 'Y', 'Ỷ' to 'Y', 'Ỹ' to 'Y', 'Ỵ' to 'Y'
    )

    private val toneMarks = mapOf(
        's' to '\'', 'f' to '`', 'r' to '?', 'x' to '~', 'j' to '.'
    )

    private val charModifiers = mapOf(
        "aw" to "ă", "Aw" to "Ă", "AW" to "Ă",
        "aa" to "â", "Aa" to "Â", "AA" to "Â",
        "oo" to "ô", "Oo" to "Ô", "OO" to "Ô",
        "ow" to "ơ", "Ow" to "Ơ", "OW" to "Ơ",
        "ee" to "ê", "Ee" to "Ê", "EE" to "Ê",
        "dd" to "đ", "Dd" to "Đ", "DD" to "Đ",
        "uw" to "ư", "Uw" to "Ư", "UW" to "Ư",
    )

    private val reverseCharModifier = mapOf(
        "ăa" to "aa", "ăA" to "aA",
        "Ăa" to "Aa", "ĂA" to "AA",
        "âa" to "aa", "âA" to "aA",
        "Âa" to "Aa", "ÂA" to "AA",
        "ôo" to "oo", "ôO" to "oO",
        "Ôo" to "Oo", "ÔO" to "OO",
        "ơw" to "ow", "ơW" to "oW",
        "Ơw" to "Ow", "ƠW" to "OW",
        "êe" to "ee", "êE" to "eE",
        "Êe" to "Ee", "ÊE" to "EE",
        "đd" to "dd", "đD" to "dD",
        "Đd" to "Dd", "ĐD" to "DD",
        "ưw" to "uw", "ưW" to "uW",
        "Ưw" to "Uw", "ƯW" to "UW"
    )

    private val toneMapping = mapOf(
        'ă' to mapOf('\'' to 'ắ', '`' to 'ằ', '?' to 'ẳ', '~' to 'ẵ', '.' to 'ặ'),
        'Ă' to mapOf('\'' to 'Ắ', '`' to 'Ằ', '?' to 'Ẳ', '~' to 'Ẵ', '.' to 'Ặ'),
        'â' to mapOf('\'' to 'ấ', '`' to 'ầ', '?' to 'ẩ', '~' to 'ẫ', '.' to 'ậ'),
        'Â' to mapOf('\'' to 'Ấ', '`' to 'Ầ', '?' to 'Ẩ', '~' to 'Ẫ', '.' to 'Ậ'),
        'ê' to mapOf('\'' to 'ế', '`' to 'ề', '?' to 'ể', '~' to 'ễ', '.' to 'ệ'),
        'Ê' to mapOf('\'' to 'Ế', '`' to 'Ề', '?' to 'Ể', '~' to 'Ễ', '.' to 'Ệ'),
        'ô' to mapOf('\'' to 'ố', '`' to 'ồ', '?' to 'ổ', '~' to 'ỗ', '.' to 'ộ'),
        'Ô' to mapOf('\'' to 'Ố', '`' to 'Ồ', '?' to 'Ổ', '~' to 'Ỗ', '.' to 'Ộ'),
        'ơ' to mapOf('\'' to 'ớ', '`' to 'ờ', '?' to 'ở', '~' to 'ỡ', '.' to 'ợ'),
        'Ơ' to mapOf('\'' to 'Ớ', '`' to 'Ờ', '?' to 'Ở', '~' to 'Ỡ', '.' to 'Ợ'),
        'ư' to mapOf('\'' to 'ứ', '`' to 'ừ', '?' to 'ử', '~' to 'ữ', '.' to 'ự'),
        'Ư' to mapOf('\'' to 'Ứ', '`' to 'Ừ', '?' to 'Ử', '~' to 'Ữ', '.' to 'Ự'),
        'a' to mapOf('\'' to 'á', '`' to 'à', '?' to 'ả', '~' to 'ã', '.' to 'ạ'),
        'A' to mapOf('\'' to 'Á', '`' to 'À', '?' to 'Ả', '~' to 'Ã', '.' to 'Ạ'),
        'e' to mapOf('\'' to 'é', '`' to 'è', '?' to 'ẻ', '~' to 'ẽ', '.' to 'ẹ'),
        'E' to mapOf('\'' to 'É', '`' to 'È', '?' to 'Ẻ', '~' to 'Ẽ', '.' to 'Ẹ'),
        'o' to mapOf('\'' to 'ó', '`' to 'ò', '?' to 'ỏ', '~' to 'õ', '.' to 'ọ'),
        'O' to mapOf('\'' to 'Ó', '`' to 'Ò', '?' to 'Ỏ', '~' to 'Õ', '.' to 'Ọ'),
        'i' to mapOf('\'' to 'í', '`' to 'ì', '?' to 'ỉ', '~' to 'ĩ', '.' to 'ị'),
        'I' to mapOf('\'' to 'Í', '`' to 'Ì', '?' to 'Ỉ', '~' to 'Ĩ', '.' to 'Ị'),
        'u' to mapOf('\'' to 'ú', '`' to 'ù', '?' to 'ủ', '~' to 'ũ', '.' to 'ụ'),
        'U' to mapOf('\'' to 'Ú', '`' to 'Ù', '?' to 'Ủ', '~' to 'Ũ', '.' to 'Ụ'),
        'y' to mapOf('\'' to 'ý', '`' to 'ỳ', '?' to 'ỷ', '~' to 'ỹ', '.' to 'ỵ'),
        'Y' to mapOf('\'' to 'Ý', '`' to 'Ỳ', '?' to 'Ỷ', '~' to 'Ỹ', '.' to 'Ỵ')
    )

    // Set of characters that should not trigger Telex transformations
    private val ignoredChars = setOf('z')

    // List of invalid sequences that should prevent Telex processing
    private val invalidSequences = listOf(
        // Single invalid letters
        "f", "w", "z", "j",
        // Consonant clusters not found in Vietnamese
        "pr", "pl", "kr", "kl", "br", "bl", "gr", "vl", "rr", "ps",
        // Other invalid combinations
        "aa", "ee", "ih", "ah", "eh", "oh", "uh",
        "il", "al", "el", "ol", "ul",
        "iq", "aq", "eq", "oq", "uq", "nd",
        "ar", "or", "ir", "ur", "er",
        "av", "ev", "uv", "iv", "uv",
        "ou",
        // Numbers (0-9) as single characters
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        // Special characters
        "`", "~", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "-", "_", "=", "+",
        "[", "]", "{", "}", "\\", "|", ";", ":", "'", "\"", ",", ".", "<", ">", "/", "?"
    )

    fun processKey(char: Char): String? {
        isReverseTone = false
        val bufferStr = buffer.toString()
        // Ignore processing for specific characters
        if (char !in modifiableChars && char !in toneMarks.keys) {
            return char.toString()  // Return the original character as is
        }
        // Check if the buffer contains any invalid sequences
        if (invalidSequences.any { bufferStr.contains(it) }) {
            return char.toString()  // Return the original character as is
        }
        if (char == '\b') return handleBackspace()

        if (char in toneMarks.keys) return applyToneMark(toneMarks[char]!!, char)

        buffer.append(char)

        if (applyCharModifiers(char)) {
            return buffer.toString()
        }
        return char.toString()
    }

    private fun applyCharModifiers(char: Char): Boolean {
        Log.d("TelexInput", "applyCharModifiers buffer: $buffer - charModified: $charModified")
        // filter invalid char
        if (char !in modifiableChars) {
            return false
        }

        if (charModified) {
            // reverse modify char
            // âa --> aa
            for ((pattern, replacement) in reverseCharModifier) {
                if (buffer.endsWith(pattern, true)) {
                    buffer.replace(buffer.length - pattern.length, buffer.length, replacement)
                    Log.d("TelexInput", "Applied modifier: $buffer")
                    return true;
                }
            }
        } else {
            // aa --> â
            for ((pattern, replacement) in charModifiers) {
                if (buffer.endsWith(pattern, true)) {
                    buffer.replace(buffer.length - pattern.length, buffer.length, replacement)
                    val check = char != 'd'
                    Log.d("TelexInput", "Applied modifier: $buffer, char: $char $check")
                    if (char != 'd' && char != 'w') {
                        charModified = true
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private fun applyToneMark(toneMark: Char, originalChar: Char): String? {
        if (toneMark != lastToneMark && toneAdded) {
            reverseTone(originalChar, false)
            toneAdded = false
        }
        val lastVowelIndex = findFirstVowelIndex(buffer, toneMapping)

        if (lastVowelIndex != -1 && !toneAdded) {
            val char = buffer[lastVowelIndex]
            Log.d("TelexInput", "Found a vowel! index: $lastVowelIndex")
            buffer.setCharAt(lastVowelIndex, getVowelWithTone(char, toneMark))
            toneAdded = true
            lastToneMark = toneMark
            return buffer.toString()
        }

        if (reverseTone(originalChar)) {
            return buffer.toString()
        }

        buffer.append(originalChar)
        return null
    }

    private fun reverseTone(originalChar: Char, willAddOriginal: Boolean = true): Boolean {
        for (i in buffer.indices) {  // Search for the last vowel
            val char = buffer[i]

            if (char in tonedVowelSet) {
                Log.d("TelexInput", "Found a toned vowel! index: $i")
                // remove tone
                val baseChar = removeToneMap[char] ?: char
                buffer.setCharAt(i, baseChar)  // remove tone mark
                if (willAddOriginal) {
                    isReverseTone = true
                    buffer.append(originalChar)
                }
                return true
            }
        }
        return false
    }

    private fun getExistingTone(char: Char): Char? {
        for ((tone, mapping) in toneMapping) {
            for ((diacritic, vowel) in mapping) {
                if (vowel == char) return tone
            }
        }
        return null
    }

    private fun getBaseVowel(char: Char): Char {
        for ((base, mapping) in toneMapping) {
            for (vowel in mapping.values) {
                if (vowel == char) return base  // Found toned vowel, return base vowel
            }
        }
        return char  // Return as-is if no match
    }

    private fun getVowelWithTone(char: Char, toneMark: Char): Char {

        return toneMapping[char]?.get(toneMark) ?: char
    }

    private fun handleBackspace(): String? {
        if (buffer.isNotEmpty()) {
            buffer.deleteCharAt(buffer.length - 1)
        }
        return if (buffer.isEmpty()) null else buffer.toString()
    }

    fun reset() {
        buffer.clear()
        toneAdded = false
        lastToneMark = null
        charModified = false
    }

    fun setBuffer(string: String) {
        buffer.clear()
        buffer.append(string)
    }

    private fun findFirstVowelIndex(buffer: StringBuilder, toneMapping: Map<Char, Map<Char, Char>>): Int {
        for (key in toneMapping.keys) {  // Loop through vowels in toneMapping
            val index = buffer.indexOf(key.toString()) // Search for the vowel in buffer
            if (index != -1) return index // Return the first found index
        }
        return -1  // Return -1 if no vowel is found
    }
}
