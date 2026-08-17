package ru.whiteleaf.notes.domain.use_case.notebooks

import ru.whiteleaf.notes.domain.repository.NotebookRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class DeleteNotebookByPathUseCase(
    private val notebookRepository: NotebookRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(notebookPath: String) {

        preferencesRepository.removeRecentNotesByNotebookPath(notebookPath)
        preferencesRepository.removeNotebookPreferences(notebookPath)

        val notebook = notebookRepository.getNotebookByPath(notebookPath)
        if (notebook != null) {
            notebookRepository.deleteNotebook(notebook)
        }
    }
}