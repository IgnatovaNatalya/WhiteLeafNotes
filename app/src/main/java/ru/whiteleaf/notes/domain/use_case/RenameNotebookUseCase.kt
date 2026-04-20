package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.repository.NotebookRepository

class RenameNotebookUseCase(private val repository: NotebookRepository) {
    suspend operator fun invoke(notebookPath: String, newName: String) {
        repository.renameNotebook(notebookPath, newName)
    }
}