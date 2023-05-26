package net.pacujo.lip

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

fun Char.isAsciiDigit() = this in '0'..'9'

fun Char.isAsciiAlpha() =
    this in 'A'..'Z' || this in 'a'..'z'

data class IRCString(val string: String)

const val CtrlA = ('A'.code and 0x1f).toChar()
private const val CtrlB = ('B'.code and 0x1f).toChar()
private const val CtrlC = ('C'.code and 0x1f).toChar()
private const val CtrlO = ('O'.code and 0x1f).toChar()
private const val CtrlR = ('R'.code and 0x1f).toChar()
private const val CtrlU = ('U'.code and 0x1f).toChar()

const val IRCBold = CtrlB.toString()
const val IRCColor = CtrlC.toString()
const val IRCOriginal = CtrlO.toString()
const val IRCItalic = CtrlR.toString()
const val IRCUnderline = CtrlU.toString()

fun IRCString.toAnnotatedString(): AnnotatedString {
    return buildAnnotatedString {
        var style = IRCTextStyle()
        var p = 0
        var q = 0
        while (q < string.length) {
            val (r, nextStyle) =
                when (string[q]) {
                    CtrlB -> q + 1 to style.copy(bold = !style.bold)
                    CtrlO -> q + 1 to IRCTextStyle()
                    CtrlR -> q + 1 to style.copy(italic = !style.italic)
                    CtrlU -> q + 1 to style.copy(underline = !style.underline)
                    CtrlC -> parseColorCodes(string, style, q + 1)
                    else -> q + 1 to null
                }
            if (nextStyle != null) {
                pushStyle(style.toSpanStyle())
                append(text = string.substring(startIndex = p, endIndex = q))
                pop()
                style = nextStyle
                p = r
            }
            q = r
        }
        pushStyle(style.toSpanStyle())
        append(string.substring(startIndex = p))
        pop()
    }
}

private val Brown = Color(red = 150, green = 75, blue = 0)
private val Purple = Color(red = 160, green = 32, blue = 240)
private val Orange = Color(red = 255, green = 165, blue = 0)
private val LightGreen = Color(red = 144, green = 238, blue = 144)
private val LightCyan = Color(red = 224, green = 255, blue = 255)
private val LightBlue = Color(red = 173, green = 216, blue = 230)
private val Pink = Color(red = 255, green = 192, blue = 203)

private fun ircColor(colorCode: Int?) =
    when (colorCode) {
        0 -> Color.White
        1 -> Color.Black
        2 -> Color.Blue
        3 -> Color.Green
        4 -> Color.Red
        5 -> Brown
        6 -> Purple
        7 -> Orange
        8 -> Color.Yellow
        9 -> LightGreen
        10 -> Color.Cyan
        11 -> LightCyan
        12 -> LightBlue
        13 -> Pink
        14 -> Color.Gray
        15 -> Color.LightGray
        else -> Color.Unspecified
    }

private fun IRCTextStyle.toSpanStyle() =
    SpanStyle(
        fontWeight = if (bold) FontWeight.Bold else null,
        fontStyle = if (italic) FontStyle.Italic else null,
        textDecoration = if (underline) TextDecoration.Underline else null,
        color = ircColor(fgColor),
        background = ircColor(bgColor),
    )

private fun Char.isASCIIDigit() = this in '0'..'9'

private fun parseColorCode(s: String, q: Int) =
    if (q + 1 >= s.length || !s[q + 1].isASCIIDigit())
        q + 1 to s[q] - '0'
    else
        q + 2 to (s[q] - '0') * 10 + (s[q + 1] - '0')

private fun parseColorCodes(
    s: String,
    style: IRCTextStyle,
    q: Int,
): Pair<Int, IRCTextStyle> {
    if (q >= s.length || !s[q].isASCIIDigit())
        return q to style.copy(fgColor = null, bgColor = null)
    val (qq, fgColor) = parseColorCode(s, q)
    if (qq + 1 >= s.length || s[qq] != ',' || !s[qq + 1].isASCIIDigit())
        return qq to style.copy(fgColor = fgColor, bgColor = null)
    val (qqq, bgColor) = parseColorCode(s, qq + 1)
    return qqq to style.copy(fgColor = fgColor, bgColor = bgColor)
}

fun AnnotatedString.markMood(mood: Mood): AnnotatedString {
    val color =
        when (mood) {
            Mood.LOG -> Color.Cyan
            Mood.INFO -> return this
            Mood.THEIRS, Mood.ERROR -> Color.Red
            Mood.MINE -> Color.Blue
        }
    return buildAnnotatedString {
        withStyle(style = SpanStyle(color = color)) {
            append(this@markMood)
        }
    }
}

fun AnnotatedString.markParagraphStyle(
    paragraphStyle: ParagraphStyle,
): AnnotatedString {
    return buildAnnotatedString {
        withStyle(style = paragraphStyle) {
            append(this@markParagraphStyle)
        }
    }
}
