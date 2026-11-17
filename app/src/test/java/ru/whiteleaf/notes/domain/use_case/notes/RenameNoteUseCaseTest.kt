package ru.whiteleaf.notes.domain.use_case.notes

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository
import kotlinx.coroutines.test.runTest

@RunWith(MockitoJUnitRunner::class)
class RenameNoteUseCaseTest {

    @Mock
    private lateinit var repository: NotesRepository

    @Mock
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var renameNoteUseCase: RenameNoteUseCase

    @Before
    fun setup() {
        renameNoteUseCase = RenameNoteUseCase(repository, preferencesRepository)
    }

    @Test
    fun `RenameNoteUseCase should call repository renameNote`() = runTest {
        val oldNote = Note("note_123", "Old Title", "Content", System.currentTimeMillis())
        val renamedNote = Note("note_123", "New Title", "Content", System.currentTimeMillis())

        `whenever`(repository.renameNote(oldNote, "New Title")).thenReturn(renamedNote)

        val result = renameNoteUseCase(oldNote, "New Title")

        verify(repository).renameNote(oldNote, "New Title")
    }

    @Test
    fun `RenameNoteUseCase should return renamed note`() = runTest {
        val oldNote = Note("note_123", "Old Title", "Content", System.currentTimeMillis())
        val renamedNote = Note("note_123", "New Title", "Content", System.currentTimeMillis())

        `whenever`(repository.renameNote(oldNote, "New Title")).thenReturn(renamedNote)

        val result = renameNoteUseCase(oldNote, "New Title")

        assertEquals("New Title", result.title)
        assertEquals(oldNote.id, result.id)
    }

    @Test
    fun `RenameNoteUseCase should call preferences updateRecentEntry`() = runTest {
        val oldNote = Note("note_123", "Old Title", "Content", System.currentTimeMillis())
        val renamedNote = Note("note_123", "New Title", "Content", System.currentTimeMillis())

        `whenever`(repository.renameNote(oldNote, "New Title")).thenReturn(renamedNote)

        renameNoteUseCase(oldNote, "New Title")

        verify(preferencesRepository).updateRecentEntry(oldNote, renamedNote)
    }

    @Test
    fun `RenameNoteUseCase should call preferences updateNoteScrollPosition`() = runTest {
        val oldNote = Note("note_123", "Old Title", "Content", System.currentTimeMillis())
        val renamedNote = Note("note_123", "New Title", "Content", System.currentTimeMillis())

        `whenever`(repository.renameNote(oldNote, "New Title")).thenReturn(renamedNote)

        renameNoteUseCase(oldNote, "New Title")

        verify(preferencesRepository).updateNoteScrollPosition(oldNote, renamedNote)
    }

    @Test
    fun `RenameNoteUseCase should preserve content when renaming`() = runTest {
        val oldNote = Note("note_123", "Old Title", "Important content here", System.currentTimeMillis())
        val renamedNote = Note("note_123", "New Title", "Important content here", System.currentTimeMillis())

        `whenever`(repository.renameNote(oldNote, "New Title")).thenReturn(renamedNote)

        val result = renameNoteUseCase(oldNote, "New Title")

        assertEquals("Important content here", result.content)
    }

    @Test
    fun `RenameNoteUseCase should preserve notebook path when renaming`() = runTest {
        val oldNote = Note("note_123", "Old Title", "Content", System.currentTimeMillis(), "/test/notebook")
        val renamedNote = Note("note_123", "New Title", "Content", System.currentTimeMillis(), "/test/notebook")

        `whenever`(repository.renameNote(oldNote, "New Title")).thenReturn(renamedNote)

        val result = renameNoteUseCase(oldNote, "New Title")

        assertEquals("/test/notebook", result.notebookPath)
    }
}
