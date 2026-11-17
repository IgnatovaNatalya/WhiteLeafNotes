package ru.whiteleaf.notes.domain.model

import org.junit.Assert.*
import org.junit.Test

class NoteFoundTest {

    @Test
    fun `NoteFound should be created with all fields`() {
        val noteFound = NoteFound(
            query = "test",
            id = "note_123",
            title = "Test Title",
            foundedInTitle = true,
            contentSearchPreview = "Some content preview",
            contentPosition = 42,
            modifiedAt = 1234567890L,
            notebookPath = "/test/notebook"
        )

        assertEquals("test", noteFound.query)
        assertEquals("note_123", noteFound.id)
        assertEquals("Test Title", noteFound.title)
        assertTrue(noteFound.foundedInTitle)
        assertEquals("Some content preview", noteFound.contentSearchPreview)
        assertEquals(42, noteFound.contentPosition)
        assertEquals(1234567890L, noteFound.modifiedAt)
        assertEquals("/test/notebook", noteFound.notebookPath)
    }

    @Test
    fun `NoteFound toNote should create Note with correct values`() {
        val noteFound = NoteFound(
            query = "test",
            id = "note_123",
            title = "Test Title",
            foundedInTitle = true,
            contentSearchPreview = "Preview",
            contentPosition = 10,
            modifiedAt = 1234567890L,
            notebookPath = "/test"
        )

        val note = noteFound.toNote()

        assertEquals("note_123", note.id)
        assertEquals("Test Title", note.title)
        assertEquals("", note.content)
        assertEquals(1234567890L, note.modifiedAt)
        assertEquals("/test", note.notebookPath)
    }

    @Test
    fun `NoteFound toNote should create Note with empty content`() {
        val noteFound = NoteFound(
            query = "test",
            id = "note_456",
            title = "Title",
            foundedInTitle = false,
            contentSearchPreview = null,
            contentPosition = null,
            modifiedAt = System.currentTimeMillis(),
            notebookPath = null
        )

        val note = noteFound.toNote()

        assertEquals("", note.content)
    }

    @Test
    fun `NoteFound toNote should preserve notebook path`() {
        val noteFound = NoteFound(
            query = "test",
            id = "note_789",
            title = "Title",
            foundedInTitle = true,
            contentSearchPreview = "Preview",
            contentPosition = 5,
            modifiedAt = 1000L,
            notebookPath = "/path/to/notebook"
        )

        val note = noteFound.toNote()

        assertEquals("/path/to/notebook", note.notebookPath)
    }

    @Test
    fun `NoteFound toNote should handle null notebook path`() {
        val noteFound = NoteFound(
            query = "test",
            id = "note_789",
            title = "Title",
            foundedInTitle = true,
            contentSearchPreview = null,
            contentPosition = null,
            modifiedAt = 1000L,
            notebookPath = null
        )

        val note = noteFound.toNote()

        assertNull(note.notebookPath)
    }

    @Test
    fun `NoteFound should handle null contentSearchPreview`() {
        val noteFound = NoteFound(
            query = "test",
            id = "note_123",
            title = "Title",
            foundedInTitle = true,
            contentSearchPreview = null,
            contentPosition = null,
            modifiedAt = System.currentTimeMillis(),
            notebookPath = "/test"
        )

        assertNull(noteFound.contentSearchPreview)
        assertNull(noteFound.contentPosition)
    }

    @Test
    fun `NoteFound should handle foundedInTitle false`() {
        val noteFound = NoteFound(
            query = "test",
            id = "note_123",
            title = "Title",
            foundedInTitle = false,
            contentSearchPreview = "Content with test",
            contentPosition = 20,
            modifiedAt = System.currentTimeMillis(),
            notebookPath = "/test"
        )

        assertFalse(noteFound.foundedInTitle)
        assertNotNull(noteFound.contentSearchPreview)
    }

    @Test
    fun `NoteFound should handle foundedInTitle true`() {
        val noteFound = NoteFound(
            query = "test",
            id = "note_123",
            title = "Test Title",
            foundedInTitle = true,
            contentSearchPreview = null,
            contentPosition = null,
            modifiedAt = System.currentTimeMillis(),
            notebookPath = "/test"
        )

        assertTrue(noteFound.foundedInTitle)
    }

