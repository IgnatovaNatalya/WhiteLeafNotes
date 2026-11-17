package ru.whiteleaf.notes.domain.use_case.notebooks

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import ru.whiteleaf.notes.domain.repository.PreferencesRepository
import kotlinx.coroutines.test.runTest

@RunWith(MockitoJUnitRunner::class)
class UnpinNotebookUseCaseTest {

    @Mock
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var unpinNotebookUseCase: UnpinNotebookUseCase

    @Before
    fun setup() {
        unpinNotebookUseCase = UnpinNotebookUseCase(preferencesRepository)
    }

    @Test
    fun `UnpinNotebookUseCase should call preferences unpinNotebook`() {
        val notebookPath = "test notebook"

        unpinNotebookUseCase(notebookPath)

        verify(preferencesRepository).unpinNotebook(notebookPath)
    }

    @Test
    fun `UnpinNotebookUseCase should handle empty path`() {
        val notebookPath = ""

        unpinNotebookUseCase(notebookPath)

        verify(preferencesRepository).unpinNotebook("")
    }

    @Test
    fun `UnpinNotebookUseCase should handle russian path`() {
        val notebookPath = "рабочие заметки"

        unpinNotebookUseCase(notebookPath)

        verify(preferencesRepository).unpinNotebook(notebookPath)
    }

}
