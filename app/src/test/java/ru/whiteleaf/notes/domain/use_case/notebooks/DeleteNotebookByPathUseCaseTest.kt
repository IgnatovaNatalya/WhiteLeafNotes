package ru.whiteleaf.notes.domain.use_case.notebooks

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.ArgumentMatchers.any
import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.repository.NotebookRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository
import kotlinx.coroutines.test.runTest

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
        val path = "/test/notebook"

        deleteNotebookUseCase(path)

        verify(preferencesRepository).removeRecentNotesByNotebookPath(path)
    }

    @Test
    fun `DeleteNotebookUseCase should call preferences removeNotebookPreferences`() = runTest {
        val path = "/test/notebook"

        deleteNotebookUseCase(path)

        verify(preferencesRepository).removeNotebookPreferences(path)
    }

    @Test
    fun `DeleteNotebookUseCase should call repository deleteNotebook when notebook exists`() = runTest {
        val path = "/test/notebook"
        val notebook = Notebook(path, System.currentTimeMillis())

        `whenever`(notebookRepository.getNotebookByPath(path)).thenReturn(notebook)

        deleteNotebookUseCase(path)

        verify(notebookRepository).deleteNotebook(notebook)
    }

    @Test
    fun `DeleteNotebookUseCase should not delete when notebook not found`() = runTest {
        val path = "/nonexistent"

        `whenever`(notebookRepository.getNotebookByPath(path)).thenReturn(null)

        deleteNotebookUseCase(path)

        verify(notebookRepository, never()).deleteNotebook(any())
    }

    @Test
    fun `DeleteNotebookUseCase should call repository renameNotebook`() = runTest {
        val oldPath = "/old/notebook"
        val newPath = "/new/notebook"
        val notebook = Notebook(oldPath, System.currentTimeMillis())

        `whenever`(notebookRepository.getNotebookByPath(oldPath)).thenReturn(notebook)

        deleteNotebookUseCase(oldPath)

        verify(notebookRepository).deleteNotebook(notebook)
    }

    @Test
    fun `DeleteNotebookUseCase should handle russian notebook path`() = runTest {
        val path = "/рабочие/заметки"

        `whenever`(notebookRepository.getNotebookByPath(path)).thenReturn(null)

        deleteNotebookUseCase(path)

        verify(preferencesRepository).removeRecentNotesByNotebookPath(path)
    }
}
