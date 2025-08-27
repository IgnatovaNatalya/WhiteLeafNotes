package com.example.txtnotesapp.presentation.root

import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.model.Notebook

sealed class DrawerMenuItem {
    //data class Header(val title: String) : DrawerMenuItem()
    data class NotebookItem(val notebook: Notebook) : DrawerMenuItem()
    data class NoteItem(val note: Note) : DrawerMenuItem()
    object CreateNotebook : DrawerMenuItem()
    object CreateNote : DrawerMenuItem()
    object Divider : DrawerMenuItem()
    //object EmptyNotebooks : DrawerMenuItem()
    //object EmptyNotes : DrawerMenuItem()
}