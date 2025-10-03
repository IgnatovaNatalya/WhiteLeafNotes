package com.example.whiteleafnotes.domain.use_case

import com.example.whiteleafnotes.domain.repository.NotebookRepository

class RenameNotebookByPathUseCase(private val repository: NotebookRepository) {
    suspend operator fun invoke(notebookPath: String, newName: String) {
        val notebook = repository.getNotebookByPath(notebookPath)
        if (notebook!=null) repository.renameNotebook(notebook, newName)
    }
}