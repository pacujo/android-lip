package net.pacujo.lip

import androidx.core.util.PatternsCompat.WEB_URL

fun findUrl(s: String, index: Int): Pair<Int, Int>? {
    var urlStart = index
    while (true) {
        val urlEnd = skipUrl(s, urlStart)
        if (urlEnd != null &&
            WEB_URL.matcher(s.deformat(urlStart, urlEnd)).matches())
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

private val formatChars = setOf(CtrlB, CtrlR, CtrlU, CtrlO, CtrlC)

private fun atFinalJam(s: String, index: Int): Boolean {
    for (i in index until s.length) {
        val c = s[i]
        when (c) {
            ' ', '<', '>' -> return true
            ')', '.', ',', ':', ';', '!', '?', '"', '\'' -> continue
            in formatChars -> continue
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

private fun String.skip(s: String, index: Int?): Int? {
    index ?: return null
    withIndex().find { (i, c) ->
        s.getOrNull(index + i) != c
    } ?: return index + length
    return null
}

private fun String.deformat(start: Int, end: Int) =
    substring(start, end).filter { it !in formatChars }
