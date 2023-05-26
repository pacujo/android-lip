package net.pacujo.lip

import android.util.Patterns

fun findUrl(s: String, index: Int): Pair<Int, Int>? {
    var urlStart = index
    while (true) {
        val urlEnd = skipUrl(s, urlStart)
        if (urlEnd != null &&
            Patterns.WEB_URL.matcher(s.substring(index, urlEnd)).matches())
            return Pair(urlStart, urlEnd)
        while (true) {
            if (urlStart == s.length)
                return null
            when (s[urlStart++].category) {
                CharCategory.LOWERCASE_LETTER,
                CharCategory.MODIFIER_LETTER,
                CharCategory.OTHER_LETTER,
                CharCategory.TITLECASE_LETTER,
                CharCategory.UPPERCASE_LETTER,
                CharCategory.COMBINING_SPACING_MARK,
                CharCategory.ENCLOSING_MARK,
                CharCategory.NON_SPACING_MARK,
                CharCategory.DECIMAL_DIGIT_NUMBER,
                CharCategory.LETTER_NUMBER,
                CharCategory.OTHER_NUMBER,
                CharCategory.CONNECTOR_PUNCTUATION,
                CharCategory.DASH_PUNCTUATION -> continue

                else -> break
            }
        }
    }
}

private fun skipUrl(s: String, index: Int): Int? {
    val schemes = listOf("http://", "https://")
    for (scheme in schemes) {
        val schemeEnd = scheme.skip(s, index) ?: continue
        var urlEnd = schemeEnd
        while (!atFinalJam(s, urlEnd))
            urlEnd++
        return urlEnd
    }
    return null
}

private fun atFinalJam(s: String, index: Int): Boolean {
    for (c in s.substring(index)) {
        when (c) {
            ' ', '<', '>' -> return true
            ')', '.', ',', ':', ';', '!', '?', '"', '\'' -> continue
            in ' '..'~' -> return false
        }
        return when (c.category) {
            CharCategory.CONTROL,
            CharCategory.CONNECTOR_PUNCTUATION,
            CharCategory.DASH_PUNCTUATION,
            CharCategory.END_PUNCTUATION,
            CharCategory.FINAL_QUOTE_PUNCTUATION,
            CharCategory.INITIAL_QUOTE_PUNCTUATION,
            CharCategory.OTHER_PUNCTUATION,
            CharCategory.START_PUNCTUATION,
            CharCategory.LINE_SEPARATOR,
            CharCategory.SPACE_SEPARATOR,
            CharCategory.PARAGRAPH_SEPARATOR -> true

            CharCategory.SURROGATE -> false
            else -> continue
        }
    }
    return true
}

private fun String.skip(s: String, index: Int?) =
    fold(index) { i, c -> c.skip(s, i) }

private fun Char.skip(s: String, index: Int?) =
    if (index == null || index >= s.length || s[index] != this)
        null
    else
        index + 1
