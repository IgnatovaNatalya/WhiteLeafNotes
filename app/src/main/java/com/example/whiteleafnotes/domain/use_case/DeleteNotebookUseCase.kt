package com.example.whiteleafnotes.domain.use_case

import com.example.whiteleafnotes.domain.model.Notebook
import com.example.whiteleafnotes.domain.repository.NotebookRepository

class DeleteNotebookUseCase(private val repository: NotebookRepository) {
    suspend operator fun invoke(notebook: Notebook) {
        repository.deleteNotebook(notebook)
    }
}