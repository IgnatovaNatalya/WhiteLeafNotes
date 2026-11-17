package ru.whiteleaf.notes.domain.use_case.notes

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
import org.mockito.ArgumentMatchers.anyString

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
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis(), "old path")

        moveNoteUseCase(note, "new path")

        verify(repository).moveNote(note, "new path")
    }

    @Test
    fun `MoveNoteUseCase should call preferences saveRecentNote when update fails`() = runTest {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis(), "old path")

        whenever(
            preferencesRepository.updateRecentNoteNotebookPath(
                "note_123",
                "old path",
                "new path"
            )
        )
            .thenReturn(false)

        moveNoteUseCase(note, "new path")

        verify(preferencesRepository).saveRecentNote(argThat { notebookPath == "new path" })
    }

    @Test
    fun `MoveNoteUseCase should call preferences updateNoteScrollPosition`() = runTest {
        val oldNote = Note("note_123", "Title", "Content", System.currentTimeMillis(), "old path")
        val newNote = oldNote.copy(notebookPath = "new path")

        whenever(
            preferencesRepository.updateRecentNoteNotebookPath(
                anyString(),
                anyString(),
                anyString()
            )
        ).thenReturn(true)

        moveNoteUseCase(oldNote, "new path")

        verify(preferencesRepository).updateNoteScrollPosition(oldNote, newNote)
    }

    @Test
    fun `MoveNoteUseCase should handle null target notebook path`() = runTest {
        val oldNote = Note("note_123", "Title", "Content", System.currentTimeMillis(), "old path")

        whenever(
            preferencesRepository.updateRecentNoteNotebookPath(
                oldNote.id,
                oldNote.notebookPath,
                null
            )
        ).thenReturn(false)

        moveNoteUseCase(oldNote, null)

        verify(repository).moveNote(oldNote, null)
    }

    @Test
    fun `MoveNoteUseCase should not call saveRecentNote when update succeeds`() = runTest {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis(), "old path")
        val newNote = note.copy(notebookPath = "new path")
        whenever(
            preferencesRepository.updateRecentNoteNotebookPath(
                "note_123",
                "old path",
                "new path"
            )
        )
            .thenReturn(true)

        moveNoteUseCase(note, "new path")

        verify(preferencesRepository, never()).saveRecentNote(newNote)
    }
}
