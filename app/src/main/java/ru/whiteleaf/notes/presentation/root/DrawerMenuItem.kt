package ru.whiteleaf.notes.presentation.root

import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.model.Notebook

sealed class DrawerMenuItem {
    data class NotebookItem(val notebook: Notebook) : DrawerMenuItem()
    data class NoteItem(val note: Note) : DrawerMenuItem()
    object CreateNotebook : DrawerMenuItem()
    object CreateNote : DrawerMenuItem()
    object Divider : DrawerMenuItem()
}