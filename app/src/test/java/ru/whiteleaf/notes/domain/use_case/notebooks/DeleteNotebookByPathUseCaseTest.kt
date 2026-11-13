package ru.whiteleaf.notes.domain.use_case.notebooks

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.repository.NotebookRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.mockingDetails

@RunWith(MockitoJUnitRunner::class)
class DeleteNotebookByPathUseCaseTest {

    @Mock
    private lateinit var notebookRepository: NotebookRepository

    @Mock
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var deleteNotebookUseCase: DeleteNotebookByPathUseCase

    @Before
    fun setup() {
        deleteNotebookUseCase = DeleteNotebookByPathUseCase(notebookRepository, preferencesRepository)
    }

    @Test
    fun `DeleteNotebookUseCase should call preferences removeRecentNotesByNotebookPath`() = runTest {
        val path = "notebook"

        deleteNotebookUseCase(path)

        verify(preferencesRepository).removeRecentNotesByNotebookPath(path)
    }

    @Test
    fun `DeleteNotebookUseCase should call preferences removeNotebookPreferences`() = runTest {
        val path = "notebook"

        deleteNotebookUseCase(path)

        verify(preferencesRepository).removeNotebookPreferences(path)
    }

    @Test
    fun `DeleteNotebookUseCase should call repository deleteNotebook when notebook exists`() = runTest {
        val path = "notebook"
        val notebook = Notebook(path, System.currentTimeMillis())

        `whenever`(notebookRepository.getNotebookByPath(path)).thenReturn(notebook)

        deleteNotebookUseCase(path)

        verify(notebookRepository).deleteNotebook(notebook)
    }

    @Test
    fun `DeleteNotebookUseCase should not delete when notebook not found`() = runTest {
        val path = "nonexistent"
        whenever(notebookRepository.getNotebookByPath(path)).thenReturn(null)

        deleteNotebookUseCase(path)

        val invocations = mockingDetails(notebookRepository).invocations
        assertTrue(
            "deleteNotebook should not be called",
            invocations.none { it.method.name == "deleteNotebook" }
        )

    }

    @Test
    fun `DeleteNotebookUseCase should call repository deleteNotebook`() = runTest {
        val path = "old notebook"

        val notebook = Notebook(path, System.currentTimeMillis())

        whenever(notebookRepository.getNotebookByPath(path)).thenReturn(notebook)

        deleteNotebookUseCase(path)

        verify(notebookRepository).deleteNotebook(notebook)
    }

}
