package ru.whiteleaf.notes.data.model

import ru.whiteleaf.notes.domain.model.Note

data class RecentNoteData(
    val id: String,
    val notebookPath: String
) {
    companion object {
        fun fromNote(note: Note): RecentNoteData {
            return RecentNoteData(
                id = note.id,
                notebookPath = note.notebookPath?:""
            )
        }
    }
}