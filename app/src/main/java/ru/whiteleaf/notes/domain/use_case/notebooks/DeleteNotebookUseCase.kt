package ru.whiteleaf.notes.domain.use_case.notebooks

import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.repository.NotebookRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class DeleteNotebookUseCase(
    private val repository: NotebookRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(notebook: Notebook) {
        preferencesRepository.removeRecentNotesByNotebookPath(notebook.path)
        repository.deleteNotebook(notebook)
    }
}