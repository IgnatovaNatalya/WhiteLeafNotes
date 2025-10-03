package com.example.whiteleafnotes.common.interfaces

import com.example.whiteleafnotes.domain.model.Notebook

interface ContextNotebookActionHandler {
    fun onDeleteNotebook(notebook: Notebook)
    fun onRenameNotebook(notebook: Notebook)
    fun onShareNotebook(notebook: Notebook)
}