package ru.whiteleaf.notes.data.model

import org.junit.Assert.*
import org.junit.Test
import ru.whiteleaf.notes.domain.model.Note

class RecentNoteTest {

    @Test
    fun `RecentNote should be created with all fields`() {
        val recentNote = RecentNote(
            id = "note_123",
            recentTitle = "Test Title",
            notebookPath = "/test/notebook",
            recentDate = 1234567890L
        )

        assertEquals("note_123", recentNote.id)
        assertEquals("Test Title", recentNote.recentTitle)
        assertEquals("/test/notebook", recentNote.notebookPath)
        assertEquals(1234567890L, recentNote.recentDate)
    }

    @Test
    fun `RecentNote should have default recentDate of 0`() {
        val recentNote = RecentNote(
            id = "note_123",
            recentTitle = "Test",
            notebookPath = "/test"
        )

        assertEquals(0L, recentNote.recentDate)
    }

    @Test
    fun `RecentNote fromNote should use note title when not empty`() {
        val note = Note(
            id = "note_123",
            title = "My Title",
            content = "Some content",
            modifiedAt = System.currentTimeMillis()
        )

        val recentNote = RecentNote.fromNote(note)

        assertEquals("My Title", recentNote.recentTitle)
        assertEquals("note_123", recentNote.id)
    }

    @Test
    fun `RecentNote fromNote should use first 40 chars of content when title is empty`() {
        val note = Note(
            id = "note_456",
            title = "",
            content = "This is a very long content that should be truncated to forty characters exactly",
            modifiedAt = System.currentTimeMillis()
        )

        val recentNote = RecentNote.fromNote(note)

        assertEquals(40, recentNote.recentTitle.length)
        assertEquals("note_456", recentNote.id)
    }

    @Test
    fun `RecentNote fromNote should use full content if less than 40 chars`() {
        val note = Note(
            id = "note_789",
            title = "",
            content = "Short content",
            modifiedAt = System.currentTimeMillis()
        )

        val recentNote = RecentNote.fromNote(note)

        assertEquals("Short content", recentNote.recentTitle)
        assertEquals(13, recentNote.recentTitle.length)
    }

    @Test
    fun `RecentNote fromNote should handle trimmed title without dashes`() {
        val note = Note(
            id = "note_123",
            title = "-Title with dash",
            content = "Content",
            modifiedAt = System.currentTimeMillis()
        )

        val recentNote = RecentNote.fromNote(note)

        assertEquals("Title with dash", recentNote.recentTitle)
    }

    @Test
    fun `RecentNote fromNote should trim leading dashes from title`() {
        val note = Note(
            id = "note_123",
            title = "-Title",
            content = "Content",
            modifiedAt = System.currentTimeMillis()
        )

        val recentNote = RecentNote.fromNote(note)

        assertEquals("Title", recentNote.recentTitle)
    }

    @Test
    fun `RecentNote fromNote should set notebookPath from note`() {
        val note = Note(
            id = "note_123",
            title = "Title",
            content = "Content",
            modifiedAt = System.currentTimeMillis(),
            notebookPath = "/test/path"
        )

        val recentNote = RecentNote.fromNote(note)

        assertEquals("/test/path", recentNote.notebookPath)
    }

    @Test
    fun `RecentNote fromNote should convert null notebookPath to empty string`() {
        val note = Note(
            id = "note_123",
            title = "Title",
            content = "Content",
            modifiedAt = System.currentTimeMillis(),
            notebookPath = null
        )

        val recentNote = RecentNote.fromNote(note)

        assertEquals("", recentNote.notebookPath)
    }

    @Test
    fun `RecentNote fromNote should set recentDate to current time`() {
        val before = System.currentTimeMillis()
        val note = Note(
            id = "note_123",
            title = "Title",
            content = "Content",
            modifiedAt = before
        )

        val recentNote = RecentNote.fromNote(note)
        val after = System.currentTimeMillis()

        assertTrue(recentNote.recentDate >= before)
        assertTrue(recentNote.recentDate <= after)
    }

