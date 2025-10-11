package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.repository.NotebookRepository

class RenameNotebookByPathUseCase(private val repository: NotebookRepository) {
    suspend operator fun invoke(notebookPath: String, newName: String) {
        val notebook = repository.getNotebookByPath(notebookPath)
        if (notebook!=null) repository.renameNotebook(notebook, newName)
    }
}