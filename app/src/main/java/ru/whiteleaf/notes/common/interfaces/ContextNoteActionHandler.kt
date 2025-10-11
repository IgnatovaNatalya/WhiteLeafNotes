package ru.whiteleaf.notes.common.interfaces

import ru.whiteleaf.notes.domain.model.Note

interface ContextNoteActionHandler {
    fun onDeleteNote(note: Note)
    fun onMoveNote(note: Note)
    fun onShareNote(note: Note)
    fun onRenameNote(note: Note)
}

