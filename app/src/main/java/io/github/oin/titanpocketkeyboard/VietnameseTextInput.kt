import android.util.Log

class VietnameseTextInput {
    private var buffer = StringBuilder()

    private val vowelMap = setOf('a', 'e', 'i', 'o', 'u', 'y', 'A', 'E', 'I', 'O', 'U', 'Y')

    private val toneMarks = mapOf(
        's' to '\'', 'f' to '`', 'r' to '?', 'x' to '~', 'j' to '.'
    )

    private val charModifiers = mapOf(
        "aw" to "ă", "aa" to "â", "oo" to "ô", "ow" to "ơ", "ee" to "ê",
        "dd" to "đ", "uw" to "ư"
    )

    fun processKey(char: Char): String? {
        if (char == '\b') return handleBackspace()

        if (char in toneMarks.keys) return applyToneMark(toneMarks[char]!!)

        buffer.append(char)

        if (applyCharModifiers()) {
            return buffer.toString()
        }
        return char.toString()
    }

    private fun applyCharModifiers(): Boolean {
        for ((pattern, replacement) in charModifiers) {
            if (buffer.endsWith(pattern, true)) {
                buffer.replace(buffer.length - pattern.length, buffer.length, replacement)
                Log.d("TelexInput", "Applied modifier: $buffer")
                return true;
            }
        }
        return false;
    }

    private fun applyToneMark(toneMark: Char): String? {
        for (i in buffer.length - 1 downTo 0) {  // Search for the last vowel
            if (buffer[i] in vowelMap) {

                val char = buffer[i]
                Log.d("TelexInput", "Found a vowel! index: $i, char: $char")
                buffer.setCharAt(i, getVowelWithTone(char, toneMark))  // Apply tone mark
                return buffer.toString()
            }
        }
        return null
    }


    private fun getVowelWithTone(char: Char, toneMark: Char): Char {
        val toneMapping = mapOf(
            'a' to mapOf('\'' to 'á', '`' to 'à', '?' to 'ả', '~' to 'ã', '.' to 'ạ'),
            'ă' to mapOf('\'' to 'ắ', '`' to 'ằ', '?' to 'ẳ', '~' to 'ẵ', '.' to 'ặ'),
            'â' to mapOf('\'' to 'ấ', '`' to 'ầ', '?' to 'ẩ', '~' to 'ẫ', '.' to 'ậ'),
            'e' to mapOf('\'' to 'é', '`' to 'è', '?' to 'ẻ', '~' to 'ẽ', '.' to 'ẹ'),
            'ê' to mapOf('\'' to 'ế', '`' to 'ề', '?' to 'ể', '~' to 'ễ', '.' to 'ệ'),
            'o' to mapOf('\'' to 'ó', '`' to 'ò', '?' to 'ỏ', '~' to 'õ', '.' to 'ọ'),
            'ô' to mapOf('\'' to 'ố', '`' to 'ồ', '?' to 'ổ', '~' to 'ỗ', '.' to 'ộ'),
            'ơ' to mapOf('\'' to 'ớ', '`' to 'ờ', '?' to 'ở', '~' to 'ỡ', '.' to 'ợ'),
            'u' to mapOf('\'' to 'ú', '`' to 'ù', '?' to 'ủ', '~' to 'ũ', '.' to 'ụ'),
            'ư' to mapOf('\'' to 'ứ', '`' to 'ừ', '?' to 'ử', '~' to 'ữ', '.' to 'ự'),
            'i' to mapOf('\'' to 'í', '`' to 'ì', '?' to 'ỉ', '~' to 'ĩ', '.' to 'ị'),
            'y' to mapOf('\'' to 'ý', '`' to 'ỳ', '?' to 'ỷ', '~' to 'ỹ', '.' to 'ỵ')
        )
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
    }
}
