package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.domain.model.Notebook
import com.example.txtnotesapp.domain.repository.NotebookRepository

class RenameNotebookUseCase(private val repository: NotebookRepository) {
    suspend operator fun invoke(notebook: Notebook, newName: String) {
        repository.renameNotebook(notebook, newName)
    }
}