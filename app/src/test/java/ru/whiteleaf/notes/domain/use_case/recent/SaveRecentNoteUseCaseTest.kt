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
class SaveRecentNoteUseCaseTest {

    @Mock
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var saveRecentNoteUseCase: SaveRecentNoteUseCase

    @Before
    fun setup() {
        saveRecentNoteUseCase = SaveRecentNoteUseCase(preferencesRepository)
    }

    @Test
    fun `SaveRecentNoteUseCase should call preferences saveRecentNote`() {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis())

        saveRecentNoteUseCase(note)

        verify(preferencesRepository).saveRecentNote(note)
    }

    @Test
    fun `SaveRecentNoteUseCase should handle note with empty title`() {
        val note = Note("note_123", "", "Content", System.currentTimeMillis())

        saveRecentNoteUseCase(note)

        verify(preferencesRepository).saveRecentNote(note)
    }

    @Test
    fun `SaveRecentNoteUseCase should handle note with null notebook path`() {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis(), null)

        saveRecentNoteUseCase(note)

        verify(preferencesRepository).saveRecentNote(note)
    }

    @Test
    fun `SaveRecentNoteUseCase should handle note with notebook path`() {
        val note = Note("note_123", "Title", "Content", System.currentTimeMillis(), "/test/notebook")

        saveRecentNoteUseCase(note)

        verify(preferencesRepository).saveRecentNote(note)
    }

    @Test
    fun `SaveRecentNoteUseCase should handle russian title`() {
        val note = Note("note_123", "Рабочие заметки", "Content", System.currentTimeMillis())

        saveRecentNoteUseCase(note)

        verify(preferencesRepository).saveRecentNote(note)
    }
}
