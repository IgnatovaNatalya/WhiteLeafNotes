package ru.whiteleaf.notes.common.interfaces

import ru.whiteleaf.notes.domain.model.Notebook

interface ContextNotebookActionHandler {
    fun onRenameNotebook(notebook: Notebook)
    fun onPinNotebook(notebook: Notebook)
    fun onUnpinNotebook(notebook: Notebook)
    fun onEncryptNotebook(notebook: Notebook)
    fun onDecryptNotebook(notebook: Notebook)
    fun onExportNotebook(notebook: Notebook)
    fun onDeleteNotebook(notebook: Notebook)
}