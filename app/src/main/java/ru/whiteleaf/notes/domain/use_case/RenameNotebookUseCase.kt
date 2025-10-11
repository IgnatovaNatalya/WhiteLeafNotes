package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.repository.NotebookRepository

class RenameNotebookUseCase(private val repository: NotebookRepository) {
    suspend operator fun invoke(notebook: Notebook, newName: String) {
        repository.renameNotebook(notebook, newName)
    }
}