package com.example.txtnotesapp.common.interfaces

import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.model.Notebook

interface ContextActionHandler {
    fun onDeleteNote(note: Note)
    fun onMoveNote(note: Note)
    fun onShareNote(note: Note)
    fun onRenameNote(note: Note)

    fun onDeleteNotebook(notebook: Notebook)
    fun onRenameNotebook(notebook: Notebook)
}