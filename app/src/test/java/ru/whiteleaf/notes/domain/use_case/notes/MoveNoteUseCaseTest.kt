package ru.whiteleaf.notes.domain.use_case.notes

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argThat
import org.mockito.kotlin.isNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository
import kotlinx.coroutines.test.runTest

@RunWith(MockitoJUnitRunner::class)
class MoveNoteUseCaseTest {

    @Mock
    private lateinit var repository: NotesRepository

    @Mock
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var moveNoteUseCase: MoveNoteUseCase

    @Before
    fun setup() {
        moveNoteUseCase = MoveNoteUseCase(repository, preferencesRepository)
    }

    @Test
    fun `MoveNoteUseCase should call repository moveNote`() = runTest {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis(), "/old/path")

        moveNoteUseCase(note, "/new/path")

        verify(repository).moveNote(note, "/new/path")
    }

    @Test
    fun `MoveNoteUseCase should call preferences saveRecentNote when update fails`() = runTest {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis(), "/old/path")

        `whenever`(preferencesRepository.updateRecentNoteNotebookPath("note_123", "/old/path", "/new/path"))
            .thenReturn(false)

        moveNoteUseCase(note, "/new/path")

        verify(preferencesRepository).saveRecentNote(argThat { notebookPath == "/new/path" })
    }

    @Test
    fun `MoveNoteUseCase should call preferences updateNoteScrollPosition`() = runTest {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis(), "/old/path")

        `whenever`(preferencesRepository.updateRecentNoteNotebookPath(any(), any(), any())).thenReturn(true)

        moveNoteUseCase(note, "/new/path")

        verify(preferencesRepository).updateNoteScrollPosition(eq(note), any())
    }

    @Test
    fun `MoveNoteUseCase should handle null target notebook path`() = runTest {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis(), "/old/path")

        `whenever`(preferencesRepository.updateRecentNoteNotebookPath(any(), any(), isNull())).thenReturn(false)

        moveNoteUseCase(note, null)

        verify(repository).moveNote(note, null)
    }

    @Test
    fun `MoveNoteUseCase should not call saveRecentNote when update succeeds`() = runTest {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis(), "/old/path")

        `whenever`(preferencesRepository.updateRecentNoteNotebookPath("note_123", "/old/path", "/new/path"))
            .thenReturn(true)

        moveNoteUseCase(note, "/new/path")

        verify(preferencesRepository, never()).saveRecentNote(any())
    }
}