    @Test
    fun `NoteFound should preserve query string`() {
        val noteFound = NoteFound(
            query = "поисковый запрос",
            id = "note_123",
            title = "Title",
            foundedInTitle = true,
            contentSearchPreview = null,
            contentPosition = null,
            modifiedAt = System.currentTimeMillis(),
            notebookPath = "/test"
        )

        assertEquals("поисковый запрос", noteFound.query)
    }

    @Test
    fun `NoteFound should handle empty query`() {
        val noteFound = NoteFound(
            query = "",
            id = "note_123",
            title = "Title",
            foundedInTitle = true,
            contentSearchPreview = null,
            contentPosition = null,
            modifiedAt = System.currentTimeMillis(),
            notebookPath = "/test"
        )

        assertEquals("", noteFound.query)
    }

    @Test
    fun `NoteFound should handle empty title`() {
        val noteFound = NoteFound(
            query = "test",
            id = "note_123",
            title = "",
            foundedInTitle = false,
            contentSearchPreview = "Content with test",
            contentPosition = 10,
            modifiedAt = System.currentTimeMillis(),
            notebookPath = "/test"
        )

        assertEquals("", noteFound.title)
    }

    @Test
    fun `NoteFound should handle zero modifiedAt`() {
        val noteFound = NoteFound(
            query = "test",
            id = "note_123",
            title = "Title",
            foundedInTitle = true,
            contentSearchPreview = null,
            contentPosition = null,
            modifiedAt = 0L,
            notebookPath = "/test"
        )

        assertEquals(0L, noteFound.modifiedAt)
    }

    @Test
    fun `NoteFound should handle large contentPosition`() {
        val noteFound = NoteFound(
            query = "test",
            id = "note_123",
            title = "Title",
            foundedInTitle = false,
            contentSearchPreview = "Long content with test at position",
            contentPosition = Int.MAX_VALUE,
            modifiedAt = System.currentTimeMillis(),
            notebookPath = "/test"
        )

        assertEquals(Int.MAX_VALUE, noteFound.contentPosition)
    }

    @Test
    fun `NoteFound printDebug should not throw`() {
        val noteFound = NoteFound(
            query = "test",
            id = "note_123",
            title = "Title",
            foundedInTitle = true,
            contentSearchPreview = "Preview",
            contentPosition = 10,
            modifiedAt = System.currentTimeMillis(),
            notebookPath = "/test"
        )

        // Should not throw exception
        noteFound.printDebug()
    }

    @Test
    fun `NoteFound toNote should create independent Note`() {
        val noteFound = NoteFound(
            query = "test",
            id = "note_123",
            title = "Title",
            foundedInTitle = true,
            contentSearchPreview = null,
            contentPosition = null,
            modifiedAt = 1000L,
            notebookPath = "/test"
        )

        val note1 = noteFound.toNote()
        val note2 = noteFound.toNote()

        assertNotSame(note1, note2)
        assertEquals(note1, note2)
    }

    @Test
    fun `NoteFound should handle russian title`() {
        val noteFound = NoteFound(
            query = "тест",
            id = "note_123",
            title = "Рабочие заметки",
            foundedInTitle = true,
            contentSearchPreview = null,
            contentPosition = null,
            modifiedAt = System.currentTimeMillis(),
            notebookPath = "/рабочие"
        )

        assertEquals("Рабочие заметки", noteFound.title)
        val note = noteFound.toNote()
        assertEquals("Рабочие заметки", note.title)
    }

    @Test
    fun `NoteFound should handle preview with special characters`() {
        val noteFound = NoteFound(
            query = "test",
            id = "note_123",
            title = "Title",
            foundedInTitle = false,
            contentSearchPreview = "Content with @#$% and test!",
            contentPosition = 15,
            modifiedAt = System.currentTimeMillis(),
            notebookPath = "/test"
        )

        assertEquals("Content with @#$% and test!", noteFound.contentSearchPreview)
    }

    @Test
    fun `NoteFound should handle emoji in title`() {
        val noteFound = NoteFound(
            query = "emoji",
            id = "note_123",
            title = "Test 😀 🎉",
            foundedInTitle = true,
            contentSearchPreview = null,
            contentPosition = null,
            modifiedAt = System.currentTimeMillis(),
            notebookPath = "/test"
        )

        assertEquals("Test 😀 🎉", noteFound.title)
    }
}
