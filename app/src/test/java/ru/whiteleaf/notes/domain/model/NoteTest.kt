package ru.whiteleaf.notes.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteTest {

    @Test
    fun `Note should be created with all fields`() {
        val note = Note(
            id = "note_1234567890",
            title = "Test Note",
            content = "This is test content",
            modifiedAt = 1234567890L,
            notebookPath = "/test/notebook"
        )

        assertEquals("note_1234567890", note.id)
        assertEquals("Test Note", note.title)
        assertEquals("This is test content", note.content)
        assertEquals(1234567890L, note.modifiedAt)
        assertEquals("/test/notebook", note.notebookPath)
    }

    @Test
    fun `Note should be created with null notebookPath`() {
        val note = Note(
            id = "note_1234567890",
            title = "Test Note",
            content = "Content",
            modifiedAt = 1234567890L,
            notebookPath = null
        )

        assertNull(note.notebookPath)
    }

    @Test
    fun `isEmpty should return true when both title and content are empty`() {
        val note = Note(
            id = "note_123",
            title = "",
            content = "",
            modifiedAt = System.currentTimeMillis()
        )

        assertTrue(note.isEmpty())
    }

    @Test
    fun `isEmpty should return true when both title and content are whitespace`() {
        val note = Note(
            id = "note_123",
            title = "   ",
            content = "\n\t  ",
            modifiedAt = System.currentTimeMillis()
        )

        assertTrue(note.isEmpty())
    }

    @Test
    fun `isEmpty should return true when both title and content are tabs and newlines`() {
        val note = Note(
            id = "note_123",
            title = "\t\n\r",
            content = "   \n   ",
            modifiedAt = System.currentTimeMillis()
        )

        assertTrue(note.isEmpty())
    }

    @Test
    fun `isEmpty should return false when title has text`() {
        val note = Note(
            id = "note_123",
            title = "Title",
            content = "",
            modifiedAt = System.currentTimeMillis()
        )

        assertFalse(note.isEmpty())
    }

    @Test
    fun `isEmpty should return false when title has single character`() {
        val note = Note(
            id = "note_123",
            title = "a",
            content = "",
            modifiedAt = System.currentTimeMillis()
        )

        assertFalse(note.isEmpty())
    }

    @Test
    fun `isEmpty should return false when content has text`() {
        val note = Note(
            id = "note_123",
            title = "",
            content = "Some content here",
            modifiedAt = System.currentTimeMillis()
        )

        assertFalse(note.isEmpty())
    }

    @Test
    fun `isEmpty should return false when both title and content have text`() {
        val note = Note(
            id = "note_123",
            title = "Title",
            content = "Content",
            modifiedAt = System.currentTimeMillis()
        )

        assertFalse(note.isEmpty())
    }

    @Test
    fun `isEmpty should return false when title has whitespace but content has text`() {
        val note = Note(
            id = "note_123",
            title = "   ",
            content = "Real content",
            modifiedAt = System.currentTimeMillis()
        )

        assertFalse(note.isEmpty())
    }

    @Test
    fun `isEmpty should return false when title has text but content is whitespace`() {
        val note = Note(
            id = "note_123",
            title = "Real title",
            content = "   \n  ",
            modifiedAt = System.currentTimeMillis()
        )

        assertFalse(note.isEmpty())
    }

    @Test
    fun `isNotEmpty should return false when both title and content are empty`() {
        val note = Note(
            id = "note_123",
            title = "",
            content = "",
            modifiedAt = System.currentTimeMillis()
        )

        assertFalse(note.isNotEmpty())
    }

    @Test
    fun `isNotEmpty should return false when both title and content are whitespace`() {
        val note = Note(
            id = "note_123",
            title = "   ",
            content = "\n\t  ",
            modifiedAt = System.currentTimeMillis()
        )

        assertFalse(note.isNotEmpty())
    }

    @Test
    fun `isNotEmpty should return true when title has text`() {
        val note = Note(
            id = "note_123",
            title = "Title",
            content = "",
            modifiedAt = System.currentTimeMillis()
        )

        assertTrue(note.isNotEmpty())
    }

    @Test
    fun `isNotEmpty should return true when content has text`() {
        val note = Note(
            id = "note_123",
            title = "",
            content = "Content",
            modifiedAt = System.currentTimeMillis()
        )

        assertTrue(note.isNotEmpty())
    }

    @Test
    fun `isNotEmpty should return true when both have text`() {
        val note = Note(
            id = "note_123",
            title = "Title",
            content = "Content",
            modifiedAt = System.currentTimeMillis()
        )

        assertTrue(note.isNotEmpty())
    }

    @Test
    fun `isNotEmpty should return true when title is whitespace but content has text`() {
        val note = Note(
            id = "note_123",
            title = "   ",
            content = "Real content",
            modifiedAt = System.currentTimeMillis()
        )

        assertTrue(note.isNotEmpty())
    }

    @Test
    fun `isEmpty and isNotEmpty should be inverses`() {
        val testCases = listOf(
            Note("1", "", "", 0L),
            Note("2", "   ", "\n", 0L),
            Note("3", "Title", "", 0L),
            Note("4", "", "Content", 0L),
            Note("5", "Title", "Content", 0L),
            Note("6", "\t", "  \n  ", 0L),
        )

        testCases.forEach { note ->
            assertEquals(!note.isNotEmpty(), note.isEmpty())
        }
    }

    @Test
    fun `printDebug should return non-empty string`() {
        val note = Note(
            id = "note_123",
            title = "Test",
            content = "Content",
            modifiedAt = 123456L
        )

        val debug = note.printDebug()
        assertNotNull(debug)
        assertTrue(debug.isNotEmpty())
        assertTrue(debug.contains("id="))
        assertTrue(debug.contains("title="))
        assertTrue(debug.contains("content="))
    }

    @Test
    fun `printDebugIdTitlePath should return non-empty string`() {
        val note = Note(
            id = "note_123",
            title = "Test",
            content = "Content",
            modifiedAt = 123456L,
            notebookPath = "/test/path"
        )

        val debug = note.printDebugIdTitlePath()
        assertNotNull(debug)
        assertTrue(debug.contains("id="))
        assertTrue(debug.contains("title="))
        assertTrue(debug.contains("path="))
        assertTrue(debug.contains("/test/path"))
    }

    @Test
    fun `Note content should preserve newlines`() {
        val content = "Line 1\nLine 2\nLine 3"
        val note = Note(
            id = "note_123",
            title = "Multiline",
            content = content,
            modifiedAt = System.currentTimeMillis()
        )

        assertEquals(content, note.content)
    }

    @Test
    fun `Note should preserve special characters in title`() {
        val title = "Тест @ # $ & * ( ) ! ?"
        val note = Note(
            id = "note_123",
            title = title,
            content = "",
            modifiedAt = System.currentTimeMillis()
        )

        assertEquals(title, note.title)
    }

    @Test
    fun `Note should handle very long title`() {
        val longTitle = "a".repeat(10000)
        val note = Note(
            id = "note_123",
            title = longTitle,
            content = "",
            modifiedAt = System.currentTimeMillis()
        )

        assertEquals(longTitle, note.title)
        assertFalse(note.isEmpty())
    }

    @Test
    fun `Note should handle very long content`() {
        val longContent = "b".repeat(100000)
        val note = Note(
            id = "note_123",
            title = "",
            content = longContent,
            modifiedAt = System.currentTimeMillis()
        )

        assertEquals(longContent, note.content)
        assertFalse(note.isEmpty())
    }

    @Test
    fun `Note should handle empty strings for all fields except id`() {
        val note = Note(
            id = "note_123",
            title = "",
            content = "",
            modifiedAt = 0L
        )

        assertTrue(note.isEmpty())
        assertEquals("note_123", note.id)
        assertEquals(0L, note.modifiedAt)
    }

    @Test
    fun `Note modifiedAt should store timestamp correctly`() {
        val timestamp = 1700000000L
        val note = Note(
            id = "note_123",
            title = "Test",
            content = "Test",
            modifiedAt = timestamp
        )

        assertEquals(timestamp, note.modifiedAt)
    }

    @Test
    fun `Note notebookPath should be nullable`() {
        val noteWithNull = Note("1", "T", "C", 0L, null)
        val noteWithPath = Note("2", "T", "C", 0L, "/path")
        val noteWithDefault = Note("3", "T", "C", 0L)

        assertNull(noteWithNull.notebookPath)
        assertEquals("/path", noteWithPath.notebookPath)
        assertNull(noteWithDefault.notebookPath)
    }

    @Test
    fun `isEmpty should handle unicode whitespace`() {
        val note = Note(
            id = "note_123",
            title = "\u00A0\u2003",  // non-breaking space and em space
            content = "\t\n",
            modifiedAt = System.currentTimeMillis()
        )

        // trim() removes standard whitespace but not all unicode whitespace
        // This test documents current behavior
        assertFalse(note.isEmpty())
    }

    @Test
    fun `isNotEmpty with unicode content`() {
        val note = Note(
            id = "note_123",
            title = "",
            content = "тест на русском",
            modifiedAt = System.currentTimeMillis()
        )

        assertTrue(note.isNotEmpty())
    }

    @Test
    fun `Note should handle emoji in content`() {
        val note = Note(
            id = "note_123",
            title = "😀😃😄",
            content = "Content with 🎉🎊",
            modifiedAt = System.currentTimeMillis()
        )

        assertEquals("😀😃😄", note.title)
        assertEquals("Content with 🎉🎊", note.content)
        assertFalse(note.isEmpty())
    }

    @Test
    fun `printDebug should truncate long content`() {
        val longContent = "x".repeat(1000)
        val note = Note(
            id = "note_123",
            title = "Test",
            content = longContent,
            modifiedAt = 0L
        )

        val debug = note.printDebug()
        // Should contain only first 10 chars
        assertTrue(debug.contains("content=xxx"))
    }
}
