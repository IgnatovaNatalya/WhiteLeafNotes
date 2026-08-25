package ru.whiteleaf.notes.presentation.notebooks

import ru.whiteleaf.notes.domain.model.Notebook

sealed class NotebooksScreenState {
    object Loading : NotebooksScreenState()
    data class Success(val notebooks: List<Notebook>) : NotebooksScreenState()
    data class Error(val message: String) : NotebooksScreenState()
}