package ru.whiteleaf.notes.domain.use_case.notebooks

import ru.whiteleaf.notes.domain.repository.NotebookRepository

class DeleteNotebookByPathUseCase(
    private val notebookRepository: NotebookRepository,
) {
    suspend operator fun invoke(notebookPath: String) {
        val notebook = notebookRepository.getNotebookByPath(notebookPath)
        if (notebook != null) {
            notebookRepository.deleteNotebook(notebook)
        }
    }
}