package ru.whiteleaf.notes.common.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HighlightAllMatchesTest {

    @Test
    fun `highlightAllMatches should return same text when query is empty`() {
        val text = "Hello World"
        val query = ""
        val color = 0xFFFF0000.toInt()

        val (spannable, matches) = highlightAllMatches(text, query, color, true)

        assertEquals(text, spannable.toString())
        assertTrue(matches.isEmpty())
    }

    @Test
    fun `highlightAllMatches should find single match`() {
        val text = "Hello World"
        val query = "World"
        val color = 0xFFFF0000.toInt()

        val (spannable, matches) = highlightAllMatches(text, query, color, true)

        assertEquals(text, spannable.toString())
        assertEquals(1, matches.size)
        assertEquals(6 to 11, matches[0])
    }

    @Test
    fun `highlightAllMatches should find multiple matches`() {
        val text = "Hello hello HELLO"
        val query = "hello"
        val color = 0xFFFF0000.toInt()

        val (spannable, matches) = highlightAllMatches(text, query, color, ignoreCase = true)

        assertEquals(text, spannable.toString())
        assertEquals(3, matches.size)
    }

    @Test
    fun `highlightAllMatches should be case sensitive when ignoreCase is false`() {
        val text = "Hello World hello"
        val query = "hello"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, ignoreCase = false)

        assertEquals(1, matches.size)
        assertEquals(12 to 17, matches[0])
    }

    @Test
    fun `highlightAllMatches should be case insensitive by default`() {
        val text = "Hello World hello HELLO"
        val query = "hello"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color)

        assertEquals(3, matches.size)
    }

    @Test
    fun `highlightAllMatches should handle overlapping queries`() {
        val text = "aaaa"
        val query = "aa"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, true)

        // Should find non-overlapping matches: positions 0-2 and 2-4
        assertEquals(2, matches.size)
    }

    @Test
    fun `highlightAllMatches should return correct positions`() {
        val text = "The quick brown fox"
        val query = "brown"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, true)

        assertEquals(1, matches.size)
        assertEquals(10 to 15, matches[0])
    }

    @Test
    fun `highlightAllMatches should handle query at start`() {
        val text = "Hello World"
        val query = "Hello"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, true)

        assertEquals(1, matches.size)
        assertEquals(0 to 5, matches[0])
    }

    @Test
    fun `highlightAllMatches should handle query at end`() {
        val text = "Hello World"
        val query = "World"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, true)

        assertEquals(1, matches.size)
        assertEquals(6 to 11, matches[0])
    }

    @Test
    fun `highlightAllMatches should handle query equal to text`() {
        val text = "Hello"
        val query = "Hello"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, true)

        assertEquals(1, matches.size)
        assertEquals(0 to 5, matches[0])
    }

    @Test
    fun `highlightAllMatches should handle no matches`() {
        val text = "Hello World"
        val query = "XYZ"
        val color = 0xFFFF0000.toInt()

        val (spannable, matches) = highlightAllMatches(text, query, color, true)

        assertEquals(text, spannable.toString())
        assertTrue(matches.isEmpty())
    }

    @Test
    fun `highlightAllMatches should handle single character query`() {
        val text = "Hello World"
        val query = "o"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, true)

        assertEquals(2, matches.size)
    }

    @Test
    fun `highlightAllMatches should handle multiline text`() {
        val text = "Line 1\nLine 2\nLine 3"
        val query = "Line"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, true)

        assertEquals(3, matches.size)
    }

    @Test
    fun `highlightAllMatches should handle Russian text`() {
        val text = "Привет мир Привет"
        val query = "привет"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, ignoreCase = true)

        assertEquals(2, matches.size)
    }

    @Test
    fun `highlightAllMatches should handle Cyrillic case insensitive`() {
        val text = "ПРИВЕТ мир Привет"
        val query = "привет"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, ignoreCase = true)

        assertEquals(2, matches.size)
    }

    @Test
    fun `highlightAllMatches should return SpannableString`() {
        val text = "Hello World"
        val query = "World"
        val color = 0xFFFF0000.toInt()

        val (spannable, _) = highlightAllMatches(text, query, color, true)

        assertNotNull(spannable)
        assertEquals(text, spannable.toString())
    }

    @Test
    fun `highlightAllMatches should handle empty text`() {
        val text = ""
        val query = "test"
        val color = 0xFFFF0000.toInt()

        val (spannable, matches) = highlightAllMatches(text, query, color, true)

        assertEquals("", spannable.toString())
        assertTrue(matches.isEmpty())
    }

    @Test
    fun `highlightAllMatches should handle special characters in text`() {
        val text = "Hello! @World# test"
        val query = "World"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, true)

        assertEquals(1, matches.size)
        assertEquals(8 to 13, matches[0])
    }

    @Test
    fun `highlightAllMatches should handle numeric query`() {
        val text = "Number 12345 in text"
        val query = "123"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, true)

        assertEquals(1, matches.size)
    }

    @Test
    fun `highlightAllMatches should preserve text with newlines`() {
        val text = "First\nSecond\nThird"
        val query = "Second"
        val color = 0xFFFF0000.toInt()

        val (spannable, matches) = highlightAllMatches(text, query, color, true)

        assertEquals(text, spannable.toString())
        assertEquals(1, matches.size)
        assertEquals(6 to 12, matches[0])
    }

    @Test
    fun `highlightAllMatches should find adjacent matches`() {
        val text = "testtest"
        val query = "test"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, true)

        assertEquals(2, matches.size)
        assertEquals(0 to 4, matches[0])
        assertEquals(4 to 8, matches[1])
    }

    @Test
    fun `highlightAllMatches should handle unicode characters`() {
        val text = "Hello 🌍 World 🌎"
        val query = "World"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, true)

        assertEquals(1, matches.size)
    }

    @Test
    fun `highlightAllMatches should handle emoji in text`() {
        val text = "Hello 😀 World 🌍"
        val query = "Hello"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, true)

        assertEquals(1, matches.size)
        assertEquals(0 to 5, matches[0])
    }

    @Test
    fun `highlightAllMatches should handle long text`() {
        val text = "a".repeat(10000)
        val query = "aaa"
        val color = 0xFFFF0000.toInt()

        val (spannable, matches) = highlightAllMatches(text, query, color, true)

        // Should find non-overlapping matches
        assertTrue(matches.isNotEmpty())
        assertEquals(text, spannable.toString())
    }

    @Test
    fun `highlightAllMatches should handle query longer than text`() {
        val text = "Hi"
        val query = "Hello World"
        val color = 0xFFFF0000.toInt()

        val (spannable, matches) = highlightAllMatches(text, query, color, true)

        assertTrue(matches.isEmpty())
        assertEquals(text, spannable.toString())
    }

    @Test
    fun `highlightAllMatches should handle whitespace query`() {
        val text = "Hello   World"
        val query = "   "
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, true)

        assertEquals(1, matches.size)
    }

    @Test
    fun `highlightAllMatches should return list of pairs`() {
        val text = "test"
        val query = "test"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, true)

        assertTrue(matches is List<Pair<Int, Int>>)
    }

    @Test
    fun `highlightAllMatches should have matches within bounds`() {
        val text = "Hello World"
        val query = "Hello"
        val color = 0xFFFF0000.toInt()

        val (_, matches) = highlightAllMatches(text, query, color, true)

        matches.forEach { (start, end) ->
            assertTrue(start >= 0)
            assertTrue(end <= text.length)
            assertTrue(start < end)
        }
    }
}