    @Test
    fun `RecentNote printDebug should return non-empty string`() {
        val recentNote = RecentNote(
            id = "note_123",
            recentTitle = "Test",
            notebookPath = "/test"
        )

        val debug = recentNote.printDebug()
        assertTrue(debug.isNotEmpty())
        assertTrue(debug.contains("id="))
        assertTrue(debug.contains("recentTitle="))
        assertTrue(debug.contains("path="))
    }

    @Test
    fun `RecentNote should handle empty id`() {
        val recentNote = RecentNote(
            id = "",
            recentTitle = "Test",
            notebookPath = "/test"
        )

        assertEquals("", recentNote.id)
    }

    @Test
    fun `RecentNote should handle empty title`() {
        val recentNote = RecentNote(
            id = "note_123",
            recentTitle = "",
            notebookPath = "/test"
        )

        assertEquals("", recentNote.recentTitle)
    }

    @Test
    fun `RecentNote should handle very long recentTitle`() {
        val longTitle = "a".repeat(10000)
        val recentNote = RecentNote(
            id = "note_123",
            recentTitle = longTitle,
            notebookPath = "/test"
        )

        assertEquals(longTitle, recentNote.recentTitle)
    }

    @Test
    fun `RecentNote should handle russian title`() {
        val recentNote = RecentNote(
            id = "note_123",
            recentTitle = "Рабочие заметки",
            notebookPath = "/рабочие"
        )

        assertEquals("Рабочие заметки", recentNote.recentTitle)
        assertEquals("/рабочие", recentNote.notebookPath)
    }

    @Test
    fun `RecentNote fromNote should handle content exactly 40 chars`() {
        val content = "a".repeat(40)
        val note = Note(
            id = "note_123",
            title = "",
            content = content,
            modifiedAt = System.currentTimeMillis()
        )

        val recentNote = RecentNote.fromNote(note)

        assertEquals(40, recentNote.recentTitle.length)
        assertEquals(content, recentNote.recentTitle)
    }

    @Test
    fun `RecentNote fromNote should truncate content at 40 chars`() {
        val content = "a".repeat(100)
        val note = Note(
            id = "note_123",
            title = "",
            content = content,
            modifiedAt = System.currentTimeMillis()
        )

        val recentNote = RecentNote.fromNote(note)

        assertEquals(40, recentNote.recentTitle.length)
        assertEquals(content.take(40), recentNote.recentTitle)
    }

    @Test
    fun `RecentNote should handle null notebook path`() {
        val recentNote = RecentNote(
            id = "note_123",
            recentTitle = "Test",
            notebookPath = null
        )

        // notebookPath is nullable in RecentNote
        assertNull(recentNote.notebookPath)
    }

    @Test
    fun `RecentNote should handle special characters in title`() {
        val recentNote = RecentNote(
            id = "note_123",
            recentTitle = "@ # $ % & * (!) ?",
            notebookPath = "/test"
        )

        assertEquals("@ # $ % & * (!) ?", recentNote.recentTitle)
    }

    @Test
    fun `RecentNote should handle emoji in title`() {
        val recentNote = RecentNote(
            id = "note_123",
            recentTitle = "Test 😀 🎉",
            notebookPath = "/test"
        )

        assertEquals("Test 😀 🎉", recentNote.recentTitle)
    }

    @Test
    fun `RecentNote fromNote should handle empty content when title empty`() {
        val note = Note(
            id = "note_123",
            title = "",
            content = "",
            modifiedAt = System.currentTimeMillis()
        )

        val recentNote = RecentNote.fromNote(note)

        assertEquals("", recentNote.recentTitle)
    }

    @Test
    fun `RecentNote should preserve exact recentDate`() {
        val timestamp = 1700000000L
        val recentNote = RecentNote(
            id = "note_123",
            recentTitle = "Test",
            notebookPath = "/test",
            recentDate = timestamp
        )

        assertEquals(timestamp, recentNote.recentDate)
    }
}
