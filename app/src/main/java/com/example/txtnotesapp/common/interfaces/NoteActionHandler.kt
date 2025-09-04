package com.example.txtnotesapp.common.interfaces

import com.example.txtnotesapp.domain.model.Note

interface NoteActionHandler {
    fun onDeleteNote(note: Note)
    fun onMoveNote(note: Note)
    fun onShareNote(note: Note)
    fun onRenameNote(note: Note)
}