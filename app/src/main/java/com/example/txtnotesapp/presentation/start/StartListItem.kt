package com.example.txtnotesapp.presentation.start

import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.model.Notebook

sealed class StartListItem {
    data class Header(val title: String) : StartListItem()
    data class NotebookItem(val notebook: Notebook) : StartListItem()
    data class NoteItem(val note: Note) : StartListItem()
    object AddNotebookButton : StartListItem()
    object AddNoteButton : StartListItem()
    object Divider : StartListItem()
    object EmptyNotebooks : StartListItem()
    object EmptyNotes : StartListItem()

    // Для DiffUtil
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return when (this) {
            is Header -> this == other
            is NotebookItem -> this == other
            is NoteItem -> this == other
            AddNotebookButton -> other is AddNotebookButton
            AddNoteButton -> other is AddNoteButton
            Divider -> other is Divider
            EmptyNotebooks -> other is EmptyNotebooks
            EmptyNotes -> other is EmptyNotes
        }
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}