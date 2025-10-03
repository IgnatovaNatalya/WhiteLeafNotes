package com.example.whiteleafnotes.domain.use_case

import com.example.whiteleafnotes.domain.model.Notebook
import com.example.whiteleafnotes.domain.repository.NotebookRepository

class RenameNotebookUseCase(private val repository: NotebookRepository) {
    suspend operator fun invoke(notebook: Notebook, newName: String) {
        repository.renameNotebook(notebook, newName)
    }
}