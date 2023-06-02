package net.pacujo.lip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

internal class UrlKtTest {
    private val url = "http://example.com/test#_Y?it=now"

    @Test
    fun findUrl_test_no_url() {
        assertNull(findUrl("hello", 3))
    }

    @Test
    fun findUrl_test_bad_start() {
        assertNull(findUrl("hello$url", 3))
    }

    @Test
    fun findUrl_test_straight_url() {
        val result = findUrl(url, 0)
        assertNotNull(result)
        val (start, end) = result!!
        assertEquals(0, start)
        assertEquals(url.length, end)
    }

    @Test
    fun findUrl_test_embedded() {
        val prefix = "text <URL:"
        val suffix = "> text"
        val result = findUrl("$prefix$url$suffix", 0)
        assertNotNull(result)
        val (start, end) = result!!
        assertEquals(prefix.length, start)
        assertEquals(start + url.length, end)
    }

    @Test
    fun findUrl_test_formatted() {
        val prefix = "text <U${IRCBold}RL:$IRCUnderline"
        val suffix = "$IRCOriginal> tex🗝t"
        val furl = "https://${IRCBold}example${IRCBold}.co${IRCItalic}m/test"
        val result = findUrl("$prefix$furl$suffix", 0)
        assertNotNull(result)
        val (start, end) = result!!
        assertEquals(prefix.length, start)
        assertEquals(start + furl.length, end)
    }
}
