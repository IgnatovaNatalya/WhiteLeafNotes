package ru.whiteleaf.notes.domain.use_case.notes

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argThat
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository
import kotlinx.coroutines.test.runTest

@RunWith(MockitoJUnitRunner::class)
class UpdateFullNoteUseCaseTest {

    @Mock
    private lateinit var repository: NotesRepository

    @Mock
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var updateFullNoteUseCase: UpdateFullNoteUseCase

    @Before
    fun setup() {
        updateFullNoteUseCase = UpdateFullNoteUseCase(repository, preferencesRepository)
    }

    @Test
    fun `UpdateFullNoteUseCase should call renameNote when title changes`() = runTest {
        val oldNote = Note("note_123", "Old Title", "Content", System.currentTimeMillis())
        val newTitle = "New Title"
        val newContent = "New Content"

        whenever(repository.renameNote(oldNote, newTitle)).thenReturn(oldNote.copy(title = newTitle))

        updateFullNoteUseCase(oldNote, newTitle, newContent)

        verify(repository).renameNote(oldNote, newTitle)
    }

    @Test
    fun `UpdateFullNoteUseCase should not call renameNote when title unchanged`() = runTest {
        val oldNote = Note("note_123", "Same Title", "Old Content", System.currentTimeMillis())
        val title = "Same Title"
        val content = "New Content"

        updateFullNoteUseCase(oldNote, title, content)

        verify(repository, never()).renameNote(oldNote, title)
    }

    @Test
    fun `UpdateFullNoteUseCase should save updated note`() = runTest {
        val oldNote = Note("note_123", "Old Title", "Old Content", System.currentTimeMillis())
        val newTitle = "New Title"
        val newContent = "New Content"
        val newNote = oldNote.copy(title = newTitle, content = newContent)

        whenever(repository.renameNote(oldNote, newTitle)).thenReturn(newNote)

        updateFullNoteUseCase(oldNote, newTitle, newContent)

        verify(repository).saveNote(newNote)
    }

    @Test
    fun `UpdateFullNoteUseCase should call preferences updateRecentEntry`() = runTest {
        val oldNote = Note("note_123", "Old Title", "Old Content", System.currentTimeMillis())
        val newTitle = "New Title"
        val newContent = "New Content"
        val newNote = oldNote.copy(title = newTitle, content = newContent)

        whenever(repository.renameNote(oldNote, newTitle)).thenReturn(newNote)

        updateFullNoteUseCase(oldNote, newTitle, newContent)

        verify(preferencesRepository).updateRecentEntry(oldNote, newNote)
    }

    @Test
    fun `UpdateFullNoteUseCase should return updated note with new title`() = runTest {
        val oldNote = Note("note_123", "Old Title", "Old Content", System.currentTimeMillis())
        val newTitle = "New Title"
        val newContent = "New Content"
        val updatedNote = oldNote.copy(title = newTitle, content = newContent)

        whenever(repository.renameNote(oldNote, newTitle)).thenReturn(updatedNote)

        val result = updateFullNoteUseCase(oldNote, newTitle, newContent)

        assertEquals("New Title", result.title)
    }

    @Test
    fun `UpdateFullNoteUseCase should return updated note with new content`() = runTest {
        val oldNote = Note("note_123", "Old Title", "Old Content", System.currentTimeMillis())
        val newTitle = "Old Title"
        val newContent = "New Content"

        updateFullNoteUseCase(oldNote, newTitle, newContent)

        verify(repository).saveNote(argThat { content == "New Content" })
    }

    @Test
    fun `UpdateFullNoteUseCase should preserve id when updating`() = runTest {
        val oldNote = Note("note_123", "Old Title", "Old Content", System.currentTimeMillis())
        val newTitle = "New Title"
        val newContent = "New Content"

        whenever(repository.renameNote(oldNote, newTitle)).thenReturn(oldNote.copy(title = newTitle, content = newContent))

        val result = updateFullNoteUseCase(oldNote, newTitle, newContent)

        assertEquals("note_123", result.id)
    }

    @Test
    fun `UpdateFullNoteUseCase should preserve notebook path when updating`() = runTest {
        val oldNote = Note("note_123", "Old Title", "Old Content", System.currentTimeMillis(), "test notebook")
        val newTitle = "New Title"
        val newContent = "New Content"

        whenever(repository.renameNote(oldNote, newTitle)).thenReturn(oldNote.copy(title = newTitle, content = newContent))

        val result = updateFullNoteUseCase(oldNote, newTitle, newContent)

        assertEquals("test notebook", result.notebookPath)
    }
}
