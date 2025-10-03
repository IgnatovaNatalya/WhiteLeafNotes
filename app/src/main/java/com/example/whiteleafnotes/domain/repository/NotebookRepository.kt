package com.example.whiteleafnotes.domain.repository

import com.example.whiteleafnotes.domain.model.Notebook

interface NotebookRepository {
    suspend fun getNotebooks(): List<Notebook>
    suspend fun createNotebook(name: String): Notebook
    suspend fun deleteNotebook(notebook: Notebook)
    suspend fun renameNotebook(notebook: Notebook, newName: String)
    suspend fun getNotebookByPath(path: String): Notebook?
    suspend fun notebookExist(path: String): Boolean
}