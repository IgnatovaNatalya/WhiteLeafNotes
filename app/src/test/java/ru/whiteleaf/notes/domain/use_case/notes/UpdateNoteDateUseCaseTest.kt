package ru.whiteleaf.notes.domain.use_case.notes


import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository
import kotlinx.coroutines.test.runTest

@RunWith(MockitoJUnitRunner::class)
class UpdateNoteDateUseCaseTest {

    @Mock
    private lateinit var repository: NotesRepository

    private lateinit var updateNoteDateUseCase: UpdateNoteDateUseCase

    @Before
    fun setup() {
        updateNoteDateUseCase = UpdateNoteDateUseCase(repository)
    }

    @Test
    fun `UpdateNoteDateUseCase should call repository updateNoteDate`() = runTest {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis())
        val newDate = 1234567890L

        updateNoteDateUseCase(note, newDate)

        verify(repository).updateNoteDate(note, newDate)
    }

    @Test
    fun `UpdateNoteDateUseCase should update to correct timestamp`() = runTest {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis())
        val newDate = 9876543210L

        whenever(repository.updateNoteDate(note, newDate)).then { null }

        updateNoteDateUseCase(note, newDate)

        verify(repository).updateNoteDate(note, 9876543210L)
    }

    @Test
    fun `UpdateNoteDateUseCase should handle zero timestamp`() = runTest {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis())

        updateNoteDateUseCase(note, 0L)

        verify(repository).updateNoteDate(note, 0L)
    }

    @Test
    fun `UpdateNoteDateUseCase should handle large timestamp`() = runTest {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis())
        val largeTimestamp = Long.MAX_VALUE

        updateNoteDateUseCase(note, largeTimestamp)

        verify(repository).updateNoteDate(note, Long.MAX_VALUE)
    }
}
