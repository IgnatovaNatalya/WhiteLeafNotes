package com.example.whiteleafnotes.domain.use_case

import com.example.whiteleafnotes.domain.model.Notebook
import com.example.whiteleafnotes.domain.repository.NotebookRepository

class CreateNotebookUseCase(private val repository: NotebookRepository) {
    suspend operator fun invoke(name: String): Notebook {
        return repository.createNotebook(name)
    }
}