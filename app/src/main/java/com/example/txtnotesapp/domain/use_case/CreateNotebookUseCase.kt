package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.domain.model.Notebook
import com.example.txtnotesapp.domain.repository.NotebookRepository

class CreateNotebookUseCase(private val repository: NotebookRepository) {
    suspend operator fun invoke(name: String): Notebook {
        return repository.createNotebook(name)
    }
}