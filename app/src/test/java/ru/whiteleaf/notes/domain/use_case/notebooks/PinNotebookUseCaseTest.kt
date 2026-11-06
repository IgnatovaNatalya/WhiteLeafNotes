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
class PinNotebookUseCaseTest {

    @Mock
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var pinNotebookUseCase: PinNotebookUseCase

    @Before
    fun setup() {
        pinNotebookUseCase = PinNotebookUseCase(preferencesRepository)
    }

    @Test
    fun `PinNotebookUseCase should call preferences pinNotebook`() {
        val notebookPath = "/test/notebook"

        pinNotebookUseCase(notebookPath)

        verify(preferencesRepository).pinNotebook(notebookPath)
    }

    @Test
    fun `PinNotebookUseCase should handle empty path`() {
        val notebookPath = ""

        pinNotebookUseCase(notebookPath)

        verify(preferencesRepository).pinNotebook("")
    }

    @Test
    fun `PinNotebookUseCase should handle russian path`() {
        val notebookPath = "/рабочие/заметки"

        pinNotebookUseCase(notebookPath)

        verify(preferencesRepository).pinNotebook(notebookPath)
    }

    @Test
    fun `PinNotebookUseCase should handle nested path`() {
        val notebookPath = "/folder1/folder2/notebook"

        pinNotebookUseCase(notebookPath)

        verify(preferencesRepository).pinNotebook(notebookPath)
    }
}
