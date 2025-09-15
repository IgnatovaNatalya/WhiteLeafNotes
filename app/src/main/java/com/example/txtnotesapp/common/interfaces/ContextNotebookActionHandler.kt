package com.example.txtnotesapp.common.interfaces

import com.example.txtnotesapp.domain.model.Notebook

interface ContextNotebookActionHandler {
    fun onDeleteNotebook(notebook: Notebook)
    fun onRenameNotebook(notebook: Notebook)
    fun onShareNotebook(notebook: Notebook)
}