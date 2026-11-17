package ru.whiteleaf.notes.domain.use_case.notes

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository
import kotlinx.coroutines.test.runTest
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking

@RunWith(MockitoJUnitRunner::class)
class InsertNoteUseCaseTest {

    @Mock
    private lateinit var repository: NotesRepository

    private lateinit var insertNoteUseCase: InsertNoteUseCase

    @Before
    fun setup() {
        insertNoteUseCase = InsertNoteUseCase(repository)
    }

    @Test
    fun `InsertNoteUseCase should create note with provided title`() = runTest {
        val title = "My Note"
        val content = "Content here"
        val notebookPath = "test notebook"

        whenever(repository.existsNote(notebookPath, title)).thenReturn(false)

        val result = insertNoteUseCase(title, content, notebookPath)

        assertEquals(title, result.title)
        assertEquals(content, result.content)
        assertEquals(notebookPath, result.notebookPath)
        assertTrue(result.modifiedAt > 0)
    }

    @Test
    fun `InsertNoteUseCase should generate id when title is null`() = runTest {
        val title: String? = null
        val content = "Content here"
        val notebookPath = "test notebook"

        wheneverBlocking { repository.existsNote(anyString(), anyString()) }.thenReturn(false)

        val result = insertNoteUseCase(title, content, notebookPath)

        assertTrue(result.id.startsWith("note_"))
        assertEquals("", result.title)
        assertEquals(content, result.content)
        assertEquals(notebookPath, result.notebookPath)
    }

    @Test
    fun `InsertNoteUseCase should sanitize title for id`() = runTest {
        val title = "note/test*file"
        val content = "Content here"
        val notebookPath = "test notebook"
        val sanitizedId = "notetestfile"

        whenever(repository.existsNote(notebookPath, sanitizedId)).thenReturn(false)

        val result = insertNoteUseCase(title, content, notebookPath)

        assertEquals(sanitizedId, result.id)
    }

    @Test
    fun `InsertNoteUseCase should add counter when id exists`() = runTest {
        val title = "My Note"
        val content = "Content here"
        val notebookPath = "test notebook"

        whenever(repository.existsNote(notebookPath, title)).thenReturn(true)
        whenever(repository.existsNote(notebookPath, "${title}_1")).thenReturn(false)

        val result = insertNoteUseCase(title, content, notebookPath)

        assertEquals("${title}_1", result.id)
    }

    @Test
    fun `InsertNoteUseCase should increment counter until unique id found`() = runTest {
        val title = "My Note"
        val content = "Content here"
        val notebookPath = "test notebook"

        whenever(repository.existsNote(notebookPath, title)).thenReturn(true)
        whenever(repository.existsNote(notebookPath, "${title}_1")).thenReturn(true)
        whenever(repository.existsNote(notebookPath, "${title}_2")).thenReturn(false)

        val result = insertNoteUseCase(title, content, notebookPath)

        assertEquals("${title}_2", result.id)
    }

    @Test
    fun `InsertNoteUseCase should set modifiedAt to current time`() = runTest {
        val title = "My Note"
        val content = "Content here"
        val notebookPath = "test notebook"

        val before = System.currentTimeMillis()

        whenever(repository.existsNote(notebookPath, title)).thenReturn(false)

        val result = insertNoteUseCase(title, content, notebookPath)
        val after = System.currentTimeMillis()

        assertTrue(result.modifiedAt >= before)
        assertTrue(result.modifiedAt <= after)
    }

    @Test
    fun `InsertNoteUseCase should call repository saveNote`() = runTest {
        val title = "My Note"
        val content = "Content here"
        val notebookPath = "test notebook"

        whenever(repository.existsNote(notebookPath, title)).thenReturn(false)

        insertNoteUseCase(title, content, notebookPath)

        val captor = argumentCaptor<Note>()
        verifyBlocking(repository) { repository.saveNote(captor.capture()) }

        val saved = captor.firstValue
        assertEquals(title, saved.id)
        assertEquals(title, saved.title)
        assertEquals(content, saved.content)
        assertEquals(notebookPath, saved.notebookPath)
        assertTrue(saved.modifiedAt > 0) // время «разумное», без привязки к конкретному значению
    }

    @Test
    fun `InsertNoteUseCase should handle empty string title `() = runTest {
        val title = ""
        val content = "Content here"
        val notebookPath = "test notebook"

        wheneverBlocking { repository.existsNote(anyString(), anyString()) }.thenReturn(false)

        val result = insertNoteUseCase(title, content, notebookPath)

        assertTrue(result.id.startsWith("note_"))
        assertEquals(title, result.title)
        assertEquals(content, result.content)
        assertEquals(notebookPath, result.notebookPath)
    }

    @Test
    fun `InsertNoteUseCase should handle whitespace only title`() = runTest {
        val title = "   "
        val content = "Content here"
        val notebookPath = "test notebook"

        wheneverBlocking { repository.existsNote(anyString(), anyString()) }.thenReturn(false)

        val result = insertNoteUseCase(title, content, notebookPath)

        // Whitespace title should be treated as blank, so generate id
        assertTrue(result.id.startsWith("note_"))
        assertEquals(title, result.title)
        assertEquals(content, result.content)
        assertEquals(notebookPath, result.notebookPath)
    }
}
