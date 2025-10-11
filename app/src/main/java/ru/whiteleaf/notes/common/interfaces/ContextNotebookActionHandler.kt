package ru.whiteleaf.notes.common.interfaces

import ru.whiteleaf.notes.domain.model.Notebook

interface ContextNotebookActionHandler {
    fun onDeleteNotebook(notebook: Notebook)
    fun onRenameNotebook(notebook: Notebook)
    fun onShareNotebook(notebook: Notebook)
}