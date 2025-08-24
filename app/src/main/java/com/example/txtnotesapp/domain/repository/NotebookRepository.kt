package com.example.txtnotesapp.domain.repository

import com.example.txtnotesapp.domain.model.Notebook

interface NotebookRepository {
    suspend fun getNotebooks(): List<Notebook>
    suspend fun createNotebook(name: String): Notebook
    suspend fun deleteNotebook(notebook: Notebook)
    suspend fun renameNotebook(notebook: Notebook, newName: String)
    suspend fun getNotebookByPath(path: String): Notebook?
}