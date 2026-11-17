package ru.whiteleaf.notes.domain.use_case.recent

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.whiteleaf.notes.data.model.RecentNote
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

@RunWith(MockitoJUnitRunner::class)
class GetRecentNotesUseCaseTest {

    @Mock
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var getRecentNotesUseCase: GetRecentNotesUseCase

    @Before
    fun setup() {
        getRecentNotesUseCase = GetRecentNotesUseCase(preferencesRepository)
    }

    @Test
    fun `GetRecentNotesUseCase should return list of recent notes`() {
        val recentNotes = listOf(
            RecentNote("note_1", "Title 1", "/notebook1"),
            RecentNote("note_2", "Title 2", "/notebook2")
        )

        `whenever`(preferencesRepository.getRecentNotes()).thenReturn(recentNotes)

        val result = getRecentNotesUseCase()

        assertEquals(2, result.size)
        assertEquals(recentNotes, result)
    }

    @Test
    fun `GetRecentNotesUseCase should return empty list when no recent notes`() {
        `whenever`(preferencesRepository.getRecentNotes()).thenReturn(emptyList())

        val result = getRecentNotesUseCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `GetRecentNotesUseCase should call preferences getRecentNotes`() {
        `whenever`(preferencesRepository.getRecentNotes()).thenReturn(emptyList())

        getRecentNotesUseCase()

        verify(preferencesRepository).getRecentNotes()
    }

    @Test
    fun `GetRecentNotesUseCase should preserve RecentNote fields`() {
        val recentNote = RecentNote("note_123", "Test Title", "/test/path", 1234567890L)
        `whenever`(preferencesRepository.getRecentNotes()).thenReturn(listOf(recentNote))

        val result = getRecentNotesUseCase()

        assertEquals(1, result.size)
        assertEquals("note_123", result[0].id)
        assertEquals("Test Title", result[0].recentTitle)
        assertEquals("/test/path", result[0].notebookPath)
        assertEquals(1234567890L, result[0].recentDate)
    }

    @Test
    fun `GetRecentNotesUseCase should handle null notebook path in recent notes`() {
        val recentNote = RecentNote("note_123", "Title", null, System.currentTimeMillis())
        `whenever`(preferencesRepository.getRecentNotes()).thenReturn(listOf(recentNote))

        val result = getRecentNotesUseCase()

        assertNull(result[0].notebookPath)
    }
}
