package com.example.whiteleafnotes.common.interfaces

import com.example.whiteleafnotes.domain.model.Note

interface ContextNoteActionHandler {
    fun onDeleteNote(note: Note)
    fun onMoveNote(note: Note)
    fun onShareNote(note: Note)
    fun onRenameNote(note: Note)
}

