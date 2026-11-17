package ru.whiteleaf.notes.domain.use_case.recent

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

@RunWith(MockitoJUnitRunner::class)
class RemoveRecentNoteUseCaseTest {

    @Mock
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var removeRecentNoteUseCase: RemoveRecentNoteUseCase

    @Before
    fun setup() {
        removeRecentNoteUseCase = RemoveRecentNoteUseCase(preferencesRepository)
    }

    @Test
    fun `RemoveRecentNoteUseCase should call preferences removeRecentNote`() {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis())

        removeRecentNoteUseCase(note)

        verify(preferencesRepository).removeRecentNote("note_123", null)
    }

    @Test
    fun `RemoveRecentNoteUseCase should pass note id`() {
        val note = Note("note_456", "Title", "Content", System.currentTimeMillis())

        removeRecentNoteUseCase(note)

        verify(preferencesRepository).removeRecentNote("note_456", null)
    }

    @Test
    fun `RemoveRecentNoteUseCase should pass notebook path when present`() {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis(), "/test/notebook")

        removeRecentNoteUseCase(note)

        verify(preferencesRepository).removeRecentNote("note_123", "/test/notebook")
    }

    @Test
    fun `RemoveRecentNoteUseCase should handle null notebook path`() {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis(), null)

        removeRecentNoteUseCase(note)

        verify(preferencesRepository).removeRecentNote("note_123", null)
    }

    @Test
    fun `RemoveRecentNoteUseCase should handle empty title`() {
        val note = Note("note_123", "", "Content", System.currentTimeMillis())

        removeRecentNoteUseCase(note)

        verify(preferencesRepository).removeRecentNote("note_123", null)
    }
}
