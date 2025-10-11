package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.repository.NotebookRepository

class DeleteNotebookUseCase(private val repository: NotebookRepository) {
    suspend operator fun invoke(notebook: Notebook) {
        repository.deleteNotebook(notebook)
    }
}