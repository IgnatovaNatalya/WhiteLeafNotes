package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.domain.model.Notebook
import com.example.txtnotesapp.domain.repository.NotebookRepository

class DeleteNotebook(private val repository: NotebookRepository) {
    suspend operator fun invoke(notebook: Notebook) {
        repository.deleteNotebook(notebook)
    }
}