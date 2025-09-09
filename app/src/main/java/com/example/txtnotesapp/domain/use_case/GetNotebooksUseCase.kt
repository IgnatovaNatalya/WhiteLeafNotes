package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.domain.model.Notebook
import com.example.txtnotesapp.domain.repository.NotebookRepository

class GetNotebooksUseCase(private val repository: NotebookRepository) {
    suspend operator fun invoke(): List<Notebook> {
        return repository.getNotebooks()
    }
}